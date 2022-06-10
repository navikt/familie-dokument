package no.nav.familie.dokument.virusscan

import no.nav.familie.dokument.storage.attachment.Format
import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service

@Service
class MetadataLoggerService {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private val taskExecutor = ThreadPoolTaskExecutor().apply {
        maxPoolSize = 10
        corePoolSize = 2
        setQueueCapacity(30)
        initialize()
    }

    fun logPdfMetadata(bytes: ByteArray) {
        try {
            if (taskExecutor.threadPoolExecutor.queue.remainingCapacity() < 5) {
                secureLogger.warn("PDF metadata - queue er for liten, ignorerer fil")
                return
            }
            val contextMap = MDC.getCopyOfContextMap()
            taskExecutor.execute {
                try {
                    MDC.setContextMap(contextMap)
                    logMetadata(bytes)
                } catch (e: Exception) {
                    secureLogger.warn("Feilet logging av metatdata", e)
                } finally {
                    MDC.clear()
                }
            }
        } catch (e: Exception) {
            secureLogger.warn("Feilet sjekk av metadata - {}", e.message)
        }
    }

    private fun logMetadata(bytes: ByteArray) {
        try {
            val start = System.currentTimeMillis()
            val tika = Tika()
            val mimeType = tika.detect(TikaInputStream.get(bytes))
            Format.fromMimeType(mimeType)
                .orElseThrow { RuntimeException("Kunne ikke konvertere vedleggstypen $mimeType") }
                .takeIf { Format.PDF == it }
                ?.let {
                    val metadata = Metadata()
                    tika.parse(TikaInputStream.get(bytes), metadata)
                    secureLogger.info("PDF metadata - {} ms={}", pdfMedata(metadata), (System.currentTimeMillis() - start))
                }
        } catch (e: Exception) {
            secureLogger.warn("Feilet sjekk av metadata - {}", e.message)
        }
    }

    private fun pdfMedata(metadata: Metadata): String {
        return listOf(
            "pdf:PDFVersion",
            "xmp:CreatorTool",
            "producer",
            "pdf:encrypted",
            "creator",
            "xmpTPg:NPages",
            "Creation-Date",
            "Last-Modified"
        ).joinToString(" ") { "$it=${metadata.get(it)}" }
    }
}
