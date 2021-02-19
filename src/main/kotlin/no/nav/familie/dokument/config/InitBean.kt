package no.nav.familie.dokument.config

import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class InitBean : InitializingBean {

    override fun afterPropertiesSet() {
        logger.info("Initierer Tika and BufferedImage::graphics")
        Tika()
        BufferedImage(20, 20, BufferedImage.TYPE_BYTE_BINARY).graphics
    }

    private val logger = LoggerFactory.getLogger(this::class.java)
}