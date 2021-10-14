package no.nav.familie.dokument.virusscan

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VirusScanService(private val client: VirusScanClient,
                       private val metadataLoggerService: MetadataLoggerService) {

    private val logger = LoggerFactory.getLogger(VirusScanService::class.java)

    fun scan(bytes: ByteArray, name: String) {
        logger.debug("Scanner {}", name)
        metadataLoggerService.logPdfMetadata(bytes)
        val scanResults = client.scan(bytes)
        if (scanResults.size != 1) {
            throw VirusScanException("Uventet respons med lengde ${scanResults.size}, forventet lengde er 1")
        }
        val scanResult: ScanResult = scanResults.first()
        logger.debug("Fikk scan result {}", scanResult);
        if (Result.OK != scanResult.result) {
            throw VirusScanException("Fil ikke godkjent, status=${scanResult.result} fil=$name")
        }
    }

}
