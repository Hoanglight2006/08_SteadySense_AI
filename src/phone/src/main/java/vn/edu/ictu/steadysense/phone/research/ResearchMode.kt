package vn.edu.ictu.steadysense.phone.research

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.Wearable
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import vn.edu.ictu.steadysense.core.ImuPayloadCodec
import vn.edu.ictu.steadysense.core.ResearchCommand
import vn.edu.ictu.steadysense.core.ResearchConfig
import vn.edu.ictu.steadysense.core.ResearchConfigCodec
import vn.edu.ictu.steadysense.core.ResearchControl
import vn.edu.ictu.steadysense.core.ResearchControlCodec
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.phone.BuildConfig
import vn.edu.ictu.steadysense.phone.data.DeviceSnapshotEntity
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.ResearchEventEntity
import vn.edu.ictu.steadysense.phone.data.ResearchParticipantEntity
import vn.edu.ictu.steadysense.phone.data.ResearchSessionEntity
import vn.edu.ictu.steadysense.phone.transport.PhoneTransferState

private val conditions = listOf("NORMAL_WEAR", "LOOSE_STRAP", "ROTATED", "REST", "DAILY_ACTIVITY_DISTRACTOR")
private val io = Executors.newSingleThreadExecutor()

data class ResearchUiState(val sessionId: String? = null, val active: Boolean = false,
                           val message: String = "Sẵn sàng")

@Composable
fun ResearchModeScreen() {
    val context = LocalContext.current
    var participant by rememberSaveable { mutableStateOf("P001") }
    var condition by rememberSaveable { mutableStateOf(conditions.first()) }
    var side by rememberSaveable { mutableStateOf("RIGHT") }
    var cycles by rememberSaveable { mutableStateOf("10") }
    var tempo by rememberSaveable { mutableStateOf("60") }
    var state by remember { mutableStateOf(ResearchUiState()) }
    LaunchedEffect(context) {
        ResearchCoordinator.latest(context) { restored ->
            if (restored != null) state = restored
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && state.sessionId != null) {
            ResearchCoordinator.export(context, state.sessionId!!, uri) { ok, message ->
                state = state.copy(message = if (ok) "Đã xuất bundle và SHA-256" else message)
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Research Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Chỉ dùng với người trưởng thành khỏe mạnh sau khi hoàn tất đồng thuận/phê duyệt cần thiết.")
        }
        item { OutlinedTextField(participant, { participant = it.uppercase() }, label = { Text("Mã ẩn danh") }, modifier = Modifier.fillMaxWidth(), enabled = !state.active) }
        item {
            Text("Điều kiện", fontWeight = FontWeight.Bold)
            Column { conditions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { value ->
                    FilterChip(condition == value, { condition = value }, { Text(value) }, enabled = !state.active)
                } }
            } }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("LEFT", "RIGHT").forEach { value ->
                    FilterChip(side == value, { side = value }, { Text(value) }, enabled = !state.active)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cycles, { cycles = it.filter(Char::isDigit) }, label = { Text("Số chu kỳ") }, modifier = Modifier.weight(1f), enabled = !state.active)
                OutlinedTextField(tempo, { tempo = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("BPM") }, modifier = Modifier.weight(1f), enabled = !state.active)
            }
        }
        item {
            if (!state.active) Button(onClick = {
                val cycleValue = cycles.toIntOrNull() ?: 0
                val tempoValue = tempo.toFloatOrNull() ?: 0f
                if (!participant.matches(Regex("P[0-9]{3,}")) || cycleValue !in 0..500 || tempoValue !in 20f..180f) {
                    state = state.copy(message = "Kiểm tra mã Pxxx, chu kỳ và BPM 20–180")
                } else ResearchCoordinator.start(context, participant, condition, side, cycleValue, tempoValue) { id, message ->
                    state = ResearchUiState(id, id != null, message)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Tạo và bắt đầu phiên thu") }
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ResearchCoordinator.marker(context, state.sessionId!!, "REP") }, modifier = Modifier.fillMaxWidth()) { Text("Đánh dấu một chu kỳ") }
                Button(onClick = { ResearchCoordinator.stop(context, state.sessionId!!, "LOCKED", null) { message -> state = state.copy(active = false, message = message) } }, modifier = Modifier.fillMaxWidth()) { Text("Dừng và khóa phiên") }
                Button(onClick = { ResearchCoordinator.stop(context, state.sessionId!!, "EXCLUDED", "OPERATOR_ABORT") { message -> state = state.copy(active = false, message = message) } }, modifier = Modifier.fillMaxWidth()) { Text("Dừng và loại phiên") }
            }
        }
        item {
            Button(onClick = { 
    val fileName = "${participant}_${condition.lowercase()}_${side.lowercase()}_test.zip"
    export.launch(fileName) 
},
                enabled = state.sessionId != null && !state.active, modifier = Modifier.fillMaxWidth()) {
                Text("Xuất bundle ZIP")
            }
            Spacer(Modifier.height(6.dp)); Text(state.message)
            Text("Đã nhận ${PhoneTransferState.storedWindows} cửa sổ IMU trên máy")
        }
    }
}

object ResearchCoordinator {
    fun start(context: Context, participant: String, condition: String, side: String,
              cycles: Int, tempo: Float, done: (String?, String) -> Unit) {
        val app = context.applicationContext
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        io.execute {
            val db = PhoneDatabase.get(app)
            db.researchDao().insertParticipant(ResearchParticipantEntity(participant, now, "DRAFT-1.0"))
            db.researchDao().insertSession(ResearchSessionEntity(sessionId, participant, condition, side,
                "1.0", cycles, tempo, now, 0, "ACTIVE", null))
            db.researchDao().insertDeviceSnapshot(DeviceSnapshotEntity(sessionId, "UNKNOWN",
                "UNKNOWN", "UNKNOWN", "accel+gyro@20Hz", BuildConfig.VERSION_NAME))
            val config = ResearchConfig(sessionId, participant, condition, side, "1.0", cycles, tempo,
                phoneSentAtEpochNanos = System.currentTimeMillis()*1_000_000L)
            send(app, TransportPaths.RESEARCH_CONFIG, ResearchConfigCodec.encode(config)) { sent ->
                if (!sent) {
                    io.execute { db.researchDao().finishSession(sessionId, System.currentTimeMillis(),
                        "EXCLUDED", "TRANSPORT_CONFIG_FAILED") }
                    done(null, "Không gửi được cấu hình: kiểm tra kết nối đồng hồ")
                }
                else send(app, TransportPaths.RESEARCH_CONTROL, ResearchControlCodec.encode(
                    ResearchControl(sessionId, ResearchCommand.START, System.currentTimeMillis()*1_000_000L))) { started ->
                    if (!started) io.execute { db.researchDao().finishSession(sessionId,
                        System.currentTimeMillis(), "EXCLUDED", "TRANSPORT_START_FAILED") }
                    done(if (started) sessionId else null, if (started) "Đang thu; đồng hồ chạy foreground" else "Không gửi được lệnh bắt đầu")
                }
            }
        }
    }

    fun marker(context: Context, sessionId: String, value: String) {
        val timestamp = System.currentTimeMillis()*1_000_000L
        io.execute { PhoneDatabase.get(context).researchDao().insertEvent(
            ResearchEventEntity(sessionId, timestamp, if (value == "REP") "REP" else "OPERATOR_MARK", value)) }
        send(context, TransportPaths.RESEARCH_CONTROL, ResearchControlCodec.encode(
            ResearchControl(sessionId, ResearchCommand.MARK, timestamp, value))) {}
    }

    fun latest(context: Context, done: (ResearchUiState?) -> Unit) = io.execute {
        val latest = PhoneDatabase.get(context).researchDao().allSessions().firstOrNull()
        onMain { done(latest?.let {
            ResearchUiState(it.id, it.status == "ACTIVE",
                if (it.status == "ACTIVE") "Đã khôi phục phiên đang thu" else "Phiên gần nhất: ${it.status}")
        }) }
    }

    fun stop(context: Context, sessionId: String, status: String, reason: String?, done: (String) -> Unit) {
        val now = System.currentTimeMillis()
        send(context, TransportPaths.RESEARCH_CONTROL, ResearchControlCodec.encode(
            ResearchControl(sessionId, ResearchCommand.STOP, now*1_000_000L))) {}
        io.execute {
            PhoneDatabase.get(context).researchDao().finishSession(sessionId, now, status, reason)
            onMain { done(if (status == "LOCKED") "Phiên đã khóa; có thể xuất bundle" else "Phiên đã được đánh dấu loại") }
        }
    }

    fun export(context: Context, sessionId: String, uri: Uri, done: (Boolean, String) -> Unit) = io.execute {
        runCatching {
            val db = PhoneDatabase.get(context)
            val session = requireNotNull(db.researchDao().sessionById(sessionId))
            require(session.endedAt > session.startedAt) { "Phiên chưa kết thúc" }
            val device = requireNotNull(db.researchDao().deviceSnapshot(sessionId))
            val frames = db.imuWindowDao().forSession(sessionId).flatMap { ImuPayloadCodec.decode(it.payload).frames }
            require(frames.isNotEmpty()) { "Phiên chưa có frame IMU" }
            val metadata = JSONObject().apply {
                put("session_id", session.id); put("participant_code", session.participantCode)
                put("condition", session.condition); put("worn_side", session.wornSide)
                put("protocol_version", session.protocolVersion); put("target_cycles", session.targetCycles)
                put("tempo_bpm", session.tempoBpm); put("started_at_epoch_millis", session.startedAt)
                put("ended_at_epoch_millis", session.endedAt)
                put("device", JSONObject().apply { put("manufacturer", device.manufacturer); put("model", device.model)
                    put("android_version", device.androidVersion); put("sampling_config", device.samplingConfig)
                    put("app_version", device.appVersion) })
            }.toString(2).toByteArray(Charsets.UTF_8)
            val imu = buildString { append("timestampEpochNanos,accelX,accelY,accelZ,gyroX,gyroY,gyroZ\n")
                frames.forEach { append("${it.timestampEpochNanos},${it.accelX},${it.accelY},${it.accelZ},${it.gyroX},${it.gyroY},${it.gyroZ}\n") } }.toByteArray()
            val events = buildString { append("timestampNanos,type,value\n"); db.researchDao().eventsForSession(sessionId).forEach {
                append("${it.timestampNanos},${csv(it.type)},${csv(it.value)}\n") } }.toByteArray()
            val files = linkedMapOf("metadata.json" to metadata, "imu.csv" to imu, "events.csv" to events)
            val manifest = files.map { (name, bytes) -> "${sha256(bytes)}  $name" }.joinToString("\n", postfix="\n").toByteArray()
            context.contentResolver.openOutputStream(uri)?.use { stream -> ZipOutputStream(stream).use { zip ->
                (files + ("manifest.sha256" to manifest)).forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
            } } ?: error("Không mở được tệp đích")
        }.fold({ onMain { done(true, "Đã xuất") } },
            { error -> onMain { done(false, error.message ?: "Xuất thất bại") } })
    }

    private fun send(context: Context, path: String, bytes: ByteArray, done: (Boolean) -> Unit) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            val node = nodes.firstOrNull() ?: return@addOnSuccessListener done(false)
            Wearable.getMessageClient(context).sendMessage(node.id, path, bytes)
                .addOnSuccessListener { done(true) }.addOnFailureListener { done(false) }
        }.addOnFailureListener { done(false) }
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun csv(value: String) = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
    private fun onMain(block: () -> Unit) = Handler(Looper.getMainLooper()).post(block)
}
