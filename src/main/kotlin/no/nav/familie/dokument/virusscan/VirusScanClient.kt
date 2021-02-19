package no.nav.familie.dokument.virusscan

import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class VirusScanClient(operations: RestOperations, private val config: VirusScanConfig) :
        AbstractPingableRestClient(operations, "virusscan") {

    private val scanUri = UriComponentsBuilder.fromUri(config.uri).path("scan").build().toUri()

    fun scan(bytes: ByteArray): List<ScanResult> {
        try {
            return putForEntity(scanUri, bytes)
        } catch (e: Exception) {
            throw VirusScanException("Feilet virusscanning", e)
        }
    }

    override val pingUri: URI
        get() = UriComponentsBuilder.fromUri(config.uri).path("liveness").build().toUri()

}
