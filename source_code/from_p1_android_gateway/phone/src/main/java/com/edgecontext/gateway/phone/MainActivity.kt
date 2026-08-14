package com.edgecontext.gateway.phone

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A participant-facing collector: one tap starts one fixed-duration session.
 * Participant IDs and session IDs are generated locally so that consecutive
 * people cannot overwrite or mix their recordings.
 */
class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var recorder: PhoneSensorRecorder
    private lateinit var status: TextView
    private lateinit var startButton: Button
    private var collectionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        recorder = PhoneSensorRecorder(this)
        setContentView(buildUi())
        handleAutomationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAutomationIntent(intent)
    }

    /** Retains the existing ADB protocol for supervised/automated runs. */
    private fun handleAutomationIntent(command: Intent) {
        if (command.getBooleanExtra("probe_watch", false)) {
            scope.launch {
                delay(1_000)
                val watchCount = sendRawWatchCommand(JSONObject().put("action", "probe").toString())
                status.text = "Connected watch nodes: $watchCount"
                Log.i("EdgeCollector", "watch_probe nodes=$watchCount")
            }
            return
        }
        val activity = command.getStringExtra("auto_activity") ?: return
        val subject = command.getStringExtra("subject_id") ?: nextParticipantId()
        val placement = command.getStringExtra("placement") ?: DEFAULT_PLACEMENT
        val duration = command.getIntExtra("duration_seconds", MEASUREMENT_DURATION_SECONDS)
        val autoClear = command.getBooleanExtra("auto_clear", true)
        val autoExport = command.getBooleanExtra("auto_export", true)
        scope.launch {
            delay(1_500)
            if (autoClear) {
                withContext(Dispatchers.IO) { AppDatabase.get(this@MainActivity).sensorSampleDao().clear() }
                status.text = "Database cleared by ADB automation. Starting $activity..."
            }
            startMeasurement(subject, activity, placement, duration, autoExport)
        }
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 40)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(this).apply {
            text = "Timed watch-phone collection"
            textSize = 22f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Press Start once. Measurement ends automatically."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 20)
        })
        startButton = button("START MEASUREMENT") { startNextParticipantMeasurement() }
        root.addView(startButton)
        status = TextView(this).apply {
            text = "Ready for ${previewNextParticipantId()}.\nKeep the phone and watch connected."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        }
        root.addView(status)
        return ScrollView(this).apply { addView(root) }
    }

    private fun startNextParticipantMeasurement() {
        startMeasurement(
            subject = nextParticipantId(),
            activity = DEFAULT_LABEL,
            placement = DEFAULT_PLACEMENT,
            durationSeconds = MEASUREMENT_DURATION_SECONDS,
            autoExport = false,
        )
    }

    private fun startMeasurement(
        subject: String,
        activity: String,
        placement: String,
        durationSeconds: Int,
        autoExport: Boolean,
    ) {
        if (collectionJob?.isActive == true) return
        val duration = durationSeconds.coerceAtLeast(10)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val trialId = "${subject}_${activity}_${placement}_$timestamp"
        val metadata = CollectionMetadata(subject, trialId, activity, placement)

        startButton.isEnabled = false
        collectionJob = scope.launch {
            try {
                val watchCount = sendWatchCommand("start", metadata)
                Log.i("EdgeCollector", "trial_start id=$trialId activity=$activity watch_nodes=$watchCount")
                check(watchCount > 0) { "No connected watch received the command" }
                recorder.start(metadata)

                for (remaining in duration downTo 1) {
                    if (!isActive) break
                    status.text = "Recording $subject\n${remaining}s remaining\nDo not press anything."
                    delay(1_000)
                }
                recorder.stop()
                sendWatchCommand("stop", metadata)
                delay(3_000) // allow the final watch batch to reach the phone database
                val counts = trialCounts(trialId)
                Log.i("EdgeCollector", "trial_saved id=$trialId phone=${counts.first} watch=${counts.second}")
                status.text = "Measurement saved for $subject\nphone=${counts.first}, watch=${counts.second}\nReady for ${previewNextParticipantId()}."
            } catch (error: Exception) {
                recorder.stop()
                runCatching { sendWatchCommand("stop", metadata) }
                status.text = "MEASUREMENT FAILED: ${error.message}\nReady to try again."
                Log.e("EdgeCollector", "measurement_failed id=$trialId", error)
                toast(status.text.toString())
            } finally {
                startButton.isEnabled = true
                if (autoExport) {
                    // The delayed launch ensures the collection coroutine has finished.
                    scope.launch {
                        delay(250)
                        exportPackage()
                    }
                }
            }
        }
    }

    private suspend fun sendWatchCommand(action: String, metadata: CollectionMetadata): Int {
        val payload = JSONObject()
            .put("action", action)
            .put("subject_id", metadata.subjectId)
            .put("session_id", metadata.sessionId)
            .put("label", metadata.label)
            .put("placement", "wrist")
            .toString()
        return sendRawWatchCommand(payload)
    }

    private suspend fun sendRawWatchCommand(payload: String): Int = withContext(Dispatchers.IO) {
        val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
        nodes.forEach { node ->
            Wearable.getMessageClient(this@MainActivity)
                .sendMessage(node.id, "/collection_command", payload.toByteArray(Charsets.UTF_8))
                .await()
        }
        nodes.size
    }

    private suspend fun trialCounts(trialId: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(this@MainActivity).sensorSampleDao()
        dao.countForTrial(trialId, "phone") to dao.countForTrial(trialId, "watch")
    }

    private fun exportPackage() {
        if (collectionJob?.isActive == true) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    CsvExporter.export(this@MainActivity, AppDatabase.get(this@MainActivity).sensorSampleDao().getAll())
                }
            }.onSuccess { files ->
                status.text = "Exported to Downloads:\n${files.first}\n${files.second}"
                Log.i("EdgeCollector", "export_complete csv=${files.first} manifest=${files.second}")
            }.onFailure { error ->
                status.text = "Export failed: ${error.message}"
                Log.e("EdgeCollector", "export_failed", error)
            }
        }
    }

    private fun stopCollection(message: String) {
        collectionJob?.cancel()
        collectionJob = null
        recorder.stop()
        scope.launch { runCatching { sendRawWatchCommand(JSONObject().put("action", "stop").toString()) } }
        if (::status.isInitialized) status.text = message
        if (::startButton.isInitialized) startButton.isEnabled = true
    }

    private fun nextParticipantId(): String {
        val prefs = getSharedPreferences(PARTICIPANT_PREFS, MODE_PRIVATE)
        val next = prefs.getInt(NEXT_PARTICIPANT_KEY, 1)
        prefs.edit().putInt(NEXT_PARTICIPANT_KEY, next + 1).apply()
        return "participant_${next.toString().padStart(3, '0')}"
    }

    private fun previewNextParticipantId(): String {
        val next = getSharedPreferences(PARTICIPANT_PREFS, MODE_PRIVATE)
            .getInt(NEXT_PARTICIPANT_KEY, 1)
        return "participant_${next.toString().padStart(3, '0')}"
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 18f
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12 }
        setOnClickListener { action() }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        stopCollection("App closed")
        recorder.close()
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val MEASUREMENT_DURATION_SECONDS = 60
        const val DEFAULT_LABEL = "unlabeled"
        const val DEFAULT_PLACEMENT = "hand"
        const val PARTICIPANT_PREFS = "collection_participants"
        const val NEXT_PARTICIPANT_KEY = "next_participant"
    }
}
