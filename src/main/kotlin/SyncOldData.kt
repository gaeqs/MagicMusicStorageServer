import data.Album
import data.Song
import util.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

suspend fun syncOldData(folder: File, username: String, password: String) {
    MONGO.createUser(username, password)

    println("User created.")

    val albumsFolder = File(folder, "albums")
    val albums = if (albumsFolder.exists()) {
        syncAlbums(albumsFolder, username)
    } else emptyList()

    val sectionsFolder = File(folder, "sections")
    if (sectionsFolder.exists()) {
        syncSections(sectionsFolder, albums, username)
    }

    println("Synchronization done.")
}

private suspend fun syncAlbums(albumsFolder: File, username: String): List<String> {
    val albums = mutableListOf<String>()
    albumsFolder.listFiles()?.forEach {
        if (!it.isFile || !it.name.lowercase().endsWith(".png")) return@forEach
        val fullName = it.name.substring(0, it.name.length - 4).trim()

        println("Copying album $fullName...")

        val albumFile = FileUtils.requestUserAlbumFile(username)
        Files.copy(it.toPath(), albumFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        val album = Album(fullName, albumFile)
        MONGO.addAlbum(username, album)

        albums += fullName
    }

    return albums
}

private suspend fun syncSections(sectionsFolder: File, albums: List<String>, username: String) {
    sectionsFolder.listFiles()?.forEach {
        if (!it.isDirectory) return@forEach
        syncSection(it.name, it, albums, username)
    }
}

private suspend fun syncSection(section: String, folder: File, albums: List<String>, username: String) {
    MONGO.addSection(username, section)
    folder.listFiles()?.forEach {
        if (!it.isFile || !it.name.lowercase().endsWith(".mp3")) return@forEach
        val fullName = it.name.substring(0, it.name.length - 4)
        val index = fullName.indexOf('-')
        if (index == -1) return@forEach
        val album = fullName.substring(0, index).trim()
        val name = fullName.substring(index + 1).trim()

        if (album.isEmpty() || name.isEmpty()) return@forEach
        if(album !in albums) return@forEach

        println("Copying song $fullName...")

        val songFile = FileUtils.requestUserSongFile(username)
        Files.copy(it.toPath(), songFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        val song = Song(songFile.name, name, "", album)
        MONGO.addSong(username, section, song)
    }
}