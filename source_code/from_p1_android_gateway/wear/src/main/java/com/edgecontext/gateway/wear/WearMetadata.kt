package com.edgecontext.gateway.wear

data class WearMetadata(
    val subjectId: String,
    val sessionId: String,
    val label: String,
    val placement: String = "wrist",
)

