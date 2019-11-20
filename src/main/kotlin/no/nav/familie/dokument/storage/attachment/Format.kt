package no.nav.familie.dokument.storage.attachment

import org.slf4j.LoggerFactory

import java.util.Arrays
import java.util.Optional

internal enum class Format private constructor(var mimeType: String) {
    PDF("application/pdf"),
    PNG("image/jpeg"),
    JPG("image/png");


    companion object {

        private val log = LoggerFactory.getLogger(Format::class.java)

        fun fromMimeType(mimeType: String): Optional<Format> {
            log.info("Forsøker å finne gydlig vedleggsformat fra detektert format {}", mimeType)
            return Arrays.stream(values()).filter { format -> format.mimeType.equals(mimeType, ignoreCase = true) }.findAny()
        }
    }
}
