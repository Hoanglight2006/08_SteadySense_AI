package vn.edu.ictu.steadysense.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import vn.edu.ictu.steadysense.wear.research.WearResearchState
import vn.edu.ictu.steadysense.wear.research.markCollection
import vn.edu.ictu.steadysense.wear.transport.WearSender
import vn.edu.ictu.steadysense.wear.transport.WearTransferState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearTheme { WearApp() } }
    }
}

private val Sky = Color(0xFF3C8DFF)
private val Mint = Color(0xFF31C7A3)
private val Ink = Color(0xFF20263A)

@Composable
private fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Sky,
            secondary = Mint,
            background = Color.Black,
            surface = Color(0xFFF8FAFF),
            onSurface = Ink,
        ),
        content = content,
    )
}

@Composable
private fun WearApp() {
    val context = LocalContext.current
    val phoneConnected = rememberPhoneConnection()
    val research = WearResearchState.snapshot
    val active = research.active
    val transfer = WearTransferState.snapshot
    LaunchedEffect(context) {
        WearSender.retryPending(context)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF203B72), Color(0xFF09101F), Color.Black)))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "IMU ĐANG THU · ${research.samples} MẪU",
                    color = Mint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${research.windows} cửa sổ · ${transfer.pending} chờ · ${transfer.acknowledged} ACK",
                    color = Color(0xFFB8C4DA),
                    fontSize = 9.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text("${research.markers}", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                Text("marker đã ghi", color = Color(0xFFB8C4DA), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Sky,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp).clickable { markCollection(context, "WEAR_BUTTON") },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Chạm để thêm marker", color = Color(0xFF8290A9), fontSize = 10.sp)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SteadySense", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Gấp – duỗi\nkhuỷu tay",
                    color = Color(0xFFC9D5E9),
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp),
                )
                Surface(
                    color = Mint,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(.78f),
                ) {
                    Text(
                        "Chờ lệnh từ máy",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (phoneConnected) {
                        "●  Đã nối · ${transfer.pending} gói chờ"
                    } else {
                        "○  Chưa thấy máy · ${transfer.pending} gói chờ"
                    },
                    color = if (phoneConnected) Mint else Color(0xFFFFC857),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun rememberPhoneConnection(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes -> connected = nodes.isNotEmpty() }
            .addOnFailureListener { connected = false }
    }
    return connected
}
