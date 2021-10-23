package request

import MONGO
import data.Song
import io.github.gaeqs.javayoutubedownloader.stream.StreamOption
import io.ktor.util.collections.*
import request.step.*
import util.LockedField
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SongDownloadTask(val user: String, val request: DownloadRequest) {

    private val cancelLock = ReentrantLock()

    @Volatile
    private var step: SongDownloadStep<*>? = null

    @Volatile
    var status = SongDownloadStatus.QUEUED
        private set

    val percentage: Double get() = step?.percentage ?: 0.0

    var cancelled: Boolean by LockedField(cancelLock, false)
        private set

    suspend fun run() {
        if (status != SongDownloadStatus.QUEUED) return
        if (checkCancelled()) return

        status = SongDownloadStatus.FETCHING
        val option = fetch()
        if (option == null || checkCancelled()) {
            if (status != SongDownloadStatus.CANCELLED) {
                status = SongDownloadStatus.ERROR
            }
            return
        }

        status = SongDownloadStatus.DOWNLOADING
        val raw = download(option)
        if (raw == null || checkCancelled()) {
            if (status != SongDownloadStatus.CANCELLED) {
                status = SongDownloadStatus.ERROR
            }
            return
        }

        status = SongDownloadStatus.CONVERTING
        val converted = convert(raw)
        raw.delete()
        if (converted == null || checkCancelled()) {
            if (status != SongDownloadStatus.CANCELLED) {
                status = SongDownloadStatus.ERROR
            }
            return
        }

        status = SongDownloadStatus.NORMALIZING
        if (!normalize(converted) || checkCancelled()) {
            converted.delete()
            if (status != SongDownloadStatus.CANCELLED) {
                status = SongDownloadStatus.ERROR
            }
            return
        }

        status = SongDownloadStatus.ENHANCING
        // This part cannot be cancelled!
        val finalFile = enhanceAndSave(converted)
        if (finalFile == null) {
            status = SongDownloadStatus.ERROR
            return
        }

        val song = Song(finalFile.name, request.name, request.artist, request.album)
        MONGO.addSong(user, request.section, song)
        status = SongDownloadStatus.FINISHED
    }

    fun cancel() {
        cancelLock.withLock {
            if (status == SongDownloadStatus.FINISHED
                || status == SongDownloadStatus.ERROR
                || status == SongDownloadStatus.ENHANCING) return
            cancelled = true;
        }
    }

    private fun checkCancelled(): Boolean {
        if (cancelled) {
            status = SongDownloadStatus.CANCELLED
            return true
        }
        return false
    }

    private fun fetch(): StreamOption? {
        val fetch = SongDownloadFetchStep(request)
        step = fetch
        return fetch.run()
    }

    private fun download(option: StreamOption): File? {
        val download = SongDownloadDownloadStep(option)
        step = download
        return download.run()
    }

    private fun convert(raw: File): File? {
        val convert = SongDownloadConvertStep(raw)
        step = convert
        return convert.run()
    }

    private fun normalize(converted: File): Boolean {
        val normalize = SongDownloadNormalizeStep(converted)
        step = normalize
        return normalize.run()
    }

    private fun enhanceAndSave(converted: File): File? {
        val enhance = SongDownloadEnhanceAndSaveStep(user, converted, request)
        step = enhance
        return enhance.run()
    }


}