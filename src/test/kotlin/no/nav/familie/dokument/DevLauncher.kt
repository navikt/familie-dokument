package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
@Import(TokenGeneratorConfiguration::class)
@EnableJwtTokenValidation
class DevLauncher

fun main(args: Array<String>) {
    val springApp = SpringApplication(DevLauncher::class.java)
    springApp.setAdditionalProfiles("dev")
//    springApp.setDefaultProperties(mapOf("spring.main.allow-bean-definition-overriding" to true))
    springApp.run(*args)
}
