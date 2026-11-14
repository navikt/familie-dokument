package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.InvalidDocumentFormat
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class FlattenPdfService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun convert(input: ByteArray): ByteArray {
        val budsjett = Utpakkingsbudsjett(MAKS_UTPAKKET_PER_PDF)
        val resultat = runCatching { flatUtSkjemafelter(input, budsjett) }
        // Spør budsjettet i stedet for å fange feilen, i tilfelle PDFBox har fanget den og lest videre
        budsjett.avvisningsgrunn?.let { grunn ->
            logger.warn("Avviser PDF. $grunn")
            throw InvalidDocumentFormat("PDF-en ble avvist. $grunn")
        }
        return resultat.getOrThrow()
    }

    private fun flatUtSkjemafelter(
        input: ByteArray,
        budsjett: Utpakkingsbudsjett,
    ): ByteArray {
        return try {
            BegrensetPdfParser(input, budsjett).parse().use { loadedPdf ->
                val pdAcroForm: PDAcroForm? = loadedPdf.documentCatalog.acroForm
                if (loadedPdf.isEncrypted) return input
                pdAcroForm?.let {
                    it.flatten()
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    loadedPdf.save(byteArrayOutputStream)
                    byteArrayOutputStream.toByteArray()
                } ?: input
            }
        } catch (e: InvalidPasswordException) {
            logger.info("PDF-en er passordbeskyttet og det er ikke mulig å flate ut potensielle pdf-form felter")
            input
        }
    }

    companion object {
        const val MAKS_UTPAKKET_PER_PDF = 8L * 1024 * 1024
    }
}
