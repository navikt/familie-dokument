package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.storage.InternalRuntimeException

class AttachmentConversionException : InternalRuntimeException {

    constructor(message: String) : super(message) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}

}
