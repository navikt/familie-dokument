package no.nav.familie.dokument.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PDFontSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.apache.batik.anim.dom.SVGDOMImplementation
import org.apache.fontbox.ttf.TTFParser
import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDMetadata
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences
import org.apache.xmpbox.XMPMetadata
import org.apache.xmpbox.type.BadFieldValueException
import org.apache.xmpbox.xml.XmpSerializer
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.jsoup.parser.Parser
import org.jsoup.parser.Tag
import org.jsoup.parser.TagSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import org.w3c.dom.DOMException
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Calendar

@Service
class PdfService {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagPdf(html: String): ByteArray {
        log.debug("Generer pdf")

        val pdfOutputStream = ByteArrayOutputStream()
        val dokument = Jsoup.parse(html, "", htmlParser()).also { fjernSvgElementerSomBatikIkkeStøtter(it) }
        val w3cDokument = W3CDom().fromJsoup(dokument)
        genererPdf(w3cDokument, pdfOutputStream)

        return pdfOutputStream.toByteArray()
    }

    /**
     * Konsumentene sender xhtml, der `<div/>` er et tomt element. Vanlig html ignorerer `/>`, og da omslutter `<div>`
     * alt som kommer etter, så layouten blir feil. Derfor kan alle tagger selvlukkes.
     * Script i svg leses som vanlig markup. Ellers blir alt etter `<svg><script/>` lest som script og mangler i pdf-en.
     */
    private fun htmlParser(): Parser =
        Parser.htmlParser().tagSet(
            TagSet.Html().onNewTag {
                it.set(Tag.SelfClose)
                if (it.namespace() == Parser.NamespaceSvg && it.normalName() == "script") it.clear(Tag.Data)
            },
        )

    /**
     * Batik feiler på svg-elementer det ikke kjenner, f.eks. metadata fra Inkscape, og da blir hele svg-en tom.
     * Elementene tegnes ikke uansett, så vi fjerner dem for at resten av svg-en skal tegnes.
     */
    private fun fjernSvgElementerSomBatikIkkeStøtter(dokument: org.jsoup.nodes.Document) {
        val batikDokument =
            SVGDOMImplementation.getDOMImplementation().createDocument(SVG_NAVNEROM, "svg", null)
        dokument
            .select("svg *")
            .filter { it.tag().namespace() == SVG_NAVNEROM }
            .filterNot { batikKanOpprette(batikDokument, it.tagName()) }
            .forEach { it.remove() }
    }

    private fun batikKanOpprette(
        batikDokument: Document,
        tagName: String,
    ): Boolean =
        try {
            batikDokument.createElementNS(SVG_NAVNEROM, tagName)
            true
        } catch (_: DOMException) {
            false
        }

    fun fontSupplier(fontName: String): PDFontSupplier {
        val font =
            TTFParser().parse(RandomAccessReadBuffer(ClassPathResource("/fonts/$fontName").inputStream)).also {
                it.isEnableGsub =
                    false
            }
        return PDFontSupplier(
            PDType0Font.load(
                PDDocument(),
                font,
                true,
            ),
        )
    }

    fun genererPdf(
        w3cDokument: Document,
        outputStream: ByteArrayOutputStream,
    ) {
        try {
            val builder =
                PdfRendererBuilder()
                    .useFastMode()
                    .useFont(
                        fontSupplier("SourceSansPro-Regular.ttf"),
                        "Source Sans Pro",
                        400,
                        BaseRendererBuilder.FontStyle.NORMAL,
                        true,
                    ).useFont(
                        fontSupplier("SourceSansPro-Bold.ttf"),
                        "Source Sans Pro",
                        700,
                        BaseRendererBuilder.FontStyle.OBLIQUE,
                        true,
                    ).useFont(
                        fontSupplier("SourceSansPro-It.ttf"),
                        "Source Sans Pro",
                        400,
                        BaseRendererBuilder.FontStyle.ITALIC,
                        true,
                    ).useSVGDrawer(FeiltolerantSvgDrawer())
                    .withW3cDocument(w3cDokument, "")
                    .buildPdfRenderer()
            builder.createPDFWithoutClosing()
            builder.pdfDocument.conform()
            builder.pdfDocument.save(outputStream)
            builder.pdfDocument.close()
        } catch (e: IOException) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
    }

    fun PDDocument.conform() {
        val xmp = XMPMetadata.createXMPMetadata()
        val catalog = this.documentCatalog
        val cal = Calendar.getInstance()
        val page = PDPage(PDRectangle.A4)

        try {
            val dc = xmp.createAndAddDublinCoreSchema()
            dc.addCreator("navikt/familie-dokument")
            dc.addDate(cal)

            val id = xmp.createAndAddPDFAIdentificationSchema()
            id.part = 2
            id.conformance = "A"

            val serializer = XmpSerializer()
            val baos = ByteArrayOutputStream()
            serializer.serialize(xmp, baos, true)

            val metadata = PDMetadata(this)
            metadata.importXMPMetadata(baos.toByteArray())
            catalog.metadata = metadata
        } catch (e: BadFieldValueException) {
            throw IllegalArgumentException(e)
        }

        val intent = PDOutputIntent(this, colorProfile.inputStream())
        intent.info = "sRGB IEC61966-2.1"
        intent.outputCondition = "sRGB IEC61966-2.1"
        intent.outputConditionIdentifier = "sRGB IEC61966-2.1"
        intent.registryName = "http://www.color.org"
        catalog.addOutputIntent(intent)
        catalog.language = "nb-NO"

        val pdViewer = PDViewerPreferences(page.cosObject)
        pdViewer.setDisplayDocTitle(true)
        catalog.viewerPreferences = pdViewer

        catalog.markInfo = PDMarkInfo(page.cosObject)
        catalog.structureTreeRoot = PDStructureTreeRoot()
        catalog.markInfo.isMarked = true
    }

    fun mergeDokumenter(dokumenter: List<ByteArray>): ByteArray {
        val pdfMerger = PDFMergerUtility()
        dokumenter.forEach { pdfMerger.addSource(RandomAccessReadBuffer(it)) }
        val output = ByteArrayOutputStream()
        pdfMerger.destinationStream = output
        pdfMerger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache())
        return output.toByteArray()
    }

    companion object {
        private const val SVG_NAVNEROM = SVGDOMImplementation.SVG_NAMESPACE_URI

        val colorProfile: ByteArray
            get() {
                val cpr = ClassPathResource("sRGB.icc")
                return FileCopyUtils.copyToByteArray(cpr.inputStream)
            }
    }
}
