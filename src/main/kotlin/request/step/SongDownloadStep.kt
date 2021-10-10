package request.step

interface SongDownloadStep<T> {

    val percentage: Double

    fun run(): T?

}