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
import android.content.Intent
import vn.edu.ictu.steadysense.phone.util.RehabReportGenerator
import vn.edu.ictu.steadysense.phone.util.VoiceGuideManager
import vn.edu.ictu.steadysense.phone.util.WorkoutReminderManager
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Description
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenResearchMode: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dailyReminder by remember { mutableStateOf(UserPreferences.isDailyReminderEnabled(context)) }
    var voiceGuide by remember { mutableStateOf(UserPreferences.isVoiceGuideEnabled(context)) }
    var watchVibration by remember { mutableStateOf(UserPreferences.isWatchVibrationEnabled(context)) }

    var watchName by remember { mutableStateOf("Đang tìm đồng hồ...") }
    var watchConnected by remember { mutableStateOf(false) }
    var isCheckingWatch by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Hồ sơ người dùng
    var patientName by remember { mutableStateOf(UserPreferences.getPatientName(context)) }
    var birthYear by remember { mutableStateOf(UserPreferences.getBirthYear(context).let { if (it == 0) "" else it.toString() }) }
    var gender by remember { mutableStateOf(UserPreferences.getGender(context)) }
    var affectedSide by remember { mutableStateOf(UserPreferences.getAffectedSide(context)) }
    var phoneNumber by remember { mutableStateOf(UserPreferences.getPhoneNumber(context)) }
    var medicalCondition by remember { mutableStateOf(UserPreferences.getCondition(context)) }
    var mobilityLevel by remember { mutableStateOf(UserPreferences.getMobilityLevel(context)) }
    var doctorName by remember { mutableStateOf(UserPreferences.getDoctorName(context)) }
    var showProfileDialog by remember { mutableStateOf(false) }

    var isExportingZip by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            isExportingZip = true
            scope.launch(Dispatchers.IO) {
                try {
                    val db = PhoneDatabase.get(context).workoutDao()
                    val allSessions = db.allSessions()
                    val allSchedules = db.allSchedules()

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                            // 1. workout_sessions.json
                            val sessionsJson = buildString {
                                append("[\n")
                                allSessions.forEachIndexed { index, s ->
                                    append("  {\n")
                                    append("    \"id\": \"${s.id}\",\n")
                                    append("    \"exerciseName\": \"${s.exerciseName}\",\n")
                                    append("    \"startedAt\": ${s.startedAt},\n")
                                    append("    \"endedAt\": ${s.endedAt},\n")
                                    append("    \"durationSeconds\": ${s.durationSeconds},\n")
                                    append("    \"selectedHand\": \"${s.selectedHand}\",\n")
                                    append("    \"completedReps\": ${s.completedReps},\n")
                                    append("    \"targetReps\": ${s.targetReps},\n")
                                    append("    \"accuracyPercentage\": ${s.accuracyPercentage},\n")
                                    append("    \"steadyScore\": ${s.steadyScore},\n")
                                    append("    \"sessionState\": \"${s.sessionState}\",\n")
                                    append("    \"scheduleId\": ${if (s.scheduleId != null) "\"${s.scheduleId}\"" else "null"}\n")
                                    append("  }${if (index < allSessions.size - 1) "," else ""}\n")
                                }
                                append("]\n")
                            }
                            zipOut.putNextEntry(ZipEntry("workout_sessions.json"))
                            zipOut.write(sessionsJson.toByteArray(Charsets.UTF_8))
                            zipOut.closeEntry()

                            // 2. workout_schedules.json
                            val schedulesJson = buildString {
                                append("[\n")
                                allSchedules.forEachIndexed { index, sc ->
                                    append("  {\n")
                                    append("    \"id\": \"${sc.id}\",\n")
                                    append("    \"exerciseName\": \"${sc.exerciseName}\",\n")
                                    append("    \"targetReps\": ${sc.targetReps},\n")
                                    append("    \"targetSets\": ${sc.targetSets},\n")
                                    append("    \"restSeconds\": ${sc.restSeconds},\n")
                                    append("    \"scheduledTime\": \"${sc.scheduledTime}\",\n")
                                    append("    \"dayOfWeek\": ${sc.dayOfWeek},\n")
                                    append("    \"repeatType\": \"${sc.repeatType}\",\n")
                                    append("    \"isCompleted\": ${sc.isCompleted},\n")
                                    append("    \"specificDate\": ${if (sc.specificDate != null) "\"${sc.specificDate}\"" else "null"}\n")
                                    append("  }${if (index < allSchedules.size - 1) "," else ""}\n")
                                }
                                append("]\n")
                            }
                            zipOut.putNextEntry(ZipEntry("workout_schedules.json"))
                            zipOut.write(schedulesJson.toByteArray(Charsets.UTF_8))
                            zipOut.closeEntry()

                            // 3. user_profile.json
                            val profileJson = buildString {
                                append("{\n")
                                append("  \"patientName\": \"${patientName.replace("\"", "\\\"")}\",\n")
                                append("  \"birthYear\": \"${birthYear}\",\n")
                                append("  \"gender\": \"${gender}\",\n")
                                append("  \"affectedSide\": \"${affectedSide}\",\n")
                                append("  \"phoneNumber\": \"${phoneNumber}\",\n")
                                append("  \"medicalCondition\": \"${medicalCondition}\",\n")
                                append("  \"mobilityLevel\": \"${mobilityLevel}\",\n")
                                append("  \"doctorName\": \"${doctorName.replace("\"", "\\\"")}\",\n")
                                append("  \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\"\n")
                                append("}\n")
                            }
                            zipOut.putNextEntry(ZipEntry("user_profile.json"))
                            zipOut.write(profileJson.toByteArray(Charsets.UTF_8))
                            zipOut.closeEntry()

                            // 4. README.txt
                            val readme = """
                                SteadySense AI - Bộ dữ liệu phục hồi chức năng
                                ------------------------------------------------
                                Xuất lúc: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
                                Phiên bản: 2.0.0
                                Giấy phép: Apache License 2.0
                                
                                Danh sách tệp:
                                1. workout_sessions.json: Toàn bộ lịch sử các buổi tập, điểm số vững tay và số rep.
                                2. workout_schedules.json: Kế hoạch lịch tập của bệnh nhân.
                                3. user_profile.json: Thông tin hồ sơ y tế phục hồi chức năng (ẩn danh/cục bộ).
                            """.trimIndent()
                            zipOut.putNextEntry(ZipEntry("README.txt"))
                            zipOut.write(readme.toByteArray(Charsets.UTF_8))
                            zipOut.closeEntry()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        isExportingZip = false
                        Toast.makeText(context, "Đã xuất dữ liệu nghiên cứu ra file ZIP thành công!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isExportingZip = false
                        Toast.makeText(context, "Lỗi khi xuất file ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun refreshWatchInfo() {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    watchName = nodes.first().displayName
                    watchConnected = true
                } else {
                    watchName = "Chưa kết nối đồng hồ"
                    watchConnected = false
                }
            }
            .addOnFailureListener {
                watchName = "Lỗi kết nối Wear OS"
                watchConnected = false
            }
    }

    LaunchedEffect(Unit) {
        refreshWatchInfo()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        item {
            SettingsHeader()
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Nhóm 1: Thiết bị & Kết nối
                SettingsGroupTitle("THIẾT BỊ & KẾT NỐI")

                // Card đồng hồ
                DeviceStatusCard(
                    icon = Icons.Rounded.Watch,
                    title = "Đồng hồ: $watchName",
                    subtitle = if (watchConnected) "Đã kết nối ● Sẵn sàng thu nhận IMU" else "Chưa tìm thấy thiết bị",
                    isConnected = watchConnected,
                    isLoading = isCheckingWatch,
                    onActionClick = {
                        if (!isCheckingWatch) {
                            scope.launch {
                                isCheckingWatch = true
                                refreshWatchInfo()
                                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                                    nodes.forEach { node ->
                                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.PING, ByteArray(0))
                                    }
                                }
                                delay(2500L)
                                isCheckingWatch = false
                            }
                        }
                    },
                    actionText = if (isCheckingWatch) "Kiểm tra..." else "Kiểm tra",
                )

                // Card điện thoại
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
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = TealDark,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                            Text("Điện thoại: $manufacturer ${Build.MODEL}", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · RAM Sẵn sàng", color = Muted, fontSize = 12.sp)
                        }
                    }
                }

                // Nhóm 2: HỒ SƠ NGƯỜI DÙNG
                SettingsGroupTitle("HỒ SƠ NGƯỜI TẬP PHỤC HỒI")

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp))
                        .clickable { showProfileDialog = true },
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Hàng 1: Avatar chữ cái đầu + Tên + Tuổi/Giới tính + Nút Sửa
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val initial = patientName.trim().takeIf { it.isNotEmpty() }?.first()?.uppercaseChar()?.toString() ?: "P"
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Teal, TealDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = initial,
                                    color = White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = patientName.ifBlank { "Chưa đặt hồ sơ người tập" },
                                    color = Ink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(2.dp))
                                val ageText = birthYear.toIntOrNull()?.let {
                                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                    val age = currentYear - it
                                    if (age in 1..120) "$age tuổi" else null
                                }
                                val subDetails = buildList {
                                    if (ageText != null) add(ageText)
                                    if (birthYear.isNotBlank()) add("SN $birthYear")
                                    if (gender.isNotBlank()) add(gender)
                                }
                                Text(
                                    text = if (subDetails.isNotEmpty()) subDetails.joinToString(" · ") else "Chạm để cập nhật thông tin cá nhân",
                                    color = Muted,
                                    fontSize = 12.5.sp,
                                )
                            }

                            Surface(
                                color = TealSoft,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Teal.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable { showProfileDialog = true },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        tint = TealDark,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Sửa",
                                        color = TealDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        // Hàng 2: Badges y tế
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ProfileInfoBadge(
                                icon = Icons.Rounded.FitnessCenter,
                                title = "Tay tập",
                                value = affectedSide.ifBlank { "Chưa chọn" },
                                badgeColor = TealSoft,
                                contentColor = TealDark,
                                modifier = Modifier.weight(1f),
                            )

                            ProfileInfoBadge(
                                icon = Icons.Rounded.Healing,
                                title = "Bệnh lý",
                                value = medicalCondition.ifBlank { "Phục hồi" },
                                badgeColor = Canvas,
                                contentColor = Ink,
                                modifier = Modifier.weight(1.2f),
                            )

                            ProfileInfoBadge(
                                icon = Icons.Rounded.CheckCircle,
                                title = "Vận động",
                                value = mobilityLevel.ifBlank { "Bình thường" },
                                badgeColor = Canvas,
                                contentColor = Ink,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Hàng 3: Bác sĩ hoặc SĐT người thân nếu có
                        val hasContactInfo = phoneNumber.isNotBlank() || doctorName.isNotBlank()
                        if (hasContactInfo) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Canvas)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                if (phoneNumber.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Phone, contentDescription = null, tint = TealDark, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Người thân: $phoneNumber", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (doctorName.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.MedicalServices, contentDescription = null, tint = TealDark, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("BS: $doctorName", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Dòng bảo mật y tế
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = Teal,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Hồ sơ y tế lưu cục bộ an toàn, phục vụ cá nhân hóa liệu trình tập",
                                color = Muted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                // Nhóm 3: Cài đặt bài tập
                SettingsGroupTitle("CÀI ĐẶT BÀI TẬP")

                SettingsToggleCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Nhắc nhở trước buổi tập",
                    subtitle = "Báo chuông nhắc nhở trước giờ tập 15 phút",
                    checked = dailyReminder,
                    onCheckedChange = {
                        dailyReminder = it
                        UserPreferences.setDailyReminderEnabled(context, it)
                        WorkoutReminderManager.onReminderSettingChanged(context, it)
                    },
                )

                SettingsToggleCard(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Giọng nói đếm số lần",
                    subtitle = "Tự động đọc to số lần gập bằng tiếng Việt",
                    checked = voiceGuide,
                    onCheckedChange = {
                        voiceGuide = it
                        UserPreferences.setVoiceGuideEnabled(context, it)
                        if (it) {
                            VoiceGuideManager.speak("Đã bật giọng nói hướng dẫn tập luyện")
                        }
                    },
                )

                SettingsToggleCard(
                    icon = Icons.Rounded.Vibration,
                    title = "Rung phản hồi trên đồng hồ",
                    subtitle = "Rung ở cổ tay khi bắt đầu và cảnh báo khi vung nhanh",
                    checked = watchVibration,
                    onCheckedChange = {
                        watchVibration = it
                        UserPreferences.setWatchVibrationEnabled(context, it)
                        if (it) {
                            sendTestVibrationToWatch(context)
                        }
                    },
                )

                // Nhóm 3: Dữ liệu & Quyền riêng tư
                SettingsGroupTitle("DỮ LIỆU & QUYỀN RIÊNG TƯ")

                // Nút xuất dữ liệu
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                val sessions = PhoneDatabase.get(context).workoutDao().allSessions()
                                val name = UserPreferences.getPatientName(context).ifBlank { "Người tập" }
                                val report = RehabReportGenerator.generateDetailedReport(
                                    patientName = name,
                                    sessions = sessions,
                                )
                                withContext(Dispatchers.Main) {
                                    RehabReportGenerator.shareReport(context, report)
                                }
                            }
                        },
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
                            Icon(Icons.Rounded.IosShare, contentDescription = null, tint = TealDark, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Xuất báo cáo tập luyện", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text("Chia sẻ tóm tắt lịch sử tập qua ứng dụng khác", color = Muted, fontSize = 12.sp)
                        }
                    }
                }

                // Xuất dữ liệu nghiên cứu (.ZIP)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable(enabled = !isExportingZip) {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            createZipLauncher.launch("SteadySense_Research_Data_$timeStamp.zip")
                        },
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
                            if (isExportingZip) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = TealDark,
                                )
                            } else {
                                Icon(Icons.Rounded.Archive, contentDescription = null, tint = TealDark, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Xuất dữ liệu nghiên cứu (.ZIP)", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (isExportingZip) "Đang nén dữ liệu vào file ZIP..." else "Nén toàn bộ cơ sở dữ liệu và hồ sơ ra file .ZIP",
                                color = Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                // Nút xóa dữ liệu
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralSoft.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Coral.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { showClearConfirmDialog = true },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CoralSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Coral, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Xóa dữ liệu cục bộ", color = Coral, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text("Xóa toàn bộ lịch sử các buổi tập đã lưu trên máy", color = Muted, fontSize = 12.sp)
                        }
                    }
                }

                // Nhóm 4: Thông tin & Tuyên bố miễn trừ
                SettingsGroupTitle("THÔNG TIN PHÁP LÝ & MIỄN TRỪ")

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { showDisclaimerDialog = true },
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
                            Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = TealDark, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Tuyên bố miễn trừ y tế", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text("Xem lại cam kết đạo đức & phạm vi nghiên cứu", color = Muted, fontSize = 12.sp)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { showLicenseDialog = true },
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
                            Icon(Icons.Rounded.Description, contentDescription = null, tint = TealDark, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Giấy phép mã nguồn mở (Apache-2.0)", color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            Text("Xem toàn văn điều khoản bản quyền phần mềm", color = Muted, fontSize = 12.sp)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Phiên bản phần mềm", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("SteadySense AI v2.0.0", color = TealDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Giấy phép mã nguồn mở Apache-2.0. Tất cả dữ liệu IMU lưu trữ hoàn toàn trên thiết bị của người dùng.",
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenResearchMode() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Mở Chế độ nghiên cứu / Thu thập dữ liệu thô",
                                color = Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog xác nhận xóa dữ liệu
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Xác nhận xóa dữ liệu?", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Toàn bộ lịch sử các buổi tập và số rep đã lưu trên máy sẽ bị xóa vĩnh viễn.", color = Muted, fontSize = 13.5.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            PhoneDatabase.get(context).workoutDao().clearAllSessions()
                            UserPreferences.clearScheduleCompletionMarks(context)
                            withContext(Dispatchers.Main) {
                                showClearConfirmDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Xóa tất cả", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Hủy", color = Ink)
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(22.dp),
        )
    }

    // Dialog xem lại Tuyên bố miễn trừ y tế
    if (showDisclaimerDialog) {
        MedicalDisclaimerDialog(
            onDismiss = { showDisclaimerDialog = false },
            onAccept = { showDisclaimerDialog = false },
        )
    }

    // Dialog xem Giấy phép mã nguồn mở Apache-2.0
    if (showLicenseDialog) {
        ApacheLicenseDialog(
            onDismiss = { showLicenseDialog = false },
        )
    }

    // Bottom Sheet chỉnh sửa hồ sơ y tế người tập phục hồi
    if (showProfileDialog) {
        var editName by remember { mutableStateOf(patientName) }
        var editBirthYear by remember { mutableStateOf(birthYear) }
        var editGender by remember { mutableStateOf(gender) }
        var editAffectedSide by remember { mutableStateOf(if (affectedSide.isNotBlank()) affectedSide else "Tay phải") }
        var editPhone by remember { mutableStateOf(phoneNumber) }
        var editCondition by remember { mutableStateOf(if (medicalCondition.isNotBlank()) medicalCondition else "Sau tai biến") }
        var editMobility by remember { mutableStateOf(if (mobilityLevel.isNotBlank()) mobilityLevel else "Trung bình") }
        var editDoctor by remember { mutableStateOf(doctorName) }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val calculatedAge = editBirthYear.toIntOrNull()?.let { y ->
            if (y in 1900..currentYear) currentYear - y else null
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showProfileDialog = false },
            sheetState = sheetState,
            containerColor = White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceBorder)
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Tiêu đề & Nút đóng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hồ sơ y tế người tập",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                        Text(
                            text = "Thông tin cá nhân hóa liệu trình phục hồi chức năng",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                    IconButton(onClick = { showProfileDialog = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = Muted,
                        )
                    }
                }

                HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

                // PHẦN 1: THÔNG TIN CÁ NHÂN
                Text(
                    text = "1. THÔNG TIN CÁ NHÂN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark,
                    letterSpacing = 0.5.sp,
                )

                // Họ tên
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Họ và tên người tập:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = { Text("VD: Bác Nguyễn Văn An", color = Muted, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                }

                // Năm sinh + Tuổi & Giới tính (Cùng 1 hàng)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Cột Năm sinh
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Năm sinh:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            if (calculatedAge != null) {
                                Spacer(Modifier.width(4.dp))
                                Text("($calculatedAge tuổi)", fontSize = 12.sp, color = TealDark, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedTextField(
                            value = editBirthYear,
                            onValueChange = { editBirthYear = it.filter { c -> c.isDigit() }.take(4) },
                            placeholder = { Text("VD: 1962", color = Muted, fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                    }

                    // Cột Giới tính
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Giới tính:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Nam", "Nữ").forEach { g ->
                                val isSelected = editGender.equals(g, ignoreCase = true)
                                Surface(
                                    color = if (isSelected) Teal else Canvas,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) Teal else SurfaceBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clickable { editGender = g },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = g,
                                            color = if (isSelected) White else Ink,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Số điện thoại người thân
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Số điện thoại người thân / liên hệ:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        placeholder = { Text("VD: 0912 345 678", color = Muted, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                }

                HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

                // PHẦN 2: TÌNH TRẠNG VẬN ĐỘNG & BỆNH LÝ
                Text(
                    text = "2. TÌNH TRẠNG VẬN ĐỘNG & TAY TẬP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark,
                    letterSpacing = 0.5.sp,
                )

                // Tay tổn thương cần tập
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tay cần tập phục hồi chức năng:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Tay trái", "Tay phải", "Cả hai tay").forEach { side ->
                            val isSelected = editAffectedSide.contains(side.replace("Tay ", ""), ignoreCase = true) || editAffectedSide == side
                            Surface(
                                color = if (isSelected) Teal else Canvas,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) Teal else SurfaceBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editAffectedSide = side }
                                    .padding(vertical = 2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FitnessCenter,
                                        contentDescription = null,
                                        tint = if (isSelected) White else TealDark,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = side,
                                        color = if (isSelected) White else Ink,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // Tình trạng bệnh lý
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tình trạng bệnh lý / Phục hồi:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    val conditions = listOf(
                        "Sau tai biến",
                        "Chấn thương cơ khớp",
                        "Parkinson",
                        "Cứng khớp tuổi già",
                        "Mỏi tay văn phòng",
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        conditions.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowItems.forEach { c ->
                                    val isSelected = editCondition.equals(c, ignoreCase = true)
                                    Surface(
                                        color = if (isSelected) TealSoft else Canvas,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isSelected) Teal else SurfaceBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { editCondition = c },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Healing,
                                                contentDescription = null,
                                                tint = if (isSelected) TealDark else Muted,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = c,
                                                color = if (isSelected) TealDark else Ink,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Mức độ vận động của tay
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mức độ cử động tay hiện tại:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    val levels = listOf(
                        "Hạn chế nhiều" to "Cơ yếu, cần người nhà hoặc tập nhẹ",
                        "Trung bình" to "Cử động co duỗi cơ bản được",
                        "Khá tốt" to "Cử động linh hoạt, duy trì sức cơ",
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        levels.forEach { (levelName, desc) ->
                            val isSelected = editMobility.equals(levelName, ignoreCase = true)
                            Surface(
                                color = if (isSelected) TealSoft else Canvas,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, if (isSelected) Teal else SurfaceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editMobility = levelName },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (isSelected) TealDark else Muted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = levelName,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TealDark else Ink,
                                            fontSize = 13.sp,
                                        )
                                        Text(
                                            text = desc,
                                            color = Muted,
                                            fontSize = 11.5.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

                // PHẦN 3: THEO DÕI Y TẾ
                Text(
                    text = "3. BÁC SĨ / KỸ THUẬT VIÊN THEO DÕI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark,
                    letterSpacing = 0.5.sp,
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bác sĩ phụ trách hoặc cơ sở điều trị:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    OutlinedTextField(
                        value = editDoctor,
                        onValueChange = { editDoctor = it },
                        placeholder = { Text("VD: BS. Nguyễn Văn A - BV Bạch Mai", color = Muted, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.MedicalServices, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Các nút Lưu & Hủy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { showProfileDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Text("Hủy", color = Ink, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            patientName = editName.trim()
                            birthYear = editBirthYear.trim()
                            gender = editGender
                            affectedSide = editAffectedSide
                            phoneNumber = editPhone.trim()
                            medicalCondition = editCondition
                            mobilityLevel = editMobility
                            doctorName = editDoctor.trim()

                            UserPreferences.setPatientName(context, patientName)
                            UserPreferences.setBirthYear(context, birthYear.toIntOrNull() ?: 0)
                            UserPreferences.setGender(context, gender)
                            UserPreferences.setAffectedSide(context, affectedSide)
                            UserPreferences.setPhoneNumber(context, phoneNumber)
                            UserPreferences.setCondition(context, medicalCondition)
                            UserPreferences.setMobilityLevel(context, mobilityLevel)
                            UserPreferences.setDoctorName(context, doctorName)

                            showProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(50.dp),
                    ) {
                        Text("Lưu hồ sơ", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
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
        Text(
            "THIẾT LẬP",
            color = TealDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Cài đặt ứng dụng",
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tùy chỉnh thiết bị, thông số bài tập và quyền riêng tư.",
            color = Muted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun DeviceStatusCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isConnected: Boolean,
    isLoading: Boolean = false,
    onActionClick: () -> Unit,
    actionText: String,
) {
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
                    .background(if (isConnected) TealSoft else AmberSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isConnected) TealDark else Amber,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = if (isConnected) TealDark else Amber, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onActionClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = TealDark,
                    )
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(actionText, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = TealDark,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = Teal,
                ),
            )
        }
    }
}

private fun sendTestVibrationToWatch(ctx: Context) {
    runCatching {
        val bytes = "TEST_VIBRATE".toByteArray(Charsets.UTF_8)
        com.google.android.gms.wearable.Wearable.getNodeClient(ctx).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                com.google.android.gms.wearable.Wearable.getMessageClient(ctx)
                    .sendMessage(node.id, vn.edu.ictu.steadysense.core.TransportPaths.EXERCISE_CUE, bytes)
            }
        }
    }
}

@Composable
private fun ProfileInfoBadge(
    icon: ImageVector,
    title: String,
    value: String,
    badgeColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    color = Muted,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ApacheLicenseDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = TealDark,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Giấy phép Apache-2.0",
                        color = Ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Mã nguồn mở SteadySense AI",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Surface(
                    color = TealSoft.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Teal.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TealDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Quyền tự do & Bảo mật", color = TealDark, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Phần mềm được phát hành theo Giấy phép Apache 2.0. Bạn được tự do sử dụng cho mục đích nghiên cứu, học tập và phát triển phục hồi chức năng cá nhân. Toàn bộ dữ liệu cảm biến được bảo mật trên máy bạn.",
                            color = Ink,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = """
                        Apache License
                        Version 2.0, January 2004
                        http://www.apache.org/licenses/

                        TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

                        1. Definitions.
                        "License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

                        "Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

                        2. Grant of Copyright License.
                        Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

                        3. Grant of Patent License.
                        Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free patent license.

                        4. Disclaimer of Warranty.
                        Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                    """.trimIndent(),
                    color = InkSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.5.sp,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Đã hiểu & Đóng", color = White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(22.dp),
    )
}


