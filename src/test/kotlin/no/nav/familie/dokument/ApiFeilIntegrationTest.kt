package no.nav.familie.dokument


import no.nav.security.token.support.core.JwtTokenConstants
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.test.JwtTokenGenerator
import org.glassfish.jersey.logging.LoggingFeature
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.bind.annotation.*
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Response
import kotlin.test.assertEquals

@Profile("feil-controller")
@RestController
@RequestMapping(path = ["/api"], produces = [MediaType.APPLICATION_JSON_VALUE])
class FeilController {

    @GetMapping("feil")
    @Unprotected
    fun feil(): Unit = throw RuntimeException("Feil")

    @GetMapping("ok")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
    fun ok(): Map<String, String> = mapOf("a" to "b")

    @PostMapping("ok")
    @ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
    fun ok(@RequestBody body: List<String>): Map<String, String> = mapOf("a" to "b")
}

@ActiveProfiles("dev", "feil-controller")
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [DevLauncher::class])
class ApiFeilIntegrationTest {

    @Value("\${local.server.port}")
    val port: Int = 0
    val contextPath = "/api"
    val tokenSubject = "12345678911"

    @Test
    fun `skal få 200 når autentisert og vi bruker get`() {
        val response = webTarget().path("/ok")
                .request()
                .header(JwtTokenConstants.AUTHORIZATION_HEADER, "Bearer ${serializedJWTToken()}")
                .get()
        assertEquals(Response.Status.OK.statusCode, response.status)
    }

    @Test
    fun `skal få 400 når man sender inn feil type objekt, liste i stedet for objekt`() {
        val response = webTarget().path("/ok")
                .request()
                .header(JwtTokenConstants.AUTHORIZATION_HEADER, "Bearer ${serializedJWTToken()}")
                .post(Entity.json("{}"))
        assertEquals(Response.Status.BAD_REQUEST.statusCode, response.status)
    }

    @Test // Tester handleExceptionInternal
    fun `skal få 415 når man sender inn feil type Content-Type`() {
        val response = webTarget().path("/ok")
                .request()
                .header(JwtTokenConstants.AUTHORIZATION_HEADER, "Bearer ${serializedJWTToken()}")
                .post(Entity.text("Hei"))
        assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.statusCode, response.status)
    }

    @Test
    fun `skal få 401 når ikke autentisert `() {
        val response = webTarget().path("/ok")
                .request()
                .get()
        assertEquals(Response.Status.UNAUTHORIZED.statusCode, response.status)
    }

    @Test
    fun `skal få 404 når endepunkt ikke eksisterer`() {
        val response = webTarget().path("/eksistererIkke")
                .request()
                .header(JwtTokenConstants.AUTHORIZATION_HEADER, "Bearer ${serializedJWTToken()}")
                .get()
        assertEquals(Response.Status.NOT_FOUND.statusCode, response.status)
    }

    @Test
    fun `skal få 500 når endepunkt kaster feil`() {
        val response = webTarget().path("/feil")
                .request()
                .get()
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.statusCode, response.status)
    }

    private fun webTarget() = client().target("http://localhost:$port$contextPath")

    private fun client() = ClientBuilder.newClient().register(LoggingFeature::class.java)

    private fun serializedJWTToken() = JwtTokenGenerator.createSignedJWT(tokenSubject).serialize()

}

