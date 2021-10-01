package data

import org.bson.codecs.pojo.annotations.BsonId
import java.io.File

data class UserSession(val id: String, val count: Int)

/*
User( name, sections( name, albums( name, image ), songs(name, artist, album) ) )
 */

data class User(
    @BsonId val name: String,
    val passwordHash: String,
    val albums: MutableSet<Album> = HashSet(),
    val sections: MutableSet<Section> = HashSet()
)

data class UserSnapshot(
    @BsonId val name: String,
    val password: String
)

data class Section(
    @BsonId val name: String,
    val songs: MutableSet<Song> = HashSet()
)

data class Song(
    @BsonId val name: String,
    var artist: String,
    val album: String
)

data class Album(
    @BsonId val name: String,
    val image: File
)