package server

import TASK_STORAGE
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.Duration

class RequestListener(val user: String, val session: DefaultWebSocketServerSession)

private val DefaultWebSocketServerSession.username: String
    get() = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Application.apiModuleWebSockets(testing: Boolean = false) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        authenticate("api-jwt") {
            webSocket("/api/socket/status") {
                val listener = RequestListener(username, this)
                TASK_STORAGE.listeners += listener
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text && frame.readText() == "all") {
                            TASK_STORAGE.sendAllRequests(listener)
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    TASK_STORAGE.listeners -= listener
                } catch (e: Throwable) {
                    TASK_STORAGE.listeners -= listener
                    e.printStackTrace()
                }
            }
        }
    }
}
