package no.nav.familie.dokument.storage.google

import com.google.cloud.storage.*
import no.nav.familie.dokument.GcpDocumentNotFound
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import java.io.InputStream

class GcpStorage(val bucketName: String, maxFileSizeMB: Int, val storage: Storage) {

    private val maxFileSizeAfterEncryption: Int = (maxFileSizeMB.toDouble() * 1000.0 * 1000.0 * ENCRYPTION_SIZE_FACTOR).toInt()

    fun makeKey(directory: String, key: String) = "${directory}_${key}"

    fun put(directory: String, key: String, data: InputStream, mediaTypeValue: String) {
        try {
            val bytes = IOUtils.toByteArray(data)
            LOG.debug("Bufret stream som gav antall bytes: " + bytes.size)
            if(bytes.size > maxFileSizeAfterEncryption){
                throw RuntimeException("GcpStorage feil: vedlegg overskrider filstørrelsesgrensen\n")
            }

            val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, makeKey(directory, key)))
                    .setContentType(mediaTypeValue).build()
            val blob = storage.create(blobInfo, bytes)
            LOG.debug("Stored file with size {}", blob.getContent().size)
        } catch (e: Exception) {
            throw RuntimeException("Feil oppsto ved bufring av stream.", e)
        }
    }

    operator fun get(directory: String, key: String): ByteArray {
        return try {
            storage.get(bucketName, makeKey(directory, key)).getContent()
        } catch (e: StorageException) {
            if (HttpStatus.SC_NOT_FOUND == e.code) {
                throw GcpDocumentNotFound()
            }
            throw e
        }
    }

    fun delete(directory: String, key: String) {
        storage.delete(BlobId.of(bucketName, makeKey(directory, key)))
        LOG.debug("Deleted file from bucket ${key}")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(GcpStorage::class.java)
        private val ENCRYPTION_SIZE_FACTOR = 1.5
    }
}