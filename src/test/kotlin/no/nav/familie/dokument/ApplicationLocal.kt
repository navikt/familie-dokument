package no.nav.familie.dokument

import no.nav.familie.dokument.config.MockOAuth2ServerInitializer
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
class ApplicationLocal

/**
 * Bruk denne launcheren hvis du skal bruke familie-brev / saksbehandling.
 * Skal du bruke familie-dokument ifm søknad så start opp ApplicationLocalSoknad
 */
fun main(args: Array<String>) {
    val springApp = SpringApplication(ApplicationLocal::class.java)
    springApp.setAdditionalProfiles("local")
    springApp.addInitializers(MockOAuth2ServerInitializer())
    springApp.run(*args)
}
