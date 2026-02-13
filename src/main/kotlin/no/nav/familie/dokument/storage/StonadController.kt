package no.nav.familie.dokument.storage

import no.nav.familie.dokument.GcpDocumentNotFound
import no.nav.familie.dokument.InvalidJsonSoknad
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.storage.mellomlager.MellomLagerService
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper

@RestController
@RequestMapping("familie/dokument/api/soknad", "api/soknad")
@RequiredIssuers(
    ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"]),
)
class StonadController(
    @Autowired val storage: MellomLagerService,
    @Autowired val contextHolder: TokenValidationContextHolder,
    @Autowired val jsonMapper: JsonMapper,
    @Autowired val hasher: Hasher,
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(
        path = ["/{stonad}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun mellomlagreSøknad(
        @PathVariable("stonad") stønad: StønadParameter,
        @RequestBody(required = true) søknad: String,
    ): ResponseEntity<Unit> {
        log.debug("Mellomlagrer søknad om overgangsstønad")

        validerGyldigJson(søknad)

        val directory = hasher.lagFnrHash(contextHolder.hentFnr())

        storage.put(directory, stønad.stønadKey, søknad)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping(path = ["/{stonad}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMellomlagretSøknad(@PathVariable("stonad") stønad: StønadParameter): ResponseEntity<String> {
        val directory = hasher.lagFnrHash(contextHolder.hentFnr())

        return try {
            ResponseEntity.ok(storage[directory, stønad.stønadKey])
        } catch (e: GcpDocumentNotFound) {
            ResponseEntity.noContent().build()
        }
    }

    @DeleteMapping(path = ["/{stonad}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun slettMellomlagretSøknad(@PathVariable("stonad") stønad: StønadParameter): ResponseEntity<String> {
        val directory = hasher.lagFnrHash(contextHolder.hentFnr())

        log.debug("Sletter mellomlagret overgangsstønad")
        storage.delete(directory, stønad.stønadKey)
        return ResponseEntity.noContent().build()
    }

    private fun validerGyldigJson(verdi: String) {
        try {
            jsonMapper.readTree(verdi)
        } catch (e: Exception) {
            throw InvalidJsonSoknad("Forsøker å mellomlagre søknad som ikke er gyldig json-verdi")
        }
    }

    @Suppress("unused", "ktlint:standard:enum-entry-name-case")
    enum class StønadParameter(val stønadKey: String) {
        overgangsstonad("overgangsstønad"),
        barnetilsyn("barnetilsyn"),
        skolepenger("skolepenger"),
        barnetrygd("barnetrygd"),
        kontantstotte("kontantstotte"),
    }
}
