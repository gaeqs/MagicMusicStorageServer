package request

enum class SongDownloadStatus {

    QUEUED,
    FETCHING,
    DOWNLOADING,
    CONVERTING,
    NORMALIZING,
    ENHANCING,
    FINISHED,
    ERROR,
    CANCELLED

}