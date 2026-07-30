package no.nav.familie.dokument.config

import no.nav.familie.dokument.storage.integrationTest.OppslagSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class SecurityConfigIntegrationTest : OppslagSpringRunnerTest() {
    @Test
    fun `internal health-endepunkt er tilgjengelig uten token`() {
        val response =
            restTemplate.exchange<String>(
                localhost("/internal/health"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `ping er tilgjengelig uten token på api-prefiks`() {
        val response =
            restTemplate.exchange<String>(
                localhost("/api/mapper/ping"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `ping er tilgjengelig uten token på familie-dokument-prefiks`() {
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/mapper/ping"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `html-til-pdf er tilgjengelig uten token på api-prefiks`() {
        val response =
            restTemplate.exchange<String>(
                localhost("/api/html-til-pdf"),
                HttpMethod.POST,
                HttpEntity("<html></html>", headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `html-til-pdf er tilgjengelig uten token på familie-dokument-prefiks`() {
        val response =
            restTemplate.exchange<String>(
                localhost("/familie/dokument/api/html-til-pdf"),
                HttpMethod.POST,
                HttpEntity("<html></html>", headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `html-til-pdf er tilgjengelig med Azure AD token`() {
        headers.setBearerAuth(token(issuerId = "azuread"))

        val response =
            restTemplate.exchange<String>(
                localhost("/api/html-til-pdf"),
                HttpMethod.POST,
                HttpEntity("<html></html>", headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `beskyttet endepunkt returnerer 401 uten token`() {
        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `beskyttet endepunkt returnerer 401 med ugyldig token`() {
        headers.setBearerAuth("ugyldig-token")

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `beskyttet endepunkt returnerer 401 med token fra ukjent issuer`() {
        headers.setBearerAuth(token(issuerId = "ukjent-issuer"))

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `beskyttet endepunkt returnerer 401 med token med feil audience`() {
        headers.setBearerAuth(token(audience = "feil-app"))

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `beskyttet endepunkt returnerer 401 med token med ugyldig acr-verdi`() {
        headers.setBearerAuth(token(acr = "Level3"))

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `beskyttet endepunkt returnerer 401 med token uten acr-claim`() {
        headers.setBearerAuth(token(acr = null))

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.GET,
                HttpEntity<String>(headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `preflight-kall fra tillatt origin returnerer CORS-headers`() {
        headers.set("Origin", "https://www.nav.no")
        headers.set("Access-Control-Request-Method", "POST")

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.OPTIONS,
                HttpEntity<String>(headers),
            )

        assertThat(response.headers["Access-Control-Allow-Origin"]).contains("https://www.nav.no")
        assertThat(response.headers["Access-Control-Allow-Methods"]?.single()).contains("POST")
    }

    @Test
    fun `preflight-kall fra ukjent origin returnerer ikke Access-Control-Allow-Origin`() {
        headers.set("Origin", "https://www.ondsinnet.no")

        val response =
            restTemplate.exchange<String>(
                localhost("/api/tilfeldig-endepunkt"),
                HttpMethod.OPTIONS,
                HttpEntity<String>(headers),
            )

        assertThat(response.headers["Access-Control-Allow-Origin"]).isNullOrEmpty()
    }
}
