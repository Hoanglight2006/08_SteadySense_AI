package com.edgecontext.gateway.phone

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.time.Instant

object CsvExporter {
    private const val HEADER = "timestamp,subject_id,session_id,device_id,placement,label,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,device_model,battery_pct,latency_ms"

    fun export(context: Context, rows: List<SensorSampleEntity>): Pair<String, String> {
        require(rows.isNotEmpty()) { "No rows to export" }
        val stamp = Instant.now().toEpochMilli()
        val baseName = "edge_context_pilot_$stamp"
        writeDownload(context, "$baseName.csv", "text/csv") { writer ->
            writer.appendLine(HEADER)
            rows.forEach { row ->
                writer.appendLine(
                    listOf(
                        row.timestamp,
                        row.subjectId,
                        row.sessionId,
                        row.deviceId,
                        row.placement,
                        row.label,
                        row.accX,
                        row.accY,
                        row.accZ,
                        row.gyroX,
                        row.gyroY,
                        row.gyroZ,
                        row.deviceModel,
                        row.batteryPct ?: "",
                        row.latencyMs ?: "",
                    ).joinToString(",") { csv(it.toString()) }
                )
            }
        }

        val manifest = buildManifest(rows, stamp)
        writeDownload(context, "$baseName.manifest.json", "application/json") { writer ->
            writer.write(manifest.toString(2))
        }
        return "$baseName.csv" to "$baseName.manifest.json"
    }

    private fun buildManifest(rows: List<SensorSampleEntity>, exportedAt: Long): JSONObject {
        fun counts(values: List<String>): JSONObject {
            val result = JSONObject()
            values.groupingBy { it }.eachCount().toSortedMap().forEach { (key, value) -> result.put(key, value) }
            return result
        }
        val trials = JSONObject()
        rows.groupBy { it.sessionId }.toSortedMap().forEach { (session, trialRows) ->
            trials.put(
                session,
                JSONObject()
                    .put("rows", trialRows.size)
                    .put("devices", counts(trialRows.map { it.deviceId }))
                    .put("labels", counts(trialRows.map { it.label }))
                    .put("placements", counts(trialRows.map { it.placement }))
                    .put("start_timestamp", trialRows.minOf { it.timestamp })
                    .put("end_timestamp", trialRows.maxOf { it.timestamp }),
            )
        }
        return JSONObject()
            .put("schema", "edge_context_pilot_manifest_v2")
            .put("exported_at", exportedAt)
            .put("total_rows", rows.size)
            .put("subjects", rows.map { it.subjectId }.distinct().sorted())
            .put("devices", counts(rows.map { it.deviceId }))
            .put("labels", counts(rows.map { it.label }))
            .put("placements", counts(rows.map { it.placement }))
            .put("trials", trials)
    }

    private fun writeDownload(
        context: Context,
        displayName: String,
        mimeType: String,
        block: (OutputStreamWriter) -> Unit,
    ) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = requireNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ) { "Could not create $displayName in Downloads" }
        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use(block)
        } ?: error("Could not open $displayName")
    }

    private fun csv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}

