package no.nav.familie.dokument.storage

import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.core.context.TokenValidationContextHolder


fun TokenValidationContextHolder.hentFnr(): String = EksternBrukerUtils.hentFnrFraToken()

