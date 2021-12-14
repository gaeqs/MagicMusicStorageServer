import io.ktor.auth.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mongo.MongoClient
import request.SongDownloadTaskStorage
import java.io.File

val MONGO = MongoClient()
val TASK_STORAGE = SongDownloadTaskStorage()

fun main(args: Array<String>) {
    val ktorArgs = args + "-config=application.conf"

    if (args.size > 3 && args[0] == "-sync") {
        val file = File(args[3])

        println("Synchronizing old data from file ${file.absolutePath} to user ${args[1]}...")

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            syncOldData(file, args[1], args[2])
        }
    }

    EngineMain.main(ktorArgs)
}

private fun validateUser(name: String, password: String): UserIdPrincipal? {
    return if (name == "patata" && password == "pototo") UserIdPrincipal(name) else null
}
