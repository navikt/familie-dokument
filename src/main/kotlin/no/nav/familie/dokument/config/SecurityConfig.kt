package no.nav.familie.dokument.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        tokenXJwtDecoder: TokenXJwtDecoder,
    ): SecurityFilterChain {
        http {
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
}
