package no.nav.familie.dokument.pdf

import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester
import org.apache.commons.io.IOUtils
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.imageio.ImageIO

class PdfGenerartorTest {

    private val pdfService = PdfService(testContentRoot)

    @Test
    fun testPdfGeneration() {
        val html = getDocument("eksempel1.html")
        val pdf = pdfService.lagPdf(html)
        Assert.assertTrue(isPdf(pdf))
        Assert.assertTrue(pdfsAreEqual("eksempel1.pdf", pdf))
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

    @Throws(IOException::class)
    private fun pdfsAreEqual(resource: String, actualPdfBytes: ByteArray): Boolean {
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH))

        // Load expected PDF document from resources, change class below.
        var expectedPdfBytes: ByteArray
        PdfGenerartorTest::class.java.getResourceAsStream("$PDF_RESOURSE_PATH/$resource")
            .use { expectedIs -> expectedPdfBytes = IOUtils.toByteArray(expectedIs) }
        // Get a list of results.
        val problems = PdfVisualTester.comparePdfDocuments(
            expectedPdfBytes, actualPdfBytes, resource, false
        )

        // Get a list of results.
        if (!problems.isEmpty()) {
            System.err.println("Found problems with test case ($resource):")
            System.err.println(problems.stream().map { p: PdfVisualTester.PdfCompareResult -> p.logMessage }
                                   .collect(Collectors.joining("\n    ", "[\n    ", "\n]")))
            System.err.println("For test case ($resource) writing failure artifacts to '$TEST_OUTPUT_PATH'")
            val outPdf = File(TEST_OUTPUT_PATH, "$resource---actual.pdf")
            Files.write(outPdf.toPath(), actualPdfBytes)
        }
        for (result in problems) {
            if (result.testImages != null) {
                var output = File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---diff.png")
                ImageIO.write(result.testImages.createDiff(), "png", output)
                output = File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---actual.png")
                ImageIO.write(result.testImages.actual, "png", output)
                output = File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---expected.png")
                ImageIO.write(result.testImages.expected, "png", output)
            }
        }
        return problems.isEmpty()

    }

    companion object {

        private const val TEST_OUTPUT_PATH = "target/regression-tests/"
        private const val PDF_RESOURSE_PATH = "pdf"
        val testContentRoot = Paths.get(
            this::
            class.java.protectionDomain.codeSource.location.toURI()
        ).toAbsolutePath()
    }
}