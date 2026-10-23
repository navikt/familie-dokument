package no.nav.familie.dokument.pdf

import com.openhtmltopdf.css.style.CssContext
import com.openhtmltopdf.extend.OutputDevice
import com.openhtmltopdf.extend.SVGDrawer
import com.openhtmltopdf.extend.UserAgentCallback
import com.openhtmltopdf.render.Box
import com.openhtmltopdf.render.RenderingContext
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import org.slf4j.LoggerFactory
import org.w3c.dom.Element

/**
 * Tegner svg med Batik, men lar en svg som feiler bli tom. Uten dette feiler hele pdf-en på grunn av én svg.
 * Logger bare feiltypen, fordi feilmeldingen kan inneholde personopplysninger fra dokumentet.
 */
class FeiltolerantSvgDrawer(
    private val svgDrawer: SVGDrawer = BatikSVGDrawer(),
) : SVGDrawer by svgDrawer {
    override fun withUserAgent(userAgentCallback: UserAgentCallback) = svgDrawer.withUserAgent(userAgentCallback)

    override fun buildSVGImage(
        svgElement: Element,
        box: Box,
        cssContext: CssContext,
        cssWidth: Double,
        cssHeight: Double,
        dotsPerPixel: Double,
    ): SVGDrawer.SVGImage =
        try {
            FeiltolerantSvgImage(svgDrawer.buildSVGImage(svgElement, box, cssContext, cssWidth, cssHeight, dotsPerPixel))
        } catch (e: RuntimeException) {
            loggFeil(e)
            TomSvgImage
        }

    private class FeiltolerantSvgImage(
        private val svgImage: SVGDrawer.SVGImage,
    ) : SVGDrawer.SVGImage by svgImage {
        override fun drawSVG(
            outputDevice: OutputDevice,
            renderingContext: RenderingContext,
            x: Double,
            y: Double,
        ) {
            try {
                svgImage.drawSVG(outputDevice, renderingContext, x, y)
            } catch (e: RuntimeException) {
                loggFeil(e)
            }
        }
    }

    private object TomSvgImage : SVGDrawer.SVGImage {
        override fun getIntrinsicWidth() = 0

        override fun getIntrinsicHeight() = 0

        override fun drawSVG(
            outputDevice: OutputDevice,
            renderingContext: RenderingContext,
            x: Double,
            y: Double,
        ) = Unit
    }

    companion object {
        private val log = LoggerFactory.getLogger(FeiltolerantSvgDrawer::class.java)

        private fun loggFeil(e: RuntimeException) = log.warn("Klarte ikke å tegne svg, den blir stående tom: ${e.javaClass.name}")
    }
}
