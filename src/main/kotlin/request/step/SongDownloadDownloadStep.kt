package request.step

import io.github.gaeqs.javayoutubedownloader.stream.StreamOption
import io.github.gaeqs.javayoutubedownloader.stream.download.StreamDownloader
import io.github.gaeqs.javayoutubedownloader.stream.download.StreamDownloaderNotifier
import util.FileUtils
import java.io.File

class SongDownloadDownloadStep(private val option: StreamOption) : SongDownloadStep<File> {
    override var percentage = 0.0

    override fun run(): File? {
        val file = FileUtils.requestTemporalFile()

        val notifier = DownloadNotifier()
        val downloader = StreamDownloader(option, file, notifier)
        downloader.run()

        if (notifier.success) return file
        file.delete()
        return null
    }


    private inner class DownloadNotifier : StreamDownloaderNotifier {

        var success: Boolean = false

        override fun onStart(downloader: StreamDownloader) {
        }

        override fun onDownload(downloader: StreamDownloader) {
            percentage = downloader.count / downloader.length.toDouble()
        }

        override fun onFinish(downloader: StreamDownloader) {
            percentage = 1.0
            success = true
        }

        override fun onError(downloader: StreamDownloader, ex: Exception?) {
            ex?.printStackTrace()
            success = false
        }
    }
}