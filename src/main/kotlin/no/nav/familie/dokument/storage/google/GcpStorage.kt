package no.nav.familie.dokument.storage.google

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import no.nav.familie.dokument.GcpDocumentNotFound
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.InputStream

class GcpStorage(private val bucketName: String, maxFileSizeMB: Int, private val storage: Storage) {

    private val maxFileSizeAfterEncryption: Int = (maxFileSizeMB.toDouble() * 1000.0 * 1000.0 * ENCRYPTION_SIZE_FACTOR).toInt()

    fun makeKey(directory: String, key: String) = "${directory}_${key}"

    fun put(directory: String, key: String, data: InputStream, mediaTypeValue: String) {
        try {
            val bytes = IOUtils.toByteArray(data)
            LOG.debug("Bufret stream som gav antall bytes: ${bytes.size}")
            if(bytes.size > maxFileSizeAfterEncryption){
                throw RuntimeException("GcpStorage feil: vedlegg overskrider filstørrelsesgrensen")
            }

            val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, makeKey(directory, key)))
                    .setContentType(mediaTypeValue).build()
            val start = System.currentTimeMillis()
            storage.create(blobInfo, bytes)
            LOG.info("Lagring av tok {}ms", (System.currentTimeMillis() - start))
        } catch (e: Exception) {
            throw RuntimeException("Feil oppsto ved lagring av fil mot gcp.", e)
        }
    }

    operator fun get(directory: String, key: String): ByteArray {
        val start = System.currentTimeMillis()
        val get = storage.get(BlobId.of(bucketName, makeKey(directory, key)))
        LOG.info("Henting av tok {}ms", (System.currentTimeMillis() - start))
        val blob = get ?: throw GcpDocumentNotFound()
        return blob.getContent()
    }

    fun delete(directory: String, key: String) {
        storage.delete(BlobId.of(bucketName, makeKey(directory, key)))
        LOG.debug("Deleted file from bucket $key")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(GcpStorage::class.java)
        private const val ENCRYPTION_SIZE_FACTOR = 1.5
    }
}