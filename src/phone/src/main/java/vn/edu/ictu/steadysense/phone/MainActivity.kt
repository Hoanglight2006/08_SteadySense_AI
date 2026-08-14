package vn.edu.ictu.steadysense.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.transport.PhoneTransferState
import vn.edu.ictu.steadysense.phone.research.ResearchModeScreen
import vn.edu.ictu.steadysense.phone.ui.Canvas
import vn.edu.ictu.steadysense.phone.ui.Coral
import vn.edu.ictu.steadysense.phone.ui.CoralSoft
import vn.edu.ictu.steadysense.phone.ui.Ink
import vn.edu.ictu.steadysense.phone.ui.Mint
import vn.edu.ictu.steadysense.phone.ui.MintSoft
import vn.edu.ictu.steadysense.phone.ui.Muted
import vn.edu.ictu.steadysense.phone.ui.Sky
import vn.edu.ictu.steadysense.phone.ui.SkySoft
import vn.edu.ictu.steadysense.phone.ui.SteadySenseTheme
import vn.edu.ictu.steadysense.phone.ui.Sun
import vn.edu.ictu.steadysense.phone.ui.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SteadySenseTheme { SteadySenseApp() } }
    }
}

private enum class Screen(val label: String, val glyph: String) {
    TODAY("Hôm nay", "●"),
    PLAN("Kế hoạch", "◆"),
    PROGRESS("Tiến độ", "▲"),
    RESEARCH("Nghiên cứu", "■"),
}

@Composable
private fun SteadySenseApp() {
    val watchConnected = rememberWatchConnection()
    val storedWindows = PhoneTransferState.storedWindows
    val context = LocalContext.current
    LaunchedEffect(context) {
        val count = withContext(Dispatchers.IO) {
            PhoneDatabase.get(context).imuWindowDao().count()
        }
        PhoneTransferState.publishStoredCount(count)
    }
    var screen by rememberSaveable { mutableStateOf(Screen.TODAY) }
    var sessionActive by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            if (!sessionActive) {
                BrightNavigation(screen = screen, onSelect = { screen = it })
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = sessionActive,
            label = "session",
            modifier = Modifier.padding(padding),
        ) { active ->
            if (active) {
                SessionScreen(
                    onFinish = {
                        completed = true
                        sessionActive = false
                        screen = Screen.PROGRESS
                    },
                    onCancel = { sessionActive = false },
                )
            } else {
                when (screen) {
                    Screen.TODAY -> TodayScreen(
                        completed = completed,
                        watchConnected = watchConnected,
                        storedWindows = storedWindows,
                        onStart = { sessionActive = true },
                    )
                    Screen.PLAN -> PlanScreen()
                    Screen.PROGRESS -> ProgressScreen(completed)
                    Screen.RESEARCH -> ResearchModeScreen()
                }
            }
        }
    }
}

@Composable
private fun PageHeader(eyebrow: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEAF3FF), Color(0xFFFFF1EA), Canvas),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Text(eyebrow.uppercase(), color = Sky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(title, color = Ink, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(7.dp))
        Text(subtitle, color = Muted, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun TodayScreen(completed: Boolean, watchConnected: Boolean, storedWindows: Int, onStart: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            PageHeader(
                eyebrow = "Thứ Năm · 13 tháng 8",
                title = if (completed) "Bạn đã làm rất tốt!" else "Chào buổi sáng, An",
                subtitle = if (completed) {
                    "Buổi tập đã được ghi nhận với tín hiệu đáng tin cậy."
                } else {
                    "Hôm nay mình cùng vận động nhẹ nhàng và an toàn nhé."
                },
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SignalCard(watchConnected, storedWindows)
                if (completed) CompletedCard() else TodayExerciseCard(onStart)
                WeekStrip(completed)
                SafetyNote()
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SignalCard(watchConnected: Boolean, storedWindows: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MintSoft),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = if (watchConnected) Mint else Sun, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (watchConnected) "✓" else "!", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (watchConnected) "Đã thấy đồng hồ Wear OS" else "Chưa thấy đồng hồ",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Text(
                    if (watchConnected) "$storedWindows cửa sổ IMU đã lưu an toàn" else "Mở ứng dụng trên đồng hồ để kết nối",
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
            Text(
                if (watchConnected) "Đã nối" else "Chờ",
                color = if (watchConnected) Mint else Coral,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun TodayExerciseCard(onStart: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Coral, Sun))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↕", color = White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text("Bài tập hôm nay", color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Gấp – duỗi khuỷu tay", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("2 hiệp · 10 lần · khoảng 8 phút", color = Muted, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("Ngồi tập", SkySoft, Sky)
                InfoPill("Tay được đỡ", CoralSoft, Coral)
                InfoPill("09:00", MintSoft, Mint)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sky),
            ) {
                Text("Bắt đầu buổi tập", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("→", fontSize = 22.sp)
            }
            Text(
                "Kế hoạch đã được kỹ thuật viên xác nhận",
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun InfoPill(text: String, background: Color, foreground: Color) {
    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun WeekStrip(completed: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Nhịp vận động tuần này", color = Ink, fontWeight = FontWeight.Bold)
                Text(if (completed) "4/5 buổi" else "3/5 buổi", color = Sky, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(15.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEachIndexed { index, day ->
                    val done = index < 3 || (completed && index == 3)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (done) Mint else if (index == 3) SkySoft else Color(0xFFF0F2F8)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (done) "✓" else "·", color = if (done) White else Sky, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(day, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF7DD))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("☀", fontSize = 24.sp)
        Spacer(Modifier.width(11.dp))
        Text(
            "Nếu thấy đau, chóng mặt hoặc khó chịu, hãy dừng tập và liên hệ người hỗ trợ.",
            color = Ink,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun CompletedCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("★", color = Sun, fontSize = 48.sp)
            Text("Hoàn thành đáng tin cậy", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("20 lần · 7 phút 42 giây", color = Muted, modifier = Modifier.padding(top = 5.dp))
            Spacer(Modifier.height(14.dp))
            InfoPill("Tín hiệu 92% · Tốt", MintSoft, Mint)
        }
    }
}

@Composable
private fun PlanScreen() {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            PageHeader(
                eyebrow = "Kế hoạch vận động",
                title = "Nhẹ nhàng, đều đặn",
                subtitle = "Bạn có thể đổi giờ hoặc hoãn buổi. Bài tập và số lần cần kỹ thuật viên phê duyệt.",
            )
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanStatusCard()
                ExerciseRow("↕", "Gấp – duỗi khuỷu tay", "Thứ 2 · 4 · 6", "2 × 10 lần", CoralSoft, Coral)
                ExerciseRow("↻", "Xoay sấp – ngửa cẳng tay", "Thứ 3 · 5", "2 × 8 lần", SkySoft, Sky)
                ExerciseRow("⇆", "Trượt khăn trên bàn", "Thứ 7", "6 phút", MintSoft, Mint)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PlanStatusCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Sky), shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("KẾ HOẠCH ĐANG ÁP DỤNG", color = White.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Tuần vận động tay", color = White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Hiệu lực đến 30/08 · Phiên bản 1", color = White.copy(alpha = .82f), fontSize = 13.sp)
            }
            Surface(color = White.copy(alpha = .18f), shape = CircleShape) {
                Text("✓", color = White, fontSize = 24.sp, modifier = Modifier.padding(13.dp))
            }
        }
    }
}

@Composable
private fun ExerciseRow(glyph: String, name: String, days: String, dose: String, tint: Color, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(tint), contentAlignment = Alignment.Center) {
                Text(glyph, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(days, color = Muted, fontSize = 13.sp)
            }
            Text(dose, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProgressScreen(completed: Boolean) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            PageHeader(
                eyebrow = "Báo cáo tuần",
                title = "Tiến bộ từng ngày",
                subtitle = "Kết quả có xét độ tin cậy của tín hiệu, không thay thế đánh giá của kỹ thuật viên.",
            )
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(if (completed) "4" else "3", "Buổi đáng tin", Mint, Modifier.weight(1f))
                    MetricCard("1", "Cần kiểm tra", Coral, Modifier.weight(1f))
                }
                ReliabilityChart(completed)
                RecentSession("Hôm nay · 09:08", if (completed) "Hoàn thành đáng tin cậy" else "Chưa bắt đầu", if (completed) Mint else Muted)
                RecentSession("Thứ Tư · 09:15", "Không đủ tín hiệu", Coral)
                RecentSession("Thứ Hai · 08:54", "Hoàn thành đáng tin cậy", Mint)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text(value, color = accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ReliabilityChart(completed: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(19.dp)) {
            Text("Độ tin cậy tín hiệu", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Tỷ lệ cửa sổ cảm biến được chấp nhận", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                val values = if (completed) listOf(.72f, .84f, .65f, .92f, .78f, .48f, .35f)
                else listOf(.72f, .84f, .65f, .25f, .18f, .12f, .08f)
                val barWidth = size.width / (values.size * 1.65f)
                values.forEachIndexed { index, value ->
                    val left = index * size.width / values.size + barWidth * .3f
                    drawRoundRect(
                        color = if (value >= .7f) Mint else if (value >= .4f) Sun else SkySoft,
                        topLeft = Offset(left, size.height * (1f - value)),
                        size = Size(barWidth, size.height * value),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSession(time: String, result: String, accent: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(White).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Gấp – duỗi khuỷu tay", color = Ink, fontWeight = FontWeight.SemiBold)
            Text(time, color = Muted, fontSize = 12.sp)
        }
        Text(result, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun SessionScreen(onFinish: () -> Unit, onCancel: () -> Unit) {
    val watchConnected = rememberWatchConnection()
    val storedWindows = PhoneTransferState.storedWindows
    var repetitions by remember { mutableIntStateOf(6) }
    val progress by animateFloatAsState(repetitions / 10f, label = "progress")
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Canvas, Color(0xFFFFF2E9))),
        ).statusBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Buổi tập đang diễn ra", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = White,
                shape = CircleShape,
                modifier = Modifier.clickable(onClick = onCancel),
            ) { Text("×", color = Muted, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
        }
        Spacer(Modifier.height(34.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(230.dp)) {
                drawArc(SkySoft, -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(22.dp.toPx(), cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(listOf(Sky, Mint, Sky)),
                    -90f,
                    progress * 360f,
                    false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(22.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$repetitions", color = Ink, fontSize = 62.sp, fontWeight = FontWeight.ExtraBold)
                Text("trên 10 lần", color = Muted, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("Gấp – duỗi khuỷu tay", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("Di chuyển chậm và thoải mái", color = Muted, fontSize = 15.sp)
        Spacer(Modifier.height(22.dp))
        SignalCard(watchConnected, storedWindows)
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(visible = repetitions < 10) {
            Button(
                onClick = { repetitions++ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkySoft, contentColor = Sky),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Mô phỏng thêm 1 lần", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onFinish,
            enabled = repetitions >= 6,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (repetitions >= 10) Mint else Sky),
            shape = RoundedCornerShape(19.dp),
        ) { Text(if (repetitions >= 10) "Hoàn thành buổi tập" else "Kết thúc và lưu", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun BrightNavigation(screen: Screen, onSelect: (Screen) -> Unit) {
    Surface(
        color = White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Screen.entries.forEach { item ->
                val selected = item == screen
                Column(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(item) }
                        .background(if (selected) SkySoft else Color.Transparent)
                        .padding(horizontal = 19.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(item.glyph, color = if (selected) Sky else Muted, fontSize = 16.sp)
                    Text(item.label, color = if (selected) Sky else Muted, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun rememberWatchConnection(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes -> connected = nodes.isNotEmpty() }
            .addOnFailureListener { connected = false }
    }
    return connected
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppPreview() {
    SteadySenseTheme { SteadySenseApp() }
}
