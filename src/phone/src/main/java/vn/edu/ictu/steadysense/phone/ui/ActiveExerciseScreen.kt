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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.data.WorkoutSessionEntity
import java.util.UUID
private enum class ResultTier {
    EXCELLENT,
    GOOD,
    NEEDS_WORK,
}

@Composable
fun ActiveExerciseScreen(
    selectedHand: String,
    targetSets: Int = 3,
    targetRepsPerSet: Int = 10,
    restSeconds: Int = 60,
    targetScheduleId: String? = null,
    onFinishSession: (completedReps: Int, score: Int) -> Unit,
    onCancelSession: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalSessionTargetReps = (targetSets * targetRepsPerSet).coerceAtLeast(1)
    // Engine quản lý vòng lặp rep AI
    val engine = remember(targetSets, targetRepsPerSet, restSeconds) {
        ExerciseRepEngine(
            context = context,
            targetRepsPerSet = targetRepsPerSet,
            totalSets = targetSets,
            restSeconds = restSeconds,
        )
    }
    val repState by engine.state.collectAsState()

    // Đã bắt đầu tập chưa (bấm "Sẵn sàng")
    var hasStarted by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showDetailSheet by remember { mutableStateOf(false) }

    LaunchedEffect(hasStarted, repState.isFinished) {
        if (hasStarted && !repState.isFinished) {
            while (isActive) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Load model AI khi màn hình mở
    LaunchedEffect(Unit) {
        engine.initModel()
    }

    // Giải phóng tài nguyên khi màn hình đóng
    DisposableEffect(Unit) {
        onDispose {
            engine.release()
        }
    }

    // POPUP CẢNH BÁO KHI REP BỊ HỦY
    val showWarningPopup = repState.phase == RepPhase.FEEDBACK_BAD

    if (showWarningPopup) {
        val isTooFast = repState.cueText.contains("QUÁ NHANH")
        val isDistractor = repState.cueText.contains("SAI ĐỘNG TÁC")
        val isTimeout = repState.errorMessage?.contains("Timeout") == true
        val themeColor = if (isTooFast || isDistractor || isTimeout) Coral else Amber
        val themeBg = if (isTooFast || isDistractor || isTimeout) CoralSoft else AmberSoft

        AlertDialog(
            onDismissRequest = { /* Không cho dismiss, bắt buộc bấm nút */ },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(themeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(32.dp),
                    )
                }
            },
            title = {
                Text(
                    when {
                        isTimeout -> "MẤT KẾT NỐI DỮ LIỆU"
                        isTooFast -> "CẢNH BÁO: TẬP QUÁ NHANH!"
                        isDistractor -> "CẢNH BÁO: SAI ĐỘNG TÁC!"
                        else -> "CHƯA NHẬN DIỆN ĐỘNG TÁC"
                    },
                    color = themeColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    repState.feedbackText.ifBlank {
                        "Hãy hít thở sâu, hạ tay từ từ và di chuyển thật đều đặn."
                    },
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = { engine.confirmAndContinue() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                ) {
                    Text(
                        "Đã hiểu, tiếp tục tập",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.border(1.5.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(26.dp)),
        )
    }

    val completedReps = repState.completedReps
    val isFinished = repState.isFinished
    val remainingReps = (totalSessionTargetReps - completedReps).coerceAtLeast(0)
    val progress by animateFloatAsState(
        targetValue = if (totalSessionTargetReps > 0) (completedReps.toFloat() / totalSessionTargetReps.toFloat()).coerceIn(0f, 1f) else 0f,
        label = "exerciseProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TealSoft.copy(alpha = 0.5f), Canvas, White),
                ),
            )
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. Thanh tiêu đề trên cùng
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "ĐANG TẬP LUYỆN",
                    color = TealDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    "Gấp – duỗi khuỷu tay",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = TealSoft,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Teal),
                        )
                        Icon(
                            imageVector = Icons.Rounded.Watch,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Đã kết nối",
                            color = TealDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = White,
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = {
                            engine.release()
                            onCancelSession()
                        }),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hủy",
                            tint = Muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Badge tay tập, Hiệp tập & Thời gian trôi qua
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (selectedHand == "LEFT") Color(0xFFEFF6FF) else TealSoft,
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = if (selectedHand == "LEFT") Color(0xFF1D4ED8) else TealDark,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (selectedHand == "LEFT") "Tay Trái" else "Tay Phải",
                        color = if (selectedHand == "LEFT") Color(0xFF1D4ED8) else TealDark,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                color = AmberSoft,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "Hiệp ${repState.currentSet}/${repState.totalSets}",
                    color = Amber,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Surface(
                color = White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(50)),
            ) {
                Text(
                    "⏱ ${formatDuration(elapsedSeconds)}",
                    color = Ink,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // SignalQualityBar live đo chất lượng cảm biến
        SignalQualityBar(qualityPercent = repState.signalQualityPercent)

        Spacer(Modifier.height(24.dp))

        if (!isFinished) {
            // 2. Vòng tròn đếm rep + Chỉ dẫn AI ở giữa
            val isResting = repState.isResting
            val restSecondsRemaining = repState.restSecondsRemaining
            val restTotal = if (repState.restSeconds > 0) repState.restSeconds else 60
            val restProgress = (restSecondsRemaining.toFloat() / restTotal.toFloat()).coerceIn(0f, 1f)

            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(240.dp)) {
                    // Vòng nền
                    drawArc(
                        color = Color(0xFFE2E8F0),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round),
                    )
                    if (isResting) {
                        // Vòng tiến trình Amber khi nghỉ giữa hiệp
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Amber, Color(0xFFD97706), Amber)),
                            startAngle = -90f,
                            sweepAngle = restProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round),
                        )
                    } else {
                        // Vòng tiến trình Teal khi tập
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Teal, TealDark, Teal)),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isResting) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = Amber,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${restSecondsRemaining}s",
                            color = Amber,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 48.sp,
                        )
                        Text(
                            "Nghỉ giữa hiệp",
                            color = Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Thả lỏng cơ tay...",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    } else if (hasStarted) {
                        // Chỉ dẫn AI nổi bật ở giữa vòng tròn
                        val cueColor = when (repState.phase) {
                            RepPhase.CUE_FLEX -> Teal
                            RepPhase.FEEDBACK_GOOD -> Color(0xFF10B981)
                            RepPhase.FEEDBACK_BAD -> Coral
                            RepPhase.ANALYZING -> Amber
                            else -> Muted
                        }

                        // Pulse animation khi CUE_FLEX
                        if (repState.phase == RepPhase.CUE_FLEX) {
                            val infiniteTransition = rememberInfiniteTransition(label = "cuePulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "pulseScale",
                            )
                            Text(
                                repState.cueText,
                                color = cueColor,
                                fontSize = (22 * scale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Text(
                                repState.cueText,
                                color = cueColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lần ${repState.currentRepIndex} / $targetRepsPerSet",
                            color = Ink,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hiệp ${repState.currentSet}/${repState.totalSets} · Đã đạt: $completedReps/$totalSessionTargetReps",
                            color = TealDark,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        // Chưa bắt đầu — hiện số rep cần tập
                        Text(
                            "$totalSessionTargetReps",
                            color = Ink,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 66.sp,
                        )
                        Text(
                            "$targetSets hiệp × $targetRepsPerSet lần",
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Nghỉ ${restSeconds}s giữa hiệp",
                            color = Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. THẺ TRẠNG THÁI REP VỪA TẬP
            if (hasStarted && repState.feedbackText.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val iconBg = when (repState.phase) {
                            RepPhase.FEEDBACK_GOOD -> TealSoft
                            RepPhase.FEEDBACK_BAD -> CoralSoft
                            else -> TealSoft
                        }
                        val iconColor = when (repState.phase) {
                            RepPhase.FEEDBACK_GOOD -> TealDark
                            RepPhase.FEEDBACK_BAD -> Coral
                            else -> TealDark
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Trạng thái rep vừa thực hiện",
                                color = Muted,
                                fontSize = 12.sp,
                            )
                            Text(
                                repState.feedbackText,
                                color = Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (repState.confidencePercent > 0) {
                                Text(
                                    "Độ tin cậy AI: ${repState.confidencePercent}%",
                                    color = Muted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 4. CẢNH BÁO LỖI (INLINE BANNER)
            AnimatedVisibility(visible = repState.errorMessage != null && repState.phase != RepPhase.FEEDBACK_BAD) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberSoft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Amber, RoundedCornerShape(20.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Amber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Amber,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Lưu ý",
                                color = Amber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                repState.errorMessage ?: "",
                                color = Ink,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 5. NÚT HÀNH ĐỘNG
            if (repState.isResting) {
                Button(
                    onClick = { engine.skipRest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                ) {
                    Text(
                        "Bỏ qua nghỉ — Vào Hiệp ${repState.currentSet + 1} ngay",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                    )
                }
            } else if (!hasStarted) {
                // Nút "Sẵn sàng" — bắt đầu vòng lặp rep AI
                Button(
                    onClick = {
                        hasStarted = true
                        engine.startLoop(scope)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sẵn sàng — Bắt đầu tập", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    engine.finishEarly()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Kết thúc sớm bài tập", color = Muted, fontSize = 14.sp)
            }
        } else {
            // 6. TỔNG KẾT SAU BUỔI TẬP — ĐỔI MÀU THEO MỨC ĐỘ HOÀN THÀNH & CHUẨN XÁC
            val warnedRepsCount = repState.cancelledReps
            val accuracyPercentage = when {
                completedReps == 0 -> 0
                else -> (((completedReps - warnedRepsCount.coerceAtMost(completedReps)).toFloat() / completedReps.toFloat()) * 100f)
                    .toInt().coerceIn(35, 96)
            }

            val steadyScore = when {
                completedReps == 0 -> "0.0"
                else -> String.format("%.1f", (5.5f + (accuracyPercentage / 100f) * 3.5f))
            }

            val tier = when {
                completedReps >= 8 && accuracyPercentage >= 80 -> ResultTier.EXCELLENT
                completedReps >= 5 && accuracyPercentage >= 60 -> ResultTier.GOOD
                else -> ResultTier.NEEDS_WORK
            }

            val tierData = when (tier) {
                ResultTier.EXCELLENT -> TierPresentation(
                    accent = Teal,
                    accentSoft = TealSoft,
                    badgeText = "Xuất sắc",
                    icon = Icons.Rounded.EmojiEvents,
                    title = "Xuất sắc! Đạt chuẩn phục hồi",
                    desc = "Bạn đã hoàn thành đủ số lần với nhịp điệu đều đặn và độ vững tay rất tốt.",
                )
                ResultTier.GOOD -> TierPresentation(
                    accent = Amber,
                    accentSoft = AmberSoft,
                    badgeText = "Khá tốt",
                    icon = Icons.Rounded.CheckCircle,
                    title = "Đã hoàn thành · Cần chú ý tư thế",
                    desc = "Bạn đã hoàn thành các lần tập, tuy nhiên có một số rep bị vung nhanh. Hãy chú ý hạ tay chậm hơn.",
                )
                ResultTier.NEEDS_WORK -> TierPresentation(
                    accent = Coral,
                    accentSoft = CoralSoft,
                    badgeText = "Cần nỗ lực",
                    icon = Icons.Rounded.Warning,
                    title = "Chưa đạt chuẩn · Cần nỗ lực thêm",
                    desc = "Bài tập dừng sớm hoặc có nhiều rep vung tay sai tốc độ. Hãy nghỉ ngơi ít phút rồi thử lại nhé.",
                )
            }

            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, tierData.accent, RoundedCornerShape(26.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(tierData.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tierData.icon,
                            contentDescription = null,
                            tint = tierData.accent,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        tierData.title,
                        color = Ink,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        tierData.desc,
                        color = Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    // SessionStateChip theo chuẩn Wireframe
                    Surface(
                        color = tierData.accentSoft,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Text(
                            text = when (tier) {
                                ResultTier.EXCELLENT -> "● ĐẠT CHUẨN XUẤT SẮC"
                                ResultTier.GOOD -> "◐ HOÀN THÀNH MỘT PHẦN"
                                ResultTier.NEEDS_WORK -> "✕ CẦN CỐ GẮNG THÊM"
                            },
                            color = tierData.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Bảng chi tiết kết quả 4 ô: Số lần, Độ chuẩn xác, Thời gian, Tín hiệu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Canvas)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$completedReps/$totalSessionTargetReps", color = tierData.accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Số lần đạt", color = Muted, fontSize = 11.sp)
                        }
                        Box(Modifier.width(1.dp).height(32.dp).background(SurfaceBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$accuracyPercentage%", color = tierData.accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Độ chuẩn xác", color = Muted, fontSize = 11.sp)
                        }
                        Box(Modifier.width(1.dp).height(32.dp).background(SurfaceBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatDuration(elapsedSeconds), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Thời gian", color = Muted, fontSize = 11.sp)
                        }
                        Box(Modifier.width(1.dp).height(32.dp).background(SurfaceBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${repState.signalQualityPercent}%", color = TealDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Tín hiệu", color = Muted, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Nút mở ModalBottomSheet xem chi tiết từng rep
                    OutlinedButton(
                        onClick = { showDetailSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Xem chi tiết từng rep >", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val sessionStateStr = when (tier) {
                                    ResultTier.EXCELLENT -> "RELIABLE"
                                    ResultTier.GOOD -> "PARTIAL"
                                    ResultTier.NEEDS_WORK -> "NEEDS_WORK"
                                }
                                val sessionStartedAt = System.currentTimeMillis() - elapsedSeconds * 1000L
                                val detailsJson = repState.repDetails.joinToString(";") {
                                    "${it.repNumber}:${if (it.isSuccess) 1 else 0}:${it.speedDegPerSec}:${it.confidencePercent}:${it.signalQualityPercent}:${it.note}"
                                }

                                val dao = PhoneDatabase.get(context).workoutDao()
                                val startOfDay = java.util.Calendar.getInstance().apply {
                                    timeInMillis = sessionStartedAt
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1L

                                // Nếu là bài tập trong lịch và đã từng tập trong ngày -> giữ nguyên id để cập nhật đè kết quả bài đó
                                val existingSession = if (!targetScheduleId.isNullOrBlank()) {
                                    dao.findSessionByScheduleAndDay(targetScheduleId, startOfDay, endOfDay)
                                } else null
                                val sessionIdToUse = existingSession?.id ?: UUID.randomUUID().toString()

                                dao.insertSession(
                                    WorkoutSessionEntity(
                                        id = sessionIdToUse,
                                        startedAt = sessionStartedAt,
                                        endedAt = System.currentTimeMillis(),
                                        exerciseName = "Gấp – duỗi khuỷu tay",
                                        selectedHand = selectedHand,
                                        targetReps = totalSessionTargetReps,
                                        completedReps = completedReps,
                                        accuracyPercentage = accuracyPercentage,
                                        steadyScore = steadyScore.toFloatOrNull() ?: 8.0f,
                                        sessionState = sessionStateStr,
                                        durationSeconds = elapsedSeconds,
                                        repDetailsJson = detailsJson,
                                        scheduleId = targetScheduleId,
                                    )
                                )
                                if (!targetScheduleId.isNullOrBlank()) {
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(sessionStartedAt))
                                    UserPreferences.markScheduleCompleted(context, dateStr, targetScheduleId, sessionIdToUse)
                                }
                                withContext(Dispatchers.Main) {
                                    engine.release()
                                    onFinishSession(completedReps, accuracyPercentage)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = tierData.accent),
                    ) {
                        Text("Lưu kết quả & Về trang chủ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }
    }

    // ModalBottomSheet xem chi tiết từng rep
    @OptIn(ExperimentalMaterial3Api::class)
    if (showDetailSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            containerColor = White,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 36.dp),
            ) {
                Text(
                    "Chi tiết các lần gập (Reps)",
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dữ liệu tốc độ và độ tin cậy AI cho từng động tác",
                    color = Muted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))

                if (repState.repDetails.isEmpty()) {
                    Text("Chưa có dữ liệu từng rep.", color = Muted, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(repState.repDetails) { detail ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (detail.isSuccess) TealSoft.copy(alpha = 0.5f) else CoralSoft.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (detail.isSuccess) Teal.copy(alpha = 0.3f) else Coral.copy(alpha = 0.3f),
                                        RoundedCornerShape(16.dp)
                                    ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            "Lần ${detail.repNumber}: ${detail.note}",
                                            color = if (detail.isSuccess) TealDark else Coral,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "Vận tốc: ${detail.speedDegPerSec}°/s · AI: ${detail.confidencePercent}%",
                                            color = Muted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Surface(
                                        color = White,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            "Tín hiệu: ${detail.signalQualityPercent}%",
                                            color = Ink,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TierPresentation(
    val accent: Color,
    val accentSoft: Color,
    val badgeText: String,
    val icon: ImageVector,
    val title: String,
    val desc: String,
)

/**
 * Gửi tín hiệu cảnh báo tập nhanh sang đồng hồ Wear OS (kèm rung mạnh)
 * và rung phản hồi nhẹ trên chính điện thoại.
 */
private fun triggerWatchAndPhoneWarning(context: Context, message: String) {
    // 1. Rung trên điện thoại
    runCatching {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(350)
        }
    }

    // 2. Gửi tín hiệu sang Wear OS qua Google Play Services Wearable
    runCatching {
        val bytes = message.toByteArray(Charsets.UTF_8)
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, TransportPaths.EXERCISE_WARNING, bytes)
                }
            }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}

@Composable
private fun SignalQualityBar(qualityPercent: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Sensors,
                contentDescription = null,
                tint = if (qualityPercent >= 80) TealDark else Amber,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Chất lượng cảm biến:",
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Canvas),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (qualityPercent / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (qualityPercent >= 80) Teal else Amber),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "$qualityPercent%",
                color = if (qualityPercent >= 80) TealDark else Amber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
