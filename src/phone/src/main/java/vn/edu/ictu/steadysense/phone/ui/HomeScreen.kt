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

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.data.WorkoutScheduleEntity
import vn.edu.ictu.steadysense.phone.data.WorkoutSessionEntity
import java.util.Calendar
import java.util.Locale
import vn.edu.ictu.steadysense.phone.R
import vn.edu.ictu.steadysense.phone.transport.PhoneTransferState

@Composable
fun HomeScreen(
    watchConnected: Boolean,
    selectedHand: String,
    onSelectHand: (String) -> Unit,
    onReconnectWatch: () -> Unit,
    onStartExercise: (exerciseType: String, minutes: Int, sets: Int, reps: Int, restSeconds: Int, scheduleId: String?) -> Unit,
) {
    val context = LocalContext.current
    val allSchedules by PhoneDatabase.get(context).workoutDao().schedulesFlow().collectAsState(initial = emptyList())
    val allSessions by PhoneDatabase.get(context).workoutDao().sessionsFlow().collectAsState(initial = emptyList())

    val todayDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    // Tính ngày hôm nay (1 = T2, ..., 7 = CN)
    val todayCal = Calendar.getInstance()
    val todayDayOfWeekInt = ((todayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1

    val todaySchedules = remember(allSchedules, todayDayOfWeekInt) {
        allSchedules.filter {
            it.repeatType == "DAILY" || it.dayOfWeek == todayDayOfWeekInt || it.dayOfWeek == 0
        }
    }

    val isScheduleCompletedToday: (WorkoutScheduleEntity) -> Boolean = remember(allSessions) {
        { sched ->
            sched.isCompleted ||
                UserPreferences.isScheduleCompleted(context, todayDateStr, sched.id) ||
                allSessions.any { it.scheduleId == sched.id }
        }
    }

    var homeExerciseTab by remember { mutableIntStateOf(0) } // 0: Theo lịch, 1: Tập tự do
    var selectedScheduleIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(todaySchedules, allSessions) {
        if (todaySchedules.isNotEmpty()) {
            val currentSelected = todaySchedules.getOrNull(selectedScheduleIndex)
            if (currentSelected == null || isScheduleCompletedToday(currentSelected)) {
                val firstUncompleted = todaySchedules.indexOfFirst { !isScheduleCompletedToday(it) }
                if (firstUncompleted != -1) {
                    selectedScheduleIndex = firstUncompleted
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        item {
            HomeHeader()
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Thẻ đồng hồ & nút kết nối lại
                WatchStatusCard(
                    watchConnected = watchConnected,
                    onReconnect = onReconnectWatch,
                )

                // 2. BỘ CHỌN 2 TAB: [Theo lịch] và [Tập tự do]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(White)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Tab 0: Theo lịch
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { homeExerciseTab = 0 },
                        color = if (homeExerciseTab == 0) TealSoft else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = if (homeExerciseTab == 0) TealDark else Muted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Theo lịch (${todaySchedules.size})",
                                color = if (homeExerciseTab == 0) TealDark else InkSecondary,
                                fontSize = 13.5.sp,
                                fontWeight = if (homeExerciseTab == 0) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }

                    // Tab 1: Tập tự do
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { homeExerciseTab = 1 },
                        color = if (homeExerciseTab == 1) TealSoft else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = if (homeExerciseTab == 1) TealDark else Muted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Tập tự do",
                                color = if (homeExerciseTab == 1) TealDark else InkSecondary,
                                fontSize = 13.5.sp,
                                fontWeight = if (homeExerciseTab == 1) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                // 3. NỘI DUNG TỪNG TAB
                if (homeExerciseTab == 0) {
                    // TAB 0: BÀI TẬP THEO LỊCH HÔM NAY
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "BÀI TẬP THEO LỊCH HÔM NAY",
                            color = Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                        if (todaySchedules.isNotEmpty()) {
                            val completedCount = todaySchedules.count { isScheduleCompletedToday(it) }
                            Text(
                                "$completedCount/${todaySchedules.size} bài hoàn thành",
                                color = TealDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (todaySchedules.isNotEmpty()) {
                        todaySchedules.forEach { sched ->
                            val isMassage = sched.exerciseName.contains("Massage")
                            val isCompleted = isScheduleCompletedToday(sched)
                            TodayScheduleExerciseCard(
                                schedule = sched,
                                isCompleted = isCompleted,
                                selectedHand = selectedHand,
                                onSelectHand = onSelectHand,
                                onStart = {
                                    if (isMassage) {
                                        onStartExercise("MASSAGE", sched.targetReps, 1, sched.targetReps, 0, sched.id)
                                    } else {
                                        onStartExercise("FLEXION", sched.targetReps, sched.targetSets, sched.targetReps, sched.restSeconds, sched.id)
                                    }
                                },
                            )
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(TealSoft),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FitnessCenter,
                                        contentDescription = null,
                                        tint = TealDark,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Hôm nay chưa có lịch hẹn cố định",
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    fontSize = 15.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Bạn có thể chuyển sang tab Tập tự do để luyện tập ngay!",
                                    color = Muted,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { homeExerciseTab = 1 },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Teal),
                                ) {
                                    Text("Chuyển sang Tập tự do", color = TealDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: LUYỆN TẬP TỰ DO (Không tính vào KPI thống kê)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "LUYỆN TẬP TỰ DO",
                            color = Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        )
                        Surface(
                            color = Canvas,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, SurfaceBorder),
                        ) {
                            Text(
                                "Không tính vào KPI",
                                color = Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    FreePracticeSection(
                        selectedHand = selectedHand,
                        onSelectHand = onSelectHand,
                        onStartFlexion = {
                            onStartExercise("FLEXION", 5, 3, 10, 60, null)
                        },
                        onStartMassage = {
                            onStartExercise("MASSAGE", 5, 1, 5, 0, null)
                        },
                    )
                }

                // 4. Lưu ý an toàn cho người dùng
                SafetyGuidelineCard()
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    val context = LocalContext.current
    val patientName = remember { UserPreferences.getPatientName(context) }
    val greetingName = if (patientName.isNotBlank()) patientName else "Bác An"

    val todayFormatted = remember {
        val cal = Calendar.getInstance()
        val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "THỨ HAI"
            Calendar.TUESDAY -> "THỨ BA"
            Calendar.WEDNESDAY -> "THỨ TƯ"
            Calendar.THURSDAY -> "THỨ NĂM"
            Calendar.FRIDAY -> "THỨ SÁU"
            Calendar.SATURDAY -> "THỨ BẢY"
            Calendar.SUNDAY -> "CHỦ NHẬT"
            else -> "HÔM NAY"
        }
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        "$dayOfWeek · $dayOfMonth THÁNG $month"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(TealSoft.copy(alpha = 0.65f), Canvas),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    todayFormatted,
                    color = TealDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Chào $greetingName",
                    color = Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            // Logo thương hiệu SteadySense AI ở góc phải
            Image(
                painter = painterResource(id = R.drawable.ic_steadysense),
                contentDescription = "SteadySense AI Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, TealSoft, RoundedCornerShape(14.dp)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Hôm nay mình cùng tập luyện nhẹ nhàng và đúng nhịp nhé.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun WatchStatusCard(
    watchConnected: Boolean,
    onReconnect: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (watchConnected) White else Color(0xFFFFF7ED),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (watchConnected) SurfaceBorder else Amber.copy(alpha = 0.5f),
                RoundedCornerShape(22.dp),
            )
            .clickable { onReconnect() },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon Vector trạng thái
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (watchConnected) TealSoft else AmberSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (watchConnected) Icons.Rounded.Watch else Icons.Rounded.SyncProblem,
                    contentDescription = null,
                    tint = if (watchConnected) TealDark else Amber,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (watchConnected) "Đồng hồ Wear OS" else "Chưa thấy đồng hồ",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                val batteryVal = PhoneTransferState.watchBatteryPercent
                val isChecking = PhoneTransferState.isCheckingConnection
                val statusText = when {
                    isChecking -> "Đang kiểm tra kết nối..."
                    watchConnected -> {
                        if (batteryVal in 0..100) "Pin: $batteryVal% · Sẵn sàng" else "Đang kết nối đồng hồ..."
                    }
                    PhoneTransferState.watchDeviceName.contains("Wi-Fi", ignoreCase = true) -> "Chỉ có Wi-Fi (Cần bật Bluetooth)"
                    else -> "Chạm để kết nối lại"
                }
                Text(
                    text = statusText,
                    color = if (watchConnected) Muted else Amber,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }

            if (watchConnected) {
                Surface(
                    color = TealSoft,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onReconnect() },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Làm mới",
                            tint = TealDark,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Đã nối",
                            color = TealDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Button(
                    onClick = onReconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                    enabled = !PhoneTransferState.isCheckingConnection,
                ) {
                    Text(
                        if (PhoneTransferState.isCheckingConnection) "Đang tìm..." else "Nối lại",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayScheduleExerciseCard(
    schedule: WorkoutScheduleEntity,
    isCompleted: Boolean,
    selectedHand: String,
    onSelectHand: (String) -> Unit,
    onStart: () -> Unit,
) {
    val isMassage = schedule.exerciseName.contains("Massage")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCompleted) 1.5.dp else 1.dp,
                color = if (isCompleted) Color(0xFF86EFAC) else SurfaceBorder,
                shape = RoundedCornerShape(24.dp)
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isCompleted) Color(0xFFDCFCE7)
                            else if (isMassage) Color(0xFFFFF7ED)
                            else TealSoft
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Rounded.Check else if (isMassage) Icons.Rounded.Spa else Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = if (isCompleted) Color(0xFF16A34A) else if (isMassage) Coral else TealDark,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = if (isCompleted) Color(0xFFDCFCE7) else Canvas,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (isCompleted) Color(0xFF86EFAC) else SurfaceBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    tint = if (isCompleted) Color(0xFF15803D) else Muted,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = schedule.scheduledTime,
                                    color = if (isCompleted) Color(0xFF15803D) else InkSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (isCompleted) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = "Đã hoàn thành",
                                    color = Color(0xFF15803D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = schedule.exerciseName,
                        color = Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(2.dp))

                    val doseStr = if (isMassage) {
                        "${schedule.targetReps} phút · Rung thả lỏng"
                    } else {
                        "${schedule.targetSets} hiệp · ${schedule.targetReps} lần · Nghỉ ${schedule.restSeconds}s"
                    }
                    Text(
                        text = doseStr,
                        color = Muted,
                        fontSize = 12.5.sp,
                    )
                }
            }

            // Chọn tay nếu là bài gập duỗi
            if (!isMassage) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HandOptionButton(
                        label = "Tay Trái",
                        isSelected = selectedHand == "LEFT",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectHand("LEFT") },
                    )
                    HandOptionButton(
                        label = "Tay Phải",
                        isSelected = selectedHand == "RIGHT",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectHand("RIGHT") },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Nút Bắt đầu / Tập lại
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) TealDark else Teal,
                    contentColor = White,
                ),
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isCompleted) "Tập lại bài này" else "Bắt đầu tập (${schedule.scheduledTime})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FreePracticeSection(
    selectedHand: String,
    onSelectHand: (String) -> Unit,
    onStartFlexion: () -> Unit,
    onStartMassage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Thẻ bài 1: Gấp - duỗi khuỷu tay tự do
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TealSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = TealDark,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gấp – duỗi khuỷu tay",
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Tập tự do · 3 hiệp · 10 lần · Nghỉ 60s",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HandOptionButton(
                        label = "Tay Trái",
                        isSelected = selectedHand == "LEFT",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectHand("LEFT") },
                    )
                    HandOptionButton(
                        label = "Tay Phải",
                        isSelected = selectedHand == "RIGHT",
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectHand("RIGHT") },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onStartFlexion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal,
                        contentColor = White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tập gấp – duỗi ngay",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Thẻ bài 2: Massage rung thư giãn
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = Coral,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Massage thả lỏng rung",
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "5 phút rung chu kỳ · Giúp giảm căng cứng",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onStartMassage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral,
                        contentColor = White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Bắt đầu massage thả lỏng",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HandOptionButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) TealSoft else Canvas)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Teal else SurfaceBorder,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = if (isSelected) TealDark else InkSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
            if (isSelected) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = TealDark,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SafetyGuidelineCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AmberSoft.copy(alpha = 0.6f))
            .border(1.dp, Amber.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Tập vừa sức, giữ nhịp thở đều. Nếu thấy mỏi hoặc chóng mặt, hãy bấm tạm dừng.",
            color = Color(0xFF78350F),
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}
