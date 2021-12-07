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


private val PipelineContext<Unit, ApplicationCall>.username: String
    get() = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Application.apiModuleGet(testing: Boolean = false) {
    routing {
        authenticate("api-jwt") {
            get("/api") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal!!.payload.getClaim("username").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, $username! Token is expired at $expiresAt ms.")
            }
            get("/api/get/songs") {
                val section = call.parameters["section"]
                if (section == null) {
                    call.respond(HttpStatusCode.BadRequest, "Parameter section not found.")
                    return@get
                }
                try {
                    val songs = MONGO.getSongs(username, section).map { it.copy(id = "") }
                    call.respondText(Json.encodeToString(songs), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    throw ex
                }
            }

            get("/api/get/sections") {
                try {
                    val sections = MONGO.getSections(username)
                    call.respondText(Json.encodeToString(sections), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    throw ex
                }
            }

            get("/api/get/albums") {
                try {
                    val sections = MONGO.getAlbums(username)
                    call.respondText(Json.encodeToString(sections), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    throw ex
                }
            }

            get("/api/get/albumCover") {
                val album = call.parameters["album"]
                if (album == null) {
                    call.respond(HttpStatusCode.BadRequest, "Parameter album not found.")
                    return@get
                }

                try {
                    val image = MONGO.getAlbumImage(username, album)
                    if (image == null || !image.isFile) {
                        call.respond(HttpStatusCode.BadRequest, "Couldn't find album $album.")
                        return@get
                    }

                    call.respondBytes(ContentType.Image.PNG, HttpStatusCode.OK) {
                        image.inputStream().readBytes()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    throw ex
                }
            }

        }
    }
}
