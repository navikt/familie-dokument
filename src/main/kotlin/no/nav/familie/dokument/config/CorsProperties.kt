package no.nav.familie.dokument.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "cors")
@ConstructorBinding
data class CorsProperties(val allowedOrigins: Set<String>)
