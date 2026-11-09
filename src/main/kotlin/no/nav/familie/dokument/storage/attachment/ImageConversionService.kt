package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.BadRequestCode
import no.nav.familie.dokument.InvalidDocumentFormat
import no.nav.familie.dokument.InvalidImageDimensions
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.util.Matrix
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

// Delvis kopi av https://github.com/navikt/pdfgen/blob/master/src/main/kotlin/no/nav/pdfgen/Utils.kt
@Service
class ImageConversionService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class ImageSize(
        val width: Float,
        val height: Float,
    )

    fun convert(
        input: ByteArray,
        detectedType: Format,
    ): ByteArray =
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val image = lesBilde(input)

            val portraitImage = toPortrait(image, detectedType)

            logger.info("Konverterer detectedType=$detectedType imageType=${image.type} portraitImageType=${portraitImage.type}")
            val pdImage = createPdImage(document, portraitImage, detectedType)
            val imageSize = scale(pdImage, page)

            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false).use {
                it.drawImage(pdImage, Matrix(imageSize.width, 0f, 0f, imageSize.height, 0f, 0f))
            }
            val byteArrayOutputStream = ByteArrayOutputStream()
            document.save(byteArrayOutputStream)
            val outputBytes = byteArrayOutputStream.toByteArray()
            logger.info(
                "Input format=${detectedType.name} h=${image.height} w=${image.width} " +
                    "Portrait h=${portraitImage.height} w=${portraitImage.width} " +
                    "ImageSize height=${imageSize.height} width=${imageSize.width} " +
                    " lowHeight=${imageSize.height < page.cropBox.height} lowWidth=${imageSize.width < page.cropBox.width}" +
                    " inputSize=${input.size / 1024} diffSize=${outputBytes.size / input.size} ",
            )
            outputBytes
        }

    internal fun lesBilde(input: ByteArray): BufferedImage =
        ImageIO.createImageInputStream(ByteArrayInputStream(input)).use { stream ->
            val reader = finnBildeleser(stream)
            try {
                reader.setInput(stream, true, true)
                val bredde = reader.getWidth(0)
                val høyde = reader.getHeight(0)
                validerDimensjoner(bredde, høyde)
                reader.read(0, leseparametre(reader, bredde, høyde))
            } finally {
                reader.dispose()
            }
        }

    private fun finnBildeleser(stream: ImageInputStream?): ImageReader =
        stream
            ?.let { ImageIO.getImageReaders(it) }
            ?.takeIf { it.hasNext() }
            ?.next()
            ?: throw InvalidDocumentFormat("Fant ingen leser for bildet")

    private fun validerDimensjoner(
        bredde: Int,
        høyde: Int,
    ) {
        if (bredde.toLong() * høyde > MAKS_ANTALL_PIKSLER) {
            logger.warn("Avviser bilde på ${bredde}x$høyde piksler. Maks antall piksler er $MAKS_ANTALL_PIKSLER")
            throw InvalidImageDimensions(BadRequestCode.IMAGE_DIMENSIONS_TOO_LARGE)
        }
        if (høyde < 400 && bredde < 400) throw InvalidImageDimensions(BadRequestCode.IMAGE_DIMENSIONS_TOO_SMALL)
    }

    private fun leseparametre(
        reader: ImageReader,
        bredde: Int,
        høyde: Int,
    ): ImageReadParam {
        val param = reader.defaultReadParam
        val subsampling = subsamplingFaktor(bredde.toLong() * høyde)
        if (subsampling > 1) {
            logger.info("Leser bilde på ${bredde}x$høyde piksler med subsampling $subsampling")
            param.setSourceSubsampling(subsampling, subsampling, 0, 0)
        }
        return param
    }

    private fun createPdImage(
        document: PDDocument,
        portraitImage: BufferedImage,
        detectedType: Format,
    ): PDImageXObject {
        val quality = 1.0f
        return try {
            JPEGFactory.createFromImage(document, portraitImage, quality)
        } catch (e: Exception) {
            if (detectedType == Format.PNG) {
                logger.info("Feilet konvertering av jpegbilde, prøver med lossless", e)
                LosslessFactory.createFromImage(document, portraitImage)
            } else {
                throw e
            }
        }
    }

    private fun toPortrait(
        image: BufferedImage,
        detectedType: Format,
    ): BufferedImage {
        if (image.height >= image.width) {
            return image
        }
        val width: Int = image.width
        val height: Int = image.height

        val dest = BufferedImage(height, width, getType(image, detectedType))

        val graphics2D = dest.createGraphics()
        graphics2D.translate((height - width) / 2, (height - width) / 2)
        graphics2D.rotate(Math.PI / 2, height / 2.toDouble(), width / 2.toDouble())
        graphics2D.drawRenderedImage(image, null)
        return dest
    }

    /**
     * En feil i ubuntu slik att hvis det er PNG og type 0 så skal den bli 5
     */
    private fun getType(
        image: BufferedImage,
        detectedType: Format,
    ): Int =
        if (image.type == BufferedImage.TYPE_CUSTOM && detectedType == Format.PNG) {
            BufferedImage.TYPE_3BYTE_BGR
        } else {
            image.type
        }

    private fun scale(
        image: PDImageXObject,
        page: PDPage,
    ): ImageSize {
        var width = image.width.toFloat()
        var height = image.height.toFloat()

        if (width > page.cropBox.width) {
            width = page.cropBox.width
            height = width * image.height.toFloat() / image.width.toFloat()
        }

        if (height > page.cropBox.height) {
            height = page.cropBox.height
            width = height * image.width.toFloat() / image.height.toFloat()
        }

        return ImageSize(width, height)
    }

    companion object {
        const val MAKS_ANTALL_PIKSLER = 250_000_000L
        private const val A4_300_DPI_PIKSLER = 2480L * 3508L

        internal fun subsamplingFaktor(antallPiksler: Long): Int =
            max(1, floor(sqrt(antallPiksler.toDouble() / A4_300_DPI_PIKSLER)).toInt())
    }
}
