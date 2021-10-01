package server

import MONGO
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


val PipelineContext<Unit, ApplicationCall>.username: String
    get() = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Application.apiModule(testing: Boolean = false) {
    routing {
        authenticate("api-jwt") {
            get("/api") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
            }
            get("/api/songs") {
                val section = call.parameters["section"]
                if (section == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                try {
                    val songs = MONGO.getSongs(username, section)
                    call.respond(HttpStatusCode.OK, Json.encodeToString(songs))
                } catch (ex : Exception) {
                    ex.printStackTrace()
                    throw ex
                }
            }
        }
    }
}
