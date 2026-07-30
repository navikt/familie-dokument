package no.nav.familie.dokument.storage.integrationTest

import no.nav.familie.dokument.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(scanBasePackages = ["no.nav.familie.dokument"])
@Import(ApplicationConfig::class)
class ApplicationLocalTestLauncher
