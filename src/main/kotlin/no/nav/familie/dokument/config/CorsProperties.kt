package no.nav.familie.dokument.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(val allowedOrigins: Set<String>)
