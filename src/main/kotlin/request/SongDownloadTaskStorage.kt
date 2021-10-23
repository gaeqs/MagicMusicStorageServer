package request

import MONGO
import data.HistoryEntry
import io.ktor.http.cio.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import server.RequestListener

class SongDownloadTaskStorage {

    val listeners = ConcurrentList<RequestListener>()

    private val taskScope = CoroutineScope(Dispatchers.IO)

    private val tasks = ConcurrentList<SongDownloadTask>()
    private val listener = { task: SongDownloadTask -> onTaskStatusChangeEvent(task) }

    fun getCurrentTasks() = tasks.toList()

    fun submitTask(task: SongDownloadTask) {
        tasks += task
        task.statusListeners += listener

        taskScope.launch {
            task.run()
            task.statusListeners -= listener

            // Save in history
            val historyEntry = HistoryEntry(request = task.request, result = task.status)
            MONGO.addHistoryEntry(task.user, historyEntry)
            tasks -= task
        }
    }

    fun sendAllRequests(listener: RequestListener) {
        taskScope.launch {
            tasks.filter { it.user == listener.user }.forEach {
                val sending = HistoryEntry(request = it.request, result = it.status)
                listener.session.send(Json.encodeToString(sending))
            }
        }
    }

    private fun onTaskStatusChangeEvent(task: SongDownloadTask) {
        val sending = HistoryEntry(request = task.request, result = task.status)
        taskScope.launch {
            listeners.filter { it.user == task.user }.forEach {
                it.session.send(Json.encodeToString(sending))
            }
        }
    }

}