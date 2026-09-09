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

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Trợ lý giọng nói tiếng Việt đọc số lần gập và nhắc nhở tư thế tập luyện.
 */
object VoiceGuideManager {
    private const val TAG = "VoiceGuideManager"

    @Volatile
    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (tts != null) return
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val viLocale = Locale("vi", "VN")
                val langResult = tts?.setLanguage(viLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Vietnamese TTS not directly supported, using default locale")
                    tts?.language = Locale.getDefault()
                }
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
                isInitialized = true
                Log.i(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized || tts == null) return
        try {
            tts?.speak(text, queueMode, null, "SS_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak text: $text", e)
        }
    }

    fun speakRep(repNumber: Int) {
        val word = when (repNumber) {
            1 -> "Một"
            2 -> "Hai"
            3 -> "Ba"
            4 -> "Bốn"
            5 -> "Năm"
            6 -> "Sáu"
            7 -> "Bảy"
            8 -> "Tám"
            9 -> "Chín"
            10 -> "Mười"
            11 -> "Mười một"
            12 -> "Mười hai"
            13 -> "Mười ba"
            14 -> "Mười bốn"
            15 -> "Mười lăm"
            16 -> "Mười sáu"
            17 -> "Mười bảy"
            18 -> "Mười tám"
            19 -> "Mười chín"
            20 -> "Hai mươi"
            else -> "$repNumber"
        }
        speak(word)
    }

    fun speakWarning(text: String) {
        speak(text)
    }

    fun speakFinish() {
        speak("Hoàn thành bài tập")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
        tts = null
        isInitialized = false
    }
}
