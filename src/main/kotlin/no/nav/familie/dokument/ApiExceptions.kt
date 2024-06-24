package no.nav.familie.dokument

import no.nav.familie.dokument.BadRequestCode.INVALID_DOCUMENT_FORMAT
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

open class BadRequestException(val code: BadRequestCode, val secureLogMessage: String? = null) : RuntimeException("CODE=${code.name}")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidJsonSoknad(msg: String) : IllegalArgumentException(msg)

class InvalidDocumentSize(code: BadRequestCode) : BadRequestException(code)

class InvalidImageDimensions(code: BadRequestCode) : BadRequestException(code)

class InvalidDocumentFormat(message: String) : BadRequestException(INVALID_DOCUMENT_FORMAT, message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class GcpDocumentNotFound : RuntimeException("Finner ikke dokumentet i Google Storage")

enum class BadRequestCode {
    DOCUMENT_MISSING,
    IMAGE_TOO_LARGE,
    IMAGE_DIMENSIONS_TOO_SMALL,
    INVALID_DOCUMENT_FORMAT,
    VIRUS_FOUND,
}
