package no.nav.familie.dokument.virusscan

import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class VirusScanClient(
    private val config: VirusScanConfig,
    consumerIdClientInterceptor: ConsumerIdClientInterceptor,
) {
    private val restClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .build()

    private val scanUri: URI =
        UriComponentsBuilder
            .fromUri(config.uri)
            .path("scan")
            .build()
            .toUri()

    fun scan(bytes: ByteArray): List<ScanResult> {
        try {
            return restClient
                .put()
                .uri(scanUri)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes)
                .retrieve()
                .body<List<ScanResult>>()!!
        } catch (e: Exception) {
            throw VirusScanException("Feilet virusscanning", e)
        }
    }
}
