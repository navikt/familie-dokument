package no.nav.familie.dokument.virusscan

data class ScanResult(val result: Result)

enum class Result {
    FOUND,
    OK
}