package request

import MONGO
import data.HistoryEntry
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

            // Save in history
            val historyEntry = HistoryEntry(request = task.request, result = task.status)
            MONGO.addHistoryEntry(task.user, historyEntry)

            tasks -= task
        }
    }

}