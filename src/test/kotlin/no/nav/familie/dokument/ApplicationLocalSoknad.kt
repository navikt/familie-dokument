package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"], excludeName = ["DevLauncher"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class ApplicationLocalSoknad

/**
 * Bruk denne launcheren hvis du skal bruke ef-søknad-api/baks-soknad-api
 * Disse appene bruker fakedings til å hente token, derfor er det en egen profil for lokal kjøring som validerer token mot fakedings
 *
 * Skal du bruke familie-dokument ifm sak/brev så start opp ApplicationLocal
 */

fun main(args: Array<String>) {
    val springApp = SpringApplication(ApplicationLocalSoknad::class.java)
    springApp.setAdditionalProfiles("local-fd")
    springApp.run(*args)
}
