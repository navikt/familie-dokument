package no.nav.familie.dokument.config

import org.slf4j.LoggerFactory
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LoggerFilter: Filter {

    private val logger = LoggerFactory.getLogger(LoggerFilter::class.java)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request as HttpServletRequest
        response as HttpServletResponse
        logger.info("[pre-handle]- " + request.method + " " + request.requestURI)
        try {
            chain.doFilter(request, response)
        } finally {
            logger.info("[post-handle] - " + request.method + " " + request.requestURI + " " + response.status)
        }
    }

}
