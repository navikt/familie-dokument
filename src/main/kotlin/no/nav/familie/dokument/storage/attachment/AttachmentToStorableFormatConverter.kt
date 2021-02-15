package no.nav.familie.dokument.storage.attachment

import org.apache.tika.Tika

class AttachmentToStorableFormatConverter(private val imageConversionService: ImageConversionService) {

    fun toStorageFormat(input: ByteArray): ByteArray {
        val mimeType = Tika().detect(input)
        val detectedType = Format.fromMimeType(mimeType)
                .orElseThrow { RuntimeException("Kunne ikke konvertere vedleggstypen $mimeType") }

        return if (Format.PDF == detectedType) {
            input
        } else {
            imageConversionService.convert(input)
        }
    }

}
