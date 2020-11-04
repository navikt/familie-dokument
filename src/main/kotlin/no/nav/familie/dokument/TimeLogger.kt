package no.nav.familie.dokument

import org.slf4j.LoggerFactory

object TimeLogger {

    private val logger = LoggerFactory.getLogger(TimeLogger::class.java)
    fun <T> log(fn: () -> T, msg: String): T {
        val start = System.currentTimeMillis()
        val response = fn.invoke()
        logger.info("$msg - tok ${System.currentTimeMillis() - start}ms")
        return response
    }
}