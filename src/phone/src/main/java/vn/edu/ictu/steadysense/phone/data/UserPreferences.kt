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

package vn.edu.ictu.steadysense.phone.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Quản lý cài đặt người dùng và cờ Tuyên bố miễn trừ y tế (Medical Disclaimer).
 */
object UserPreferences {
    private const val PREF_NAME = "steadysense_user_prefs"

    private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    private const val KEY_DEFAULT_REPS = "default_reps"
    private const val KEY_DAILY_REMINDER = "daily_reminder"
    private const val KEY_VOICE_GUIDE = "voice_guide"
    private const val KEY_WATCH_VIBRATION = "watch_vibration"
    private const val KEY_FIRST_APP_OPEN_TIME = "first_app_open_time"
    private const val KEY_SCHEDULES_INITIALIZED = "schedules_initialized"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun hasInitializedDefaultSchedules(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCHEDULES_INITIALIZED, false)
    }

    fun setHasInitializedDefaultSchedules(context: Context, initialized: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCHEDULES_INITIALIZED, initialized).apply()
    }

    fun getFirstAppOpenTime(context: Context): Long {
        val prefs = getPrefs(context)
        var time = prefs.getLong(KEY_FIRST_APP_OPEN_TIME, 0L)
        if (time == 0L) {
            time = System.currentTimeMillis()
            prefs.edit().putLong(KEY_FIRST_APP_OPEN_TIME, time).apply()
        }
        return time
    }

    fun isDisclaimerAccepted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }

    fun setDisclaimerAccepted(context: Context, accepted: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted).apply()
    }

    fun getDefaultTargetReps(context: Context): Int {
        return getPrefs(context).getInt(KEY_DEFAULT_REPS, 10)
    }

    fun setDefaultTargetReps(context: Context, reps: Int) {
        getPrefs(context).edit().putInt(KEY_DEFAULT_REPS, reps).apply()
    }

    fun isDailyReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DAILY_REMINDER, true)
    }

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DAILY_REMINDER, enabled).apply()
    }

    fun isVoiceGuideEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VOICE_GUIDE, true)
    }

    fun setVoiceGuideEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VOICE_GUIDE, enabled).apply()
    }

    fun isWatchVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WATCH_VIBRATION, true)
    }

    fun setWatchVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WATCH_VIBRATION, enabled).apply()
    }

    // --- Hồ sơ người dùng (User Profile) ---

    private const val KEY_PATIENT_NAME = "patient_name"
    private const val KEY_BIRTH_YEAR = "birth_year"
    private const val KEY_GENDER = "gender"
    private const val KEY_AFFECTED_SIDE = "affected_side"
    private const val KEY_WEEKLY_TARGET_SESSIONS = "weekly_target_sessions"

    fun getPatientName(context: Context): String {
        return getPrefs(context).getString(KEY_PATIENT_NAME, "") ?: ""
    }

    fun setPatientName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_PATIENT_NAME, name).apply()
    }

    fun getBirthYear(context: Context): Int {
        return getPrefs(context).getInt(KEY_BIRTH_YEAR, 0)
    }

    fun setBirthYear(context: Context, year: Int) {
        getPrefs(context).edit().putInt(KEY_BIRTH_YEAR, year).apply()
    }

    fun getGender(context: Context): String {
        return getPrefs(context).getString(KEY_GENDER, "") ?: ""
    }

    fun setGender(context: Context, gender: String) {
        getPrefs(context).edit().putString(KEY_GENDER, gender).apply()
    }

    fun getAffectedSide(context: Context): String {
        return getPrefs(context).getString(KEY_AFFECTED_SIDE, "") ?: ""
    }

    fun setAffectedSide(context: Context, side: String) {
        getPrefs(context).edit().putString(KEY_AFFECTED_SIDE, side).apply()
    }

    fun getWeeklyTargetSessions(context: Context): Int {
        return getPrefs(context).getInt(KEY_WEEKLY_TARGET_SESSIONS, 7)
    }

    fun setWeeklyTargetSessions(context: Context, target: Int) {
        getPrefs(context).edit().putInt(KEY_WEEKLY_TARGET_SESSIONS, target).apply()
    }

    private const val KEY_PHONE_NUMBER = "phone_number"
    private const val KEY_CONDITION = "medical_condition"
    private const val KEY_MOBILITY_LEVEL = "mobility_level"
    private const val KEY_DOCTOR_NAME = "supervising_doctor"

    fun getPhoneNumber(context: Context): String =
        getPrefs(context).getString(KEY_PHONE_NUMBER, "") ?: ""

    fun setPhoneNumber(context: Context, phone: String) {
        getPrefs(context).edit().putString(KEY_PHONE_NUMBER, phone).apply()
    }

    fun getCondition(context: Context): String =
        getPrefs(context).getString(KEY_CONDITION, "Phục hồi chức năng vận động") ?: "Phục hồi chức năng vận động"

    fun setCondition(context: Context, condition: String) {
        getPrefs(context).edit().putString(KEY_CONDITION, condition).apply()
    }

    fun getMobilityLevel(context: Context): String =
        getPrefs(context).getString(KEY_MOBILITY_LEVEL, "Trung bình") ?: "Trung bình"

    fun setMobilityLevel(context: Context, level: String) {
        getPrefs(context).edit().putString(KEY_MOBILITY_LEVEL, level).apply()
    }

    fun getDoctorName(context: Context): String =
        getPrefs(context).getString(KEY_DOCTOR_NAME, "") ?: ""

    fun setDoctorName(context: Context, doctor: String) {
        getPrefs(context).edit().putString(KEY_DOCTOR_NAME, doctor).apply()
    }

    // --- Quản lý trạng thái hoàn thành bài tập theo lịch ---

    fun markScheduleCompleted(context: Context, dateStr: String, scheduleId: String, sessionId: String) {
        getPrefs(context).edit().putString("sched_completed_${dateStr}_${scheduleId}", sessionId).apply()
    }

    fun isScheduleCompleted(context: Context, dateStr: String, scheduleId: String): Boolean {
        return getPrefs(context).contains("sched_completed_${dateStr}_${scheduleId}")
    }

    fun clearScheduleCompletionMarks(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("sched_completed_") }.forEach { key ->
            editor.remove(key)
        }
        editor.apply()
    }
}
