package no.nav.familie.dokument.storage.attachment

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.util.Matrix
import org.springframework.stereotype.Service
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

// Kopi av https://github.com/navikt/pdfgen/blob/master/src/main/kotlin/no/nav/pdfgen/Utils.kt
@Service
class ImageConversionService {

    private data class ImageSize(val width: Float, val height: Float)

    fun convert(input: ByteArray): ByteArray {
        return PDDocument().use { document ->
            val imageStream = ByteArrayInputStream(input)
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val image = toPortait(ImageIO.read(imageStream))

            val quality = 1.0f

            val pdImage = JPEGFactory.createFromImage(document, image, quality)
            val imageSize = scale(pdImage, page)

            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false).use {
                it.drawImage(pdImage, Matrix(imageSize.width, 0f, 0f, imageSize.height, 0f, 0f))
            }
            val byteArrayOutputStream = ByteArrayOutputStream()
            document.save(byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        }
    }

    private fun toPortait(image: BufferedImage): BufferedImage {
        if (image.height >= image.width) {
            return image
        }

        val rotateTransform = AffineTransform.getRotateInstance(Math.toRadians(90.0),
                                                                (image.height / 2f).toDouble(),
                                                                (image.height / 2f).toDouble())

        return AffineTransformOp(rotateTransform, AffineTransformOp.TYPE_BILINEAR)
                .filter(image, BufferedImage(image.height, image.width, image.type))
    }

    private fun scale(image: PDImageXObject, page: PDPage): ImageSize {
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
}
