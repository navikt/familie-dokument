package no.nav.familie.dokument.config

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-oauth-selv")
@EnableMockOAuth2Server
class MockOauthServerConfig