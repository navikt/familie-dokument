package no.nav.familie.dokument.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtAudienceValidator
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

@Component
class TokenXJwtDecoder(
    @Value("\${TOKEN_X_JWKS_URI}") jwksUri: String,
    @Value("\${TOKEN_X_ISSUER}") issuer: String,
    @Value("\${TOKEN_X_CLIENT_ID}") audience: String,
) : JwtDecoder {
    private val decoder =
        run {
            NimbusJwtDecoder.withJwkSetUri(jwksUri).build().apply {
                setJwtValidator(
                    createDefaultWithValidators(
                        JwtIssuerValidator(issuer),
                        JwtAudienceValidator(audience),
                        JwtClaimValidator<String>("acr") { acr -> acr == "Level4" || acr == "idporten-loa-high" },
                    ),
                )
            }
        }

    override fun decode(token: String?): Jwt? = decoder.decode(token)
}
