package no.nav.familie.dokument.pdf

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester
import no.nav.familie.dokument.TestUtil.toByteArray
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.imageio.ImageIO
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

class PdfGeneratorTest {
    private val pdfService = PdfService()

    @Test
    fun testPdfGeneration() {
        val html = getDocument("eksempel1.html")
        val pdf = pdfService.lagPdf(html)
        assertTrue(isPdf(pdf))
        assertTrue(isPDF_2_ACompliant(pdf))
        assertTrue(pdfsAreEqual("eksempel1.pdf", toByteArray("$PDF_RESOURSE_PATH/eksempel1.pdf"), pdf))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("svgSomBatikIkkeStøtterAlt")
    fun `skal tegne svg som har elementer batik ikke støtter`(
        beskrivelse: String,
        svg: String,
    ) {
        // Arrange
        val html = "<html><head><title>$beskrivelse</title></head><body>$svg</body></html>"

        // Act
        val pdf = pdfService.lagPdf(html)

        // Assert
        assertTrue(isPdf(pdf))
        assertTrue(isPDF_2_ACompliant(pdf))
        val mørkePiksler = antallMørkePiksler(pdf)
        assertTrue(mørkePiksler > 100, "svg-en ble ikke tegnet, fant $mørkePiksler mørke piksler")
    }

    @Test
    fun `skal generere pdf med tom svg når batik ikke klarer å tegne den`() {
        // Arrange
        val html =
            """
            <html><head><title>Ugyldig svg</title></head><body style="font-family: 'Source Sans Pro'">
            <svg width="200" height="20"><text font-size="-3">tekst</text></svg><p>Teksten etter skal vises</p>
            </body></html>
            """.trimIndent()

        // Act
        val pdf = pdfService.lagPdf(html)

        // Assert
        assertEquals(emptySet<String>(), pdfA2ABrudd(pdf))
        val tekst = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }
        assertTrue(tekst.contains("Teksten etter skal vises"), "Teksten etter svg-en mangler i pdf-en: $tekst")
    }

    @Test
    fun `skal tegne svg-elementer med svg-prefiks`() {
        // Arrange
        val html =
            """
            <html><head><title>Prefiks</title></head><body>
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:svg="http://www.w3.org/2000/svg" width="200" height="200">
              <svg:rect width="200" height="200" fill="#000000" />
            </svg>
            </body></html>
            """.trimIndent()

        // Act
        val pdf = pdfService.lagPdf(html)

        // Assert
        val mørkePiksler = antallMørkePiksler(pdf)
        assertTrue(mørkePiksler > 10_000, "svg:rect ble ikke tegnet, fant $mørkePiksler mørke piksler")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """<div style="display:none"/>""",
            """<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><script href="a.js"/></svg>""",
        ],
    )
    fun `skal tolke selvlukkende tagger som tomme elementer`(selvlukkendeTagg: String) {
        // Arrange
        val html =
            """
            <html><head><title>Selvlukkende</title></head><body>
            $selvlukkendeTagg<p>Teksten etter skal vises</p>
            </body></html>
            """.trimIndent()

        // Act
        val pdf = pdfService.lagPdf(html)

        // Assert
        val tekst = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }
        assertTrue(tekst.contains("Teksten etter skal vises"), "Teksten etter $selvlukkendeTagg mangler i pdf-en: $tekst")
    }

    /**
     * Genererer pdf fra html som konsumentene av /api/html-til-pdf sender, og sammenligner tittel, tekst og utseende
     * med fasit-pdf-en som ligger ved siden av html-filen. Kildene til html-filene er beskrevet i
     * pdf/konsumenter/README.md.
     *
     * Ny html-fil legges under src/test/resources/pdf/konsumenter/<konsument>/. Fasit oppdateres manuelt: Når
     * fasiten mangler eller avviker, skrives pdf-en til target/regression-tests/ny-fasit med samme mappestruktur som
     * fasiten. Er den riktig, kopieres den inn som fasit.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("konsumentHtml")
    fun `skal generere pdf som er lik fasit for html fra konsument`(html: String) {
        // Arrange
        val fasit = html.removeSuffix(".html") + ".pdf"
        val fasitFil = Paths.get(SRC_TEST_RESOURCES, PDF_RESOURSE_PATH, fasit).toFile()
        File(NY_FASIT_PATH, fasit).delete()

        // Act
        val pdf = pdfService.lagPdf(getDocument(html))

        // Assert
        assertTrue(isPdf(pdf), "$html ga ikke gyldig pdf")
        val forventet =
            fasitFil.takeIf { it.exists() }?.readBytes()
                ?: fail("Mangler fasit for $html. ${skrivNyFasit(fasit, fasitFil, pdf)}")
        assertTrue(pdfsAreEqual(fasit, forventet, pdf)) {
            "$html avviker visuelt fra fasit, se avvikene i $TEST_OUTPUT_PATH. ${skrivNyFasit(fasit, fasitFil, pdf)}"
        }
        assertEquals(tekstinnhold(forventet), tekstinnhold(pdf)) {
            "$html har annen tittel eller tekst enn fasit. ${skrivNyFasit(fasit, fasitFil, pdf)}"
        }
        assertEquals(KJENTE_PDFA_BRUDD[html].orEmpty(), pdfA2ABrudd(pdf)) {
            "$html har andre PDF/A-2A-brudd enn de som står i KJENTE_PDFA_BRUDD"
        }
    }

    private fun skrivNyFasit(
        fasit: String,
        fasitFil: File,
        pdf: ByteArray,
    ): String {
        val nyFasit = File(NY_FASIT_PATH, fasit)
        nyFasit.parentFile.mkdirs()
        nyFasit.writeBytes(pdf)
        return "Er den nye pdf-en riktig, kopier $nyFasit til $fasitFil."
    }

    /**
     * Dokumenttittel og uttrekkbar tekst, som ikke fanges av den visuelle sammenligningen.
     */
    private fun tekstinnhold(pdf: ByteArray): String =
        Loader.loadPDF(pdf).use { "${it.documentInformation.title}\n${PDFTextStripper().getText(it)}" }

    private fun antallMørkePiksler(pdf: ByteArray): Int {
        val side = Loader.loadPDF(pdf).use { PDFRenderer(it).renderImageWithDPI(0, 72f) }
        return (0 until side.width).sumOf { x -> (0 until side.height).count { y -> side.getRGB(x, y) and 0xff < 64 } }
    }

    private fun getDocument(fixtureName: String): String =
        IOUtils.toString(
            this.javaClass.getResourceAsStream("/$PDF_RESOURSE_PATH/$fixtureName"),
            StandardCharsets.UTF_8,
        )

    /**
     * Test if the data in the given byte array represents a PDF file.
     */
    fun isPdf(data: ByteArray): Boolean {
        if (data.size > 4 && data[0] == 0x25.toByte() && // %
            data[1] == 0x50.toByte() && // P
            data[2] == 0x44.toByte() && // D
            data[3] == 0x46.toByte() && // F
            data[4] == 0x2D.toByte() // -
        ) {
            // version 1.3 file terminator
            if (data[data.size - 7] == 0x25.toByte() && // %
                data[data.size - 6] == 0x25.toByte() && // %
                data[data.size - 5] == 0x45.toByte() && // E
                data[data.size - 4] == 0x4F.toByte() && // O
                data[data.size - 3] == 0x46.toByte() && // F
                data[data.size - 2] == 0x20.toByte() && // SPACE
                data[data.size - 1] == 0x0A.toByte() // EOL
            ) {
                return true
            }

            // version 1.3 file terminator
            if (data[data.size - 6] == 0x25.toByte() && // %
                data[data.size - 5] == 0x25.toByte() && // %
                data[data.size - 4] == 0x45.toByte() && // E
                data[data.size - 3] == 0x4F.toByte() && // O
                data[data.size - 2] == 0x46.toByte() && // F
                data[data.size - 1] == 0x0A.toByte() // EOL
            ) {
                return true
            }
        }
        return false
    }

    private fun pdfsAreEqual(
        resource: String,
        expectedPdfBytes: ByteArray,
        actualPdfBytes: ByteArray,
    ): Boolean {
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH))
        val artifactName = resource.replace('/', '_')

        // Get a list of results.
        val problems =
            PdfVisualTester.comparePdfDocuments(
                expectedPdfBytes,
                actualPdfBytes,
                artifactName,
                false,
            )

        // Get a list of results.
        if (problems.isNotEmpty()) {
            System.err.println("Found problems with test case ($resource):")
            System.err.println(
                problems
                    .stream()
                    .map { p: PdfVisualTester.PdfCompareResult -> p.logMessage }
                    .collect(Collectors.joining("\n    ", "[\n    ", "\n]")),
            )
            System.err.println("For test case ($resource) writing failure artifacts to '$TEST_OUTPUT_PATH'")
            File(TEST_OUTPUT_PATH, "$artifactName---actual.pdf").writeBytes(actualPdfBytes)
        }
        for (result in problems) {
            if (result.testImages != null) {
                var output = File(TEST_OUTPUT_PATH, "$artifactName---${result.pageNumber}---diff.png")
                ImageIO.write(result.testImages.createDiff(), "png", output)
                output = File(TEST_OUTPUT_PATH, "$artifactName---${result.pageNumber}---actual.png")
                ImageIO.write(result.testImages.actual, "png", output)
                output = File(TEST_OUTPUT_PATH, "$artifactName---${result.pageNumber}---expected.png")
                ImageIO.write(result.testImages.expected, "png", output)
            }
        }
        return problems.isEmpty()
    }

    private fun isPDF_2_ACompliant(pdf: ByteArray): Boolean = pdfA2ABrudd(pdf).isEmpty()

    private fun pdfA2ABrudd(pdf: ByteArray): Set<String> {
        VeraGreenfieldFoundryProvider.initialise()
        val validator = Foundries.defaultInstance().createValidator(PDFAFlavour.PDFA_2_A, false)
        val parser = Foundries.defaultInstance().createParser(ByteArrayInputStream(pdf))
        return validator
            .validate(parser)
            .testAssertions
            .filter { it.status == TestAssertion.Status.FAILED }
            .map { "${it.ruleId.clause}-${it.ruleId.testNumber}" }
            .toSortedSet()
    }

    companion object {
        private const val TEST_OUTPUT_PATH = "target/regression-tests/"
        private const val NY_FASIT_PATH = "${TEST_OUTPUT_PATH}ny-fasit"
        private const val PDF_RESOURSE_PATH = "pdf"
        private const val SRC_TEST_RESOURCES = "src/test/resources"
        private const val KONSUMENT_PATH = "konsumenter"

        /**
         * Tekst i @page-margbokser (f.eks. sidetall) arver ikke font-family fra body. Uten font-family, eller med en
         * generisk familie som sans-serif, tegnes den med en standardfont i PDFBox (Times-Roman/Helvetica) som ikke
         * bygges inn i pdf-en.
         */
        private const val FONT_IKKE_INNEBYGD = "6.2.11.4.1-1"

        private val KJENTE_PDFA_BRUDD: Map<String, Set<String>> =
            listOf(
                "hm-pdf-generator/notat.html",
                "tilleggsstonader-sak/internt_vedtak_barnetilsyn.html",
                "tilleggsstonader-sak/internt_vedtak_boutgifter.html",
                "tilleggsstonader-sak/internt_vedtak_daglig_reise_tso.html",
                "tilleggsstonader-sak/internt_vedtak_daglig_reise_tsr.html",
                "tilleggsstonader-sak/internt_vedtak_laeremidler.html",
                "tilleggsstonader-sak/internt_vedtak_reise_til_samling_tso.html",
                "tilleggsstonader-sak/internt_vedtak_reise_til_samling_tsr.html",
                "tilleggsstonader-sak/kjoreliste_behandling_brev.html",
                "tilleggsstonader-soknad-api/soknad_barnetilsyn.html",
                "tilleggsstonader-soknad-api/soknad_kjoreliste.html",
                "tilleggsstonader-soknad-api/soknad_laeremidler.html",
                "tilleggsstonader-soknad-api/soknad_reise_til_samling.html",
            ).associate { "$KONSUMENT_PATH/$it" to setOf(FONT_IKKE_INNEBYGD) }

        @JvmStatic
        fun konsumentHtml(): List<String> {
            val pdfRot = Path.of(SRC_TEST_RESOURCES, PDF_RESOURSE_PATH)
            return pdfRot
                .resolve(KONSUMENT_PATH)
                .walk()
                .filter { it.extension == "html" }
                .map { it.relativeTo(pdfRot).invariantSeparatorsPathString }
                .sorted()
                .toList()
        }

        @JvmStatic
        fun svgSomBatikIkkeStøtterAlt(): List<Arguments> =
            listOf(
                Arguments.of(
                    "metadata og attributter fra inkscape",
                    """
                    <svg xmlns="http://www.w3.org/2000/svg" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:cc="http://creativecommons.org/ns#" xmlns:dc="http://purl.org/dc/elements/1.1/"
                         xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
                         xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"
                         width="40" height="40" viewBox="0 0 40 40" inkscape:version="1.3">
                      <metadata><rdf:RDF><cc:Work rdf:about=""><dc:format>image/svg+xml</dc:format></cc:Work></rdf:RDF></metadata>
                      <sodipodi:namedview id="namedview" pagecolor="#ffffff" inkscape:zoom="1" />
                      <g inkscape:label="Lag 1" inkscape:groupmode="layer"><rect width="40" height="40" fill="#c30000" /></g>
                    </svg>
                    """.trimIndent(),
                ),
                Arguments.of(
                    "ukjente elementer i svg uten xmlns",
                    """
                    <svg width="20" height="20">
                      <filter id="f"><feDropShadow dx="1" dy="1" /></filter>
                      <foo />
                      <rect width="20" height="20" fill="#000000" />
                    </svg>
                    """.trimIndent(),
                ),
            )
    }
}
