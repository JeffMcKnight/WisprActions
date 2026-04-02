package at.mcknight.wispractions

import kotlinx.serialization.Serializable

@Serializable
data class IntentData(
    val action: String,
    val duration: Int,
    val name: String,
    val timeUnits: String
)
