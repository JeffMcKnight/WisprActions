package at.mcknight.wispractions

import kotlinx.serialization.Serializable

@Serializable
data class IntentData(
    val duration: Int,
    val name: String,
    val timeunits: String
)
