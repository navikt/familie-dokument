package no.nav.familie.dokument.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import no.nav.familie.dokument.storage.Storage
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

open class S3Storage internal constructor(private val s3: AmazonS3, maxFileSizeMB: Int) : Storage<InputStream, ByteArray> {
    private val maxFileSizeAfterEncryption: Int

    init {
        S3Initializer(s3).initializeBucket(VEDLEGG_BUCKET)
        maxFileSizeAfterEncryption = (maxFileSizeMB.toDouble() * 1000.0 * 1000.0 * ENCRYPTION_SIZE_FACTOR).toInt()
        log.debug("S3 Storage initialized")
    }

    override fun put(directory: String, key: String, data: InputStream) {

        val bytes: ByteArray
        val b: ByteArrayInputStream
        try {

            bytes = IOUtils.toByteArray(data)
            log.debug("Bufret stream som gav antall bytes: " + bytes.size)
        } catch (e: IOException) {
            throw RuntimeException("Feil oppsto ved bufring av stream.", e)
        }

        val objectMetadata = ObjectMetadata()
        objectMetadata.contentLength = bytes.size.toLong();
        val request = PutObjectRequest(VEDLEGG_BUCKET, fileName(directory, key), ByteArrayInputStream(bytes), objectMetadata)
        request.getRequestClientOptions().setReadLimit(maxFileSizeAfterEncryption)

        val result = s3.putObject(request)
        log.debug("Stored file with size {}", result.getMetadata().getContentLength())
    }

    override operator fun get(directory: String, key: String): ByteArray {
        return readString(fileName(directory, key))
    }

    override fun delete(directory: String, key: String) {
        val request = DeleteObjectRequest(VEDLEGG_BUCKET, fileName(directory, key))
        s3.deleteObject(request)
        log.debug("Deleted file from bucket")
    }

    private fun readString(filename: String): ByteArray {
        try {
            fileContent(filename).use { inputStream ->

                val buffer = ByteArrayOutputStream()
                inputStream.copyTo(buffer, 4096)

                return buffer.toByteArray()
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable parse $filename", e)
        }

    }

    private fun fileContent(filename: String): InputStream {
        val vedleggObjekt = s3.getObject(VEDLEGG_BUCKET, filename)
        log.debug("Loading file with size {}", vedleggObjekt.objectMetadata.contentLength)
        return vedleggObjekt.objectContent
    }

    private fun fileName(directory: String, key: String): String {
        return directory + "_" + key
    }

    companion object {

        private val log = LoggerFactory.getLogger(S3Storage::class.java)

        private val VEDLEGG_BUCKET = "familievedlegg"
        private val ENCRYPTION_SIZE_FACTOR = 1.5
    }

}
