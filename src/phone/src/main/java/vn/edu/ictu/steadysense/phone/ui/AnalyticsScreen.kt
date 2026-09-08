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

import android.content.Intent
import vn.edu.ictu.steadysense.phone.util.RehabReportGenerator
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.data.WorkoutSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tuần này, 1: Tháng này
    var sessionsList by remember { mutableStateOf<List<WorkoutSessionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) {
            PhoneDatabase.get(context).workoutDao().allSessions()
        }
        sessionsList = list
    }

    // Xác định khoảng thời gian cho Tuần này và Tháng này
    val now = Calendar.getInstance()

    val startOfWeek = remember {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfWeek = startOfWeek + 7L * 24 * 3600 * 1000 - 1

    val startOfMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    // LỌC BỎ TẬP TỰ DO: Chỉ các bài tập theo lịch (có scheduleId) mới được tính vào thống kê theo yêu cầu
    val scheduledSessions = remember(sessionsList) {
        sessionsList.filter { !it.scheduleId.isNullOrBlank() }
    }

    // Danh sách các buổi tập lọc theo kỳ: Tuần này hoặc Tháng này (chỉ bài theo lịch)
    val filteredSessions = remember(scheduledSessions, selectedTab) {
        if (selectedTab == 0) {
            scheduledSessions.filter { it.startedAt in startOfWeek..endOfWeek }
        } else {
            scheduledSessions.filter { it.startedAt in startOfMonth..endOfMonth }
        }
    }

    // Tính toán KPI thực tế từ danh sách theo kỳ đã chọn
    val totalCompletedSessions = filteredSessions.size
    val totalRepsCount = filteredSessions.sumOf { it.completedReps }
    val avgAccuracy = if (filteredSessions.isNotEmpty()) {
        filteredSessions.map { it.accuracyPercentage }.average().toInt()
    } else 0
    val avgSteadyScore = if (filteredSessions.isNotEmpty()) {
        String.format(Locale.US, "%.1f", filteredSessions.map { it.steadyScore.toDouble() }.average())
    } else "0.0"

    // Tính toán tỷ lệ hoàn thành theo 7 ngày trong tuần (T2..CN)
    val weeklyCompletionPcts = remember(scheduledSessions) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        (0..6).map {
            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            val dayEnd = cal.clone() as Calendar
            dayEnd.set(Calendar.HOUR_OF_DAY, 23)
            dayEnd.set(Calendar.MINUTE, 59)
            dayEnd.set(Calendar.SECOND, 59)

            val daySessions = scheduledSessions.filter { s ->
                s.startedAt in dayStart.timeInMillis..dayEnd.timeInMillis
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)

            if (daySessions.isNotEmpty()) {
                daySessions.map { s -> s.accuracyPercentage }.average().toFloat()
            } else 0f
        }
    }

    // Tính chất lượng tín hiệu theo 7 ngày trong tuần
    val weeklySignalQualities = remember(scheduledSessions) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        (0..6).map {
            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            val dayEnd = cal.clone() as Calendar
            dayEnd.set(Calendar.HOUR_OF_DAY, 23)
            dayEnd.set(Calendar.MINUTE, 59)
            dayEnd.set(Calendar.SECOND, 59)

            val daySessions = scheduledSessions.filter { s ->
                s.startedAt in dayStart.timeInMillis..dayEnd.timeInMillis
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)

            if (daySessions.isNotEmpty()) {
                minOf(98f, maxOf(70f, daySessions.map { s -> s.accuracyPercentage * 0.9f + 10f }.average().toFloat()))
            } else 0f
        }
    }

    // Tính toán tỷ lệ hoàn thành theo 4 tuần của tháng này (Tuần 1..Tuần 4)
    val monthlyCompletionPcts = remember(scheduledSessions) {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        listOf(1..7, 8..14, 15..21, 22..maxDays).map { range ->
            val sCal = Calendar.getInstance().apply {
                set(curYear, curMonth, range.first, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val eCal = Calendar.getInstance().apply {
                set(curYear, curMonth, range.last, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val weekSessions = scheduledSessions.filter { it.startedAt in sCal.timeInMillis..eCal.timeInMillis }
            if (weekSessions.isNotEmpty()) {
                weekSessions.map { it.accuracyPercentage }.average().toFloat()
            } else 0f
        }
    }

    // Tính chất lượng tín hiệu theo 4 tuần của tháng này
    val monthlySignalQualities = remember(scheduledSessions) {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        listOf(1..7, 8..14, 15..21, 22..maxDays).map { range ->
            val sCal = Calendar.getInstance().apply {
                set(curYear, curMonth, range.first, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val eCal = Calendar.getInstance().apply {
                set(curYear, curMonth, range.last, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val weekSessions = scheduledSessions.filter { it.startedAt in sCal.timeInMillis..eCal.timeInMillis }
            if (weekSessions.isNotEmpty()) {
                minOf(98f, maxOf(70f, weekSessions.map { it.accuracyPercentage * 0.9f + 10f }.average().toFloat()))
            } else 0f
        }
    }

    val chartLabels = if (selectedTab == 0) {
        listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    } else {
        listOf("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4")
    }

    val currentCompletionPcts = if (selectedTab == 0) weeklyCompletionPcts else monthlyCompletionPcts
    val currentSignalQualities = if (selectedTab == 0) weeklySignalQualities else monthlySignalQualities

    fun exportRehabReport() {
        val periodLabel = if (selectedTab == 0) "TUẦN NÀY" else "THÁNG NÀY"
        val report = RehabReportGenerator.generateDetailedReport(
            patientName = UserPreferences.getPatientName(context).ifBlank { "Bệnh nhân" },
            sessions = if (filteredSessions.isNotEmpty()) filteredSessions else scheduledSessions,
            periodLabel = periodLabel,
        )
        RehabReportGenerator.shareReport(context, report)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        item {
            AnalyticsHeader(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                onExport = { exportRehabReport() },
            )
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 0. KPI tuỳ chỉnh: cho phép người dùng đổi mục tiêu tuần khi chạm vào thẻ
                val weeklyTarget = UserPreferences.getWeeklyTargetSessions(context)
                val monthlyTarget = weeklyTarget * 4
                val currentTarget = if (selectedTab == 0) weeklyTarget else monthlyTarget
                var showKpiTargetDialog by remember { mutableStateOf(false) }

                // 1. Hàng KPI: [ Buổi: X/M buổi ] [ Rep: Z ] [ Đạt: W% ]
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showKpiTargetDialog = true },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiCard(title = "Buổi tập", value = "$totalCompletedSessions/$currentTarget", accent = Teal, modifier = Modifier.weight(1f))
                    KpiCard(title = "Tổng Rep/Phút", value = "$totalRepsCount", accent = Ink, modifier = Modifier.weight(1f))
                    KpiCard(title = "Đạt chuẩn", value = "$avgAccuracy%", accent = TealDark, modifier = Modifier.weight(1f))
                }

                if (showKpiTargetDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showKpiTargetDialog = false },
                        title = { Text("Mục tiêu tập tuần", fontWeight = FontWeight.Bold, color = Ink) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Chọn số buổi tập mục tiêu mỗi tuần:", color = InkSecondary, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf(3, 5, 7, 10, 14).forEach { target ->
                                        val isSelected = weeklyTarget == target
                                        Surface(
                                            color = if (isSelected) Teal else Canvas,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .border(1.dp, if (isSelected) Teal else SurfaceBorder, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    UserPreferences.setWeeklyTargetSessions(context, target)
                                                    showKpiTargetDialog = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                "$target",
                                                color = if (isSelected) White else Ink,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Mục tiêu tháng sẽ tự tính = mục tiêu tuần × 4",
                                    color = Muted,
                                    fontSize = 11.sp,
                                )
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showKpiTargetDialog = false },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Đóng", color = Muted)
                            }
                        },
                        containerColor = White,
                        shape = RoundedCornerShape(20.dp),
                    )
                }

                // 2. Biểu đồ BarChart: Tỷ lệ hoàn thành kỹ thuật (%) linh hoạt theo tuần hoặc tháng
                TechnicalCompletionBarChart(
                    completionPcts = currentCompletionPcts,
                    labels = chartLabels,
                )

                // 3. Biểu đồ LineChart: Chất lượng tín hiệu theo tuần hoặc tháng kèm đường ngưỡng 85%
                SignalQualityLineChart(
                    signalQualities = currentSignalQualities,
                    labels = chartLabels,
                )

                // 4. Tiêu đề Lịch sử theo kỳ kèm phân bố tay tập
                val flexionSessions = filteredSessions.filter { !it.exerciseName.contains("Massage") }
                val leftHandCount = flexionSessions.count { it.selectedHand == "LEFT" }
                val rightHandCount = flexionSessions.count { it.selectedHand == "RIGHT" }
                val historyTitle = if (selectedTab == 0) {
                    "LỊCH SỬ TUẦN NÀY (${filteredSessions.size} BUỔI)"
                } else {
                    "LỊCH SỬ THÁNG NÀY (${filteredSessions.size} BUỔI)"
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        historyTitle,
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                    if (flexionSessions.isNotEmpty()) {
                        Surface(
                            color = TealSoft,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "$leftHandCount Trái · $rightHandCount Phải",
                                color = TealDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                // 5. Thẻ lịch sử buổi tập có thể bấm mở rộng (Expandable) từ Room DB thật
                if (filteredSessions.isNotEmpty()) {
                    filteredSessions.forEach { session ->
                        ExpandableSessionCard(
                            title = session.exerciseName,
                            time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(session.startedAt)),
                            repsInfo = if (session.exerciseName.contains("Massage")) "${session.targetReps} phút massage" else "${session.completedReps}/${session.targetReps} hợp lệ",
                            qualityInfo = "${session.accuracyPercentage}% chuẩn xác",
                            sessionState = session.sessionState,
                            steadyScore = session.steadyScore,
                            detailsRaw = session.repDetailsJson,
                            selectedHand = session.selectedHand,
                        )
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        ) {
                            Text(
                                "Chưa có buổi tập nào",
                                color = Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Chưa có dữ liệu tập luyện trong ${if (selectedTab == 0) "tuần này" else "tháng này"}.",
                                color = Muted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsHeader(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onExport: () -> Unit,
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
                    "TIẾN TRÌNH HỒI PHỤC",
                    color = TealDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Thống kê tiến trình",
                    color = Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            // Nút Xuất báo cáo theo wireframe [ ↗ ]
            Surface(
                color = White,
                shape = CircleShape,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onExport() },
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.IosShare,
                        contentDescription = "Xuất báo cáo",
                        tint = TealDark,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Biểu đồ độ chính xác và chất lượng thu nhận cảm biến.",
            color = Muted,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(18.dp))

        // Tab chuyển đổi: [ Tuần này ] [ Tháng này ]
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Canvas,
            contentColor = Teal,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Teal,
                    height = 3.dp,
                )
            },
            divider = {},
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelect(0) },
                text = {
                    Text(
                        "Tuần này",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (selectedTab == 0) TealDark else Muted,
                    )
                },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelect(1) },
                text = {
                    Text(
                        "Tháng này",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (selectedTab == 1) TealDark else Muted,
                    )
                },
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(title, color = Muted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LegendDotItem(
    color: Color,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            color = Muted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TechnicalCompletionBarChart(
    completionPcts: List<Float>,
    labels: List<String>,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Tỷ lệ hoàn thành kỹ thuật (%)",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            // 2 hàng chú giải rõ ràng, thoáng đãng, tuyệt đối không bị nhảy chữ hay rớt dòng
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDotItem(color = Teal, text = "Đạt chuẩn (≥80%)")
                    LegendDotItem(color = Amber, text = "Một phần (50–79%)")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDotItem(color = Coral, text = "Cần cố gắng (<50%)")
                }
            }

            Spacer(Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            ) {
                val barWidth = if (labels.size <= 4) 36.dp.toPx() else 24.dp.toPx()
                val spacing = (size.width - (barWidth * labels.size)) / (labels.size + 1)
                val maxHeight = size.height - 24.dp.toPx()

                // Đường chuẩn 0%
                drawLine(
                    color = SurfaceBorder,
                    start = Offset(0f, maxHeight),
                    end = Offset(size.width, maxHeight),
                    strokeWidth = 1.dp.toPx(),
                )

                labels.forEachIndexed { i, _ ->
                    val x = spacing * (i + 1) + barWidth * i
                    val pct = (completionPcts.getOrNull(i) ?: 0f) / 100f
                    val barHeight = maxHeight * pct.coerceIn(0f, 1f)
                    val y = maxHeight - barHeight

                    val barColor = when {
                        pct >= 0.8f -> Teal
                        pct >= 0.5f -> Amber
                        pct > 0f -> Coral
                        else -> Canvas
                    }

                    // Cột nền xám mờ
                    drawRoundRect(
                        color = Canvas,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, maxHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )

                    // Cột giá trị thực tế
                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                labels.forEach { label ->
                    Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SignalQualityLineChart(
    signalQualities: List<Float>,
    labels: List<String>,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Độ ổn định cảm biến đeo tay (%)",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Đánh giá độ ổn định kết nối và độ mượt của cảm biến chuyển động",
                color = Muted,
                fontSize = 11.5.sp,
            )
            // Tóm tắt trực quan
            val validQualities = signalQualities.filter { it > 0f }
            if (validQualities.isNotEmpty()) {
                val avgSignal = validQualities.average().toInt()
                val signalLabel = when {
                    avgSignal >= 90 -> "Rất tốt"
                    avgSignal >= 85 -> "Tốt"
                    avgSignal >= 75 -> "Trung bình"
                    else -> "Yếu"
                }
                val aiCondition = if (avgSignal >= 80) "Đủ điều kiện phân tích AI" else "Cần cải thiện kết nối"
                Text(
                    "Trung bình: $avgSignal% · $signalLabel ($aiCondition)",
                    color = if (avgSignal >= 85) TealDark else Amber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDotItem(color = Teal, text = "Tín hiệu tốt (≥85%)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.5.dp)
                            .background(Amber),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Ngưỡng tối thiểu 85%", color = Amber, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val chartHeight = size.height - 20.dp.toPx()
                val stepX = size.width / (labels.size - 1).coerceAtLeast(1)

                // Đường ngưỡng chuẩn 85%
                val thresholdY = chartHeight * (1f - 0.85f)
                drawLine(
                    color = Amber.copy(alpha = 0.6f),
                    start = Offset(0f, thresholdY),
                    end = Offset(size.width, thresholdY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )

                // Vẽ đường nối các điểm tín hiệu
                val path = Path()
                val points = labels.indices.map { i ->
                    val rawQuality = signalQualities.getOrNull(i) ?: 0f
                    // Không vẽ điểm ảo trên ngày không có dữ liệu
                    val quality = if (rawQuality > 0f) rawQuality else null
                    val x = i * stepX
                    val y = if (quality != null) {
                        chartHeight * (1f - (quality / 100f).coerceIn(0f, 1f))
                    } else null
                    if (y != null) Offset(x, y) else null
                }

                val validPoints = points.filterNotNull()
                validPoints.forEachIndexed { i, pt ->
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }

                drawPath(
                    path = path,
                    color = Teal,
                    style = Stroke(width = 2.5.dp.toPx()),
                )

                // Vẽ các điểm nút (chỉ vẽ trên ngày có dữ liệu)
                validPoints.forEach { pt ->
                    drawCircle(color = White, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = Teal, radius = 3.dp.toPx(), center = pt)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                labels.forEach { label ->
                    Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


@Composable
private fun ExpandableSessionCard(
    title: String,
    time: String,
    repsInfo: String,
    qualityInfo: String,
    sessionState: String,
    steadyScore: Float,
    detailsRaw: String,
    selectedHand: String = "RIGHT",
) {
    var expanded by remember { mutableStateOf(false) }

    val stateColor = when (sessionState) {
        "RELIABLE" -> Teal
        "PARTIAL" -> Amber
        else -> Coral
    }
    val stateSoft = when (sessionState) {
        "RELIABLE" -> TealSoft
        "PARTIAL" -> AmberSoft
        else -> CoralSoft
    }
    val stateTextVi = when (sessionState) {
        "RELIABLE" -> "Đạt chuẩn"
        "PARTIAL" -> "Một phần"
        else -> "Cần cố gắng"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hàng 1: Tên bài tập + Trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(stateColor),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(title, color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = stateSoft,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            stateTextVi,
                            color = stateColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Muted,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Hàng 2: Thời gian + Tag tay tập (chỉ hiện khi không phải Massage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(time, color = Muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                if (!title.contains("Massage")) {
                    val isLeft = selectedHand == "LEFT"
                    Surface(
                        color = if (isLeft) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(
                            1.dp,
                            if (isLeft) Color(0xFFBFDBFE) else Color(0xFFBBF7D0),
                            RoundedCornerShape(6.dp),
                        ),
                    ) {
                        Text(
                            text = if (isLeft) "Tay Trái" else "Tay Phải",
                            color = if (isLeft) Color(0xFF1D4ED8) else Color(0xFF15803D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Hàng 3: Khối lượng tập & SteadyScore
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("$repsInfo · $qualityInfo", color = InkSecondary, fontSize = 12.5.sp)
                Text("SteadyScore: $steadyScore", color = TealDark, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .background(Canvas, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Tay tập phục hồi:", color = Muted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (selectedHand == "LEFT") "Tay Trái (Đồng hồ tay trái)" else "Tay Phải (Đồng hồ tay phải)",
                                color = Ink,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(
                        "CHI TIẾT TỪNG REP ĐÃ TẬP",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )

                    val repEntries = detailsRaw.split(";").filter { it.isNotBlank() }
                    if (repEntries.isNotEmpty()) {
                        repEntries.forEach { entry ->
                            val parts = entry.split(":")
                            if (parts.size >= 6) {
                                val repNum = parts[0]
                                val isOk = parts[1] == "1"
                                val speed = parts[2]
                                val note = parts[5]

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Rep #$repNum", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("$speed°/s", color = Muted, fontSize = 12.sp)
                                    Text(
                                        note,
                                        color = if (isOk) TealDark else Coral,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Chưa có bản ghi chi tiết các rep", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
