package no.nav.familie.dokument

object TestUtil {

    fun toByteArray(filename: String): ByteArray {
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        return javaClass.classLoader.getResource(filename).readBytes()
    }
}
