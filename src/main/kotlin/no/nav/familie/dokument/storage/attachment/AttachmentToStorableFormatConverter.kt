package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.InvalidDocumentFormat
import org.apache.tika.Tika

class AttachmentToStorableFormatConverter(
    private val imageConversionService: ImageConversionService,
    private val flattenPdfService: FlattenPdfService,
) {

    fun toStorageFormat(input: ByteArray): ByteArray {
        val mimeType = Tika().detect(input)
        val detectedType = Format.fromMimeType(mimeType)
            .orElseThrow { InvalidDocumentFormat("Kunne ikke konvertere vedleggstypen $mimeType") }

        return if (Format.PDF == detectedType) {
            flattenPdfService.convert(input)
        } else {
            imageConversionService.convert(input, detectedType)
        }
    }
}
