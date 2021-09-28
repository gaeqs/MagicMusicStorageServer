import com.mongodb.MongoClientSettings
import data.Album
import data.UserSession
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.util.ObjectMappingConfiguration
import java.io.File
import java.util.concurrent.TimeUnit

fun main() {

    val settings = MongoClientSettings.builder()
        .applyToClusterSettings {
            it.serverSelectionTimeout(5, TimeUnit.SECONDS)
        }.build()

    val client = KMongo.createClient(settings).coroutine

    val database = client.getDatabase("test")
    val collection = database.getCollection<Album>("artists_test")

    runBlocking {

        collection.insertOne(Album("Test artist", File("/test.png")))
        val album = collection.findOne(Album::name eq "Test artist" )

        println(album)
        println("Done.")
    }

    embeddedServer(Netty, port = 25565) {

        install(Sessions) {
            cookie<UserSession>("user_session")
        }

        install(Authentication) {
            basic("auth-basic") {
                realm = "Access to the '/' path"

                skipWhen { call -> call.sessions.get<UserSession>() != null }
                validate { credentials -> validateUser(credentials.name, credentials.password) }

            }
        }

        routing {

            get("/") {
                call.respondText("hello world!")

            }

        }
    }.start(wait = true)
}

private fun validateUser(name: String, password: String): UserIdPrincipal? {
    return if (name == "patata" && password == "pototo") UserIdPrincipal(name) else null
}
