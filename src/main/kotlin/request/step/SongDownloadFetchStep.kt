package request.step

import io.github.gaeqs.javayoutubedownloader.JavaYoutubeDownloader
import io.github.gaeqs.javayoutubedownloader.decoder.MultipleDecoderMethod
import io.github.gaeqs.javayoutubedownloader.stream.StreamOption
import request.DownloadRequest

class SongDownloadFetchStep(private val request: DownloadRequest) : SongDownloadStep<StreamOption> {
    override val percentage = 0.0

    override fun run(): StreamOption? {
        val video = JavaYoutubeDownloader.decodeOrNull(request.url, MultipleDecoderMethod.AND, "html")
        if (video == null) {
            println("Couldn't decode video ${request.url}: no videos found!")
            return null
        }

        val option = video.streamOptions.stream()
            .filter { it.type.hasAudio() }
            .min { o1, o2 -> o1.type.audioQuality.ordinal - o2.type.audioQuality.ordinal }
            .orElse(null)

        if (option == null) {
            println("Couldn't decode video ${request.url}: no videos with audio found")
            return null
        }

        return option
    }
}