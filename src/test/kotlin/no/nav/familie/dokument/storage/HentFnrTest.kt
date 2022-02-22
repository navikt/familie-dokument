package no.nav.familie.dokument.storage

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.test.JwtTokenGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

internal class HentFnrTest {

    private val sub: String = "11111111111"
    private val pid: String = "22222222222"

    private val contextHolder = SpringTokenValidationContextHolder()

    @AfterEach
    internal fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    internal fun `skal kaste feil hvis det ikke finnes en token`() {
        mockRequestContextHolder(emptyMap())
        assertThatThrownBy { contextHolder.hentFnr() }
                .hasMessage("Finner ikke token for ekstern bruker - issuers=[]")
    }

    @Test
    internal fun `skal kaste feil hvis det ikke finnes subject eller pid`() {
        mockContext()
        assertThatThrownBy { contextHolder.hentFnr() }
                .hasMessage("Finner ikke sub/pid på token")
    }

    @Test
    internal fun `returnerer subject hvis ikke pid finnes`() {
        mockContext(sub = sub)
        assertThat(contextHolder.hentFnr()).isEqualTo(sub)
    }

    @Test
    internal fun `returnerer pid hvis pid og sub finnes finnes`() {
        mockContext(sub = sub, pid = pid)
        assertThat(contextHolder.hentFnr()).isEqualTo(pid)
    }

    @Test
    internal fun `returnerer pid hvis kun pid finnes`() {
        mockContext(pid = pid)
        assertThat(contextHolder.hentFnr()).isEqualTo(pid)
    }

    private fun mockContext(sub: String? = null, pid: String? = null) {

        val builder = JWTClaimsSet.Builder()
        sub?.let { builder.subject(it) }
        pid?.let { builder.claim("pid", it) }
        val jwtToken = JwtTokenGenerator.createSignedJWT(builder.build())

        mockRequestContextHolder(mapOf("selvbetjening" to JwtToken(jwtToken.serialize())))
    }

    private fun mockRequestContextHolder(map: Map<String, JwtToken>) {
        val requestAttributes = mockk<RequestAttributes>()
        every { requestAttributes.getAttribute(any(), any()) } returns TokenValidationContext(map)
        RequestContextHolder.setRequestAttributes(requestAttributes)
    }

}