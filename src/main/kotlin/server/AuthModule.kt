package server

import MONGO
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.serialization.Serializable
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

@Serializable
private data class LoginUser(val username: String, val password: String)

fun Application.authModule(testing: Boolean = false) {
    install(ContentNegotiation) {
        json()
    }

    val privateKeyString = environment.config.property("jwt.privateKey").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    val jwkProvider = JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("api-jwt") {
            realm = myRealm
            verifier(jwkProvider, issuer) {
                acceptLeeway(3)
            }
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        post("/genAuth") {
            val user = call.receive<LoginUser>()
            MONGO.createUser(user.username, user.password)
            call.respond(HttpStatusCode.OK)
        }
        post("/login") {
            try {
                println("Someone called!")
                val user = call.receive<LoginUser>()

                if (!MONGO.checkUser(user.username, user.password)) {
                    call.respond(HttpStatusCode.Unauthorized, "Couldn't find user.")
                    return@post
                }

                val publicKey = jwkProvider.get("VvZAUeXFyC-YpBUeChqO5x6hrc417WYih5R8hIHdG5s").publicKey
                val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
                val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
                val token = JWT.create()
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("username", user.username)
                    .withExpiresAt(Date(System.currentTimeMillis() + 1000 * 60 * 30))
                    .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
                call.respond(hashMapOf("token" to token))
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
        static(".well-known") {
            staticRootFolder = File("certs")
            file("jwks.json")
        }

    }
}