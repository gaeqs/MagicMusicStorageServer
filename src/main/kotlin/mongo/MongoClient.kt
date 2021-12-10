package mongo

import com.mongodb.MongoClientSettings
import com.mongodb.MongoWriteException
import data.*
import mongo.exception.MongoExceptionCodes
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.KMongo
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.util.concurrent.TimeUnit

class MongoClient {

    companion object {
        const val DATABASE_NAME = "magic_music_storage"
        const val USERS_COLLECTION = "users"
    }

    private val client: CoroutineClient
    private val database: CoroutineDatabase

    val collection: CoroutineCollection<User>

    init {
        val settings = MongoClientSettings.builder().applyToClusterSettings {
            it.serverSelectionTimeout(5, TimeUnit.SECONDS)
        }.build()
        client = KMongo.createClient(settings).coroutine
        database = client.getDatabase(DATABASE_NAME)
        collection = database.getCollection(USERS_COLLECTION)
    }

    suspend fun createUser(name: String, password: String): Boolean {
        try {
            collection.insertOne(User(name, BCrypt.hashpw(password, BCrypt.gensalt())))
            return true
        } catch (ex: MongoWriteException) {
            if (ex.code == MongoExceptionCodes.DUPLICATE_KEY) {
                return false
            }
            throw ex
        }
    }

    suspend fun checkUser(name: String, password: String): Boolean {
        val hash = collection.projection(User::passwordHash, User::name eq name).first() ?: return false
        return BCrypt.checkpw(password, hash)
    }

    suspend fun getSongs(user: String, section: String): List<Song> {
        val result = collection
            .findAndCast<UserQuery>(User::name eq user)
            .projection(User::sections elemMatch (Section::name eq section))
            .first()

        if (result?.sections == null) return emptyList()
        return result.sections.flatMap { it.songs }
    }

    suspend fun getSections(user: String): List<String> {
        val data = collection.findAndCast<UserQuery>(User::name eq user)
            .projection(User::sections / Section::name).first() ?: return emptyList()
        return data.sections.map { it.name }
    }

    suspend fun getSectionsAndSongs(user: String): Map<String, List<Song>> {
        val data = collection.findAndCast<UserQuery>(User::name eq user)
            .projection(User::sections / Section::name, User::sections / Section::songs).first() ?: return emptyMap()

        return data.sections.associate { Pair(it.name, it.songs.map { s -> s.copy(id = "") }) }
    }

    suspend fun getAlbums(user: String): List<String> {
        val data = collection.findAndCast<UserQuery>(User::name eq user)
            .projection(User::albums / Album::name).first() ?: return emptyList()
        return data.albums.map { it.name }
    }

    suspend fun getAlbumImage(user: String, album: String): File? {
        val data = collection.findAndCast<UserQuery>(User::name eq user)
            .projection(User::albums elemMatch (Album::name eq album)).first() ?: return null
        return data.albums.firstOrNull()?.image
    }

    suspend fun hasSection(user: String, section: String): Boolean {
        return collection.findOne(
            User::name eq user,
            User::sections elemMatch (Section::name eq section)
        ) != null
    }

    suspend fun hasAlbum(user: String, album: String): Boolean {
        return collection.findOne(
            User::name eq user,
            User::albums elemMatch (Album::name eq album)
        ) != null
    }

    suspend fun addSection(user: String, section: String): Boolean {
        if (hasSection(user, section)) return false

        try {
            collection.updateOne(User::name eq user, addToSet(User::sections, Section(section)))
            return true
        } catch (ex: MongoWriteException) {
            if (ex.code == MongoExceptionCodes.DUPLICATE_KEY) {
                return false
            }
            ex.printStackTrace()
            throw ex
        }
    }

    suspend fun addAlbum(user: String, album: Album): Boolean {
        try {
            collection.updateOne(User::name eq user, addToSet(User::albums, album))
            return true
        } catch (ex: MongoWriteException) {
            if (ex.code == MongoExceptionCodes.DUPLICATE_KEY) {
                return false
            }
            ex.printStackTrace()
            throw ex
        }
    }

    suspend fun hasSectionSong(user: String, section: String, song: String): Boolean {
        return collection.findOne(
            User::name eq user,
            User::sections elemMatch (and(
                Section::name eq section,
                Section::songs elemMatch (Song::name eq song)
            ))
        ) != null
    }

    suspend fun addSong(user: String, section: String, song: Song): Boolean {
        if (hasSectionSong(user, section, song.name)) return false
        try {
            collection.updateOne(
                and(User::name eq user, User::sections / Section::name eq section),
                addToSet(User::sections.posOp / Section::songs, song)
            )
            return true
        } catch (ex: MongoWriteException) {
            if (ex.code == MongoExceptionCodes.DUPLICATE_KEY) {
                return false
            }
            ex.printStackTrace()
            throw ex
        }
    }

    suspend fun addHistoryEntry(user: String, entry: HistoryEntry) {
        collection.updateOne(
            User::name eq user,
            push(User::history, entry)
        )
    }


}