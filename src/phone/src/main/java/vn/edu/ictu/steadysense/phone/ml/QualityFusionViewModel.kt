package vn.edu.ictu.steadysense.phone.ml

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.edu.ictu.steadysense.core.ImuWindow
import vn.edu.ictu.steadysense.core.ImuFrame
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase

/**
 * ViewModel chạy quality_fusion on-device trên các cửa sổ IMU gần nhất trong DB.
 *
 * Nguyên tắc: chỉ gọi model sau khi dữ liệu đã được lưu an toàn vào Room.
 * Kết quả inference KHÔNG được dùng để tuyên bố hiệu quả lâm sàng.
 */
class QualityFusionViewModel(app: Application) : AndroidViewModel(app) {

    /** Snapshot kết quả inference gần nhất. null = chưa chạy hoặc không đủ dữ liệu. */
    var result by mutableStateOf<QualityFusionInference.Result?>(null)
        private set

    /** Trạng thái loading. */
    var isRunning by mutableStateOf(false)
        private set

    /** Thông báo lỗi nếu có. */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var inference: QualityFusionInference? = null

    /**
     * Chạy inference trên [windowCount] cửa sổ IMU gần nhất trong DB của phiên [sessionId].
     * Kết quả là vote theo majority class với quality gate.
     *
     * Đây là kiểm chứng kỹ thuật — không phải chẩn đoán.
     */
    fun runOnSession(sessionId: String, windowCount: Int = 10) {
        if (isRunning) return
        isRunning = true
        errorMessage = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                if (inference == null) {
                    inference = QualityFusionInference(ctx)
                    Log.i(TAG, "Model loaded")
                }
                val dao = PhoneDatabase.get(ctx).imuWindowDao()
                val entities = dao.latestBySession(sessionId, windowCount)
                if (entities.isEmpty()) {
                    errorMessage = "Không có cửa sổ IMU nào cho phiên $sessionId"
                    return@launch
                }

                // Chuyển entity -> ImuWindow và chạy inference
                val results = entities.mapNotNull { entity ->
                    val window = entityToWindow(entity)
                    inference!!.infer(window)
                }

                if (results.isEmpty()) {
                    errorMessage = "Không có cửa sổ nào đủ frame để inference"
                    return@launch
                }

                // Majority vote: lấy class xuất hiện nhiều nhất trong số cửa sổ đạt quality gate
                val passed = results.filter { it.qualityGatePass }
                val effective = if (passed.isNotEmpty()) passed else results
                val votes = effective.groupingBy { it.predictedClass }.eachCount()
                val winnerClass = votes.maxByOrNull { it.value }!!.key
                // Dùng kết quả đại diện (có confidence cao nhất cho winner class)
                val representative = effective
                    .filter { it.predictedClass == winnerClass }
                    .maxByOrNull { it.classProbabilities[winnerClass] }!!

                result = representative
                Log.i(TAG, "Inference: ${representative.label} " +
                    "quality=[${representative.predictedQuality.joinToString { "%.2f".format(it) }}] " +
                    "gatePass=${representative.qualityGatePass} " +
                    "windows=${results.size} passed=${passed.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Inference failed", e)
                errorMessage = "Lỗi inference: ${e.message}"
            } finally {
                isRunning = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inference?.close()
    }

    companion object {
        private const val TAG = "SteadySenseML"
    }
}

// ── Chuyển ImuWindowEntity sang ImuWindow ─────────────────────────────────
private fun entityToWindow(entity: vn.edu.ictu.steadysense.phone.data.ImuWindowEntity): ImuWindow {
    return vn.edu.ictu.steadysense.core.ImuPayloadCodec.decode(entity.payload)
}
