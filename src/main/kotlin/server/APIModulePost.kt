package server

import MONGO
import TASK_STORAGE
import data.Album
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import request.DownloadRequest
import request.SongDownloadTask
import util.FileUtils
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private val PipelineContext<Unit, ApplicationCall>.username: String
    get() = call.principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Application.apiModulePost(testing: Boolean = false) {
    routing {
        authenticate("api-jwt") {
            post("/api/post/section") {
                @Serializable
                data class SectionWrapper(val name: String)

                val section: SectionWrapper
                try {
                    section = call.receive()
                } catch (ex: Exception) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }


                if (MONGO.addSection(username, section.name)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respondText("Section already exists.", status = HttpStatusCode.Conflict)
                }
            }
            post("/api/post/album") {
                @Serializable
                data class AlbumWrapper(val name: String)

                val parts = call.receiveMultipart().readAllParts().associateBy { it.name }
                val name = username

                // Parse header
                val headerRaw = parts["header"]
                if (headerRaw !is PartData.FormItem) {
                    call.respondText("Header not found.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val header = Json.decodeFromString<AlbumWrapper>(headerRaw.value)
                if (header.name.length > 128) {
                    call.respondText("Name cannot be larger than 128 characters.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                if (MONGO.hasAlbum(name, header.name)) {
                    call.respondText("Album already exists.", status = HttpStatusCode.Conflict)
                    return@post
                }

                // Parse image
                val fileRaw = parts["image"]
                if (fileRaw !is PartData.FileItem) {
                    call.respondText("Image not found.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val image: BufferedImage
                try {
                    image = ImageIO.read(fileRaw.streamProvider())
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respondText("Bad image format.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                // Write image
                val file = FileUtils.requestUserAlbumFile(name)
                ImageIO.write(image, "PNG", file)
                if (MONGO.addAlbum(name, Album(header.name, file))) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respondText("Album already exists.", status = HttpStatusCode.Conflict)
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
                if (tasks.any {
                        it.user == name &&
                                it.request.name == request.name &&
                                it.request.section == request.section &&
                                it.request.album == request.album
                    } || MONGO.hasSectionSong(name, request.section, request.name, request.album)) {
                    call.respondText("Song already exists.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                TASK_STORAGE.submitTask(SongDownloadTask(name, request))

                call.respondText("Downloading.", status = HttpStatusCode.OK)
            }

            post("/api/post/cancelRequest") {
                @Serializable
                data class CancelRequestWrapper(val name: String, val section: String, val album: String)

                val request: CancelRequestWrapper
                try {
                    request = call.receive()
                } catch (ex: Exception) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                TASK_STORAGE.getCurrentTasks()
                    .filter {
                        it.request.name == request.name &&
                                it.request.section == request.section &&
                                it.request.album == request.album
                    }
                    .forEach { it.cancel() }

                call.respondText("Ok", status = HttpStatusCode.OK)
            }

            post("/api/post/deleteSong") {
                @Serializable
                data class DeleteSongRequest(val name: String, val section: String, val album: String)

                val request: DeleteSongRequest
                try {
                    request = call.receive()
                } catch (ex: Exception) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val user = username
                val song = MONGO.getSong(user, request.section, request.name, request.album)
                if (song == null) {
                    call.respondText("Song not found.", status = HttpStatusCode.BadRequest)
                    return@post
                }

                FileUtils.getUserSongFile(user, song.id)?.delete()
                MONGO.deleteSong(user, request.section, request.name, request.album)
                call.respondText("Ok", status = HttpStatusCode.OK)
            }

            post("/api/post/deleteSection") {
                @Serializable
                data class DeleteSectionRequest(val name: String)

                val request: DeleteSectionRequest
                try {
                    request = call.receive()
                } catch (ex: Exception) {
                    call.respondText("Bad format.", status = HttpStatusCode.BadRequest)
                    return@post
                }
                val user = username
                val songs = MONGO.getSongs(user, request.name)
                songs.forEach { FileUtils.getUserSongFile(user, it.id)?.delete() }

                MONGO.deleteSection(user, request.name)
                call.respondText("Ok", status = HttpStatusCode.OK)
            }
        }
    }
}
