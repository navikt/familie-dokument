package no.nav.familie.dokument.storage.integrationTest

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [ApplicationLocalTestLauncher::class])
@ActiveProfiles(
    "integrationtest",
    "mock-kodeverk",
    "mock-dokument",
    "mock-pdl",
    "mock-pdlApp2AppClient",
    "mock-mottak",
    "kodeverk-cache-test",
    "mock-saf",
    "mock-saksbehandling",
)
@EnableMockOAuth2Server
abstract class OppslagSpringRunnerTest {

    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()
    protected val headers = HttpHeaders()

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        headers.clear()
        loggingEvents.clear()
        resetWiremockServers()
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    protected fun getPort(): String {
        return port.toString()
    }

    protected fun localhost(uri: String): String {
        return LOCALHOST + getPort() + uri
    }

    protected fun søkerBearerToken(
        personident: String = "12345678911",
    ): String {
        val clientId = "lokal:teamfamilie:familie-dokument"
        return mockOAuth2Server.issueToken(
            issuerId = "tokenx",
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = "tokenx",
                subject = personident,
                audience = listOf("familie-app"),
                claims = mapOf("acr" to "Level4", "client_id" to clientId),
                expiry = 3600,
            ),
        ).serialize()
    }

    companion object {

        private const val LOCALHOST = "http://localhost:"
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
