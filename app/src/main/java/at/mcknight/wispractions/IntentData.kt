package at.mcknight.wispractions

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class IntentData(
    val action: String,
    val extras: Map<String, JsonElement>
)
