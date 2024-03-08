package no.nav.familie.dokument.config

import com.google.cloud.storage.Storage
import io.mockk.mockk
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.familie.dokument.virusscan.VirusScanService
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("integration-test")
@EnableJwtTokenValidation
@Configuration
class IntegrationTestConfig {

    @Bean
    @Primary
    fun storageMock(): Storage = mockk()

    @Bean
    @Primary
    fun virusScanService(): VirusScanService = mockk(relaxed = true)

    @Bean
    fun imageConversionService(): ImageConversionService {
        val imageConversionServiceMock = mockk<ImageConversionService>()
        return imageConversionServiceMock
    }
}
