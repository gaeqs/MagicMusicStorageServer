package request.step

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


class SongDownloadNormalizeStep(private val converted: File) : SongDownloadStep<Boolean> {
    override var percentage = 0.0

    override fun run(): Boolean {
        try {
            val processBuilder = ProcessBuilder("mp3gain", "/r", converted.absolutePath)
            val process = processBuilder.start()

            val br = process.errorReader()
            var line: String?

            while (br.readLine().also { line = it } != null) {
                val index = line!!.indexOf('%')
                if (index != -1) {
                    val int = line!!.substring(0, index).trim().toIntOrNull()
                    if (int != null) {
                        percentage = int / 100.0
                    }
                } else if (line!!.trim().isNotEmpty()) {
                    println("[MP3GAIN] $line")
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