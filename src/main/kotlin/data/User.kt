package data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import request.DownloadRequest
import request.SongDownloadStatus
import java.io.File

data class User(
    @BsonId val name: String,
    val passwordHash: String,
    val history: List<HistoryEntry> = mutableListOf(),
    val albums: MutableSet<Album> = HashSet(),
    val sections: MutableSet<Section> = HashSet()
)

data class UserQuery(
    @BsonId val name: String = "",
    val passwordHash: String = "",
    val history: List<HistoryEntry> = mutableListOf(),
    val albums: MutableSet<Album> = HashSet(),
    val sections: MutableSet<Section> = HashSet()
)

@Serializable
data class Section(
    @BsonId val name: String = "",
    val songs: MutableSet<Song> = HashSet()
)

@Serializable
data class Song(
    @BsonId val id: String = "",
    val name: String = "",
    val artist: String = "",
    val album: String = "",
)

data class Album(
    @BsonId val name: String = "",
    val image: File? = null
)

@Serializable
data class HistoryEntry(
    val time: Long = Clock.System.now().epochSeconds,
    val request: DownloadRequest = DownloadRequest("", "", "", "", ""),
    val result: SongDownloadStatus = SongDownloadStatus.ERROR
)