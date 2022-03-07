package no.nav.familie.dokument.storage.attachment

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream


@Service
class FlattenPdfService {

    fun convert(input: ByteArray): ByteArray {
        val loadedPdf = PDDocument.load(input)
        val pdAcroForm: PDAcroForm? = loadedPdf.documentCatalog.acroForm
        return pdAcroForm?.let {
            it.flatten()
            val byteArrayOutputStream = ByteArrayOutputStream()
            loadedPdf.save(byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } ?: input
    }
}
