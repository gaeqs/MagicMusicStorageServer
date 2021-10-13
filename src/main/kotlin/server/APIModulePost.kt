package server

import MONGO
import TASK_STORAGE
import data.Album
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import request.DownloadRequest
import request.SongDownloadTask
import util.FileUtils
import util.receiveImage
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@Serializable
private data class SectionWrapper(val section: String)

@Serializable
private data class AlbumWrapper(val album: String)

private val PipelineContext<Unit, ApplicationCall>.username: String
    get() = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Application.apiModulePost(testing: Boolean = false) {
    routing {
        authenticate("api-jwt") {
            post("/api/post/section") {
                val section: SectionWrapper
                try {
                    section = call.receive()
                } catch (ex: ContentTransformationException) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                if (MONGO.addSection(username, section.section)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
            post("/api/post/album") {
                val album: AlbumWrapper
                val image: BufferedImage
                try {
                    album = call.receive()
                    image = receiveImage()
                } catch (ex: Exception) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }
                val name = username

                if (MONGO.hasAlbum(name, album.album)) {
                    call.respond(HttpStatusCode.Conflict)
                    return@post
                }


                val file = FileUtils.requestUserSongFile(name)
                ImageIO.write(image, "PNG", file)

                if (MONGO.addAlbum(username, Album(album.album, file))) {
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
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }
                val name = username

                // Check if the section exists
                if (!MONGO.hasSection(name, request.section)) {
                    call.respondText("Section not found.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                // Check if the album exists
                if (!MONGO.hasAlbum(name, request.album)) {
                    call.respondText("Album not found.", status = HttpStatusCode.BadRequest)
                    return@post
                }


                // Check if the song already exists
                val tasks = TASK_STORAGE.getCurrentTasks()
                if (tasks.any { it.user == name && it.request.name == request.name }
                    || MONGO.hasSectionSong(name, request.section, request.name)) {
                    call.respondText("Song already exists.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                TASK_STORAGE.submitTask(SongDownloadTask(name, request))
            }
        }
    }
}
