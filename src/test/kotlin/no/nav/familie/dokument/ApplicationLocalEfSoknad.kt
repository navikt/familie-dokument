package no.nav.familie.dokument.no.nav.familie.dokument

import no.nav.familie.dokument.ApplicationLocalSoknad
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"], excludeName = ["DevLauncher"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class ApplicationLocalEfSoknad

/**
 * Bruk denne launcheren hvis du skal bruke ef-søknad-api
 * Familie-ef-søknad bruker fakedings til å hente token, derfor er det en egen ef-profil for lokal kjøring som validerer token mot fakedings
 *
 * Skal du bruke familie-dokument ifm sak/brev så start opp ApplicationLocal
 */

fun main(args: Array<String>) {
    val springApp = SpringApplication(ApplicationLocalEfSoknad::class.java)
    springApp.setAdditionalProfiles("local-ef")
    springApp.run(*args)
}
