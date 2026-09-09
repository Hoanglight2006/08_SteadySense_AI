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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MedicalDisclaimerDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(TealSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.HealthAndSafety,
                    contentDescription = null,
                    tint = TealDark,
                    modifier = Modifier.size(34.dp),
                )
            }
        },
        title = {
            Text(
                "Tuyên bố miễn trừ y tế",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = Canvas,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                ) {
                    Text(
                        "SteadySense AI là nguyên mẫu nghiên cứu kỹ thuật, KHÔNG phải thiết bị y tế.",
                        color = Coral,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Text(
                    "Ứng dụng này:",
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                DisclaimerBulletPoint("Không chẩn đoán bất kỳ bệnh lý hoặc chấn thương nào.")
                DisclaimerBulletPoint("Không thay thế sự hướng dẫn, chỉ định và giám sát của bác sĩ hoặc kỹ thuật viên phục hồi chức năng.")
                DisclaimerBulletPoint("Không đưa ra khuyến nghị điều trị hay can thiệp y khoa.")

                Text(
                    "Dữ liệu cảm biến được lưu trữ cục bộ trên thiết bị của bạn. Không có dữ liệu sức khỏe định danh nào được tải lên máy chủ ngoài.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) {
                Text(
                    "Tôi đã hiểu và tiếp tục",
                    color = White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.border(1.5.dp, SurfaceBorder, RoundedCornerShape(26.dp)),
    )
}

@Composable
private fun DisclaimerBulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("• ", color = Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(text, color = InkSecondary, fontSize = 12.5.sp, lineHeight = 17.sp)
    }
}
