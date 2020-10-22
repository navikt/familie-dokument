package no.nav.familie.dokument.virusscan

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import java.net.URI

@ConfigurationProperties(prefix = "virus")
@ConstructorBinding
class VirusScanConfig constructor(@DefaultValue(DEFAULT_CLAM_URI) val uri: URI) {

    companion object {

        private const val DEFAULT_CLAM_URI = "http://clamav.nais.svc.nais.local/scan"
    }
}
