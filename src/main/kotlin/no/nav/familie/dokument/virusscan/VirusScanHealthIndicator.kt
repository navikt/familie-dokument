package no.nav.familie.dokument.virusscan

import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.stereotype.Component

@Component
class VirusScanHealthIndicator(client: VirusScanClient) : AbstractHealthIndicator(client, "virusscan")
