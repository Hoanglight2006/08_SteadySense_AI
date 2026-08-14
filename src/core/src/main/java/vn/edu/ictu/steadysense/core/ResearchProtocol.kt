package vn.edu.ictu.steadysense.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class ResearchConfig(
    val sessionId: String,
    val participantCode: String,
    val condition: String,
    val wornSide: String,
    val protocolVersion: String,
    val targetCycles: Int,
    val tempoBpm: Float,
    val configVersion: Int = 1,
    val phoneSentAtEpochNanos: Long = 0L,
)

enum class ResearchCommand { START, STOP, MARK }

data class ResearchControl(
    val sessionId: String,
    val command: ResearchCommand,
    val timestampEpochNanos: Long,
    val value: String = "",
)

object ResearchConfigCodec {
    private const val MAGIC = 0x53535243 // SSRC
    private const val VERSION = 1

    fun encode(value: ResearchConfig): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC); out.writeInt(VERSION)
            out.writeUTF(value.sessionId); out.writeUTF(value.participantCode)
            out.writeUTF(value.condition); out.writeUTF(value.wornSide)
            out.writeUTF(value.protocolVersion); out.writeInt(value.targetCycles)
            out.writeFloat(value.tempoBpm); out.writeInt(value.configVersion)
            out.writeLong(value.phoneSentAtEpochNanos)
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): ResearchConfig = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "Invalid research config magic" }
        require(input.readInt() == VERSION) { "Unsupported research config version" }
        ResearchConfig(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(),
            input.readUTF(), input.readInt(), input.readFloat(), input.readInt(), input.readLong())
    }
}

object ResearchControlCodec {
    private const val MAGIC = 0x53535245 // SSRE
    private const val VERSION = 1

    fun encode(value: ResearchControl): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC); out.writeInt(VERSION); out.writeUTF(value.sessionId)
            out.writeInt(value.command.ordinal); out.writeLong(value.timestampEpochNanos)
            out.writeUTF(value.value)
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): ResearchControl = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "Invalid research control magic" }
        require(input.readInt() == VERSION) { "Unsupported research control version" }
        val session = input.readUTF()
        val ordinal = input.readInt()
        require(ordinal in ResearchCommand.entries.indices) { "Invalid research command" }
        ResearchControl(session, ResearchCommand.entries[ordinal], input.readLong(), input.readUTF())
    }
}
