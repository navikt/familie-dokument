package no.nav.familie.dokument.storage.attachment

import java.util.Arrays
import java.util.Optional

@Suppress("unused")
enum class Format(var mimeType: String) {

    PDF("application/pdf"),
    PNG("image/png"),
    JPEG("image/jpeg"),
    ;

    companion object {

        fun fromMimeType(mimeType: String): Optional<Format> {
            return Arrays.stream(values()).filter { format -> format.mimeType.equals(mimeType, ignoreCase = true) }.findAny()
        }
    }
}
