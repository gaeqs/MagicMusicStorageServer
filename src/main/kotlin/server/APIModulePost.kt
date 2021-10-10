package server

import MONGO
import TASK_STORAGE
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import request.DownloadRequest


private val PipelineContext<Unit, ApplicationCall>.username: String
    get() = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Application.apiModulePost(testing: Boolean = false) {
    routing {
        authenticate("api-jwt") {
            post("/api/post/section") {
                val section = call.parameters["section"]
                if (section == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@post
                }
                if (MONGO.addSection(username, section)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
            post("/api/post/request") {
                val request: DownloadRequest
                try {
                    request = call.receive()
                } catch (ex: ContentTransformationException) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest);
                    return@post
                }
                val name = username

                // Check if the section exists
                if (!MONGO.hasSection(name, request.section)) {
                    call.respondText("Section not found.", status = HttpStatusCode.BadRequest);
                    return@post
                }

                // Check if the album exists
                if (!MONGO.hasAlbum(name, request.album)) {
                    call.respondText("Section not found.", status = HttpStatusCode.BadRequest);
                    return@post
                }

                val tasks = TASK_STORAGE.getCurrentTasks()

                // Check if the song already exists
                if (tasks.any { it.user == name && it.request.name == request.name }
                    || MONGO.hasSectionSong(name, request.section, request.name)) {
                    call.respondText("Song already exists.", status = HttpStatusCode.BadRequest);
                    return@post
                }

                // TODO requests


            }
        }
    }
}
