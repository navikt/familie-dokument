package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class DevLauncher

/**
 * Familie-dokument er ikke ansvarlig for håndtering av oauth ved lokal kjøring
 * Denne porten må også settes i eks ef-soknad-api/ba-soknad-api
 */
private val mockOauth2ServerPort: String = "11588"

fun main(args: Array<String>) {
    val springApp = SpringApplication(DevLauncher::class.java)
    springApp.setAdditionalProfiles("dev")
    springApp.setDefaultProperties(mapOf("mock-oauth2-server.port" to mockOauth2ServerPort))
    springApp.run(*args)
}
