package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.InvalidDocumentFormat
import no.nav.familie.dokument.TestUtil
import no.nav.familie.dokument.storage.attachment.Testpdfer.KATALOG
import no.nav.familie.dokument.storage.attachment.Testpdfer.Objektstrøm
import no.nav.familie.dokument.storage.attachment.Testpdfer.SIDER
import no.nav.familie.dokument.storage.attachment.Testpdfer.Xref
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSObjectKey
import org.apache.pdfbox.cos.COSStream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal class FlattenPdfServiceTest {
    private val samvaersavtaleAdobe: ByteArray = TestUtil.toByteArray("samvaersavtaler/samvaersavtale_adobe.pdf")
    private val samvaersavtaleChrome: ByteArray = TestUtil.toByteArray("samvaersavtaler/samvaersavtale_chrome.pdf")
    private val samvaersavtaleEndretMedChrome: ByteArray = TestUtil.toByteArray("samvaersavtaler/samvaersavtale_endret_i_chrome.pdf")

    private val grense = FlattenPdfService.MAKS_UTPAKKET_PER_PDF.toInt()
    private val forStor = "mer enn ${grense / (1024 * 1024)} MiB"

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

    @Test
    internal fun `skal ikke endre pdf uten skjema`() {
        // Arrange
        val pdf = pdfFraPdfBox()

        // Act
        val konvertert = FlattenPdfService().convert(pdf)

        // Assert
        assertThat(konvertert).isSameAs(pdf)
    }

    @Test
    internal fun `skal ikke endre pdf med skjema uten felter`() {
        // Arrange
        val katalogMedTomtSkjema = "<< /Type /Catalog /Pages 3 0 R /AcroForm << /Fields [] >> >>"
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(katalogMedTomtSkjema, SIDER))))

        // Act
        val konvertert = FlattenPdfService().convert(pdf)

        // Assert
        assertThat(konvertert).isSameAs(pdf)
    }

    @Test
    internal fun `skal ikke endre pdf som er kryptert uten passord`() {
        // Arrange
        val pdf = pdfFraPdfBox(StandardProtectionPolicy("eier", "", AccessPermission()))

        // Act
        val konvertert = FlattenPdfService().convert(pdf)

        // Assert
        assertThat(konvertert).isSameAs(pdf)
    }

    @Test
    internal fun `skal ikke endre pdf som er beskyttet med passord`() {
        // Arrange
        val pdf = pdfFraPdfBox(StandardProtectionPolicy("eier", "bruker", AccessPermission()))

        // Act
        val konvertert = FlattenPdfService().convert(pdf)

        // Assert
        assertThat(konvertert).isSameAs(pdf)
    }

    @Test
    internal fun `skal flate ut skjema der objektstrømmen pakkes ut til mindre enn grensen`() {
        // Arrange
        val katalogMedSkjema = "<< /Type /Catalog /Pages 3 0 R /AcroForm << /Fields [4 0 R] >> >>"
        val tekstfelt = "<< /FT /Tx /T (felt) >>"
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(katalogMedSkjema, SIDER, tekstfelt), utpakketStørrelse = grense / 2)))

        // Act
        val konvertert = FlattenPdfService().convert(pdf)

        // Assert
        Loader.loadPDF(konvertert).use { assertThat(it.documentCatalog.acroForm.fields).isEmpty() }
    }

    @Test
    internal fun `skal flate ut skjema der samme skrift pakkes ut for hvert felt`() {
        // Arrange
        val pdf = skjemaMedInnebygdSkrift(antallFelter = 20)

        // Act
        val konvertert = FlattenPdfService().convert(pdf)

        // Assert
        Loader.loadPDF(konvertert).use { assertThat(it.documentCatalog.acroForm.fields).isEmpty() }
    }

    @Test
    internal fun `skal avvise pdf der et filter som er oppgitt to ganger skjuler utpakking til mer enn grensen`() {
        // Arrange
        // '>' avslutter ASCIIHex, så et filter som ble kjørt to ganger ville gitt en tom strøm
        val objektstrøm =
            Objektstrøm(
                listOf(KATALOG, SIDER),
                utpakketStørrelse = 2 * grense,
                filter = "[/ASCIIHexDecode /ASCIIHexDecode /FlateDecode]",
                kod = { Testpdfer.asciiHex(Testpdfer.deflate(it).also { deflatet -> deflatet[0] = '>'.code.toByte() }) },
            )
        val pdf = Testpdfer.pdf(listOf(objektstrøm))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der en objektstrøm pakkes ut til mer enn grensen`() {
        // Arrange
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), utpakketStørrelse = 2 * grense)))
        assertThat(pdf.size).isLessThan(100 * 1024)

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der flere objektstrømmer til sammen pakkes ut til mer enn grensen`() {
        // Arrange
        val katalogMedSkjemaIEgenStrøm = "<< /Type /Catalog /Pages 4 0 R /AcroForm 5 0 R >>"
        val pdf =
            Testpdfer.pdf(
                listOf(
                    Objektstrøm(listOf(katalogMedSkjemaIEgenStrøm, SIDER), utpakketStørrelse = grense * 2 / 3),
                    Objektstrøm(listOf("<< /Fields [] >>"), utpakketStørrelse = grense * 2 / 3),
                ),
            )

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der samme objektstrøm pakkes ut flere ganger til mer enn grensen`() {
        // Arrange
        val katalogMedFelterSomMangler = "<< /Type /Catalog /Pages 3 0 R /AcroForm << /Fields [4 0 R 5 0 R] >> >>"
        val objektstrøm = Objektstrøm(listOf(katalogMedFelterSomMangler, SIDER), utpakketStørrelse = grense / 2)
        val pdf = Testpdfer.pdf(listOf(objektstrøm), Xref(objekterSomMangler = 2))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise skjema der samme skrift pakkes ut for mange ganger`() {
        // Arrange
        val pdf = skjemaMedInnebygdSkrift(antallFelter = 200)

        BegrensetPdfParser(pdf, maksBytes = 1024 * 1024).parse().use { dokument ->
            // Act & Assert
            assertThatThrownBy { dokument.documentCatalog.acroForm }
                .isInstanceOfSatisfying(InvalidDocumentFormat::class.java) { assertThat(it.secureLogMessage).contains("for mange ganger") }
        }
    }

    @Test
    internal fun `skal avvise pdf uten xref der en objektstrøm pakkes ut til mer enn grensen`() {
        // Arrange
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), utpakketStørrelse = 2 * grense)), Xref(finnes = false))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der flere filtre til sammen pakker ut til mer enn grensen`() {
        // Arrange
        val objektstrøm =
            Objektstrøm(
                listOf(KATALOG, SIDER),
                utpakketStørrelse = grense * 4 / 10,
                filter = "[/FlateDecode /ASCIIHexDecode]",
                kod = { Testpdfer.deflate(Testpdfer.asciiHex(it)) },
            )
        val pdf = Testpdfer.pdf(listOf(objektstrøm))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der predictor ber om større radbuffere enn grensen`() {
        // Arrange
        val filter = "/FlateDecode /DecodeParms << /Predictor 12 /Columns ${grense * 3 / 4} >>"
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), filter = filter)))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der predictor i en liste av parametre ber om større radbuffere enn grensen`() {
        // Arrange
        val filter = "[/FlateDecode] /DecodeParms [<< /Predictor 12 /Columns ${grense * 3 / 4} >>]"
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), filter = filter)))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    @Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    internal fun `skal avvise pdf der predictor gir rader uten lengde`() {
        // Arrange
        val filter = "/FlateDecode /DecodeParms << /Predictor 2 /Columns 0 >>"
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), filter = filter)))

        // Act & Assert
        assertAvvist(pdf, "ugyldig radlengde")
    }

    @Test
    internal fun `skal avvise pdf der en objektstrøm uten filter er større enn grensen`() {
        // Arrange
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), utpakketStørrelse = 2 * grense, filter = null, kod = { it })))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der xref-strømmen har rader uten bredde`() {
        // Arrange
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER))), Xref(radbredder = "[0 0 0]"))

        // Act & Assert
        assertAvvist(pdf, "ugyldig radbredde")
    }

    @Test
    internal fun `skal avvise pdf der xref-strømmen har for brede rader`() {
        // Arrange
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER))), Xref(radbredder = "[1 $grense 2]"))

        // Act & Assert
        assertAvvist(pdf, "ugyldig radbredde")
    }

    @Test
    internal fun `skal telle smale rader i xref-strømmen som åtte bytes`() {
        // Arrange
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER))), Xref(radbredder = "[1 0 0]", utpakketStørrelse = grense / 4))

        // Act & Assert
        assertAvvist(pdf, forStor)
    }

    @Test
    internal fun `skal avvise pdf der en strøm oppgir negativ lengde`() {
        // Arrange
        val sluttPåForrigeStrøm = "endstream\nendobj\n"
        val hode = "4 0 obj\n<< /Length -00 >>\nstream\n"
        val lengdeTilbakeTilForrigeStrøm = -(sluttPåForrigeStrøm.length + hode.length)
        val pdf =
            (
                "%PDF-1.4\n" +
                    "1 0 obj\n<< /Type /Catalog /Pages 2 0 R /AcroForm 4 0 R >>\nendobj\n" +
                    "2 0 obj\n<< /Type /Pages /Kids [] /Count 0 >>\nendobj\n" +
                    "3 0 obj\n<< /Length 0 >>\nstream\n\n$sluttPåForrigeStrøm" +
                    hode.replace("-00", "$lengdeTilbakeTilForrigeStrøm") +
                    "\nendstream\nendobj\n%%EOF\n"
            ).toByteArray(Charsets.ISO_8859_1)

        // Act & Assert
        assertAvvist(pdf, "negativ lengde")
    }

    @Test
    internal fun `skal rette lengden på en strøm som oppgir feil lengde`() {
        // Arrange
        val katalogMedSider = "<< /Type /Catalog /Pages 4 0 R >>"
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(katalogMedSider, SIDER)), Objektstrøm(listOf("<< >>"), lengde = 1L shl 30)))

        BegrensetPdfParser(pdf, FlattenPdfService.MAKS_UTPAKKET_PER_PDF).parse().use { dokument ->
            // Act
            val objektstrøm = dokument.document.getObjectFromPool(COSObjectKey(2, 0)).`object` as COSStream

            // Assert
            assertThat(objektstrøm.getLong(COSName.LENGTH)).isLessThan(1024)
        }
    }

    @Test
    internal fun `skal avvise pdf der en objektstrøm har bildefilter`() {
        // Arrange
        val jpeg = TestUtil.toByteArray("dummy/jpg_dummy.jpg")
        val pdf = Testpdfer.pdf(listOf(Objektstrøm(listOf(KATALOG, SIDER), filter = "/DCTDecode", kod = { jpeg })))

        // Act & Assert
        assertAvvist(pdf, "filter som ikke støttes")
    }

    @Test
    internal fun `skal stoppe utpakking når en strøm leses som InputStream`() {
        // Arrange
        val katalogMedSider = "<< /Type /Catalog /Pages 4 0 R >>"
        val pdf =
            Testpdfer.pdf(
                listOf(
                    Objektstrøm(listOf(katalogMedSider, SIDER)),
                    Objektstrøm(listOf("<< >>"), utpakketStørrelse = 2 * grense),
                ),
            )

        BegrensetPdfParser(pdf, FlattenPdfService.MAKS_UTPAKKET_PER_PDF).parse().use { dokument ->
            val objektstrøm = dokument.document.getObjectFromPool(COSObjectKey(2, 0)).`object` as COSStream

            // Act & Assert
            assertThatThrownBy { objektstrøm.createInputStream() }
                .isInstanceOfSatisfying(InvalidDocumentFormat::class.java) { assertThat(it.secureLogMessage).contains(forStor) }
        }
    }

    private fun assertAvvist(
        pdf: ByteArray,
        grunn: String,
    ) {
        assertThatThrownBy { FlattenPdfService().convert(pdf) }
            .isInstanceOfSatisfying(InvalidDocumentFormat::class.java) { assertThat(it.secureLogMessage).contains(grunn) }
    }

    private fun skjemaMedInnebygdSkrift(antallFelter: Int): ByteArray {
        val out = ByteArrayOutputStream()
        PDDocument().use { dokument ->
            val side = PDPage().also { dokument.addPage(it) }
            val skrift = javaClass.getResourceAsStream("/fonts/SourceSansPro-Regular.ttf").use { PDType0Font.load(dokument, it, false) }
            val skjema = PDAcroForm(dokument).also { dokument.documentCatalog.acroForm = it }
            skjema.defaultResources = PDResources().apply { put(COSName.getPDFName("F1"), skrift) }
            skjema.defaultAppearance = "/F1 10 Tf 0 g"
            repeat(antallFelter) { nummer ->
                val felt = PDTextField(skjema).apply { partialName = "felt$nummer" }
                val widget = felt.widgets.single()
                widget.rectangle = PDRectangle(50f, 700f - nummer * 20f, 300f, 15f)
                widget.page = side
                side.annotations.add(widget)
                skjema.fields.add(felt)
                felt.value = "Svar"
            }
            skjema.needAppearances = true
            dokument.save(out)
        }
        return out.toByteArray()
    }

    private fun pdfFraPdfBox(beskyttelse: StandardProtectionPolicy? = null): ByteArray {
        val out = ByteArrayOutputStream()
        PDDocument().use { dokument ->
            dokument.addPage(PDPage())
            beskyttelse?.let { dokument.protect(it) }
            dokument.save(out)
        }
        return out.toByteArray()
    }
}
