package no.nav.familie.dokument.storage

import no.nav.security.token.support.core.context.TokenValidationContextHolder

fun TokenValidationContextHolder.hentFnr(): String {
    return this.tokenValidationContext.getJwtToken("selvbetjening").subject
}
