package request.step

import util.ConversionUtils
import util.FileUtils
import ws.schild.jave.info.MultimediaInfo
import ws.schild.jave.progress.EncoderProgressListener
import java.io.File

class SongDownloadConvertStep(private val raw: File) : SongDownloadStep<File> {
    override var percentage = 0.0

    override fun run(): File? {
        val result = FileUtils.requestTemporalFile(".mp3")

        if (ConversionUtils.convertToMP3(raw, result, ConversionNotifier())) {
            return result
        }
        result.delete()
        return null
    }

    private inner class ConversionNotifier : EncoderProgressListener {

        override fun sourceInfo(info: MultimediaInfo) {
            println("[JAVE] Multimedia info: $info")
        }

        override fun progress(permil: Int) {
            percentage = permil / 1000.0
        }

        override fun message(message: String) {
            println("[JAVE] $message")
        }

    }
}