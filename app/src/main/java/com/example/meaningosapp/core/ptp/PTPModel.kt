package com.example.meaningosapp.core.ptp

import kotlinx.serialization.Serializable

@Serializable
data class PTPRoot(
    val version: String,
    val layers: List<PTPLayer>
)

@Serializable
data class PTPLayer(
    val layer: Int,
    val name: String,
    val primitives: List<PTPPrimitive>
)

@Serializable
data class PTPPrimitive(
    val name: String,
    val definition: String,
    val dependencies: List<String>
)
