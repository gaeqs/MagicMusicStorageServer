package server

import TASK_STORAGE
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.time.Duration

class RequestListener(val user: String, val session: DefaultWebSocketServerSession) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RequestListener

        if (session != other.session) return false

        return true
    }

    override fun hashCode(): Int {
        return session.hashCode()
    }
}

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
                } finally {
                    TASK_STORAGE.listeners -= listener
                }
            }
        }
    }
}
