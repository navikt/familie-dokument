package no.nav.familie.dokument.pdf

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester
import no.nav.familie.dokument.TestUtil.toByteArray
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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

    /**
     * Genererer pdf fra html som konsumentene av /api/html-til-pdf sender, og sammenligner tittel, tekst og utseende
     * med fasit-pdf-en som ligger ved siden av html-filen. Kildene til html-filene er beskrevet i
     * pdf/konsumenter/README.md.
     *
     * Ny html-fil legges under src/test/resources/pdf/konsumenter/<konsument>/. Fasit-pdf-er som mangler eller avviker
     * skrives på nytt med `mvn test -Dtest=PdfGeneratorTest -DoppdaterPdfFasit=true`.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("konsumentHtml")
    fun `skal generere pdf som er lik fasit for html fra konsument`(html: String) {
        // Arrange
        val fasit = html.removeSuffix(".html") + ".pdf"
        val fasitFil = Paths.get(SRC_TEST_RESOURCES, PDF_RESOURSE_PATH, fasit).toFile()

        // Act
        val pdf = pdfService.lagPdf(getDocument(html))

        // Assert
        if (System.getProperty(OPPDATER_FASIT_PROPERTY).toBoolean() &&
            (
                !fasitFil.exists() ||
                    tekstinnhold(fasitFil.readBytes()) != tekstinnhold(pdf) ||
                    !pdfsAreEqual(fasit, fasitFil.readBytes(), pdf)
            )
        ) {
            fasitFil.writeBytes(pdf)
            println("Oppdaterte fasit $fasitFil")
        }
        assertTrue(isPdf(pdf), "$html ga ikke gyldig pdf")
        assertEquals(KJENTE_PDFA_BRUDD[html].orEmpty(), pdfA2ABrudd(pdf), "$html har endrede PDF/A-2A-brudd")
        val forventet =
            fasitFil.takeIf { it.exists() }?.readBytes()
                ?: error("Mangler fasit $fasit. Kjør med -D$OPPDATER_FASIT_PROPERTY=true for å generere den.")
        assertEquals(tekstinnhold(forventet), tekstinnhold(pdf), "$html har annen tittel eller tekst enn fasit")
        assertTrue(pdfsAreEqual(fasit, forventet, pdf), "$html avviker visuelt fra fasit, se $TEST_OUTPUT_PATH")
    }

    /**
     * Dokumenttittel og uttrekkbar tekst, som ikke fanges av den visuelle sammenligningen.
     */
    private fun tekstinnhold(pdf: ByteArray): String =
        Loader.loadPDF(pdf).use { "${it.documentInformation.title}\n${PDFTextStripper().getText(it)}" }

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
        private const val PDF_RESOURSE_PATH = "pdf"
        private const val SRC_TEST_RESOURCES = "src/test/resources"
        private const val KONSUMENT_PATH = "konsumenter"
        private const val OPPDATER_FASIT_PROPERTY = "oppdaterPdfFasit"

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
    }
}
