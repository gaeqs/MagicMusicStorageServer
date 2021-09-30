import data.UserSession
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import kotlinx.coroutines.runBlocking
import mongo.MongoClient

fun main() {

    //val section = Section("patata")
    //val section2 = Section("patata2")
    //section.songs += Song("song1", "artist1", "my album")
    //section.songs += Song("song2", "artist2", "my album")
    //section2.songs += Song("song3", "artist3", "my album")
    //val album = Album("my album", File("/"))
    //var user = User("pepe", "sjhjdhjdj", mutableSetOf(album), mutableSetOf(section, section2))

    val client = MongoClient()

    runBlocking {
        //users.insertOne(user)

        client.createUser("pepe", "a")

        //users.updateOne(
        //    "{_id: 'pepe'}",
        //    "{\$set: { 'sections.$[i].songs.$[j]._id': 'song2' }}",
        //    UpdateOptions().arrayFilters(listOf("i._id" eq "patata", "j._id" eq "song22"))
        //)

        //val result = users.updateOne("{_id: 'pepe', 'sections.songs._id': 'song2'}",
        //    "{\$set: { 'sections.$.songs.$._id': 'song22' }}")
        //println(result)

        //users.updateOne(
        //    and(User::name eq "pepe", User::sections / Section::name eq "patata"),
        //    //push(User::sections, Section("patata", mutableSetOf(Song("song", "artist", "album"))))
        //)


        //val user = users
        //    .findAndCast<Map<Any, Any>>(User::name eq "pepe")
        //    .projection(
        //        User::sections elemMatch and(
        //            Section::name eq "patata",
        //            Section::songs elemMatch (Song::name eq "song")
        //        )
        //    )
        //    //.projection(User::sections / Section::songs elemMatch (Song::name eq "song"))
        //    .first()
        //println(user)
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
