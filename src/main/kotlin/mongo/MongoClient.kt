package mongo

import com.mongodb.MongoClientSettings
import com.mongodb.MongoWriteException
import data.User
import mongo.exception.MongoExceptionCodes
import org.litote.kmongo.SetTo
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.set
import org.mindrot.jbcrypt.BCrypt
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

    suspend fun updatePassword(name: String, password: String) {
        collection.updateOne(
            User::name eq name,
            set(SetTo(User::passwordHash, password))
        )
    }


}