package vn.edu.ictu.steadysense.phone.util

import android.content.Context
import android.content.Intent
import vn.edu.ictu.steadysense.phone.data.WorkoutSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RehabReportGenerator {
    fun generateDetailedReport(
        patientName: String = "Bác An",
        sessions: List<WorkoutSessionEntity>,
        periodLabel: String = "TUẦN NÀY",
    ): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val nowStr = dateFormat.format(Date())

        val totalSessions = sessions.size
        val flexionSessions = sessions.filter { !it.exerciseName.contains("Massage") }
        val massageSessions = sessions.filter { it.exerciseName.contains("Massage") }

        val totalFlexionReps = flexionSessions.sumOf { it.completedReps }
        val totalMassageMins = massageSessions.sumOf { it.targetReps }

        val avgAccuracy = if (sessions.isNotEmpty()) {
            sessions.map { it.accuracyPercentage }.average().toInt()
        } else 0

        val avgSteadyScore = if (sessions.isNotEmpty()) {
            String.format(Locale.US, "%.1f", sessions.map { it.steadyScore.toDouble() }.average())
        } else "0.0"

        val passedSessions = sessions.count { it.sessionState == "RELIABLE" || it.accuracyPercentage >= 80 }
        val passRate = if (totalSessions > 0) (passedSessions * 100 / totalSessions) else 0

        val leftCount = sessions.count { it.selectedHand == "LEFT" }
        val rightCount = sessions.count { it.selectedHand == "RIGHT" }

        val clinicalAssessment = when {
            avgAccuracy >= 85 -> "Rất tích cực (Đạt chuẩn xuất sắc) — Biên độ gập và nhịp độ vận động rất ổn định, cơ lực tiến triển rõ rệt."
            avgAccuracy >= 65 -> "Khá tốt (Đạt yêu cầu) — Nhịp độ tương đối đều, cần chú ý kiểm soát không vung tay nhanh ở các rep cuối."
            avgAccuracy > 0 -> "Cần kiên trì theo dõi thêm — Ghi nhận hiện tượng rung giật cơ hoặc vung tay quá nhanh khi gập duỗi."
            else -> "Chưa có đủ dữ liệu tập luyện để đưa ra kết luận lâm sàng."
        }

        val sb = StringBuilder()
        sb.appendLine("============================================================")
        sb.appendLine("       BÁO CÁO TIẾN TRÌNH PHỤC HỒI CHỨC NĂNG VẬN ĐỘNG")
        sb.appendLine("                  HỆ THỐNG STEADYSENSE AI")
        sb.appendLine("============================================================")
        sb.appendLine("I. HỒ SƠ NGƯỜI BỆNH & THIẾT BỊ GIÁM SÁT")
        sb.appendLine("• Người tập            : $patientName (Mã HS: BN-STEADY-2026)")
        sb.appendLine("• Thời gian kết xuất    : $nowStr")
        sb.appendLine("• Thiết bị thu nhận    : Samsung Galaxy Watch FE (Wear OS 5.0)")
        sb.appendLine("• Cảm biến IMU         : 6 bậc tự do (Gia tốc kế + Con quay hồi chuyển 20Hz)")
        sb.appendLine("• Mô hình AI phân tích  : MobileNetV3 + Bi-LSTM (SteadySense Quality Fusion)")
        sb.appendLine("")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("II. TỔNG QUAN CHỈ SỐ VẬN ĐỘNG $periodLabel")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("• Tổng số buổi đã tập   : $totalSessions buổi")
        sb.appendLine("• Phân bổ tay tập phục hồi: $leftCount buổi tay trái, $rightCount buổi tay phải")
        sb.appendLine("• Khối lượng bài tập    : $totalFlexionReps lần gập khuỷu tay + $totalMassageMins phút massage thư giãn")
        sb.appendLine("• Tỷ lệ đạt chuẩn kỹ thuật: $passRate% ($passedSessions/$totalSessions buổi đạt chuẩn)")
        sb.appendLine("• Độ chính xác trung bình : $avgAccuracy%")
        sb.appendLine("• Điểm vững tay trung bình: $avgSteadyScore / 10.0 (SteadyScore)")
        sb.appendLine("• Đánh giá lâm sàng AI  : $clinicalAssessment")
        sb.appendLine("")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("III. CHI TIẾT CÁC PHIÊN TẬP ĐÃ LƯU TRỮ")
        sb.appendLine("------------------------------------------------------------")

        if (sessions.isNotEmpty()) {
            sessions.forEachIndexed { idx, s ->
                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(s.startedAt))
                val isMassage = s.exerciseName.contains("Massage")
                val handLabel = if (s.selectedHand == "LEFT") "Tay Trái" else "Tay Phải"
                val stateText = when (s.sessionState) {
                    "RELIABLE" -> "● ĐẠT CHUẨN XUẤT SẮC"
                    "PARTIAL" -> "◐ HOÀN THÀNH MỘT PHẦN"
                    else -> "✕ CẦN CỐ GẮNG THÊM"
                }

                sb.appendLine("[Buổi ${idx + 1}] $timeStr — ${s.exerciseName} ($handLabel)")
                sb.appendLine("   - Tay phục hồi : $handLabel (đồng hồ đeo cổ tay $handLabel)")
                if (isMassage) {
                    sb.appendLine("   - Thời lượng   : ${s.targetReps} phút xoa bóp & thả lỏng cơ")
                    sb.appendLine("   - Trạng thái   : $stateText (${s.accuracyPercentage}% thời gian)")
                    sb.appendLine("   - SteadyScore  : ${String.format(Locale.US, "%.1f", s.steadyScore)} / 10.0")
                    sb.appendLine("   - Tác dụng     : Giảm co thắt, hỗ trợ tuần hoàn máu vùng cẳng tay.")
                } else {
                    sb.appendLine("   - Khối lượng   : ${s.completedReps}/${s.targetReps} lần gập hợp lệ")
                    sb.appendLine("   - Trạng thái   : $stateText (${s.accuracyPercentage}% chuẩn xác)")
                    sb.appendLine("   - SteadyScore  : ${String.format(Locale.US, "%.1f", s.steadyScore)} / 10.0")
                    val note = when {
                        s.accuracyPercentage >= 80 -> "Vận động kiểm soát tốt, biên độ đầy đủ, nhịp điệu đều đặn."
                        s.accuracyPercentage >= 50 -> "Biên độ đạt yêu cầu, một vài lần gập còn hơi gấp gáp."
                        else -> "Có hiện tượng vung tay quá nhanh hoặc chưa đạt đủ biên độ gập khuỷu tay."
                    }
                    sb.appendLine("   - Nhận xét kỹ thuật: $note")
                }
                sb.appendLine("")
            }
        } else {
            sb.appendLine("   (Chưa có phiên tập nào được ghi nhận)")
            sb.appendLine("")
        }

        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("IV. KHUYẾN NGHỊ CHUYÊN MÔN CHO GIAI ĐOẠN TIẾP THEO")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("1. Kiểm soát nhịp độ (Tempo):")
        sb.appendLine("   • Thực hiện theo nguyên tắc 2-1-2: 2 giây gập vào, giữ 1 giây ở đỉnh, 2 giây duỗi ra.")
        sb.appendLine("   • Lắng nghe nhịp rung và giọng đọc hướng dẫn tiếng Việt từ điện thoại/đồng hồ.")
        sb.appendLine("2. Thả lỏng sau tập:")
        sb.appendLine("   • Thực hiện bài tập 'Thả lỏng & Massage' 5 phút sau khi kết thúc các bài gập duỗi.")
        sb.appendLine("3. Lưu ý an toàn:")
        sb.appendLine("   • Khi có cảm giác đau nhức bất thường, hãy tạm dừng và tham vấn ý kiến bác sĩ.")
        sb.appendLine("   • Người bệnh có thể xuất báo cáo này định kỳ để gửi cho Bác sĩ điều trị / Kỹ thuật viên Phục hồi chức năng theo dõi từ xa.")
        sb.appendLine("============================================================")
        sb.appendLine("Bản quyền © 2026 SteadySense AI — Phục hồi chức năng thông minh")
        sb.appendLine("Lưu ý: Báo cáo mang tính tham khảo hỗ trợ theo dõi, không thay thế chẩn đoán chuyên môn của bác sĩ.")
        sb.appendLine("============================================================")

        return sb.toString()
    }

    fun shareReport(context: Context, reportContent: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportContent)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Xuất báo cáo phục hồi chức năng"))
    }
}
