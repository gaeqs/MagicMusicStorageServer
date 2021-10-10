package request

import kotlinx.serialization.Serializable

@Serializable
data class DownloadRequest(
    val url: String,
    val name: String,
    val artist: String,
    val album: String,
    val section: String
)