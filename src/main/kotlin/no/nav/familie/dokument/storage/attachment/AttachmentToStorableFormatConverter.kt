package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.TimeLogger
import org.apache.tika.Tika

class AttachmentToStorableFormatConverter(private val imageConversionService: ImageConversionService) {

    fun toStorageFormat(input: ByteArray): ByteArray {
        val detectedType = TimeLogger.log({
                                              Format.fromMimeType(Tika().detect(input))
                                                      .orElseThrow { RuntimeException("Kunne ikke konvertere vedleggstypen") }
                                          }, "AttachmentToStorableFormatConverter::finnFormat")
        return if (Format.PDF == detectedType) {
            input
        } else {
            imageConversionService.convert(input)
        }
    }

}
