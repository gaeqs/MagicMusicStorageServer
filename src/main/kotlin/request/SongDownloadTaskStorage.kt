package request

import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongDownloadTaskStorage {

    private val taskScope = CoroutineScope(Dispatchers.IO)

    private val tasks = ConcurrentList<SongDownloadTask>()

    fun getCurrentTasks() = tasks.toList()

    fun submitTask(task: SongDownloadTask) {
        tasks += task
        taskScope.launch {
            task.run()
            tasks -= task
        }
    }

}