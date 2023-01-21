package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class LocalApplicationLauncher

/**
 * Bruk denne launcheren hvis du skal bruke familie-brev / saksbehandling.
 * Skal du bruke familie-dokument ifm søknad så start opp LocalApplicationLauncherSoknad
 */
fun main(args: Array<String>) {
    val springApp = SpringApplication(LocalApplicationLauncher::class.java)
    springApp.setAdditionalProfiles("local", "mock-oauth-selv")
    springApp.run(*args)
}
