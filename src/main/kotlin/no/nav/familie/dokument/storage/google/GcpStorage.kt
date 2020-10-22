package no.nav.familie.dokument.storage.google

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.*
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import java.io.InputStream

class GcpStorage(maxFileSizeMB: Int, retrySettings: RetrySettings) {

    private val maxFileSizeAfterEncryption: Int = (maxFileSizeMB.toDouble() * 1000.0 * 1000.0 * ENCRYPTION_SIZE_FACTOR).toInt()
    private val storage: Storage

    init {
        storage = StorageOptions
                .newBuilder()
                .setRetrySettings(retrySettings)
                .build()
                .service
        LOG.info("Google Storage intialized")
    }

    fun makeKey(directory: String, key: String) = "${directory}_${key}"

    fun put(directory: String, key: String, data: InputStream) {
        try {
            val bytes = IOUtils.toByteArray(data)
            LOG.debug("Bufret stream som gav antall bytes: " + bytes.size)
            if(bytes.size > maxFileSizeAfterEncryption){
                throw RuntimeException("GcpStorage feil: vedlegg overskrider filstørrelsesgrensen\n")
            }

            val blobInfo = BlobInfo.newBuilder(BlobId.of(VEDLEGG_BUCKET, makeKey(directory, key)))
                    .setContentType(MediaType.APPLICATION_PDF_VALUE).build()
            val blob = storage.create(blobInfo, bytes)
            LOG.debug("Stored file with size {}", blob.getContent().size)
        } catch (e: Exception) {
            throw RuntimeException("Feil oppsto ved bufring av stream.", e)
        }
    }

    operator fun get(directory: String, key: String): ByteArray {
        return try {
            storage.get(VEDLEGG_BUCKET, makeKey(directory, key)).getContent()
        } catch (e: StorageException) {
            if (HttpStatus.SC_NOT_FOUND == e.code) {
                throw e
            }
            throw e
        }
    }

    fun delete(directory: String, key: String) {
        storage.delete(BlobId.of(VEDLEGG_BUCKET, makeKey(directory, key)))
        LOG.debug("Deleted file from bucket ${directory}")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(GcpStorage::class.java)
        private val VEDLEGG_BUCKET = "familie-dokument"
        private val ENCRYPTION_SIZE_FACTOR = 1.5
    }
}