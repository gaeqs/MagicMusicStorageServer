package request.step

import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.id3.ID3v1Tag
import request.DownloadRequest
import util.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class SongDownloadEnhanceAndSaveStep(
    private val user: String,
    private val converted: File,
    private val request: DownloadRequest
) : SongDownloadStep<File> {
    override var percentage = 0.0

    override fun run(): File? {
        try {
            val mp3 = MP3File(converted)

            if (!mp3.hasID3v1Tag()) mp3.iD3v1Tag = ID3v1Tag()
            val tag = mp3.iD3v1Tag
            tag.setTitle(request.name)
            tag.setComment(request.url)
            tag.setAlbum(request.album)
            tag.setArtist(request.artist)
            mp3.save()

            val file = FileUtils.requestUserSongFile(user)
            Files.move(converted.toPath(), file.toPath())
            return file
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}