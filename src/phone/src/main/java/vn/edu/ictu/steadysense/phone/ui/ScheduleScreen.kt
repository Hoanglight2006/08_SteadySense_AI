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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.data.WorkoutScheduleEntity
import vn.edu.ictu.steadysense.phone.data.WorkoutSessionEntity
import vn.edu.ictu.steadysense.phone.util.WorkoutReminderManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun normalizeTo24Hour(timeStr: String): String {
    val trimmed = timeStr.trim()
    val match = Regex("""(\d{1,2}):(\d{2})""").find(trimmed) ?: return trimmed
    var hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    val isPm = trimmed.contains("Chiều", ignoreCase = true) || trimmed.contains("Tối", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)
    val isAm = trimmed.contains("Sáng", ignoreCase = true) || trimmed.contains("AM", ignoreCase = true)
    if (isPm && hour < 12) hour += 12
    if (isAm && hour == 12) hour = 0
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

private enum class DayState {
    RELIABLE,   // ● Đã tập tốt
    PARTIAL,    // ◐ Tập một phần
    NOT_YET,    // ○ Chưa tập
    MISSED,     // ✕ Bỏ lỡ
}

private data class WeekDayInfo(
    val dayLabel: String,      // T2, T3...
    val dateNum: String,       // 7, 8...
    val fullDateStr: String,   // 08/09
    val dayOfWeekInt: Int,     // 1=T2, ..., 7=CN
    val state: DayState,
    val isToday: Boolean,
    val isPast: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onStartWorkout: (exerciseType: String, minutes: Int, sets: Int, reps: Int, restSeconds: Int, scheduleId: String?) -> Unit = { _, _, _, _, _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val schedulesFromDb by PhoneDatabase.get(context).workoutDao().schedulesFlow().collectAsState(initial = emptyList())
    val sessionsFromDb by PhoneDatabase.get(context).workoutDao().sessionsFlow().collectAsState(initial = emptyList())

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var showAddScheduleSheet by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<WorkoutScheduleEntity?>(null) }

    fun refreshData() {
        scope.launch(Dispatchers.IO) {
            val db = PhoneDatabase.get(context).workoutDao()
            var schedules = db.allSchedules()
            if (schedules.isEmpty() && !UserPreferences.hasInitializedDefaultSchedules(context)) {
                // Khởi tạo lịch mặc định ban đầu dạng 24h chuẩn với tiêu chuẩn 3 hiệp x 10 lần, nghỉ 60s
                val defaultSched1 = WorkoutScheduleEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseName = "Gấp – duỗi khuỷu tay",
                    targetReps = 10,
                    scheduledTime = "08:30",
                    dayOfWeek = 0, // 0 = hằng ngày
                    repeatType = "DAILY",
                    isCompleted = false,
                    targetSets = 3,
                    restSeconds = 60,
                )
                val defaultSched2 = WorkoutScheduleEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseName = "Thả lỏng & Massage",
                    targetReps = 5,
                    scheduledTime = "16:00",
                    dayOfWeek = 0,
                    repeatType = "DAILY",
                    isCompleted = false,
                    targetSets = 1,
                    restSeconds = 0,
                )
                db.insertSchedule(defaultSched1)
                db.insertSchedule(defaultSched2)
                UserPreferences.setHasInitializedDefaultSchedules(context, true)
            } else {
                // Chuẩn hoá các lịch cũ nếu còn chứa chữ Sáng / Chiều / Tối về 24h
                schedules.forEach { s ->
                    val norm = normalizeTo24Hour(s.scheduledTime)
                    if (norm != s.scheduledTime) {
                        db.insertSchedule(s.copy(scheduledTime = norm))
                    }
                }
            }
            WorkoutReminderManager.scheduleNextReminder(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val schedulesList = schedulesFromDb
    val sessionsList = sessionsFromDb

    // Tính toán tuần (hỗ trợ chuyển tuần linh hoạt)
    var weekOffset by remember { mutableIntStateOf(0) }
    val todayCal = Calendar.getInstance()
    val todayDayOfWeek = (todayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0 = Mon, ..., 6 = Sun
    LaunchedEffect(todayDayOfWeek) {
        selectedDayIndex = todayDayOfWeek
    }

    val weekDays: List<WeekDayInfo> = remember(sessionsList, schedulesList, weekOffset) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        if (weekOffset != 0) {
            cal.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
        }

        val dayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val firstOpenMillis = vn.edu.ictu.steadysense.phone.data.UserPreferences.getFirstAppOpenTime(context)
        val firstOpenCal = Calendar.getInstance().apply {
            timeInMillis = firstOpenMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        (0..6).map { idx ->
            val curTime = cal.timeInMillis
            val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString()
            val fullDate = dateFormat.format(Date(curTime))
            val isToday = (weekOffset == 0) && (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
            val isPast = if (weekOffset < 0) {
                true
            } else if (weekOffset > 0) {
                false
            } else {
                cal.before(today) && !isToday
            }

            // Tìm các buổi tập trong ngày này
            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            val dayEnd = cal.clone() as Calendar
            dayEnd.set(Calendar.HOUR_OF_DAY, 23)
            dayEnd.set(Calendar.MINUTE, 59)
            dayEnd.set(Calendar.SECOND, 59)

            val sessionsOnDay = sessionsList.filter {
                it.startedAt in dayStart.timeInMillis..dayEnd.timeInMillis
            }

            // Kiểm tra ngày này có lịch tập được giao không
            val dayOfWeekInt = idx + 1 // 1=T2..7=CN
            val dayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(curTime))
            val hasScheduleOnDay = schedulesList.any {
                it.repeatType == "DAILY" ||
                    (it.repeatType == "WEEKLY" && (it.dayOfWeek == dayOfWeekInt || it.dayOfWeek == 0)) ||
                    ((it.repeatType == "ONCE" || it.repeatType == "SPECIFIC_DATE") && it.specificDate == dayDateStr)
            }

            // Ngày trước khi người dùng cài/mở app lần đầu không tính là bỏ lỡ
            val isBeforeInstall = dayEnd.before(firstOpenCal)

            val state = when {
                sessionsOnDay.any { it.sessionState == "RELIABLE" || it.accuracyPercentage >= 80 } -> DayState.RELIABLE
                sessionsOnDay.isNotEmpty() -> DayState.PARTIAL
                isPast && !isBeforeInstall && hasScheduleOnDay -> DayState.MISSED
                else -> DayState.NOT_YET
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
            WeekDayInfo(
                dayLabel = dayLabels[idx],
                dateNum = dayNum,
                fullDateStr = fullDate,
                dayOfWeekInt = dayOfWeekInt,
                state = state,
                isToday = isToday,
                isPast = isPast,
            )
        }
    }

    val completedSessionsThisWeek = weekDays.count { it.state == DayState.RELIABLE || it.state == DayState.PARTIAL }
    var targetSessionsThisWeek by remember { mutableIntStateOf(UserPreferences.getWeeklyTargetSessions(context)) }

    val selectedDayStartCal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        if (weekOffset != 0) {
            add(Calendar.DAY_OF_YEAR, weekOffset * 7)
        }
        add(Calendar.DAY_OF_MONTH, selectedDayIndex)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDayStartCal.time)

    val selectedDayOfWeekInt = selectedDayIndex + 1
    val scheduledExercisesForSelectedDay = schedulesList.filter { sched ->
        when (sched.repeatType) {
            "DAILY" -> true
            "WEEKLY" -> sched.dayOfWeek == selectedDayOfWeekInt || sched.dayOfWeek == 0
            "ONCE", "SPECIFIC_DATE" -> sched.specificDate == selectedDateStr
            else -> sched.dayOfWeek == selectedDayOfWeekInt || sched.dayOfWeek == 0
        }
    }
    val selectedDayEndCal = selectedDayStartCal.clone() as Calendar
    selectedDayEndCal.set(Calendar.HOUR_OF_DAY, 23)
    selectedDayEndCal.set(Calendar.MINUTE, 59)
    selectedDayEndCal.set(Calendar.SECOND, 59)

    val sessionsOnSelectedDay = sessionsList.filter {
        it.startedAt in selectedDayStartCal.timeInMillis..selectedDayEndCal.timeInMillis
    }

    val completedScheduleIds = remember(scheduledExercisesForSelectedDay, sessionsOnSelectedDay, selectedDayIndex, weekOffset) {
        val completedIds = mutableSetOf<String>()

        val selectedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDayStartCal.time)

        // Kiểm tra chính xác 1-1 theo ID bài tập:
        scheduledExercisesForSelectedDay.forEach { sched ->
            val isMarkedInPrefs = UserPreferences.isScheduleCompleted(context, selectedDateStr, sched.id)
            val hasSessionMatch = sessionsOnSelectedDay.any { it.scheduleId == sched.id }
            if (sched.isCompleted || isMarkedInPrefs || hasSessionMatch) {
                completedIds.add(sched.id)
            }
        }

        completedIds
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        item {
            ScheduleHeader(
                onAddClick = { showAddScheduleSheet = true },
            )
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WeekCalendarCard(
                    weekDays = weekDays,
                    selectedIndex = selectedDayIndex,
                    weekOffset = weekOffset,
                    onSelect = { selectedDayIndex = it },
                    onWeekOffsetChange = { newOffset ->
                        weekOffset = newOffset
                        if (newOffset == 0) {
                            selectedDayIndex = todayDayOfWeek
                        }
                    },
                    onGoToToday = {
                        weekOffset = 0
                        selectedDayIndex = todayDayOfWeek
                    },
                )

                WeekSummaryBar(
                    completed = completedSessionsThisWeek,
                    currentTarget = targetSessionsThisWeek,
                    onSelectTarget = { newTarget ->
                        targetSessionsThisWeek = newTarget
                        UserPreferences.setWeeklyTargetSessions(context, newTarget)
                    },
                )

                val selectedInfo = weekDays.getOrNull(selectedDayIndex)
                val isSelectedDayToday = selectedInfo?.isToday == true
                val isSelectedDayPast = selectedInfo?.isPast == true

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "BÀI TẬP TRONG NGÀY",
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    val dayTitle = if (selectedInfo != null) {
                        if (selectedInfo.isToday) "Hôm nay · ${selectedInfo.dayLabel}, ${selectedInfo.fullDateStr}"
                        else "${selectedInfo.dayLabel}, ${selectedInfo.fullDateStr}"
                    } else "Hôm nay"
                    Text(
                        dayTitle,
                        color = TealDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (!isSelectedDayToday) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isSelectedDayPast) "Bạn đang xem lịch ngày cũ. Chỉ có thể bắt đầu tập bài của ngày hôm nay." else "Bạn đang xem lịch ngày tiếp theo. Chỉ có thể bắt đầu tập bài của ngày hôm nay.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }

                if (scheduledExercisesForSelectedDay.isNotEmpty()) {
                    scheduledExercisesForSelectedDay.forEach { sched ->
                        val isMassage = sched.exerciseName.contains("Massage")
                        val isCompletedToday = completedScheduleIds.contains(sched.id)

                        ScheduledExerciseItem(
                            time = normalizeTo24Hour(sched.scheduledTime),
                            title = sched.exerciseName,
                            dose = if (isMassage) "${sched.targetReps} phút thả lỏng" else "${sched.targetSets} hiệp · ${sched.targetReps} lần/hiệp · Nghỉ ${sched.restSeconds}s",
                            icon = if (isMassage) Icons.Rounded.Spa else Icons.Rounded.FitnessCenter,
                            isCompleted = isCompletedToday,
                            canStart = isSelectedDayToday,
                            isPast = isSelectedDayPast,
                            onStart = {
                                if (isSelectedDayToday) {
                                    if (isMassage) {
                                        onStartWorkout("MASSAGE", sched.targetReps, 1, sched.targetReps, 0, sched.id)
                                    } else {
                                        onStartWorkout("FLEXION", sched.targetReps, sched.targetSets, sched.targetReps, sched.restSeconds, sched.id)
                                    }
                                } else {
                                    val msg = if (isSelectedDayPast) "Bài tập này thuộc ngày cũ, chỉ được tập bài của ngày hiện tại." else "Chưa tới ngày tập bài này, chỉ được tập bài của ngày hiện tại."
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                scheduleToDelete = sched
                            },
                        )
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            "Không có lịch tập nào trong ngày này.",
                            color = Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }

    // Hộp thoại xác nhận xóa bài tập
    if (scheduleToDelete != null) {
        val sched = scheduleToDelete!!
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = {
                Text("Xóa bài tập khỏi lịch", fontWeight = FontWeight.Bold, color = Ink)
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn xóa bài tập \"${sched.exerciseName}\" lúc ${normalizeTo24Hour(sched.scheduledTime)} khỏi lịch tập?",
                    color = InkSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = sched.id
                        scheduleToDelete = null
                        scope.launch(Dispatchers.IO) {
                            PhoneDatabase.get(context).workoutDao().deleteSchedule(idToDelete)
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Xóa", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { scheduleToDelete = null },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Hủy", color = Muted)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(20.dp),
        )
    }

    // Modal BottomSheet Thêm buổi tập mới
    if (showAddScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddScheduleSheet = false },
            containerColor = White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            AddScheduleSheetContent(
                initialDateStr = selectedDateStr,
                onDismiss = { showAddScheduleSheet = false },
                onSave = { newSchedules ->
                    scope.launch(Dispatchers.IO) {
                        val dao = PhoneDatabase.get(context).workoutDao()
                        newSchedules.forEach { s ->
                            dao.insertSchedule(s)
                        }
                        refreshData()
                        withContext(Dispatchers.Main) {
                            showAddScheduleSheet = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ScheduleHeader(
    onAddClick: () -> Unit,
) {
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
                    "KẾ HOẠCH TẬP LUYỆN",
                    color = TealDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lịch tập phục hồi",
                    color = Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Surface(
                color = Teal,
                shape = CircleShape,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAddClick() },
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Thêm buổi tập",
                        tint = White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Duy trì tập đều đặn mỗi ngày để cơ tay linh hoạt hơn.",
            color = Muted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun WeekSummaryBar(
    completed: Int,
    currentTarget: Int,
    onSelectTarget: (Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "MỤC TIÊU & TIẾN ĐỘ TUẦN",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$completed",
                            color = TealDark,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "/$currentTarget buổi hoàn thành",
                            color = InkSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }

                Surface(
                    color = if (completed >= currentTarget) Color(0xFFDCFCE7) else TealSoft,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (completed >= currentTarget) Color(0xFF15803D) else TealDark,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        val pct = if (currentTarget > 0) ((completed.toFloat() / currentTarget) * 100).toInt() else 0
                        Text(
                            text = "$pct% chỉ tiêu",
                            color = if (completed >= currentTarget) Color(0xFF15803D) else TealDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Hàng chọn mục tiêu nhanh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Đổi mục tiêu tuần:",
                    color = InkSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Tự động tính cho tháng (×4)",
                    color = Muted,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(3, 5, 7, 10, 14).forEach { target ->
                    val isSelected = currentTarget == target
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Teal else Canvas)
                            .border(1.dp, if (isSelected) Teal else SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { onSelectTarget(target) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$target",
                            color = if (isSelected) White else Ink,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekCalendarCard(
    weekDays: List<WeekDayInfo>,
    selectedIndex: Int,
    weekOffset: Int,
    onSelect: (Int) -> Unit,
    onWeekOffsetChange: (Int) -> Unit,
    onGoToToday: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Thanh chuyển tuần điều hướng phong cách WeCare
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val weekTitle = when (weekOffset) {
                        0 -> "Tuần này"
                        -1 -> "Tuần trước"
                        1 -> "Tuần tới"
                        else -> if (weekOffset < 0) "${-weekOffset} tuần trước" else "${weekOffset} tuần tới"
                    }
                    val firstDayStr = weekDays.firstOrNull()?.fullDateStr ?: ""
                    val lastDayStr = weekDays.lastOrNull()?.fullDateStr ?: ""

                    Text(
                        text = weekTitle,
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "($firstDayStr – $lastDayStr)",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (weekOffset != 0) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onGoToToday() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Today,
                                    contentDescription = null,
                                    tint = TealDark,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Hôm nay",
                                    color = TealDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Surface(
                        color = Canvas,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onWeekOffsetChange(weekOffset - 1) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Tuần trước",
                                tint = InkSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Surface(
                        color = Canvas,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onWeekOffsetChange(weekOffset + 1) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Tuần tiếp",
                                tint = InkSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // Hàng 7 ngày trong tuần
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                weekDays.forEachIndexed { index, day ->
                    val isSelected = index == selectedIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(index) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            day.dayLabel,
                            color = if (isSelected) TealDark else Muted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Teal else Canvas)
                                .border(
                                    width = if (day.isToday && !isSelected) 1.5.dp else 0.dp,
                                    color = if (day.isToday && !isSelected) Teal else Color.Transparent,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.dateNum,
                                color = if (isSelected) White else Ink,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Chấm trạng thái WeCare:
                        // RELIABLE (● mint), PARTIAL (◐ vàng), NOT_YET (○ xám), MISSED (✕ coral)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (day.state) {
                                        DayState.RELIABLE -> Teal
                                        DayState.PARTIAL -> Amber
                                        DayState.MISSED -> Coral
                                        DayState.NOT_YET -> SurfaceBorder
                                    }
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dòng chú giải trạng thái
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Teal))
                    Spacer(Modifier.width(4.dp))
                    Text("Đạt chuẩn", color = Muted, fontSize = 10.5.sp)
                }
                Spacer(Modifier.width(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Amber))
                    Spacer(Modifier.width(4.dp))
                    Text("Một phần", color = Muted, fontSize = 10.5.sp)
                }
                Spacer(Modifier.width(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Coral))
                    Spacer(Modifier.width(4.dp))
                    Text("Bỏ lỡ", color = Muted, fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ScheduledExerciseItem(
    time: String,
    title: String,
    dose: String,
    icon: ImageVector,
    isCompleted: Boolean,
    canStart: Boolean = true,
    isPast: Boolean = false,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    val isMassage = title.contains("Massage")
    val iconBg = when {
        isCompleted -> Color(0xFFDCFCE7)
        isMassage -> Color(0xFFFFF7ED)
        else -> TealSoft
    }
    val iconTint = when {
        isCompleted -> Color(0xFF16A34A)
        isMassage -> Coral
        else -> TealDark
    }
    val iconVector = when {
        isCompleted -> Icons.Rounded.Check
        isMassage -> Icons.Rounded.Spa
        else -> Icons.Rounded.FitnessCenter
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCompleted) 1.5.dp else 1.dp,
                color = if (isCompleted) Color(0xFF86EFAC) else SurfaceBorder,
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(time, color = TealDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    if (isCompleted) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                "✓ Đã tập",
                                color = Color(0xFF15803D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(dose, color = Muted, fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCompleted) {
                    if (canStart) {
                        OutlinedButton(
                            onClick = onStart,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Teal),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Tập lại",
                                color = TealDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    if (canStart) {
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = Teal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Bắt đầu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                    } else {
                        val lockText = if (isPast) "Đã qua ngày" else "Chưa tới ngày"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { onStart() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = lockText,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Xóa bài tập",
                        tint = Coral.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddScheduleSheetContent(
    initialDateStr: String? = null,
    onDismiss: () -> Unit,
    onSave: (List<WorkoutScheduleEntity>) -> Unit,
) {
    val context = LocalContext.current
    var selectedExercise by remember { mutableStateOf("Gấp – duỗi khuỷu tay") }
    var setCount by remember { mutableIntStateOf(3) }
    var repCount by remember { mutableIntStateOf(10) }
    var massageMinutes by remember { mutableIntStateOf(5) }
    var restSeconds by remember { mutableIntStateOf(60) }
    var selectedTime by remember { mutableStateOf("08:30") }
    var repeatMode by remember { mutableStateOf(if (initialDateStr != null) "SPECIFIC_DATE" else "DAILY") } // "DAILY", "WEEKLY", "SPECIFIC_DATE"
    var selectedDaysOfWeek by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // 1=T2..7=CN
    var selectedSpecificDate by remember {
        mutableStateOf(initialDateStr ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    val isMassage = selectedExercise == "Thả lỏng & Massage"

    fun showCustomTimePicker() {
        val cal = Calendar.getInstance()
        val curHour = selectedTime.substringBefore(":").toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
        val curMinute = selectedTime.substringAfter(":").take(2).toIntOrNull() ?: cal.get(Calendar.MINUTE)
        val picker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            },
            curHour,
            curMinute,
            true, // Chế độ 24 giờ chuẩn
        )
        picker.show()
    }

    fun showCustomDatePicker() {
        val cal = Calendar.getInstance()
        val parts = selectedSpecificDate.split("-")
        val curYear = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val curMonth = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        val curDay = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedSpecificDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            curYear,
            curMonth,
            curDay,
        )
        picker.show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 60.dp),
    ) {
        Text(
            "Thêm buổi tập mới",
            color = Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Thiết lập lịch tập luyện phục hồi chức năng tự động (định dạng 24h)",
            color = Muted,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(18.dp))

        // 1. Chọn bài tập
        Text("Bài tập", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Gấp – duỗi khuỷu tay", "Thả lỏng & Massage").forEach { name ->
                val isSelected = selectedExercise == name
                Surface(
                    color = if (isSelected) TealSoft else Canvas,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(1.dp, if (isSelected) Teal else SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedExercise = name }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        name,
                        color = if (isSelected) TealDark else InkSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Định lượng: Nếu Massage -> Thời gian (phút); Nếu Gập tay -> Số hiệp (Sets) & Số lần mỗi hiệp (Reps)
        if (isMassage) {
            Text("Thời gian massage (Phút)", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15).forEach { mins ->
                    val isSelected = massageMinutes == mins
                    Surface(
                        color = if (isSelected) TealSoft else Canvas,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(1.dp, if (isSelected) Teal else SurfaceBorder, RoundedCornerShape(12.dp))
                            .clickable { massageMinutes = mins }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "$mins phút",
                            color = if (isSelected) TealDark else InkSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Số hiệp (Sets)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Số hiệp tập (Sets)", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Canvas)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (setCount > 1) setCount-- },
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text("−", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Text(
                            "$setCount hiệp",
                            color = Ink,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        OutlinedButton(
                            onClick = { if (setCount < 10) setCount++ },
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text("+", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                }

                // 2. Số lần mỗi hiệp (Reps)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Số lần / hiệp", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Canvas)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        OutlinedButton(
                            onClick = { if (repCount > 5) repCount-- },
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text("−", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        Text(
                            "$repCount lần",
                            color = Ink,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        OutlinedButton(
                            onClick = { if (repCount < 25) repCount++ },
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text("+", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Thẻ tổng khối lượng tập
            Surface(
                color = TealSoft,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Tổng mục tiêu: ${setCount * repCount} lần gập ($setCount hiệp × $repCount lần/hiệp)",
                    color = TealDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Thời gian nghỉ giữa các hiệp", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(30 to "30s", 45 to "45s", 60 to "60s (Chuẩn)", 90 to "90s", 120 to "2p").forEach { (sec, label) ->
                    val isRestSelected = restSeconds == sec
                    Surface(
                        color = if (isRestSelected) Teal else Canvas,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .border(1.dp, if (isRestSelected) Teal else SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { restSeconds = sec }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text(
                            label,
                            color = if (isRestSelected) White else InkSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isRestSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Khung giờ tập (chuẩn hoá 24h)
        Text("Khung giờ tập (24h)", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // Thẻ đồng hồ số to, trực quan, chạm vào là đổi giờ
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TealSoft.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Teal, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { showCustomTimePicker() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = "Đồng hồ",
                            tint = TealDark,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Giờ bắt đầu tập",
                            color = Muted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            selectedTime,
                            color = TealDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Surface(
                    color = Teal,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Đổi giờ",
                            color = White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Hoặc chọn nhanh khung giờ gợi ý:",
            color = Muted,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("07:30", "08:30", "10:00", "14:30", "16:00", "19:30", "21:00").forEach { preset ->
                val isSelected = selectedTime == preset
                Surface(
                    color = if (isSelected) Teal else Canvas,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, if (isSelected) Teal else SurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedTime = preset }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        preset,
                        color = if (isSelected) White else Ink,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4. Chế độ Lặp lại: Hằng ngày vs Chọn ngày trong tuần vs Ngày cụ thể
        Text("Lặp lại lịch tập", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "DAILY" to "Hằng ngày",
                "WEEKLY" to "Theo thứ",
                "SPECIFIC_DATE" to "Ngày cụ thể",
            ).forEach { (mode, label) ->
                val isModeSelected = repeatMode == mode
                Surface(
                    color = if (isModeSelected) TealSoft else Canvas,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, if (isModeSelected) Teal else SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { repeatMode = mode }
                        .padding(vertical = 9.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            color = if (isModeSelected) TealDark else InkSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Nếu chọn "Theo thứ": hiển thị 7 nút chọn ngày T2..CN
        if (repeatMode == "WEEKLY") {
            Spacer(Modifier.height(10.dp))
            val daysList = listOf("T2" to 1, "T3" to 2, "T4" to 3, "T5" to 4, "T6" to 5, "T7" to 6, "CN" to 7)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                daysList.forEach { (label, dayNum) ->
                    val isDaySelected = selectedDaysOfWeek.contains(dayNum)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDaySelected) Teal else Canvas)
                            .border(1.dp, if (isDaySelected) Teal else SurfaceBorder, CircleShape)
                            .clickable {
                                selectedDaysOfWeek = if (isDaySelected) {
                                    if (selectedDaysOfWeek.size > 1) selectedDaysOfWeek - dayNum else selectedDaysOfWeek
                                } else {
                                    selectedDaysOfWeek + dayNum
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (isDaySelected) White else InkSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Nếu chọn "Ngày cụ thể": hiển thị Card chọn ngày lịch
        if (repeatMode == "SPECIFIC_DATE") {
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealSoft.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Teal, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showCustomDatePicker() },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Lịch",
                                tint = TealDark,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Ngày tập cụ thể",
                                color = Muted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            val displayFormatted = try {
                                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedSpecificDate)
                                val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
                                sdf.format(d ?: Date()).replaceFirstChar { it.uppercase() }
                            } catch (e: Exception) {
                                selectedSpecificDate
                            }
                            Text(
                                displayFormatted,
                                color = TealDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }

                    Surface(
                        color = Teal,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Chọn lịch",
                                color = White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowStr = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val afterTomorrowStr = sdf.format(cal.time)

                listOf(
                    "Hôm nay" to todayStr,
                    "Ngày mai" to tomorrowStr,
                    "Ngày kia" to afterTomorrowStr,
                ).forEach { (label, dateVal) ->
                    val isChipSelected = selectedSpecificDate == dateVal
                    Surface(
                        color = if (isChipSelected) Teal else Canvas,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, if (isChipSelected) Teal else SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedSpecificDate = dateVal }
                            .padding(vertical = 7.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                label,
                                color = if (isChipSelected) White else InkSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Nút Hủy & Lưu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Hủy", color = Muted, fontSize = 14.sp)
            }
            Button(
                onClick = {
                    val normalizedTimeStr = normalizeTo24Hour(selectedTime)
                    val items = when (repeatMode) {
                        "DAILY" -> listOf(
                            WorkoutScheduleEntity(
                                id = UUID.randomUUID().toString(),
                                exerciseName = selectedExercise,
                                targetReps = if (isMassage) massageMinutes else repCount,
                                scheduledTime = normalizedTimeStr,
                                dayOfWeek = 0,
                                repeatType = "DAILY",
                                isCompleted = false,
                                targetSets = if (isMassage) 1 else setCount,
                                restSeconds = if (isMassage) 0 else restSeconds,
                                specificDate = null,
                            ),
                        )
                        "WEEKLY" -> selectedDaysOfWeek.map { dayInt ->
                            WorkoutScheduleEntity(
                                id = UUID.randomUUID().toString(),
                                exerciseName = selectedExercise,
                                targetReps = if (isMassage) massageMinutes else repCount,
                                scheduledTime = normalizedTimeStr,
                                dayOfWeek = dayInt,
                                repeatType = "WEEKLY",
                                isCompleted = false,
                                targetSets = if (isMassage) 1 else setCount,
                                restSeconds = if (isMassage) 0 else restSeconds,
                                specificDate = null,
                            )
                        }
                        else -> { // "SPECIFIC_DATE"
                            val cal = Calendar.getInstance()
                            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedSpecificDate)
                            if (d != null) cal.time = d
                            val dayOfWeekInt = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1 // 1=T2..7=CN
                            listOf(
                                WorkoutScheduleEntity(
                                    id = UUID.randomUUID().toString(),
                                    exerciseName = selectedExercise,
                                    targetReps = if (isMassage) massageMinutes else repCount,
                                    scheduledTime = normalizedTimeStr,
                                    dayOfWeek = dayOfWeekInt,
                                    repeatType = "SPECIFIC_DATE",
                                    isCompleted = false,
                                    targetSets = if (isMassage) 1 else setCount,
                                    restSeconds = if (isMassage) 0 else restSeconds,
                                    specificDate = selectedSpecificDate,
                                ),
                            )
                        }
                    }
                    onSave(items)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) {
                Text("Lưu lịch", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
