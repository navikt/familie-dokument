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
import java.lang.IllegalArgumentException

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

        storage.put(directory, stønad.stønadKey, søknad)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping(path = ["/{stonad}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMellomlagretSøknad(@PathVariable("stonad") stønad: StønadParameter): ResponseEntity<String> {
        val directory = contextHolder.hentFnr()
        val data = storage[directory, stønad.stønadKey]
        return if(data!= null) ResponseEntity.ok(data) else ResponseEntity.noContent().build()
    }

    @DeleteMapping(path = ["/{stonad}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettMellomlagretSøknad(@PathVariable("stonad") stønad: StønadParameter): ResponseEntity<String> {
        val directory = contextHolder.hentFnr()
        log.debug("Sletter mellomlagret overgangsstønad")
        storage.delete(directory, stønad.stønadKey)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    private fun validerGyldigJson(verdi: String) {
        try {
            objectMapper.readTree(verdi);
        } catch (e: Exception) {
            throw IllegalArgumentException("Forsøker å mellomlagre søknad som ikke er gyldig json-verdi")
        }
    }

    enum class StønadParameter(val stønadKey: String) {
        overgangsstonad("overgangsstønad"),
        barnetilsyn("barnetilsyn"),
        skolepenger("skolepenger")
    }
}
