package no.nav.familie.dokument.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.openhtmltopdf.slf4j.Slf4jLogger
import com.openhtmltopdf.util.XRLog
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.filter.RequestTimeFilter
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.client.RestOperations
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@Import(ConsumerIdClientInterceptor::class)
@EnableRetry
class ApplicationConfig {
    init {
        XRLog.setLoggerImpl(Slf4jLogger())
    }

    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        val serverFactory = JettyServletWebServerFactory()
        serverFactory.port = 8082
        return serverFactory
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter(NavSystemtype.NAV_INTEGRASJON)
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun requestTimeFilter(): FilterRegistrationBean<RequestTimeFilter> {
        val filterRegistration = FilterRegistrationBean<RequestTimeFilter>()
        filterRegistration.filter = RequestTimeFilter()
        filterRegistration.order = 2
        return filterRegistration
    }

    @Bean
    fun restOperations(consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestOperations =
        RestTemplateBuilder()
            .connectTimeout(Duration.of(3, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(2, ChronoUnit.MINUTES))
            .additionalInterceptors(consumerIdClientInterceptor)
            .build()

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return objectMapper
    }
}
