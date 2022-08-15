package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"], excludeName = ["DevLauncher"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class DevLauncherSoknad

/**
 * Bruk denne launcheren hvis du skal bruke ef-søknad-api/ba-søknad-
 * Familie-dokument er ikke ansvarlig for håndtering av oauth ved lokal kjøring
 * Denne porten må også settes i eks ef-soknad-api/ba-soknad-api
 *
 * Skal du bruke familie-dokument ifm sak/brev så start opp Devlauncher
 */
private val mockOauth2ServerPort: String = "11588"

fun main(args: Array<String>) {
    val springApp = SpringApplication(DevLauncherSoknad::class.java)
    springApp.setAdditionalProfiles("dev")
    springApp.setDefaultProperties(mapOf("mock-oauth2-server.port" to mockOauth2ServerPort))
    springApp.run(*args)
}
