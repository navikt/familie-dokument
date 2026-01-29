package no.nav.familie.dokument

import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.familie.util.FnrGenerator
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.test.assertEquals

@Profile("feil-controller")
@RestController
@RequestMapping(path = ["/api"], produces = [MediaType.APPLICATION_JSON_VALUE])
class FeilController {

    @GetMapping("feil")
    @Unprotected
    fun feil(): Unit = throw RuntimeException("Feil")

    @GetMapping("ok")
    @ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
    fun ok(): Map<String, String> = mapOf("a" to "b")

    @PostMapping("ok")
    @ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
    fun ok(@RequestBody body: List<String>): Map<String, String> = mapOf("a" to "b")
}

@ActiveProfiles("local", "feil-controller")
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [ApplicationLocal::class])
@EnableMockOAuth2Server
class ApiFeilIntegrationTest {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @LocalServerPort
    private var port: Int? = 0
    private val contextPath = "/api"

    private val restTemplate = TestRestTemplate()
    private val headers = HttpHeaders()

    @AfterEach
    internal fun tearDown() {
        headers.clear()
    }

    @Test
    fun `skal få 200 når autentisert og vi bruker get`() {
        headers.setBearerAuth(søkerBearerToken())
        val response = restTemplate.exchange<Any>(path("ok"), HttpMethod.GET, HttpEntity<Any>(headers))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `skal få 400 når man sender inn feil type objekt, liste i stedet for objekt`() {
        headers.setBearerAuth(søkerBearerToken())
        val response =
            restTemplate.exchange<Any>(path("ok"), HttpMethod.POST, HttpEntity<Any>(mapOf<String, String>(), headers))
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test // Tester handleExceptionInternal
    fun `skal få 415 når man sender inn feil type Content-Type`() {
        headers.setBearerAuth(søkerBearerToken())
        val response = restTemplate.exchange<Any>(path("ok"), HttpMethod.POST, HttpEntity<Any>("Hei", headers))
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.statusCode)
    }

    @Test
    fun `skal få 401 når ikke autentisert `() {
        val response = restTemplate.exchange<Any>(path("ok"), HttpMethod.GET, HttpEntity<Any>(headers))
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `skal få 404 når endepunkt ikke eksisterer`() {
        headers.setBearerAuth(søkerBearerToken())
        val response = restTemplate.exchange<Any>(path("eksistererIkke"), HttpMethod.GET, HttpEntity<Any>(headers))
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `skal få 500 når endepunkt kaster feil`() {
        val response = restTemplate.exchange<Any>(path("feil"), HttpMethod.GET, HttpEntity<Any>(headers))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    private fun path(path: String) = "http://localhost:$port$contextPath/$path"

    protected fun søkerBearerToken(personident: String = FnrGenerator.generer()): String {
        return jwt(personident)
    }

    private fun jwt(fnr: String) = mockOAuth2Server.token(subject = fnr)

    private fun MockOAuth2Server.token(
        subject: String,
        issuerId: String = EksternBrukerUtils.ISSUER_TOKENX,
        clientId: String = UUID.randomUUID().toString(),
        audience: String = "familie-app",
        claims: Map<String, Any> = mapOf("acr" to "Level4"),

    ): String {
        return this.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = subject,
                audience = listOf(audience),
                claims = claims,
                expiry = 3600,
            ),
        ).serialize()
    }
}
