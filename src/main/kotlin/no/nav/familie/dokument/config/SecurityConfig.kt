package no.nav.familie.dokument.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.jsonMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
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
    @Order(1)
    fun publicFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher(
                "/internal/**",
                "/api/html-til-pdf",
                "/familie/dokument/api/html-til-pdf",
                "/api/mapper/ping",
                "/familie/dokument/api/mapper/ping",
            )
            cors { configurationSource = corsConfigurationSource() }
            csrf { disable() }
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }

    @Bean
    @Order(2)
    fun securedFilterChain(
        http: HttpSecurity,
        tokenXJwtDecoder: TokenXJwtDecoder,
    ): SecurityFilterChain {
        http {
            cors { configurationSource = corsConfigurationSource() }
            csrf { disable() }
            authorizeHttpRequests {
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                jwt { jwtDecoder = tokenXJwtDecoder }
            }
            exceptionHandling {
                accessDeniedHandler = accessDeniedHandler()
                authenticationEntryPoint = authenticationEntryPoint()
            }
        }
        return http.build()
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowedOrigins = corsProperties.allowedOrigins.toList()
        corsConfiguration.allowedHeaders = listOf("origin", "content-type", "content-length", "accept", "authorization", "nav-consumer-id")
        corsConfiguration.allowCredentials = true
        corsConfiguration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfiguration)
        }
    }

    private fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            response.apply {
                status = HttpServletResponse.SC_FORBIDDEN
                contentType = MediaType.APPLICATION_JSON_VALUE
                characterEncoding = Charsets.UTF_8.name()
                jsonMapper.writeValue(
                    writer,
                    Ressurs(
                        data = null,
                        status = Ressurs.Status.IKKE_TILGANG,
                        melding = "Bruker har ikke tilgang",
                        frontendFeilmelding = "Du har ikke tilgang",
                        stacktrace = null,
                    ),
                )
            }
        }

    private fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _: AuthenticationException ->
            response.apply {
                status = HttpServletResponse.SC_UNAUTHORIZED
                addHeader("WWW-Authenticate", "Bearer realm=\"familie-dokument\"")
                contentType = MediaType.APPLICATION_JSON_VALUE
                characterEncoding = Charsets.UTF_8.name()
                jsonMapper.writeValue(
                    writer,
                    failure<Nothing>(
                        errorMessage = "401 Unauthorized",
                        frontendFeilmelding = "Kall ikke autorisert",
                    ),
                )
            }
        }
}
