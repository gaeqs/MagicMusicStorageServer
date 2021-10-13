package util

import java.io.File
import java.util.*

object FileUtils {

    object Constants {
        val MAIN_FOLDER = checkFolder(File("MagicMusicStorage"))
        val TEMP_FOLDER = checkFolder(File(MAIN_FOLDER, "temp"), true)
        val USER_FOLDER = checkFolder(File(MAIN_FOLDER, "user"))

        val SONGS_FOLDER_NAME = "songs"
        val ALBUMS_FOLDER_NAME = "albums"
    }

    /**
     * Checks the status of the given folder.
     *
     * If the folder doesn't exist, this method will try to create it. If the creation is unsuccessful this function
     * throws an [IllegalStateException].
     *
     * If [deleteOnExit] is true, the folder will be tagged to be removed when the JVM is being shut down.
     *
     * If [folder] exists but it's not a directory, this function throws an [IllegalStateException].
     *
     * @param folder the folder to check.
     * @param deleteOnExit whether the folder should be tagged to be removed when the JVM is being shut down.
     * @return the folder itself.
     */
    fun checkFolder(folder: File, deleteOnExit: Boolean = false): File {
        if (!folder.exists()) {
            val result = folder.mkdirs()
            if (!result) throw IllegalStateException("Couldn't create folder ${folder.absolutePath}")
        }

        if (deleteOnExit) {
            folder.deleteOnExit()
        }

        if (!folder.isDirectory) throw IllegalStateException("Couldn't create folder ${folder.absolutePath}")
        return folder
    }

    /**
     * Returns a temporal file.
     * @return the file.
     */
    fun requestTemporalFile(suffix: String = ""): File {
        var file: File
        do {
            file = File(Constants.TEMP_FOLDER, UUID.randomUUID().toString() + suffix)
        } while (file.exists())
        return file
    }

    /**
     * Returns a file for a song.
     * @return the file.
     */
    fun requestUserSongFile(user: String): File {
        val userFolder = checkFolder(File(Constants.USER_FOLDER, user))
        val songsFolder = checkFolder(File(userFolder, Constants.SONGS_FOLDER_NAME))

        var file: File
        do {
            file = File(songsFolder, UUID.randomUUID().toString() + ".mp3")
        } while (file.exists())
        return file
    }

    /**
     * Returns a file for an album.
     * @return the file.
     */
    fun requestUserAlbumFile(user: String): File {
        val userFolder = checkFolder(File(Constants.USER_FOLDER, user))
        val albums = checkFolder(File(userFolder, Constants.ALBUMS_FOLDER_NAME))

        var file: File
        do {
            file = File(albums, UUID.randomUUID().toString() + ".png")
        } while (file.exists())
        return file
    }

}