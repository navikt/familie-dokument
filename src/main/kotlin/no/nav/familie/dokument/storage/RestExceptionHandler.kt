package no.nav.familie.dokument.storage

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import java.time.LocalDateTime
import java.util.LinkedHashMap

@ControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    protected fun handleIllegalArgumentException(ex: IllegalArgumentException, req: ServletWebRequest): Any {
        return ResponseEntity
                .badRequest()
                .body<Map<String, Any?>>(lagErrorBody(HttpStatus.BAD_REQUEST, ex, req))
    }

    @ExceptionHandler(RuntimeException::class)
    protected fun handleRuntimeException(ex: RuntimeException, req: ServletWebRequest): Any {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body<Map<String, Any?>>(lagErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex, req))
    }

    @ExceptionHandler(NotImplementedError::class)
    protected fun handleNotImplementedError(ex: NotImplementedError, req: ServletWebRequest): Any {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body<Map<String, Any?>>(lagErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex, req))
    }

    private fun lagErrorBody(
            status: HttpStatus,
            ex: Throwable,
            req: ServletWebRequest
    ): MutableMap<String, Any?> {
        val body: MutableMap<String, Any?> = LinkedHashMap()
        body["timestamp"] = LocalDateTime.now().toString()
        body["status"] = status.value()
        body["error"] = status.reasonPhrase
        body["type"] = ex.javaClass.simpleName
        body["path"] = req.request.requestURI
        body["message"] = ex.message
        LOG.error("En feil har oppstått ${ex.message}")
        secureLogger.error("En feil har oppstått $body", ex)
        return body
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val LOG = LoggerFactory.getLogger(RestExceptionHandler::class.java)
    }
}