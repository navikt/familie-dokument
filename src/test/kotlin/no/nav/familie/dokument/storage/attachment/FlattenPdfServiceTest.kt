package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.TestUtil
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal class FlattenPdfServiceTest {

    private val samvaersavtaleAdobe: ByteArray = TestUtil.toByteArray("samvaersavtaler/samvaersavtale_adobe.pdf")
    private val samvaersavtaleChrome: ByteArray = TestUtil.toByteArray("samvaersavtaler/samvaersavtale_chrome.pdf")
    private val samvaersavtaleEndretMedChrome: ByteArray = TestUtil.toByteArray("samvaersavtaler/samvaersavtale_endret_i_chrome.pdf")

    @Test
    internal fun `skal konvertere adobe-versjon med normale verdier`() {
        val convertedAdobe = FlattenPdfService().convert(samvaersavtaleAdobe)
        Files.createDirectories(Paths.get("target/samvaersavtale"))
        File("target/samvaersavtale", "samvaersavtale_konvertert_adobe.pdf").writeBytes(convertedAdobe)
    }

    @Test
    internal fun `skal konvertere chrome-versjon med normale verdier`() {
        val convertedChrome = FlattenPdfService().convert(samvaersavtaleChrome)
        Files.createDirectories(Paths.get("target/samvaersavtale"))
        File("target/samvaersavtale", "samvaersavtale_konvertert_chrome.pdf").writeBytes(convertedChrome)
    }

    @Test
    internal fun `skal konvertere editert chrome-versjon med normale verdier`() {
        val convertedChrome = FlattenPdfService().convert(samvaersavtaleEndretMedChrome)
        Files.createDirectories(Paths.get("target/samvaersavtale"))
        File("target/samvaersavtale", "samvaersavtale_konvertert_chrome_endret.pdf").writeBytes(convertedChrome)
    }
}
