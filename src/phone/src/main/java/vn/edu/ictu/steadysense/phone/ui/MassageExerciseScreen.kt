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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.data.WorkoutSessionEntity
import vn.edu.ictu.steadysense.phone.transport.ExerciseDataBridge
import com.google.android.gms.wearable.Wearable
import vn.edu.ictu.steadysense.core.TransportPaths
import java.util.UUID

@Composable
fun MassageExerciseScreen(
    targetMinutes: Int = 5,
    selectedHand: String = "RIGHT",
    targetScheduleId: String? = null,
    onFinishSession: () -> Unit,
    onCancelSession: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalDurationSeconds = targetMinutes * 60
    var secondsRemaining by remember { mutableIntStateOf(totalDurationSeconds) }
    var isPaused by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Lắng nghe lệnh chạm màn hình đồng hồ để tạm dừng/tiếp tục đồng bộ
    LaunchedEffect(Unit) {
        ExerciseDataBridge.massageControlEvents.collect { cmd ->
            if (cmd == "TOGGLE_PAUSE" && !isFinished) {
                isPaused = !isPaused
                val syncCmd = if (!isPaused) "RESUME:MASSAGE:$secondsRemaining" else "PAUSE:MASSAGE:$secondsRemaining"
                val bytes = syncCmd.toByteArray(Charsets.UTF_8)
                runCatching {
                    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                        nodes.forEach { node ->
                            Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, bytes)
                        }
                    }
                }
            }
        }
    }

    // Định kỳ đồng bộ thời gian còn lại sang đồng hồ để đảm bảo khớp 100%
    LaunchedEffect(secondsRemaining, isPaused) {
        if (!isPaused && !isFinished && secondsRemaining > 0 && secondsRemaining % 5 == 0) {
            val syncCmd = "SYNC:MASSAGE:$secondsRemaining"
            val bytes = syncCmd.toByteArray(Charsets.UTF_8)
            runCatching {
                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, bytes)
                    }
                }
            }
        }
    }

    // Khi hoàn tất hoặc thoát màn hình: báo đồng hồ dừng chế độ massage
    LaunchedEffect(isFinished) {
        if (isFinished) {
            val bytes = "STOP".toByteArray(Charsets.UTF_8)
            runCatching {
                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, bytes)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val bytes = "STOP".toByteArray(Charsets.UTF_8)
            runCatching {
                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, bytes)
                    }
                }
            }
        }
    }

    // Hướng dẫn thư giãn theo từng mốc thời gian
    val massageTips = listOf(
        "Bước 1: Dùng các đầu ngón tay đối diện xoa tròn nhẹ nhàng vùng cẳng tay từ cổ tay lên khuỷu tay.",
        "Bước 2: Dùng ngón cái ấn miết nhẹ theo đường dọc cơ bắp để giảm căng cứng và tăng lưu thông máu.",
        "Bước 3: Thả lỏng toàn bộ cánh tay trên đùi, xoa bóp nhẹ phần bắp tay và các khớp ngón tay.",
        "Bước 4: Hít sâu bằng mũi và thở chậm bằng miệng, giữ cơ thể ở trạng thái thư giãn tối đa.",
    )

    val currentTipIndex = ((totalDurationSeconds - secondsRemaining) / 45) % massageTips.size

    // Bộ đếm thời gian
    LaunchedEffect(isPaused, isFinished, secondsRemaining) {
        if (!isPaused && !isFinished && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            if (secondsRemaining <= 0) {
                isFinished = true
            }
        }
    }

    // Hiệu ứng nhịp thở thư giãn (pulsing glow)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val elapsedSec = (totalDurationSeconds - secondsRemaining).coerceAtLeast(1)
    val completionRatio = (elapsedSec.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
    val accuracyPercentage = (completionRatio * 100).toInt().coerceIn(0, 100)
    val steadyScore = 0f // Massage thả lỏng cơ, không đo độ vững tay
    val sessionState = when {
        completionRatio >= 0.80f -> "RELIABLE"
        completionRatio >= 0.45f -> "PARTIAL"
        else -> "NEEDS_WORK"
    }

    fun saveAndComplete() {
        val completedMinutes = (elapsedSec / 60).coerceAtLeast(if (elapsedSec >= 30) 1 else 0)
        scope.launch(Dispatchers.IO) {
            val dao = PhoneDatabase.get(context).workoutDao()
            val startOfDay = java.util.Calendar.getInstance().apply {
                timeInMillis = startTime
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
                    startedAt = startTime,
                    endedAt = System.currentTimeMillis(),
                    exerciseName = "Thả lỏng & Massage",
                    selectedHand = selectedHand,
                    targetReps = targetMinutes,
                    completedReps = completedMinutes,
                    accuracyPercentage = accuracyPercentage,
                    steadyScore = steadyScore,
                    sessionState = sessionState,
                    durationSeconds = elapsedSec,
                    repDetailsJson = "1:1:0:$accuracyPercentage:$accuracyPercentage:Massage thả lỏng (${elapsedSec}s/${totalDurationSeconds}s)",
                    scheduleId = targetScheduleId,
                )
            )
            if (!targetScheduleId.isNullOrBlank()) {
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(startTime))
                UserPreferences.markScheduleCompleted(context, dateStr, targetScheduleId, sessionIdToUse)
            }
            withContext(Dispatchers.Main) {
                onFinishSession()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    color = TealSoft,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Thư giãn & Phục hồi",
                            color = TealDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Surface(
                    color = White,
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showCancelDialog = true },
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Thả lỏng & Massage",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Xoa bóp nhẹ nhàng vùng cẳng tay và các khớp ngón tay",
                color = Muted,
                fontSize = 13.5.sp,
            )

            Spacer(Modifier.height(28.dp))

            if (!isFinished) {
                // VÒNG TRÒN ĐẾM NGƯỢC THƯ GIÃN
                Box(
                    modifier = Modifier.size(250.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val progress = (totalDurationSeconds - secondsRemaining).toFloat() / totalDurationSeconds.toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val radius = diameter / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Vòng nền xám nhẹ
                        drawCircle(
                            color = SurfaceBorder,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth),
                        )

                        // Vòng tiến trình Teal dịu mát
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Teal, TealDark, Teal),
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }

                    // Đồng hồ đếm ngược và nhịp thở
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        val minutes = secondsRemaining / 60
                        val secs = secondsRemaining % 60
                        val timeStr = String.format("%02d:%02d", minutes, secs)

                        Text(
                            timeStr,
                            color = Ink,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isPaused) "ĐANG TẠM DỪNG" else "HÍT THỞ SÂU...",
                            color = if (isPaused) Amber else TealDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Card chỉ dẫn xoa bóp trị liệu
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(TealSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelfImprovement,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Chỉ dẫn massage",
                                color = TealDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                massageTips[currentTipIndex],
                                color = InkSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Các nút điều khiển
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            isPaused = !isPaused
                            // Đồng bộ trạng thái tạm dừng/tiếp tục sang đồng hồ
                            val cmd = if (!isPaused) "RESUME:MASSAGE:$secondsRemaining" else "PAUSE:MASSAGE:$secondsRemaining"
                            val bytes = cmd.toByteArray(Charsets.UTF_8)
                            runCatching {
                                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                                    nodes.forEach { node ->
                                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, bytes)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isPaused) Teal else SurfaceBorder),
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = if (isPaused) TealDark else InkSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isPaused) "Tiếp tục" else "Tạm dừng",
                            color = if (isPaused) TealDark else InkSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = {
                            isFinished = true
                            // Gửi STOP sang đồng hồ khi hoàn thành
                            runCatching {
                                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                                    nodes.forEach { node ->
                                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, "STOP".toByteArray(Charsets.UTF_8))
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Hoàn thành", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // MÀN HÌNH HOÀN TẤT BUỔI MASSAGE
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(26.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val iconBg = when (sessionState) {
                            "RELIABLE" -> TealSoft
                            "PARTIAL" -> AmberSoft
                            else -> CoralSoft
                        }
                        val iconTint = when (sessionState) {
                            "RELIABLE" -> TealDark
                            "PARTIAL" -> Amber
                            else -> Coral
                        }
                        val titleText = when (sessionState) {
                            "RELIABLE" -> "HOÀN THÀNH XUẤT SẮC"
                            "PARTIAL" -> "HOÀN THÀNH MỘT PHẦN"
                            else -> "DỪNG TẬP SỚM"
                        }
                        val headline = when (sessionState) {
                            "RELIABLE" -> "Cơ bắp đã được thư giãn!"
                            "PARTIAL" -> "Đã thư giãn một phần cơ bắp"
                            else -> "Thời gian tập chưa đủ mục tiêu"
                        }
                        val badgeLabel = when (sessionState) {
                            "RELIABLE" -> "● ĐẠT CHUẨN ($accuracyPercentage%)"
                            "PARTIAL" -> "◐ MỘT PHẦN ($accuracyPercentage%)"
                            else -> "○ CẦN CỐ GẮNG ($accuracyPercentage%)"
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Spa,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(38.dp),
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            titleText,
                            color = iconTint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            headline,
                            color = Ink,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(16.dp))

                        // Thẻ tóm tắt 3 chỉ số theo thời gian tập thật
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Canvas)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Thời gian", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("${elapsedSec / 60}m ${elapsedSec % 60}s", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(SurfaceBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hoàn thành", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("$accuracyPercentage%", color = iconTint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(SurfaceBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Chế độ", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("Thả lỏng cơ", color = TealDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Surface(
                            color = iconBg,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                badgeLabel,
                                color = iconTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Nút lưu và đóng
                        Button(
                            onClick = { saveAndComplete() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Lưu kết quả & Xem tiến trình", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Dialog xác nhận hủy
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Dừng bài tập massage?", fontWeight = FontWeight.Bold, color = Ink) },
                text = { Text("Bạn có muốn dừng buổi thả lỏng sớm không? Kết quả hiện tại sẽ không được lưu.", color = InkSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelDialog = false
                            // Gửi STOP sang đồng hồ khi hủy
                            runCatching {
                                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                                    nodes.forEach { node ->
                                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, "STOP".toByteArray(Charsets.UTF_8))
                                    }
                                }
                            }
                            onCancelSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Dừng bài tập", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCancelDialog = false },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Tiếp tục massage", color = Ink)
                    }
                },
                containerColor = White,
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}
