package no.nav.familie.dokument.config

import com.google.cloud.storage.Storage
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.dokument.storage.attachment.ImageConversionService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@Profile("integration-test")
@Configuration
class IntegrationTestConfig {
    @Bean
    @Primary
    fun tokenValidationContextHolderMock(): TokenValidationContextHolder {
        val tokenValidationContextHolder =  mockk<TokenValidationContextHolder>()
        every{tokenValidationContextHolder.tokenValidationContext} returns mockk()
        return tokenValidationContextHolder
    }

    @Bean
    @Primary
    fun storageMock(): Storage {
        val storageMock = mockk<Storage>()
        return storageMock
    }

    @Bean
    fun imageConversionService(): ImageConversionService{
        val imageConversionServiceMock = mockk<ImageConversionService>()
        return imageConversionServiceMock
    }

    @Bean
    fun objectMapper(): ObjectMapper{
        return ObjectMapper()
    }
}