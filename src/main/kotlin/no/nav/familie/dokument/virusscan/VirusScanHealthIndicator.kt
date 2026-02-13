package no.nav.familie.dokument.virusscan

import no.nav.familie.restklient.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local && !local-fd && !integration-test")
class VirusScanHealthIndicator(client: VirusScanClient) : AbstractHealthIndicator(client, "virusscan")
