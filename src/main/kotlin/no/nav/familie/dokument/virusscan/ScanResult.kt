package no.nav.familie.dokument.virusscan

import com.fasterxml.jackson.annotation.JsonProperty

data class ScanResult(@JsonProperty("Result") val result: Result)

enum class Result {
    FOUND,
    OK
}