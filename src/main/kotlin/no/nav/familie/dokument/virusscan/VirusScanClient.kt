package no.nav.familie.dokument.virusscan

import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class VirusScanClient(operations: RestOperations, private val config: VirusScanConfig) :
        AbstractPingableRestClient(operations, "virusscan") {

    fun scan(bytes: ByteArray): List<ScanResult> {
        try {
            return putForEntity(config.uri, bytes)
        } catch (e: Exception) {
            throw VirusScanException("Feilet virusscanning", e)
        }
    }

    override val pingUri: URI
        get() = config.uri

}
