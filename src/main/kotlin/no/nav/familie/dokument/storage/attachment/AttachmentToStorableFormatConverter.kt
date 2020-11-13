package no.nav.familie.dokument.storage.attachment

import org.apache.tika.Tika

class AttachmentToStorableFormatConverter(private val imageConversionService: ImageConversionService) {

    fun toStorageFormat(input: ByteArray): ByteArray {
        val detectedType = Format.fromMimeType(Tika().detect(input))
                .orElseThrow { RuntimeException("Kunne ikke konvertere vedleggstypen") }

        return if (Format.PDF == detectedType) {
            input
        } else {
            imageConversionService.convert(input)
        }
    }

}
