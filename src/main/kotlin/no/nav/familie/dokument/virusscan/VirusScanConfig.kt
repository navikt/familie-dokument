package no.nav.familie.dokument.virusscan

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class VirusScanConfig constructor(@Value("\${CLAM_AV_VIRUS_URL}") val uri: URI)
