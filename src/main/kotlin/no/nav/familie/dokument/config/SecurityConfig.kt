package no.nav.familie.dokument.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties::class)
class SecurityConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        tokenXJwtDecoder: TokenXJwtDecoder,
    ): SecurityFilterChain {
        http {
            cors { configurationSource = corsConfigurationSource() }
            csrf { disable() }
            authorizeHttpRequests {
                authorize("/internal/**", permitAll)
                authorize("/api/html-til-pdf", permitAll)
                authorize("/familie/dokument/api/html-til-pdf", permitAll)
                authorize("/api/mapper/ping", permitAll)
                authorize("/familie/dokument/api/mapper/ping", permitAll)
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                jwt { jwtDecoder = tokenXJwtDecoder }
            }
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowedOrigins = corsProperties.allowedOrigins.toList()
        corsConfiguration.allowedHeaders = listOf("origin", "content-type", "content-length", "accept", "authorization", "nav-consumer-id")
        corsConfiguration.allowCredentials = true
        corsConfiguration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfiguration)
        }
    }
}
