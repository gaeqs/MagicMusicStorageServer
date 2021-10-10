package data

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.io.File

data class User(
    @BsonId val name: String,
    val passwordHash: String,
    val albums: MutableSet<Album> = HashSet(),
    val sections: MutableSet<Section> = HashSet()
)

data class UserSnapshot(
    @BsonId val name: String = "",
    val password: String = ""
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