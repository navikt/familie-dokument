package no.nav.familie.dokument

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
class DevLauncher

fun main(args: Array<String>) {
    SpringApplication.run(DevLauncher::class.java, *args)
}