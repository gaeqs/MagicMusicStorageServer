package util

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.errors.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Returns the image given by the client.
 *
 * If the image is not found or an IOException occurs, this method sends [HttpStatusCode.BadRequest] and throws a [NoSuchElementException].
 * @return the image.
 * @throws NoSuchElementException
 */
suspend fun PipelineContext<Unit, ApplicationCall>.receiveImage(): BufferedImage {
    try {
        val receiver = call.receiveMultipart()
        val file: PartData.FileItem = receiver.readPart() as PartData.FileItem

        val image: BufferedImage? = ImageIO.read(file.streamProvider.invoke())
        if (image == null) {
            call.respondText("Invalid image!", status = HttpStatusCode.BadRequest)
            throw NoSuchElementException()
        }
        return image
    } catch (e: IOException) {
        call.respondText("Invalid image!", status = HttpStatusCode.BadRequest)
        throw NoSuchElementException()
    }
}