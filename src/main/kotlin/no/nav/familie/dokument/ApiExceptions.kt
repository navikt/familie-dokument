package no.nav.familie.dokument

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.IllegalArgumentException

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidJsonSoknad(msg: String): IllegalArgumentException(msg)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidDocumentSize(msg: String): IllegalArgumentException(msg)

@ResponseStatus(HttpStatus.NOT_FOUND)
class GcpDocumentNotFound : RuntimeException("Finner ikke dokumentet i Google Storage")