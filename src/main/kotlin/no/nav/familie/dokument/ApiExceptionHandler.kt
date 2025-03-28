package no.nav.familie.dokument

import no.nav.familie.dokument.storage.google.GcpRateLimitException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.concurrent.TimeoutException

@Suppress("unused")
@ControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private fun rootCause(throwable: Throwable): String {
        return NestedExceptionUtils.getMostSpecificCause(throwable).javaClass.simpleName
    }

    fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        status: HttpStatus,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (ex is HttpRequestMethodNotSupportedException ||
            ex is HttpMediaTypeNotSupportedException ||
            ex is HttpMediaTypeNotAcceptableException ||
            ex is GcpRateLimitException
        ) {
            secureLogger.warn("En feil har oppstått", ex)
            logger.warn("En feil har oppstått - throwable=${rootCause(ex)} status=${status.value()}")
        } else {
            secureLogger.error("En feil har oppstått", ex)
            logger.error("En feil har oppstått - throwable=${rootCause(ex)} status=${status.value()}")
        }
        return super.handleExceptionInternal(ex, body, headers, status, request)
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<String>> {
        val responseStatus = throwable::class.annotations.find { it is ResponseStatus }?.let { it as ResponseStatus }
        if (responseStatus != null) {
            return håndtertResponseStatusFeil(throwable, responseStatus)
        }
        return uventetFeil(throwable)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleThrowable(ex: BadRequestException): ResponseEntity<Ressurs<String>> {
        logger.warn("Bad request - ${ex.javaClass.simpleName}-${ex.code}")
        ex.secureLogMessage?.let {
            secureLogger.warn("Bad request - ${ex.javaClass.simpleName}-${ex.code} - msg=${ex.secureLogMessage}")
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Ressurs.failure(ex.message))
    }

    private fun uventetFeil(throwable: Throwable): ResponseEntity<Ressurs<String>> {
        val rootCause = NestedExceptionUtils.getMostSpecificCause(throwable)
        if (rootCause is TimeoutException) {
            secureLogger.warn("En feil har oppstått", throwable)
            logger.warn("En feil har oppstått - throwable=${rootCause.javaClass.simpleName} ")
        } else {
            secureLogger.error("En feil har oppstått", throwable)
            logger.error("En feil har oppstått - throwable=${rootCause.javaClass.simpleName} ")
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure("Uventet feil"))
    }

    // Denne håndterer eks JwtTokenUnauthorizedException
    private fun håndtertResponseStatusFeil(
        throwable: Throwable,
        responseStatus: ResponseStatus,
    ): ResponseEntity<Ressurs<String>> {
        val status = if (responseStatus.value != HttpStatus.INTERNAL_SERVER_ERROR) responseStatus.value else responseStatus.code
        val loggMelding = "En håndtert feil har oppstått" +
            " throwable=${rootCause(throwable)}" +
            " reason=${responseStatus.reason}" +
            " status=$status"

        loggFeil(throwable, loggMelding)
        return ResponseEntity.status(status).body(Ressurs.failure("Håndtert feil"))
    }

    private fun loggFeil(throwable: Throwable, loggMelding: String) {
        when (throwable) {
            is JwtTokenUnauthorizedException -> logger.debug(loggMelding)
            is GcpDocumentNotFound -> logger.warn(loggMelding)
            else -> logger.error(loggMelding)
        }
    }
}
