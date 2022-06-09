package no.nav.familie.dokument.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Storage
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.familie.dokument.testutils.ExtensionMockUtil
import no.nav.familie.dokument.virusscan.VirusScanService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("integration-test")
@Configuration
class IntegrationTestConfig {

    @Bean
    @Primary
    fun tokenValidationContextHolderMock(): TokenValidationContextHolder {
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>()
        ExtensionMockUtil.setUpMockHentFnr()
        every { tokenValidationContextHolder.tokenValidationContext } returns mockk()
        return tokenValidationContextHolder
    }

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

    @Bean
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return objectMapper
    }
}
