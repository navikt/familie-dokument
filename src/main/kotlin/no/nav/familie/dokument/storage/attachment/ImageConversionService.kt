package no.nav.familie.dokument.storage.attachment

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.familie.http.client.HttpClientUtil
import no.nav.familie.http.client.HttpRequestUtil
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.eclipse.jetty.http.HttpHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
class ImageConversionService(@Value("\${SOKNAD_PDF_SVG_SUPPORT_GENERATOR_URL}") private val imageToPdfEndpointBaseUrl: URI,
                                      private val contextHolder: TokenValidationContextHolder) {

    private val client: HttpClient = HttpClientUtil.create()

    fun convert(bytes: ByteArray, detectedType: Format): ByteArray {
        try {
            val request = HttpRequestUtil.createRequest(contextHolder.tokenValidationContext.getJwtToken("selvbetjening").tokenAsString)
                    .header(HttpHeader.CONTENT_TYPE.asString(), detectedType.mimeType)
                    .uri(URI.create(imageToPdfEndpointBaseUrl.toString() + "v1/genpdf/image/kontantstotte"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

            if (HttpStatus.Series.SUCCESSFUL != HttpStatus.Series.resolve(response.statusCode())) {
                throw RuntimeException("Response fra pdf-generator: " + response.statusCode() + ". Response.entity: " + String(response.body()))
            }

            log.info("Konvertert bilde({}) til pdf", detectedType.mimeType)

            return response.body()
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feiler under konvertering av innsending til json. " + e.message)
        } catch (e: InterruptedException) {
            throw RuntimeException("Timer ut under innsending. " + e.message)
        } catch (e: IOException) {
            throw RuntimeException("Ukjent IO feil i " + javaClass.name + "." + e.message)
        }

    }

    companion object {

        private val log = LoggerFactory.getLogger(ImageConversionService::class.java)
    }
}
