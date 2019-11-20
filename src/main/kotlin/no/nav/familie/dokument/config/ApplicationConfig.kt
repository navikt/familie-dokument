package no.nav.familie.dokument.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.familie.log.filter.LogFilter
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean

@SpringBootConfiguration
class ApplicationConfig {

    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        val serverFactory = JettyServletWebServerFactory()
        serverFactory.port = 8082
        return serverFactory
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}