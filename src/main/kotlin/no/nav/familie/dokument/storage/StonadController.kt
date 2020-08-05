package no.nav.familie.dokument.storage

import com.amazonaws.services.s3.model.AmazonS3Exception
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
@RequestMapping("api/soknad")
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class StonadController(@Autowired val storage: MellomLagerService,
                       @Autowired val contextHolder: TokenValidationContextHolder,
                       @Autowired val objectMapper: ObjectMapper) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping(
            path = ["/{stonad}"],
            consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun mellomlagreSøknad(@PathVariable("stonad") stønad: StønadParameter,
                          @RequestBody(required = true) søknad: String): ResponseEntity<Unit> {
        log.debug("Mellomlagrer søknad om overgangsstønad")

        validerGyldigJson(søknad)
        val directory = contextHolder.hentFnr()

        try {
            storage.put(directory, stønad.stønadKey, søknad)
        } catch (e: RuntimeException) {
            secureLogger.warn("Kunne ikke mellomlagre overgangsstønad for $directory", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping(path = ["/{stonad}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMellomlagretSøknad(@PathVariable("stonad") stønad: StønadParameter): ResponseEntity<String> {
        val directory = contextHolder.hentFnr()
        return try {
            val data = storage[directory, stønad.stønadKey]
            ResponseEntity.ok(data)
        } catch (e: RuntimeException) {
            if (e is AmazonS3Exception && e.statusCode == 404) {
                ResponseEntity.noContent().build()
            } else {
                secureLogger.info("Noe gikk galt", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    @DeleteMapping(path = ["/{stonad}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettMellomlagretSøknad(@PathVariable("stonad") stønad: StønadParameter): ResponseEntity<String> {
        val directory = contextHolder.hentFnr()
        return try {
            log.debug("Sletter mellomlagret overgangsstønad")
            storage.delete(directory, stønad.stønadKey)
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        } catch (e: RuntimeException) {
            secureLogger.warn("Kunne ikke slette mellomlagret overgangsstønad for $directory", e)
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


    enum class StønadParameter(val stønadKey: String) {
        overgangsstonad("overgangsstønad"),
        barnetilsyn("barnetilsyn"),
        skolepenger("skolepenger")
    }
}
