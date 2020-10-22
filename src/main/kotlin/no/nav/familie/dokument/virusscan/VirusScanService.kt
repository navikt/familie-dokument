package no.nav.familie.dokument.virusscan

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VirusScanService(private val client: VirusScanClient) {

    private val logger = LoggerFactory.getLogger(VirusScanService::class.java)

    fun scan(bytes: ByteArray, name: String) {
        logger.debug("Scanner {}", name)
        val scanResults = client.scan(bytes)
        if (scanResults.size != 1) {
            throw VirusScanException("Uventet respons med lengde ${scanResults.size}, forventet lengde er 1")
        }
        val scanResult: ScanResult = scanResults.first()
        logger.debug("Fikk scan result {}", scanResult);
        if (Result.OK != scanResult.result) {
            logger.warn("Fant virus i {}, status {}", name, scanResult.result)
        }
    }

}
