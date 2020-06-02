package no.nav.familie.dokument.storage

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/soknad/overgangsstonad")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class OvergangstonadController(@Autowired val storage: MellomLagerService,
                               @Autowired val contextHolder: TokenValidationContextHolder,
                               @Autowired val objectMapper: ObjectMapper) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val overgangsstønadKey = "overgangsstønad"

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun mellomlagreSøknad(@RequestBody(required = true) søknad: String): ResponseEntity<Unit> {

        log.debug("Mellomlagrer søknad om overgangsstønad")

        validerGyldigJson(søknad)
        val directory = contextHolder.hentFnr()

        storage.put(directory, overgangsstønadKey, søknad)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAttachment(): ResponseEntity<String> {
        val directory = contextHolder.hentFnr()
        return try {
            val data = storage[directory, overgangsstønadKey]
            log.debug("Loaded file with {}", data)
            ResponseEntity.ok(data)
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun validerGyldigJson(verdi: String) {
        try {
            objectMapper.readTree(verdi);
        } catch (e: Exception) {
            error("Forsøker å mellomlagre søknad som ikke er gyldig json-verdi")
        }
    }

}
