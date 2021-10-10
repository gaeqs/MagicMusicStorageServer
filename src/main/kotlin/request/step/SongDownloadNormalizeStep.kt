package request.step

import java.io.File
import java.io.InputStreamReader

class SongDownloadNormalizeStep(private val converted: File) : SongDownloadStep<Boolean> {
    override var percentage = 0.0

    override fun run(): Boolean {
        try {
            val processBuilder = ProcessBuilder("mp3gain", "-r", converted.absolutePath)
            val process = processBuilder.start()
            InputStreamReader(process.inputStream).readLines().forEach {
                val index = it.indexOf('%')
                if (index != -1) {
                    val int = it.substring(0, index).trim().toIntOrNull()
                    if (int != null) {
                        percentage = int / 100.0
                    }
                } else if (!it.trim().isEmpty()) {
                    println("[MP3GAIN] $it")
                }
            }
            process.waitFor()
            percentage = 1.0
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }
}