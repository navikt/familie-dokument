package no.nav.familie.dokument.pdf

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester
import no.nav.familie.dokument.TestUtil.toByteArray
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.flavours.PDFAFlavour
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.imageio.ImageIO

class PdfGenerartorTest {

    private val pdfService = PdfService()

    @Test
    fun testPdfGeneration() {
        val html = getDocument("eksempel1.html")
        val pdf = pdfService.lagPdf(html)
        assertTrue(isPdf(pdf))
        assertTrue(isPDF_2_ACompliant(pdf))
        assertTrue(pdfsAreEqual("eksempel1.pdf", pdf))
    }

    private fun getDocument(fixtureName: String): String {
        return IOUtils.toString(
                this.javaClass.getResourceAsStream("/$PDF_RESOURSE_PATH/$fixtureName"),
                StandardCharsets.UTF_8
        )
    }

    /**
     * Test if the data in the given byte array represents a PDF file.
     */
    fun isPdf(data: ByteArray): Boolean {
        if (data.size > 4 && data[0] == 0x25.toByte() && // %
            data[1] == 0x50.toByte() && // P
            data[2] == 0x44.toByte() && // D
            data[3] == 0x46.toByte() && // F
            data[4] == 0x2D.toByte()
        ) { // -

            // version 1.3 file terminator
            if (data[5] == 0x31.toByte() && data[6] == 0x2E.toByte() &&
                data[7] == 0x33.toByte() && data[data.size - 7] == 0x25.toByte() && // %
                data[data.size - 6] == 0x25.toByte() && // %
                data[data.size - 5] == 0x45.toByte() && // E
                data[data.size - 4] == 0x4F.toByte() && // O
                data[data.size - 3] == 0x46.toByte() && // F
                data[data.size - 2] == 0x20.toByte() && // SPACE
                data[data.size - 1] == 0x0A.toByte()
            ) { // EOL
                return true
            }

            // version 1.3 file terminator
            if (data[5] == 0x31.toByte() && data[6] == 0x2E.toByte() &&
                data[7] == 0x34.toByte() && data[data.size - 6] == 0x25.toByte() && // %
                data[data.size - 5] == 0x25.toByte() && // %
                data[data.size - 4] == 0x45.toByte() && // E
                data[data.size - 3] == 0x4F.toByte() && // O
                data[data.size - 2] == 0x46.toByte() && // F
                data[data.size - 1] == 0x0A.toByte()
            ) { // EOL
                return true
            }
        }
        return false
    }

    private fun pdfsAreEqual(resource: String, actualPdfBytes: ByteArray): Boolean {
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH))

        // Load expected PDF document from resources, change class below.
        val expectedPdfBytes = toByteArray("$PDF_RESOURSE_PATH/$resource")

        // Get a list of results.
        val problems = PdfVisualTester.comparePdfDocuments(
                expectedPdfBytes, actualPdfBytes, resource, false
        )

        // Get a list of results.
        if (problems.isNotEmpty()) {
            System.err.println("Found problems with test case ($resource):")
            System.err.println(problems.stream().map { p: PdfVisualTester.PdfCompareResult -> p.logMessage }
                                       .collect(Collectors.joining("\n    ", "[\n    ", "\n]")))
            System.err.println("For test case ($resource) writing failure artifacts to '$TEST_OUTPUT_PATH'")
            File(TEST_OUTPUT_PATH, "$resource---actual.pdf").writeBytes(actualPdfBytes)
        }
        for (result in problems) {
            if (result.testImages != null) {
                var output = File(TEST_OUTPUT_PATH, "$resource---${result.pageNumber}---diff.png")
                ImageIO.write(result.testImages.createDiff(), "png", output)
                output = File(TEST_OUTPUT_PATH, "$resource---${result.pageNumber}---actual.png")
                ImageIO.write(result.testImages.actual, "png", output)
                output = File(TEST_OUTPUT_PATH, "$resource---${result.pageNumber}---expected.png")
                ImageIO.write(result.testImages.expected, "png", output)
            }
        }
        return problems.isEmpty()

    }

    private fun isPDF_2_ACompliant(pdf: ByteArray): Boolean {
        VeraGreenfieldFoundryProvider.initialise();
        val pdfaFlavour = PDFAFlavour.PDFA_2_U
        val validator = Foundries.defaultInstance().createValidator(pdfaFlavour, false)
        val parser = Foundries.defaultInstance().createParser(ByteArrayInputStream(pdf))
        return validator.validate(parser).isCompliant
    }

    companion object {

        private const val TEST_OUTPUT_PATH = "target/regression-tests/"
        private const val PDF_RESOURSE_PATH = "pdf"
    }
}