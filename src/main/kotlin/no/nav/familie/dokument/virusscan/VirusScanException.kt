package no.nav.familie.dokument.virusscan

class VirusScanException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, exception: Exception) : super(message, exception)
}
