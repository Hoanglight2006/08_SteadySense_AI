/*
 * Copyright 2026 SteadySense AI Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vn.edu.ictu.steadysense.phone.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import vn.edu.ictu.steadysense.core.ImuWindow
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.ml.QualityFusionInference
import vn.edu.ictu.steadysense.phone.transport.ExerciseDataBridge
import vn.edu.ictu.steadysense.phone.util.VoiceGuideManager

/**
 * Các giai đoạn trong vòng đời 1 rep tập luyện.
 *
 * IDLE: Chờ bắt đầu rep mới
 * CUE_FLEX: Đang ra hiệu "Gập tay!" (rung + hiển thị)
 * WAITING_DATA: Đợi IMU window từ đồng hồ
 * ANALYZING: AI đang chạy inference
 * FEEDBACK_GOOD: Rep đạt chuẩn — hiện phản hồi tích cực
 * FEEDBACK_BAD: Rep nhanh/sai — chờ người dùng xác nhận
 */
enum class RepPhase {
    IDLE,
    CUE_FLEX,
    WAITING_DATA,
    ANALYZING,
    FEEDBACK_GOOD,
    FEEDBACK_BAD,
}

data class RepDetail(
    val repNumber: Int,
    val isSuccess: Boolean,
    val speedDegPerSec: Int,
    val confidencePercent: Int,
    val signalQualityPercent: Int,
    val note: String,
)

/**
 * Snapshot trạng thái engine cho UI observe.
 */
data class RepEngineState(
    val phase: RepPhase = RepPhase.IDLE,
    val completedReps: Int = 0,
    val cancelledReps: Int = 0,
    val targetReps: Int = 10,
    val currentRepIndex: Int = 0,
    val currentSet: Int = 1,
    val totalSets: Int = 1,
    val restSeconds: Int = 60,
    val isResting: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val cueText: String = "Bấm \"Sẵn sàng\" để bắt đầu",
    val feedbackText: String = "",
    val confidencePercent: Int = 0,
    val signalQualityPercent: Int = 86,
    val errorMessage: String? = null,
    val isFinished: Boolean = false,
    val repDetails: List<RepDetail> = emptyList(),
)

/**
 * State machine quản lý vòng lặp rep tập luyện.
 *
 * Luồng: IDLE → CUE_FLEX → WAITING_DATA → ANALYZING → FEEDBACK_GOOD/BAD → IDLE
 *
 * Khi FEEDBACK_BAD: rep bị hủy, engine dừng ở trạng thái đó cho đến khi
 * người dùng gọi [confirmAndContinue]. Sau đó mới chuyển về IDLE → CUE tiếp.
 */
class ExerciseRepEngine(
    private val context: Context,
    private val targetRepsPerSet: Int = 10,
    private val totalSets: Int = 1,
    private val restSeconds: Int = 60,
) {
    private val _state = MutableStateFlow(
        RepEngineState(
            targetReps = targetRepsPerSet,
            currentSet = 1,
            totalSets = totalSets,
            restSeconds = restSeconds,
        )
    )
    val state: StateFlow<RepEngineState> = _state

    private var inference: QualityFusionInference? = null
    private var modelLoaded = false
    private var modelError = false
    private var loopJob: Job? = null
    private var scope: CoroutineScope? = null
    @Volatile
    private var skipRestRequested = false

    companion object {
        private const val TAG = "ExerciseRepEngine"
        private const val IMU_TIMEOUT_MS = 15_000L
        private const val CUE_DISPLAY_MS = 1_500L
        private const val FEEDBACK_GOOD_DISPLAY_MS = 1_200L
    }

    /**
     * Khởi tạo model AI. Gọi 1 lần khi bắt đầu session.
     * Nếu model không load được, engine sẽ fallback về chế độ không AI.
     */
    suspend fun initModel() {
        withContext(Dispatchers.IO) {
            try {
                inference = QualityFusionInference(context)
                modelLoaded = true
                modelError = false
                Log.i(TAG, "AI model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load AI model", e)
                modelError = true
                modelLoaded = false
                _state.value = _state.value.copy(
                    errorMessage = "AI chưa sẵn sàng: ${e.message?.take(50)}",
                )
            }
        }
    }

    /**
     * Bắt đầu vòng lặp rep. Gọi khi người dùng bấm "Sẵn sàng".
     */
    fun startLoop(coroutineScope: CoroutineScope) {
        if (loopJob?.isActive == true) return
        scope = coroutineScope
        loopJob = coroutineScope.launch {
            runRepLoop()
        }
    }

    /**
     * Bỏ qua thời gian nghỉ giữa hiệp.
     */
    fun skipRest() {
        skipRestRequested = true
    }

    /**
     * Người dùng xác nhận sau khi rep bị hủy do nhanh.
     * Chuyển engine về IDLE để bắt đầu rep tiếp.
     */
    fun confirmAndContinue() {
        val current = _state.value
        if (current.phase == RepPhase.FEEDBACK_BAD) {
            _state.value = current.copy(
                phase = RepPhase.IDLE,
                cueText = "Chuẩn bị rep tiếp...",
                feedbackText = "",
            )
        }
    }

    /**
     * Kết thúc sớm bài tập.
     */
    fun finishEarly() {
        loopJob?.cancel()
        _state.value = _state.value.copy(isFinished = true, isResting = false)
    }

    /**
     * Giải phóng tài nguyên khi session kết thúc.
     */
    fun release() {
        loopJob?.cancel()
        try {
            inference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing inference", e)
        }
        inference = null
    }

    private class RepCompletedException : CancellationException("Rep completed")

    // ─── Vòng lặp chính ────────────────────────────────────────────────

    private suspend fun runRepLoop() {
        for (setIndex in 1..totalSets) {
            _state.value = _state.value.copy(
                currentSet = setIndex,
                currentRepIndex = 0,
                targetReps = targetRepsPerSet,
            )

            if (UserPreferences.isVoiceGuideEnabled(context)) {
                if (setIndex == 1) {
                    VoiceGuideManager.speak("Bắt đầu hiệp 1")
                } else {
                    VoiceGuideManager.speak("Bắt đầu hiệp $setIndex")
                }
            }

            var repIndex = 0
            while (repIndex < targetRepsPerSet) {
                repIndex++
                val cueTime = System.currentTimeMillis()
                val totalRepNumber = (setIndex - 1) * targetRepsPerSet + repIndex

                // ① CUE_FLEX: Ra hiệu "Gập tay!"
                _state.value = _state.value.copy(
                    phase = RepPhase.CUE_FLEX,
                    currentRepIndex = repIndex,
                    cueText = "GẬP TAY NGAY!",
                    feedbackText = "",
                    errorMessage = null,
                )
                vibrateLight(context)
                sendCueToWatch(context, "H$setIndex L$repIndex\nGẬP TAY!")
                delay(CUE_DISPLAY_MS)

                // ② WAITING_DATA: Bắt đầu nhận dữ liệu NGAY SAU KHI hiệu lệnh đếm nhịp kết thúc
                val repStartTime = System.currentTimeMillis()
                _state.value = _state.value.copy(
                    phase = RepPhase.WAITING_DATA,
                    cueText = "Đang gập tay... (Gập lên và hạ xuống)",
                )

                val repFrames = mutableListOf<vn.edu.ictu.steadysense.core.ImuFrame>()
                var motionStarted = false
                var motionStartTime = 0L

                try {
                    withTimeout(IMU_TIMEOUT_MS) {
                        ExerciseDataBridge.windows
                            .filter { it.receivedAtMillis >= repStartTime }
                            .collect { timestamped ->
                                val window = timestamped.window
                                if (window.frames.isEmpty()) return@collect

                                repFrames.addAll(window.frames)
                                // Giữ tối đa 120 frame gần nhất (~6 giây)
                                while (repFrames.size > 120) {
                                    repFrames.removeAt(0)
                                }

                                // Tính tốc độ góc các frame trong buffer
                                val gyros = repFrames.map { f ->
                                    kotlin.math.sqrt(f.gyroX * f.gyroX + f.gyroY * f.gyroY + f.gyroZ * f.gyroZ)
                                }
                                val peakGyro = gyros.maxOrNull() ?: 0f
                                val maxGyroIdx = gyros.indices.maxByOrNull { gyros[it] } ?: 0

                                Log.d(TAG, "H$setIndex Rep $repIndex buffer=${repFrames.size} frames, peakGyro=$peakGyro, motionStarted=$motionStarted")

                                // 1. Nếu tay vẫn đang nghỉ hoặc cử động cực nhẹ (< 0.75 rad/s ~ 43°/s):
                                // Tiếp tục chờ người dùng thực hiện động tác gập tay thật sự
                                if (peakGyro < 0.75f) {
                                    Log.d(TAG, "Tay đang nghỉ (peak=$peakGyro), tiếp tục đợi người dùng gập tay...")
                                    return@collect
                                }

                                if (!motionStarted) {
                                    motionStarted = true
                                    motionStartTime = System.currentTimeMillis()
                                    _state.value = _state.value.copy(
                                        phase = RepPhase.ANALYZING,
                                        cueText = "Đang gập tay...",
                                    )
                                    Log.d(TAG, "Bắt đầu phát hiện cử động tại peakGyro=$peakGyro. Chờ gập lên và hạ xuống hết biên độ...")
                                }

                                // 2. Chờ hoàn thành trọn vẹn cả chu kỳ (Gập lên VÀ Duỗi xuống):
                                val elapsedSinceMotion = System.currentTimeMillis() - motionStartTime
                                val framesAfterPeak = repFrames.size - 1 - maxGyroIdx
                                val recentAvgGyro = gyros.takeLast(minOf(8, gyros.size)).average().toFloat()

                                val motionCompleted = repFrames.size >= 40 &&
                                    elapsedSinceMotion >= 1_400L &&
                                    ((framesAfterPeak >= 14 && recentAvgGyro < 1.6f) || elapsedSinceMotion >= 2_800L)

                                if (!motionCompleted) {
                                    Log.d(TAG, "Đang thu thập tiếp để trọn vẹn cả biên độ (elapsed=${elapsedSinceMotion}ms, frames=${repFrames.size}, framesAfterPeak=$framesAfterPeak, recentAvg=$recentAvgGyro)...")
                                    return@collect
                                }

                                // 3. Cắt cửa sổ 40 frame ĐƯỢC CĂN GIỮA tại đỉnh chuyển động (peakGyro)
                                val startIdx = (maxGyroIdx - 20).coerceIn(0, (repFrames.size - 40).coerceAtLeast(0))
                                val bestFrames = repFrames.subList(startIdx, startIdx + 40)
                                val bestWindow = ImuWindow(bestFrames)

                                Log.d(TAG, "Cắt trọn vẹn 40 frame quanh đỉnh (startIdx=$startIdx, peakGyro=$peakGyro, framesAfterPeak=$framesAfterPeak) -> Chạy AI")
                                val result = analyzeWindow(bestWindow)

                                val currentQuality = if (result != null && result.predictedQuality.size >= 2) {
                                    ((result.predictedQuality[0] + result.predictedQuality[1]) / 2f * 100f).toInt().coerceIn(65, 98)
                                } else 86

                                // 4. Phân loại theo kết quả AI (KHÔNG GHI ĐÈ KẾT QUẢ AI):
                                when (result?.predictedClass) {
                                    0 -> {
                                        // 🟢 AI: CYCLIC_MOTION — Bài tập gập khuỷu tay chuẩn!
                                        val confidence = (result.classProbabilities[0] * 100).toInt()
                                        val speedDeg = (peakGyro * 57.3f).toInt()

                                        // Kiểm tra xem gập có bị quá nhanh không (vung giật cực mạnh > 6.8 rad/s ~ 390°/s)
                                        if (peakGyro > 6.8f) {
                                            // ❌ GẬP QUÁ NHANH
                                            val newDetails = _state.value.repDetails + RepDetail(
                                                repNumber = totalRepNumber,
                                                isSuccess = false,
                                                speedDegPerSec = speedDeg,
                                                confidencePercent = confidence,
                                                signalQualityPercent = currentQuality,
                                                note = "Hiệp $setIndex: Quá nhanh ($speedDeg°/s)",
                                            )
                                            _state.value = _state.value.copy(
                                                phase = RepPhase.FEEDBACK_BAD,
                                                cancelledReps = _state.value.cancelledReps + 1,
                                                cueText = "QUÁ NHANH! ✗",
                                                feedbackText = "AI ghi nhận đúng động tác gập tay nhưng tốc độ quá nhanh ($speedDeg°/s). Hãy gập chậm và đều hơn.",
                                                confidencePercent = confidence,
                                                signalQualityPercent = currentQuality,
                                                repDetails = newDetails,
                                            )
                                            if (UserPreferences.isVoiceGuideEnabled(context)) {
                                                VoiceGuideManager.speakWarning("Gập chậm lại")
                                            }
                                            vibrateStrong(context)
                                            sendWarningToWatch(context, "QUÁ NHANH!\nChậm lại")
                                            repIndex--
                                            throw RepCompletedException()
                                        } else {
                                            // ✅ ĐẠT CHUẨN XUẤT SẮC
                                            val newDetails = _state.value.repDetails + RepDetail(
                                                repNumber = totalRepNumber,
                                                isSuccess = true,
                                                speedDegPerSec = speedDeg,
                                                confidencePercent = confidence,
                                                signalQualityPercent = currentQuality,
                                                note = "Hiệp $setIndex: Đạt chuẩn ✓",
                                            )
                                            _state.value = _state.value.copy(
                                                phase = RepPhase.FEEDBACK_GOOD,
                                                completedReps = _state.value.completedReps + 1,
                                                cueText = "Đạt chuẩn ✓",
                                                feedbackText = "H$setIndex L$repIndex: Đạt chuẩn ✓ (Độ chính xác AI: $confidence%)",
                                                confidencePercent = confidence,
                                                signalQualityPercent = currentQuality,
                                                repDetails = newDetails,
                                            )
                                            if (UserPreferences.isVoiceGuideEnabled(context)) {
                                                VoiceGuideManager.speakRep(repIndex)
                                            }
                                            sendCueToWatch(context, "H$setIndex L$repIndex\nĐẠT CHUẨN ✓")
                                            vibrateLight(context)
                                            throw RepCompletedException()
                                        }
                                    }
                                    2 -> {
                                        // 🔴 AI: DISTRACTOR — Lắc tay / chuyển động nhiễu / sai động tác!
                                        val confidence = (result.classProbabilities[2] * 100).toInt()
                                        val speedDeg = (peakGyro * 57.3f).toInt()
                                        val newDetails = _state.value.repDetails + RepDetail(
                                            repNumber = totalRepNumber,
                                            isSuccess = false,
                                            speedDegPerSec = speedDeg,
                                            confidencePercent = confidence,
                                            signalQualityPercent = currentQuality,
                                            note = "Hiệp $setIndex: Sai động tác / Nhiễu",
                                        )
                                        _state.value = _state.value.copy(
                                            phase = RepPhase.FEEDBACK_BAD,
                                            cancelledReps = _state.value.cancelledReps + 1,
                                            cueText = "SAI ĐỘNG TÁC! ✗",
                                            feedbackText = "AI phát hiện cử động lắc/nhiễu (${confidence}%), không phải động tác gập khuỷu tay. Hãy gập - duỗi cẳng tay đúng bài tập.",
                                            confidencePercent = confidence,
                                            signalQualityPercent = currentQuality,
                                            repDetails = newDetails,
                                        )
                                        if (UserPreferences.isVoiceGuideEnabled(context)) {
                                            VoiceGuideManager.speakWarning("Hãy gập khuỷu tay")
                                        }
                                        vibrateStrong(context)
                                        sendWarningToWatch(context, "SAI ĐỘNG TÁC!\nHãy gập khuỷu tay")
                                        repIndex--
                                        throw RepCompletedException()
                                    }
                                    1 -> {
                                        // 🟡 AI: REST — Người dùng đang nghỉ
                                        Log.d(TAG, "AI nhận diện REST, tiếp tục đợi cử động...")
                                        return@collect
                                    }
                                    else -> {
                                        // Fallback khi AI không load được (result == null)
                                        val speedDeg = (peakGyro * 57.3f).toInt()
                                        if (peakGyro > 6.8f) {
                                            val newDetails = _state.value.repDetails + RepDetail(
                                                repNumber = totalRepNumber,
                                                isSuccess = false,
                                                speedDegPerSec = speedDeg,
                                                confidencePercent = 70,
                                                signalQualityPercent = 80,
                                                note = "Hiệp $setIndex: Vung tay nhanh ($speedDeg°/s)",
                                            )
                                            _state.value = _state.value.copy(
                                                phase = RepPhase.FEEDBACK_BAD,
                                                cancelledReps = _state.value.cancelledReps + 1,
                                                cueText = "QUÁ NHANH! ✗",
                                                feedbackText = "Tốc độ vung tay quá nhanh ($speedDeg°/s). Hãy gập và hạ tay từ từ.",
                                                repDetails = newDetails,
                                            )
                                            if (UserPreferences.isVoiceGuideEnabled(context)) {
                                                VoiceGuideManager.speakWarning("Gập chậm lại")
                                            }
                                            vibrateStrong(context)
                                            sendWarningToWatch(context, "QUÁ NHANH!\nChậm lại")
                                            repIndex--
                                            throw RepCompletedException()
                                        } else {
                                            val newDetails = _state.value.repDetails + RepDetail(
                                                repNumber = totalRepNumber,
                                                isSuccess = true,
                                                speedDegPerSec = speedDeg,
                                                confidencePercent = 80,
                                                signalQualityPercent = 85,
                                                note = "Hiệp $setIndex: Đã ghi nhận",
                                            )
                                            _state.value = _state.value.copy(
                                                phase = RepPhase.FEEDBACK_GOOD,
                                                completedReps = _state.value.completedReps + 1,
                                                cueText = "Đạt chuẩn ✓",
                                                feedbackText = "H$setIndex L$repIndex: Đã ghi nhận",
                                                repDetails = newDetails,
                                            )
                                            if (UserPreferences.isVoiceGuideEnabled(context)) {
                                                VoiceGuideManager.speakRep(repIndex)
                                            }
                                            sendCueToWatch(context, "H$setIndex L$repIndex\nĐẠT CHUẨN ✓")
                                            vibrateLight(context)
                                            throw RepCompletedException()
                                        }
                                    }
                                }
                            }
                    }
                } catch (_: RepCompletedException) {
                    // Hoàn thành đánh giá 1 rep
                } catch (e: TimeoutCancellationException) {
                    if (!motionStarted) {
                        _state.value = _state.value.copy(
                            phase = RepPhase.FEEDBACK_BAD,
                            cueText = "Chưa gập tay",
                            feedbackText = "Chưa nhận thấy cử động gập tay trong 15 giây. Hãy bấm tiếp tục khi bạn sẵn sàng gập rep này.",
                            errorMessage = null,
                        )
                    } else {
                        _state.value = _state.value.copy(
                            phase = RepPhase.FEEDBACK_BAD,
                            cueText = "Mất kết nối",
                            feedbackText = "Kiểm tra kết nối đồng hồ rồi bấm tiếp tục",
                            errorMessage = "Timeout: Không nhận được dữ liệu IMU trong ${IMU_TIMEOUT_MS / 1000}s",
                        )
                        sendWarningToWatch(context, "MẤT KẾT NỐI\nKiểm tra đồng hồ")
                    }
                    repIndex--
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in rep loop", e)
                }

                if (_state.value.phase == RepPhase.FEEDBACK_BAD) {
                    waitForConfirmation()
                } else if (_state.value.phase == RepPhase.FEEDBACK_GOOD) {
                    delay(FEEDBACK_GOOD_DISPLAY_MS)
                }
            }

            // NGHỈ GIỮA CÁC HIỆP
            if (setIndex < totalSets) {
                skipRestRequested = false
                _state.value = _state.value.copy(
                    phase = RepPhase.IDLE,
                    isResting = true,
                    restSecondsRemaining = restSeconds,
                    cueText = "Nghỉ giữa hiệp",
                    feedbackText = "Hoàn thành hiệp $setIndex/$totalSets. Nghỉ ngơi trước khi sang hiệp ${setIndex + 1}.",
                )
                if (UserPreferences.isVoiceGuideEnabled(context)) {
                    VoiceGuideManager.speak("Hoàn thành hiệp $setIndex. Nghỉ ngơi.")
                }
                sendCueToWatch(context, "NGHỈ HIỆP $setIndex\n${restSeconds}s")

                var remaining = restSeconds
                while (remaining > 0 && !skipRestRequested) {
                    _state.value = _state.value.copy(restSecondsRemaining = remaining)
                    delay(1000L)
                    remaining--
                }
                _state.value = _state.value.copy(
                    isResting = false,
                    restSecondsRemaining = 0,
                    cueText = "Sẵn sàng hiệp ${setIndex + 1}!",
                    feedbackText = "",
                )
            }
        }

        // Hoàn thành tất cả các hiệp
        if (UserPreferences.isVoiceGuideEnabled(context)) {
            VoiceGuideManager.speakFinish()
        }
        sendCueToWatch(context, "HOÀN THÀNH\nBUỔI TẬP!")
        _state.value = _state.value.copy(isFinished = true, isResting = false)
    }

    /**
     * Chờ cho đến khi phase chuyển khỏi FEEDBACK_BAD (qua confirmAndContinue).
     */
    private suspend fun waitForConfirmation() {
        while (_state.value.phase == RepPhase.FEEDBACK_BAD) {
            delay(200)
        }
    }

    /**
     * Chạy AI inference trên 1 IMU window. Trả về null nếu AI không khả dụng.
     */
    private suspend fun analyzeWindow(window: ImuWindow): QualityFusionInference.Result? {
        if (!modelLoaded || inference == null) return null
        return withContext(Dispatchers.Default) {
            try {
                inference?.infer(window)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Inference exception", e)
                _state.value = _state.value.copy(
                    errorMessage = "Lỗi phân tích: ${e.message?.take(40)}",
                )
                null
            }
        }
    }

    // ─── Vibration helpers ──────────────────────────────────────────────

    private fun vibrateLight(ctx: Context) {
        if (!UserPreferences.isWatchVibrationEnabled(ctx)) return
        runCatching {
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    }

    private fun vibrateStrong(ctx: Context) {
        if (!UserPreferences.isWatchVibrationEnabled(ctx)) return
        runCatching {
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            val timings = longArrayOf(0, 400, 150, 400, 150, 500)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }

    // ─── Gửi message sang đồng hồ ──────────────────────────────────────

    private fun sendCueToWatch(ctx: Context, message: String) {
        runCatching {
            val shouldVibrate = UserPreferences.isWatchVibrationEnabled(ctx)
            val payload = if (shouldVibrate) message else "SILENT:$message"
            val bytes = payload.toByteArray(Charsets.UTF_8)
            Wearable.getNodeClient(ctx).connectedNodes
                .addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(ctx)
                            .sendMessage(node.id, TransportPaths.EXERCISE_CUE, bytes)
                    }
                }
        }
    }

    private fun sendWarningToWatch(ctx: Context, message: String) {
        runCatching {
            val shouldVibrate = UserPreferences.isWatchVibrationEnabled(ctx)
            val payload = if (shouldVibrate) message else "SILENT:$message"
            val bytes = payload.toByteArray(Charsets.UTF_8)
            Wearable.getNodeClient(ctx).connectedNodes
                .addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(ctx)
                            .sendMessage(node.id, TransportPaths.EXERCISE_WARNING, bytes)
                    }
                }
        }
    }
}
