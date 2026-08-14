package com.edgecontext.gateway.wear

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class MainActivity : Activity(), MessageClient.OnMessageReceivedListener {
    private lateinit var sender: WearSensorSender
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sender = WearSensorSender(this)
        setContentView(buildUi())
        Wearable.getMessageClient(this).addListener(this)
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 24, 20, 30)
        }
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "WATCH READY"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        status = TextView(this).apply {
            text = "Leave this screen open.\nStart sitting/standing blocks on the phone."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 18)
        }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "EMERGENCY STOP"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setOnClickListener {
                sender.stop()
                status.text = "Stopped. Waiting for phone command."
            }
        })
        return scroll
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/collection_command") return
        val command = JSONObject(String(event.data, Charsets.UTF_8))
        runOnUiThread {
            when (command.optString("action")) {
                "start" -> {
                    val metadata = WearMetadata(
                        subjectId = command.getString("subject_id"),
                        sessionId = command.getString("session_id"),
                        label = command.getString("label"),
                        placement = command.optString("placement", "wrist"),
                    )
                    sender.start(metadata)
                    Log.i("EdgeCollector", "watch_trial_start id=${metadata.sessionId} activity=${metadata.label}")
                    status.text = "RECORDING ${metadata.label}\n${metadata.sessionId}\nControlled by phone"
                }
                "stop" -> {
                    sender.stop()
                    Log.i("EdgeCollector", "watch_trial_stop")
                    status.text = "Trial saved. Waiting for next phone command."
                }
            }
        }
    }

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(this)
        sender.close()
        super.onDestroy()
    }
}

