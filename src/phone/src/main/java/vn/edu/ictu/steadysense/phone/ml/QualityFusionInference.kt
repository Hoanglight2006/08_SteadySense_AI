package vn.edu.ictu.steadysense.phone.ml

import android.content.Context
import android.util.Log
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import vn.edu.ictu.steadysense.core.ImuWindow
import java.io.File
import java.io.FileOutputStream

/**
 * Chạy model quality_fusion.pt on-device bằng PyTorch Mobile Lite.
 *
 * Model nhận:
 *   embeddings   [1, 2, 12]  — 2 modality (accel, gyro), 12 đặc trưng mỗi modality
 *   modality_mask [1, 2]     — mask (1.0 = hợp lệ, 0.0 = bị thiếu/mất)
 *
 * Model trả:
 *   logits  [1, 3] — CYCLIC_MOTION / REST / DISTRACTOR
 *   quality [1, 2] — chất lượng ước lượng mỗi modality (0..1)
 *   weights [1, 2] — trọng số attention
 *
 * Đây là kiểm chứng kỹ thuật on-device; không phải chẩn đoán lâm sàng.
 */
class QualityFusionInference(context: Context) {

    private val module: Module = loadModule(context)

    /** Kết quả inference một cửa sổ IMU. */
    data class Result(
        /** 0=CYCLIC_MOTION, 1=REST, 2=DISTRACTOR */
        val predictedClass: Int,
        val classProbabilities: FloatArray,   // [3]
        val predictedQuality: FloatArray,     // [2] accel, gyro
        val attentionWeights: FloatArray,     // [2]
        /** true khi quality gate chấp nhận (cả hai modality ≥ ngưỡng) */
        val qualityGatePass: Boolean,
    ) {
        val label: String get() = CLASS_NAMES[predictedClass]
        val isCyclicMotion: Boolean get() = predictedClass == 0

        companion object {
            val CLASS_NAMES = arrayOf("CYCLIC_MOTION", "REST", "DISTRACTOR")
        }
    }

    /**
     * Chạy inference trên một [ImuWindow] (40 frame × 6 kênh).
     * Hàm này phải gọi trên background thread (IO/Default).
     *
     * @param window cửa sổ IMU từ PhoneDatabase / transport
     * @param qualityThreshold ngưỡng quality tối thiểu cho mỗi modality (mặc định 0.5)
     * @return null nếu window không đủ frame
     */
    fun infer(window: ImuWindow, qualityThreshold: Float = QUALITY_THRESHOLD): Result? {
        val frames = window.frames
        if (frames.size < WINDOW_SIZE) {
            Log.w(TAG, "Window có ${frames.size} frame < $WINDOW_SIZE, bỏ qua")
            return null
        }

        // Tính 12 đặc trưng cho mỗi modality (accel=0, gyro=1)
        val accelEmb = extractEmbedding(frames.map { floatArrayOf(it.accelX, it.accelY, it.accelZ) })
        val gyroEmb  = extractEmbedding(frames.map { floatArrayOf(it.gyroX,  it.gyroY,  it.gyroZ)  })

        // embeddings: [1, 2, 12]
        val embData = FloatArray(1 * MODALITIES * EMB_DIM)
        accelEmb.copyInto(embData, destinationOffset = 0)
        gyroEmb.copyInto(embData,  destinationOffset = EMB_DIM)
        val embTensor  = Tensor.fromBlob(embData,  longArrayOf(1, MODALITIES.toLong(), EMB_DIM.toLong()))

        // modality_mask: [1, 2] — tất cả hợp lệ
        val maskData   = FloatArray(1 * MODALITIES) { 1.0f }
        val maskTensor = Tensor.fromBlob(maskData, longArrayOf(1, MODALITIES.toLong()))

        val output = module.forward(IValue.from(embTensor), IValue.from(maskTensor)).toTuple()

        val logits  = output[0].toTensor().dataAsFloatArray  // [3]
        val quality = output[1].toTensor().dataAsFloatArray  // [2]
        val weights = output[2].toTensor().dataAsFloatArray  // [2]

        val probs = softmax(logits)
        val predicted = probs.indices.maxByOrNull { probs[it] } ?: 0
        val gatePass = quality.all { it >= qualityThreshold }

        return Result(
            predictedClass      = predicted,
            classProbabilities  = probs,
            predictedQuality    = quality,
            attentionWeights    = weights,
            qualityGatePass     = gatePass,
        )
    }

    fun close() = module.destroy()

    // ─── Đặc trưng thủ công (khớp 100% với embeddings.py) ───────────────
    // 12 đặc trưng: mean (3), std (3), rms, energy, peak_to_peak, dominant_freq, zero_crossing_rate, coverage
    private fun extractEmbedding(frames: List<FloatArray>): FloatArray {
        val n = frames.size
        val xs = frames.map { it[0] }
        val ys = frames.map { it[1] }
        val zs = frames.map { it[2] }

        val meanX = xs.average().toFloat(); val meanY = ys.average().toFloat(); val meanZ = zs.average().toFloat()
        val stdX  = std(xs);               val stdY  = std(ys);               val stdZ  = std(zs)

        val mag = frames.map { Math.sqrt((it[0]*it[0] + it[1]*it[1] + it[2]*it[2]).toDouble()).toFloat() }
        val energy = mag.sumOf { (it * it).toDouble() }.toFloat() / n
        val rms = Math.sqrt(energy.toDouble()).toFloat()
        val p2p = if (n > 0) (mag.maxOrNull() ?: 0f) - (mag.minOrNull() ?: 0f) else 0f
        
        val meanMag = mag.average().toFloat()
        val centeredMag = mag.map { it - meanMag }
        val domFreq = dominantFrequency(centeredMag, 20f)
        
        val centeredX = xs.map { it - meanX }
        var zc = 0
        for (i in 0 until n - 1) {
            val sign1 = Math.signum(centeredX[i])
            val sign2 = Math.signum(centeredX[i+1])
            if (sign1 != sign2) zc++
        }
        val zcr = zc.toFloat() / Math.max(1, n - 1)
        val coverage = n.toFloat() / WINDOW_SIZE

        return floatArrayOf(meanX, meanY, meanZ, stdX, stdY, stdZ, rms, energy, p2p, domFreq, zcr, coverage)
    }

    private fun std(vals: List<Float>): Float {
        val m = vals.average(); return Math.sqrt(vals.sumOf { (it - m) * (it - m) } / vals.size).toFloat()
    }

    /** Trả về dominant frequency bằng DFT thô (Hz) bỏ qua DC component. */
    private fun dominantFrequency(signal: List<Float>, sampleRateHz: Float): Float {
        val n = signal.size
        if (n < 4 || sampleRateHz <= 0f) return 0f
        
        var bestK = 1
        var bestMag = 0f
        // Lấy đỉnh phổ cho nửa mảng, bắt đầu từ k=1 (bỏ DC)
        for (k in 1..n / 2) {
            var re = 0.0; var im = 0.0
            for (i in 0 until n) {
                val angle = 2 * Math.PI * k * i / n
                re += signal[i] * Math.cos(angle)
                im -= signal[i] * Math.sin(angle)
            }
            val mag = Math.sqrt(re * re + im * im).toFloat()
            if (mag > bestMag) { bestMag = mag; bestK = k }
        }
        return (bestK.toFloat() / n) * sampleRateHz
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exp = logits.map { Math.exp((it - max).toDouble()).toFloat() }
        val sum = exp.sum()
        return exp.map { it / sum }.toFloatArray()
    }

    companion object {
        private const val TAG           = "SteadySenseML"
        private const val MODEL_ASSET   = "quality_fusion.pt"
        const val WINDOW_SIZE           = 40
        const val MODALITIES            = 2
        const val EMB_DIM               = 12
        const val QUALITY_THRESHOLD     = 0.85f

        /** Label class index → tên điều kiện */
        val CLASS_NAMES = Result.CLASS_NAMES

        /** Copy asset ra file tạm rồi load — PyTorch Mobile yêu cầu file path thật. */
        private fun loadModule(context: Context): Module {
            val file = File(context.filesDir, MODEL_ASSET)
            // Ép copy đè luôn để đảm bảo khi app update model asset, file trong bộ nhớ đệm cũng được update
            context.assets.open(MODEL_ASSET).use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "Đã copy model asset -> ${file.absolutePath}")
            return LiteModuleLoader.load(file.absolutePath)
        }
    }
}
