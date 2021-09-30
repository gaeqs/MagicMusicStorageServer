import com.mongodb.MongoClientSettings
import data.Section
import data.Song
import data.User
import data.UserSession
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.util.concurrent.TimeUnit

fun main() {

    val settings = MongoClientSettings.builder()
        .applyToClusterSettings {
            it.serverSelectionTimeout(5, TimeUnit.SECONDS)
        }.build()

    val client = KMongo.createClient(settings).coroutine
    val database = client.getDatabase("test")
    val users = database.getCollection<User>("users")

    //val user = User("pepe", "aksjjsdjd")
    runBlocking {
        //users.insertOne(user)

        //val result = users.updateOne("{_id: 'pepe', 'sections._id': 'potato'}",
        //    "{\$push: { 'sections.$.songs': { _id: 'song2', artist: 'artist2', album: 'album2'  } }}")

        val result = users.updateOne("{_id: 'pepe', 'sections.songs._id': 'song2'}",
            "{\$set: { 'sections.$.songs.$._id': 'song22' }}")

        println(result)

        //users.updateOne(
        //    and(User::name eq "pepe", User::sections / Section::name eq "patata"),
        //    //push(User::sections, Section("patata", mutableSetOf(Song("song", "artist", "album"))))
        //)


        val user = users
            .findAndCast<Map<Any, Any>>(User::name eq "pepe")
            .projection(
                User::sections elemMatch and(
                    Section::name eq "patata",
                    Section::songs elemMatch (Song::name eq "song")
                )
            )
            //.projection(User::sections / Section::songs elemMatch (Song::name eq "song"))
            .first()
        println(user)
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
