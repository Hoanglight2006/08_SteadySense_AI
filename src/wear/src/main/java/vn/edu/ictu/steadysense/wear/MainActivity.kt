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

package vn.edu.ictu.steadysense.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.wear.research.WearResearchState
import vn.edu.ictu.steadysense.wear.transport.WearAckService
import vn.edu.ictu.steadysense.wear.transport.WearConnectionState
import vn.edu.ictu.steadysense.wear.transport.WearExerciseAlertState
import vn.edu.ictu.steadysense.wear.transport.WearSender

import android.bluetooth.BluetoothAdapter

class MainActivity : ComponentActivity() {
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED && context != null) {
                WearAckService.sendWatchStatus(context)
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED && context != null) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    WearConnectionState.setConnected(false)
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Wearable.getNodeClient(context).connectedNodes
                        .addOnSuccessListener { nodes ->
                            WearConnectionState.updateFromNodes(nodes)
                            if (WearConnectionState.isPhoneConnected) {
                                WearSender.retryPending(context)
                                WearAckService.sendWatchStatus(context)
                            }
                        }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WearAckService.sendWatchStatus(this)
        runCatching {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        runCatching {
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        }
        setContent {
            WearTheme {
                WearApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WearAckService.sendWatchStatus(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            unregisterReceiver(batteryReceiver)
        }
        runCatching {
            unregisterReceiver(bluetoothReceiver)
        }
    }
}

// Bảng màu Wecare tối ưu cho màn hình AMOLED đồng hồ
private val Teal = Color(0xFF0FB7A4)
private val TealSoft = Color(0xFF134E48)
private val Amber = Color(0xFFFBBF24)
private val Ink = Color(0xFF0F172A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF94A3B8)

@Composable
private fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            secondary = Amber,
            background = Color.Black,
            surface = Color(0xFF1E293B),
            onSurface = TextWhite,
        ),
        content = content,
    )
}

@Composable
private fun WearApp() {
    val context = LocalContext.current
    val phoneConnected = rememberPhoneConnection()
    val research = WearResearchState.snapshot
    val isSessionActive = research.active || WearExerciseAlertState.sessionActiveByPhone
    val alertMessage = WearExerciseAlertState.warningMessage
    val cueMessage = WearExerciseAlertState.cueMessage

    val isMassage = WearExerciseAlertState.isMassageMode
    val massageMinutes = WearExerciseAlertState.massageMinutes
    val massageSecondsRemaining = WearExerciseAlertState.massageSecondsRemaining
    val isMassagePaused = WearExerciseAlertState.isMassagePaused

    LaunchedEffect(context) {
        WearSender.retryPending(context)
    }

    // Định kỳ gửi cập nhật pin mỗi 20 giây khi app wear đang chạy
    LaunchedEffect(Unit) {
        while (isActive) {
            WearAckService.sendWatchStatus(context)
            delay(20_000L)
        }
    }

    // Tự động xóa cảnh báo sau 4 giây
    LaunchedEffect(alertMessage) {
        if (alertMessage != null) {
            delay(4000)
            WearExerciseAlertState.clearWarning()
        }
    }

    // Tự động xóa chỉ dẫn sau 3 giây
    LaunchedEffect(cueMessage) {
        if (cueMessage != null) {
            delay(3000)
            WearExerciseAlertState.clearCue()
        }
    }

    if (alertMessage != null) {
        // MÀN HÌNH CẢNH BÁO ĐỎ RỰC KÈM RUNG KHI TẬP QUÁ NHANH
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFE11D48), // Vivid Rose Red
                            Color(0xFF881337), // Deep Rose
                            Color.Black,
                        ),
                    ),
                )
                .clickable { WearExerciseAlertState.clearWarning() }
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF1F2)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "CẢNH BÁO!",
                    color = Color(0xFFFECDD3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = alertMessage,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Chạm để tắt",
                    color = Color(0xFFFDA4AF),
                    fontSize = 10.sp,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF082F2B), Color(0xFF031412), Color.Black)))
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isSessionActive && isMassage) {
                WearMassageView(
                    totalDurationSec = (if (massageMinutes <= 0) 5 else massageMinutes) * 60,
                    remainingSec = massageSecondsRemaining,
                    isPaused = isMassagePaused,
                    phoneConnected = phoneConnected,
                    onTogglePause = {
                        val cmd = "TOGGLE_PAUSE:MASSAGE".toByteArray(Charsets.UTF_8)
                        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                            nodes.forEach { node ->
                                Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.EXERCISE_SESSION, cmd)
                            }
                        }
                    },
                )
            } else if (isSessionActive) {
                // Vòng cung tiến trình Teal khi nối, Amber khi mất kết nối
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    drawArc(
                        color = if (phoneConnected) Color(0xFF134E48) else Color(0xFF78350F),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (phoneConnected) "ĐANG TẬP" else "MẤT KẾT NỐI",
                        color = if (phoneConnected) Teal else Amber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (phoneConnected) "Gấp – duỗi tay" else "Tạm dừng bài tập",
                        color = if (phoneConnected) TextWhite else Amber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))

                    if (!phoneConnected) {
                        Text(
                            "Đang chờ kết nối lại...\nHãy giữ nguyên vị trí",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                        )
                    } else if (cueMessage != null) {
                        // Hiện chỉ dẫn AI từ điện thoại
                        val cueColor = when {
                            cueMessage.contains("GẬP") -> Teal
                            cueMessage.contains("ĐẠT") || cueMessage.contains("✓") -> Color(0xFF34D399)
                            cueMessage.contains("NHANH") || cueMessage.contains("✗") -> Color(0xFFFB7185)
                            else -> TextWhite
                        }
                        Text(
                            text = cueMessage,
                            color = cueColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp,
                        )
                    } else {
                        // Không có cue → hiện trạng thái theo dõi
                        Text(
                            "Đang theo dõi...",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (phoneConnected) Teal else Amber, CircleShape),
                        )
                        Text(
                            if (phoneConnected) " Đã nối máy" else " Chờ máy...",
                            color = if (phoneConnected) Teal else Amber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                // MÀN HÌNH CHỜ (IDLE) PHONG CÁCH WECARE - Không hiện gấp-duỗi khi chưa vào bài
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "SteadySense AI",
                        color = Teal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sẵn sàng",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Chọn bài tập trên\nđiện thoại để bắt đầu",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (phoneConnected) Teal else Amber, CircleShape),
                        )
                        Text(
                            if (phoneConnected) " Đã nối máy" else " Chờ máy...",
                            color = if (phoneConnected) Teal else Amber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberPhoneConnection(): Boolean {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        while (isActive) {
            Wearable.getNodeClient(context).connectedNodes
                .addOnSuccessListener { nodes ->
                    val wasConnected = WearConnectionState.isPhoneConnected
                    WearConnectionState.updateFromNodes(nodes)
                    if (!wasConnected && WearConnectionState.isPhoneConnected) {
                        WearSender.retryPending(context)
                        WearAckService.sendWatchStatus(context)
                    }
                }
                .addOnFailureListener {
                    WearConnectionState.setConnected(false)
                }
            delay(1500L)
        }
    }
    return WearConnectionState.isPhoneConnected
}

@Composable
private fun WearMassageView(
    totalDurationSec: Int,
    remainingSec: Int,
    isPaused: Boolean,
    phoneConnected: Boolean,
    onTogglePause: () -> Unit,
) {
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000L)
            WearExerciseAlertState.tickMassageCountdown()
        }
    }

    val progress = (remainingSec.toFloat() / totalDurationSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val timeFormatted = "%02d:%02d".format(mins, secs)

    val infiniteTransition = rememberInfiniteTransition(label = "massagePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val currentScale = if (isPaused) 1.0f else pulseScale

    // Circular timer arc
    Canvas(Modifier.fillMaxSize().padding(4.dp)) {
        // Background track
        drawArc(
            color = Color(0xFF134E48),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
        // Active progress track
        drawArc(
            color = if (isPaused) Amber else Teal,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTogglePause() }
            .padding(horizontal = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer(scaleX = currentScale, scaleY = currentScale)
                .clip(CircleShape)
                .background(if (isPaused) Color(0xFF78350F) else Color(0xFF0F766E)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Rounded.Pause else Icons.Rounded.Spa,
                contentDescription = null,
                tint = if (isPaused) Color(0xFFFDE68A) else Color(0xFF5EEAD4),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (isPaused) "ĐANG TẠM DỪNG" else "MASSAGE",
            color = if (isPaused) Amber else Color(0xFF2DD4BF),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = timeFormatted,
            color = TextWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                remainingSec <= 0 -> "Hoàn thành"
                isPaused -> "Chạm để tiếp tục"
                else -> "Thư giãn · Thở đều"
            },
            color = if (isPaused) Amber else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (phoneConnected) Teal else Amber, CircleShape),
            )
            Text(
                if (phoneConnected) " Đã nối máy" else " Chờ máy...",
                color = if (phoneConnected) Teal else Amber,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
