package no.nav.familie.dokument

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
@EnableJwtTokenValidation
class Launcher

fun main(args: Array<String>) {
    SpringApplication.run(Launcher::class.java, *args)
}