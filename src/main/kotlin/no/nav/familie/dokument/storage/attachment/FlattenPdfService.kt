package no.nav.familie.dokument.storage.attachment

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream


@Service
class FlattenPdfService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun convert(input: ByteArray): ByteArray {
        return try {
            val loadedPdf = PDDocument.load(input)
            val pdAcroForm: PDAcroForm? = loadedPdf.documentCatalog.acroForm
            if (loadedPdf.isEncrypted) return input
            pdAcroForm?.let {
                it.flatten()
                val byteArrayOutputStream = ByteArrayOutputStream()
                loadedPdf.save(byteArrayOutputStream)
                byteArrayOutputStream.toByteArray()
            } ?: input
        } catch (e: InvalidPasswordException) {
            logger.info("PDF-en er passordbeskyttet og det er ikke mulig å flate ut potensielle pdf-form felter")
            input
        }
    }
}
