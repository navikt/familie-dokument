package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
class DevLauncher

fun main(args: Array<String>) {
    val springApp = SpringApplication(DevLauncher::class.java)
    springApp.setAdditionalProfiles("dev")
//    springApp.setDefaultProperties(mapOf("spring.main.allow-bean-definition-overriding" to true))
    springApp.run(*args)
}
