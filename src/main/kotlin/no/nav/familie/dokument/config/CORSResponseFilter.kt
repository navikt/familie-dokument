package no.nav.familie.dokument.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
@Order(0)
@EnableConfigurationProperties(CorsProperties::class)
class CORSResponseFilter(val corsProperties: CorsProperties) : Filter {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val request = servletRequest as HttpServletRequest
        val response = servletResponse as HttpServletResponse
        if (erCorsOk(request)) {
            setCorsHeaders(response, request)
        }

        if (erOptionRequest(request)) {
            response.status = HttpServletResponse.SC_OK
        } else {
            filterChain.doFilter(servletRequest, servletResponse)
        }
    }

    private fun setCorsHeaders(response: HttpServletResponse, request: HttpServletRequest) {
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))
        response.addHeader("Access-Control-Allow-Headers", "origin, content-type, content-length, accept, authorization")
        response.addHeader("Access-Control-Allow-Credentials", "true")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
    }

    private fun erCorsOk(request: HttpServletRequest): Boolean {
        val headerNames: Enumeration<String> = request.headerNames
        while (headerNames.hasMoreElements()) {
            LoggerFactory.getLogger(this::class.java).info("Header: " + request.getHeader(headerNames.nextElement()))
        }
        val (allowedOrigins) = corsProperties
        return allowedOrigins.contains(request.getHeader("Origin"))
    }

    private fun erOptionRequest(request: HttpServletRequest) = "OPTIONS" == request.method.toUpperCase() && erCorsOk(request)
}
