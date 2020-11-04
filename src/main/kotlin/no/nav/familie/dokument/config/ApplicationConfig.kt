package no.nav.familie.dokument.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.filter.LogFilter
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

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
    fun loggerFilter(): FilterRegistrationBean<LoggerFilter> {
        val filterRegistration = FilterRegistrationBean<LoggerFilter>()
        filterRegistration.filter = LoggerFilter()
        filterRegistration.order = 2
        return filterRegistration
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return objectMapper;
    }
}