package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"], excludeName = ["DevLauncher"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class ApplicationLocalSoknad

/**
 * Bruk denne launcheren hvis du skal bruke ef-søknad-api/ba-søknad-
 * Familie-dokument er ikke ansvarlig for håndtering av oauth ved lokal kjøring
 * Denne porten må også settes i eks ef-soknad-api/ba-soknad-api
 *
 * Skal du bruke familie-dokument ifm sak/brev så start opp ApplicationLocal
 */
private val mockOauth2ServerPort: String = "11588"

fun main(args: Array<String>) {
    val springApp = SpringApplication(ApplicationLocalSoknad::class.java)
    springApp.setAdditionalProfiles("local")
    springApp.setDefaultProperties(mapOf("mock-oauth2-server.port" to mockOauth2ServerPort))
    springApp.run(*args)
}
