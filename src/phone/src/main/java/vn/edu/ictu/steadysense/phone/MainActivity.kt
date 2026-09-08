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

package vn.edu.ictu.steadysense.phone

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import vn.edu.ictu.steadysense.core.TransportPaths
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import vn.edu.ictu.steadysense.phone.research.ResearchModeScreen
import vn.edu.ictu.steadysense.phone.transport.PhoneTransferState
import vn.edu.ictu.steadysense.phone.ui.ActiveExerciseScreen
import vn.edu.ictu.steadysense.phone.ui.AnalyticsScreen
import vn.edu.ictu.steadysense.phone.ui.Canvas
import vn.edu.ictu.steadysense.phone.ui.HomeScreen
import vn.edu.ictu.steadysense.phone.ui.MassageExerciseScreen
import vn.edu.ictu.steadysense.phone.ui.MedicalDisclaimerDialog
import vn.edu.ictu.steadysense.phone.ui.Muted
import vn.edu.ictu.steadysense.phone.ui.ScheduleScreen
import vn.edu.ictu.steadysense.phone.ui.SettingsScreen
import vn.edu.ictu.steadysense.phone.ui.SteadySenseTheme
import vn.edu.ictu.steadysense.phone.ui.Teal
import vn.edu.ictu.steadysense.phone.ui.TealSoft
import vn.edu.ictu.steadysense.phone.ui.White

import vn.edu.ictu.steadysense.phone.util.VoiceGuideManager
import vn.edu.ictu.steadysense.phone.util.WorkoutReminderManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VoiceGuideManager.init(this)
        WorkoutReminderManager.createNotificationChannel(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (UserPreferences.isDailyReminderEnabled(this)) {
            WorkoutReminderManager.scheduleNextReminder(this)
        }
        setContent {
            SteadySenseTheme {
                SteadySenseApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceGuideManager.shutdown()
    }
}

// 4 TAB CHUẨN NGƯỜI DÙNG VỚI ICON VECTOR ĐỒNG BỘ
private enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Rounded.Home),
    SCHEDULE("Lịch tập", Icons.Rounded.CalendarMonth),
    ANALYTICS("Thống kê", Icons.Rounded.BarChart),
    SETTINGS("Cài đặt", Icons.Rounded.Settings),
}

@Composable
private fun SteadySenseApp() {
    val context = LocalContext.current
    var watchConnected by remember { mutableStateOf(false) }

    // Kiểm tra kết nối đồng hồ WearOS và yêu cầu gửi pin
    fun refreshWatchConnection() {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                watchConnected = nodes.isNotEmpty()
                if (nodes.isEmpty()) {
                    PhoneTransferState.publishWatchBattery(-1)
                } else {
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.PING, ByteArray(0))
                    }
                }
            }
            .addOnFailureListener {
                watchConnected = false
                PhoneTransferState.publishWatchBattery(-1)
            }
    }

    // Polling tự động liên tục mỗi 15 giây để cập nhật pin thực tế theo thời gian thực
    LaunchedEffect(Unit) {
        while (isActive) {
            refreshWatchConnection()
            delay(15_000L)
        }
    }

    LaunchedEffect(context) {
        val count = withContext(Dispatchers.IO) {
            PhoneDatabase.get(context).imuWindowDao().count()
        }
        PhoneTransferState.publishStoredCount(count)
    }

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    // Khi người dùng chuyển về tab Trang chủ, chủ động cập nhật pin ngay lập tức
    LaunchedEffect(screen) {
        if (screen == Screen.HOME) {
            refreshWatchConnection()
        }
    }
    var selectedHand by rememberSaveable { mutableStateOf("RIGHT") }
    var sessionActive by rememberSaveable { mutableStateOf(false) }
    var activeExerciseType by rememberSaveable { mutableStateOf("FLEXION") }
    var activeExerciseMinutes by rememberSaveable { mutableStateOf(5) }
    var activeExerciseSets by rememberSaveable { mutableStateOf(3) }
    var activeExerciseReps by rememberSaveable { mutableStateOf(10) }
    var activeExerciseRestSeconds by rememberSaveable { mutableStateOf(60) }
    var activeScheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    var inResearchMode by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable {
        mutableStateOf(!UserPreferences.isDisclaimerAccepted(context))
    }

    if (showDisclaimer) {
        MedicalDisclaimerDialog(
            onDismiss = { showDisclaimer = false },
            onAccept = {
                UserPreferences.setDisclaimerAccepted(context, true)
                showDisclaimer = false
            }
        )
    }

    if (inResearchMode) {
        // Chế độ kỹ thuật viên ẩn trong Cài đặt
        Scaffold(
            containerColor = Canvas,
            topBar = {
                Surface(
                    color = White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "← Quay lại Cài đặt",
                            color = Teal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { inResearchMode = false }
                                .padding(8.dp),
                        )
                    }
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                ResearchModeScreen()
            }
        }
    } else {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                if (!sessionActive) {
                    WecareBottomNavigation(
                        currentScreen = screen,
                        onSelect = { screen = it },
                    )
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = sessionActive,
                label = "sessionTransition",
                modifier = Modifier.padding(padding),
            ) { active ->
                if (active) {
                    if (activeExerciseType == "MASSAGE") {
                        MassageExerciseScreen(
                            targetMinutes = activeExerciseMinutes,
                            selectedHand = selectedHand,
                            targetScheduleId = activeScheduleId,
                            onFinishSession = {
                                notifyWatchExerciseSession(context, false)
                                activeScheduleId = null
                                sessionActive = false
                                screen = Screen.ANALYTICS
                            },
                            onCancelSession = {
                                notifyWatchExerciseSession(context, false)
                                activeScheduleId = null
                                sessionActive = false
                            },
                        )
                    } else {
                        ActiveExerciseScreen(
                            selectedHand = selectedHand,
                            targetSets = activeExerciseSets,
                            targetRepsPerSet = activeExerciseReps,
                            restSeconds = activeExerciseRestSeconds,
                            targetScheduleId = activeScheduleId,
                            onFinishSession = { _, _ ->
                                notifyWatchExerciseSession(context, false)
                                activeScheduleId = null
                                sessionActive = false
                                screen = Screen.ANALYTICS // Tập xong đưa sang tab Thống kê để xem kết quả
                            },
                            onCancelSession = {
                                notifyWatchExerciseSession(context, false)
                                activeScheduleId = null
                                sessionActive = false
                            },
                        )
                    }
                } else {
                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            watchConnected = watchConnected,
                            selectedHand = selectedHand,
                            onSelectHand = { selectedHand = it },
                            onReconnectWatch = { refreshWatchConnection() },
                            onStartExercise = { exerciseType, minutes, sets, reps, restSeconds, scheduleId ->
                                activeExerciseType = exerciseType
                                activeExerciseMinutes = minutes
                                activeExerciseSets = sets
                                activeExerciseReps = reps
                                activeExerciseRestSeconds = restSeconds
                                activeScheduleId = scheduleId
                                if (exerciseType == "FLEXION") {
                                    notifyWatchExerciseSession(context, true, isMassage = false)
                                } else {
                                    notifyWatchExerciseSession(context, true, isMassage = true, minutes = minutes)
                                }
                                sessionActive = true
                            },
                        )
                        Screen.SCHEDULE -> ScheduleScreen(
                            onStartWorkout = { exerciseType, minutes, sets, reps, restSeconds, scheduleId ->
                                activeExerciseType = exerciseType
                                activeExerciseMinutes = minutes
                                activeExerciseSets = sets
                                activeExerciseReps = reps
                                activeExerciseRestSeconds = restSeconds
                                activeScheduleId = scheduleId
                                if (exerciseType == "FLEXION") {
                                    notifyWatchExerciseSession(context, true, isMassage = false)
                                } else {
                                    notifyWatchExerciseSession(context, true, isMassage = true, minutes = minutes)
                                }
                                sessionActive = true
                            },
                        )
                        Screen.ANALYTICS -> AnalyticsScreen()
                        Screen.SETTINGS -> SettingsScreen(
                            onOpenResearchMode = { inResearchMode = true },
                        )
                    }
                }
            }
        }
    }
}

private fun notifyWatchExerciseSession(
    context: Context,
    active: Boolean,
    isMassage: Boolean = false,
    minutes: Int = 5,
) {
    val cmd = when {
        !active -> "STOP"
        isMassage -> "START:MASSAGE:$minutes"
        else -> "START"
    }
    val bytes = cmd.toByteArray(Charsets.UTF_8)
    runCatching {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, bytes)
            }
        }
    }
}

@Composable
private fun WecareBottomNavigation(
    currentScreen: Screen,
    onSelect: (Screen) -> Unit,
) {
    Surface(
        color = White,
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Screen.entries.forEach { item ->
                val isSelected = item == currentScreen
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(item) }
                        .background(if (isSelected) TealSoft else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Teal else Muted,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        color = if (isSelected) Teal else Muted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppPreview() {
    SteadySenseTheme {
        SteadySenseApp()
    }
}
