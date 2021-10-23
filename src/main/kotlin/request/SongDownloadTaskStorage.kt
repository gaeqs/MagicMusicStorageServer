package request

import MONGO
import data.HistoryEntry
import io.ktor.http.cio.websocket.*
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import server.RequestListener

@Serializable
private data class TaskStatusPacket(
    val request: DownloadRequest,
    val status: SongDownloadStatus,
    val percentage: Double
)

class SongDownloadTaskStorage {

    val listeners = ConcurrentList<RequestListener>()

    private val taskScope = CoroutineScope(Dispatchers.IO)
    private val tasks = ConcurrentList<SongDownloadTask>()
    private val lastPackets = mutableMapOf<SongDownloadTask, TaskStatusPacket>()

    init {
        taskScope.launch {
            while (isActive) {
                delay(500)
                if (listeners.isNotEmpty() && tasks.isNotEmpty()) {
                    tasks.forEach { onTaskStatusChangeEvent(it) }
                }
            }
        }
    }

    fun getCurrentTasks() = tasks.toList()

    fun submitTask(task: SongDownloadTask) {
        tasks += task

        taskScope.launch {
            task.run()
            onTaskStatusChangeEvent(task)

            // Save in history
            val historyEntry = HistoryEntry(request = task.request, result = task.status)
            MONGO.addHistoryEntry(task.user, historyEntry)
            tasks -= task
        }
    }

    fun sendAllRequests(listener: RequestListener) {
        taskScope.launch {
            tasks.filter { it.user == listener.user }.forEach {
                val sending = TaskStatusPacket(it.request, it.status, it.percentage)
                listener.session.send(Json.encodeToString(sending))
            }
        }
    }

    private fun onTaskStatusChangeEvent(task: SongDownloadTask) {
        val lastPacket = lastPackets[task]
        if (lastPacket != null && lastPacket.status == task.status && lastPacket.percentage == task.percentage) return
        val sending = TaskStatusPacket(task.request, task.status, task.percentage)
        lastPackets[task] = sending

        taskScope.launch {
            listeners.filter { it.user == task.user }.forEach {
                it.session.send(Json.encodeToString(sending))
            }
        }
    }

}