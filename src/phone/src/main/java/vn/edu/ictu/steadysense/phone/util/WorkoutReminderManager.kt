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

package vn.edu.ictu.steadysense.phone.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.edu.ictu.steadysense.phone.MainActivity
import vn.edu.ictu.steadysense.phone.R
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase
import vn.edu.ictu.steadysense.phone.data.UserPreferences
import java.util.Calendar

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Gấp – duỗi khuỷu tay"
        val scheduledTime = intent.getStringExtra("SCHEDULED_TIME") ?: "08:30"
        val isExactTime = intent.getBooleanExtra("IS_EXACT_TIME", true)
        val notifId = intent.getIntExtra("NOTIF_ID", 1002)

        Log.i("WorkoutReminder", "Received reminder broadcast for $exerciseName at $scheduledTime (exact=$isExactTime)")

        val title = if (isExactTime) {
            "SteadySense AI · Đã đến giờ tập!"
        } else {
            "SteadySense AI · Sắp đến giờ tập!"
        }

        val message = if (isExactTime) {
            "Đã đến giờ tập \"$exerciseName\" ($scheduledTime). Hãy mở ứng dụng và bắt đầu bài tập ngay bây giờ."
        } else {
            "Còn 15 phút nữa đến lịch tập \"$exerciseName\" ($scheduledTime). Hãy khởi động nhẹ nhàng và chuẩn bị."
        }

        WorkoutReminderManager.showNotification(
            context = context,
            notificationId = notifId,
            title = title,
            message = message,
        )

        // Reschedule for next day / week
        WorkoutReminderManager.scheduleNextReminder(context)
    }
}

object WorkoutReminderManager {
    private const val TAG = "WorkoutReminderManager"
    const val CHANNEL_ID = "steadysense_reminders"
    private const val CONFIRMATION_NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Nhắc nhở lịch tập SteadySense"
            val descriptionText = "Thông báo nhắc nhở khi đến giờ tập phục hồi chức năng"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
    ) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing or denied", e)
        }
    }

    fun onReminderSettingChanged(context: Context, enabled: Boolean) {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (enabled) {
            showNotification(
                context = context,
                notificationId = CONFIRMATION_NOTIFICATION_ID,
                title = "Đã bật nhắc nhở buổi tập",
                message = "SteadySense AI sẽ thông báo khi đến giờ tập và trước giờ tập 15 phút để bạn sẵn sàng.",
            )
            scheduleNextReminder(context)
        } else {
            cancelReminders(context)
            notificationManager?.cancel(CONFIRMATION_NOTIFICATION_ID)
            notificationManager?.cancel(1002)
        }
    }

    fun scheduleNextReminder(context: Context) {
        if (!UserPreferences.isDailyReminderEnabled(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PhoneDatabase.get(context).workoutDao()
                val schedules = db.allSchedules()
                if (schedules.isEmpty()) return@launch

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch
                val now = Calendar.getInstance()

                for (s in schedules) {
                    val hour = s.scheduledTime.substringBefore(":").toIntOrNull() ?: 8
                    val minute = s.scheduledTime.substringAfter(":").take(2).toIntOrNull() ?: 30

                    // 1. Báo ĐÚNG GIỜ TẬP
                    val calExact = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (!calExact.after(now)) {
                        // Nếu giờ hôm nay đã qua, lên lịch ngày mai
                        calExact.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val exactReqCode = Math.abs(s.id.hashCode() % 100000)
                    val exactIntent = Intent(context, WorkoutReminderReceiver::class.java).apply {
                        putExtra("EXERCISE_NAME", s.exerciseName)
                        putExtra("SCHEDULED_TIME", s.scheduledTime)
                        putExtra("IS_EXACT_TIME", true)
                        putExtra("NOTIF_ID", exactReqCode)
                    }
                    val pendingExact = PendingIntent.getBroadcast(
                        context,
                        exactReqCode,
                        exactIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
                    )

                    setAlarmSafe(alarmManager, calExact.timeInMillis, pendingExact)
                    Log.i(TAG, "Scheduled EXACT reminder for ${s.exerciseName} at ${calExact.time}")

                    // 2. Báo TRƯỚC GIỜ TẬP 15 PHÚT
                    val calPrior = (calExact.clone() as Calendar).apply {
                        add(Calendar.MINUTE, -15)
                    }
                    if (calPrior.after(now)) {
                        val priorReqCode = exactReqCode + 200000
                        val priorIntent = Intent(context, WorkoutReminderReceiver::class.java).apply {
                            putExtra("EXERCISE_NAME", s.exerciseName)
                            putExtra("SCHEDULED_TIME", s.scheduledTime)
                            putExtra("IS_EXACT_TIME", false)
                            putExtra("NOTIF_ID", priorReqCode)
                        }
                        val pendingPrior = PendingIntent.getBroadcast(
                            context,
                            priorReqCode,
                            priorIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
                        )
                        setAlarmSafe(alarmManager, calPrior.timeInMillis, pendingPrior)
                        Log.i(TAG, "Scheduled 15-MIN PRIOR reminder for ${s.exerciseName} at ${calPrior.time}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling reminders", e)
            }
        }
    }

    private fun setAlarmSafe(alarmManager: AlarmManager, timeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot set exact alarm, falling back to inexact", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PhoneDatabase.get(context).workoutDao()
                val schedules = db.allSchedules()
                for (s in schedules) {
                    val exactReqCode = Math.abs(s.id.hashCode() % 100000)
                    val priorReqCode = exactReqCode + 200000

                    cancelPending(context, alarmManager, exactReqCode)
                    cancelPending(context, alarmManager, priorReqCode)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling reminders", e)
            }
        }
    }

    private fun cancelPending(context: Context, alarmManager: AlarmManager, reqCode: Int) {
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
