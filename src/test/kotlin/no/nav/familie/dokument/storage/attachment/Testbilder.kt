package no.nav.familie.dokument.storage.attachment

import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.awt.image.Raster
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

internal object Testbilder {
    const val PNG_RGB = 2
    const val PNG_PALETT = 3
    const val PNG_RGBA = 6

    private val kanalerPerPngFargetype = mapOf(0 to 1, PNG_RGB to 3, PNG_PALETT to 1, 4 to 2, PNG_RGBA to 4)

    // Startkolonne, startrad, kolonnesteg og radsteg for de sju passeringene i Adam7-interlacing
    private val adam7 =
        listOf(
            intArrayOf(0, 0, 8, 8),
            intArrayOf(4, 0, 8, 8),
            intArrayOf(0, 4, 4, 8),
            intArrayOf(2, 0, 4, 4),
            intArrayOf(0, 2, 2, 4),
            intArrayOf(1, 0, 2, 2),
            intArrayOf(0, 1, 1, 2),
        )

    private const val ZLIB_BLOKK = 16 * 1024 * 1024

    fun png(
        bredde: Int,
        høyde: Int,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(bredde, høyde, BufferedImage.TYPE_BYTE_BINARY), "png", out)
        return out.toByteArray()
    }

    /** Lager en PNG der alle piksler er 0. [ekstraChunks] legges rett etter IHDR. */
    fun pngMedNuller(
        bredde: Int,
        høyde: Int,
        bitdybde: Int,
        fargetype: Int,
        interlaced: Boolean = false,
        ekstraChunks: List<Pair<String, ByteArray>> = emptyList(),
    ): ByteArray {
        val ihdr =
            ByteBuffer
                .allocate(13)
                .putInt(bredde)
                .putInt(høyde)
                .put(bitdybde.toByte())
                .put(fargetype.toByte())
                .put(0)
                .put(0)
                .put(if (interlaced) 1 else 0)
                .array()

        // Hver rad starter med en filterbyte. Filter 0 og piksler med verdien 0 gir bare nuller.
        val bitsPerPiksel = bitdybde * kanalerPerPngFargetype.getValue(fargetype)
        val bytesPerRad = { b: Int -> 1 + (b.toLong() * bitsPerPiksel + 7) / 8 }
        val antallBytes =
            if (interlaced) {
                adam7
                    .map { (x, y, dx, dy) -> (bredde - x + dx - 1) / dx to (høyde - y + dy - 1) / dy }
                    .filter { (b, h) -> b > 0 && h > 0 }
                    .sumOf { (b, h) -> h * bytesPerRad(b) }
            } else {
                høyde * bytesPerRad(bredde)
            }

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
        out.write(pngChunk("IHDR", ihdr))
        ekstraChunks.forEach { (type, data) -> out.write(pngChunk(type, data)) }
        if (fargetype == PNG_PALETT) out.write(pngChunk("PLTE", byteArrayOf(0, 0, 0)))
        out.write(pngChunk("IDAT", nullerKomprimert(antallBytes)))
        out.write(pngChunk("IEND", byteArrayOf()))
        return out.toByteArray()
    }

    /** Komprimert tekstfelt (zTXt) eller fargeprofil (iCCP) som blir [antallBytes] stor når den pakkes ut. */
    fun komprimertPngChunk(
        type: String,
        antallBytes: Long,
    ): Pair<String, ByteArray> = type to "navn".toByteArray() + byteArrayOf(0, 0) + nullerKomprimert(antallBytes)

    /**
     * Lager en gyldig zlib-strøm med [antallBytes] nuller uten å komprimere alle nullene. Etter FULL_FLUSH er
     * komprimerte blokker uavhengige av hverandre, så én komprimert blokk på 16 MiB kan gjentas.
     */
    fun nullerKomprimert(antallBytes: Long): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        val nuller = ByteArray(ZLIB_BLOKK)
        val heleBlokker = antallBytes / ZLIB_BLOKK
        val out = ByteArrayOutputStream()
        if (heleBlokker > 0) {
            out.write(deflater.komprimer(nuller, ZLIB_BLOKK, avslutt = false))
            val blokk = deflater.komprimer(nuller, ZLIB_BLOKK, avslutt = false)
            repeat((heleBlokker - 1).toInt()) { out.write(blokk) }
        }
        val slutt = deflater.komprimer(nuller, (antallBytes % ZLIB_BLOKK).toInt(), avslutt = true)
        deflater.end()

        // Deflaterens sjekksum dekker bare nullene den faktisk komprimerte. Adler-32 av n nuller er (n mod 65521) << 16 | 1.
        out.write(slutt, 0, slutt.size - 4)
        out.write(ByteBuffer.allocate(4).putInt((((antallBytes % 65521) shl 16) or 1).toInt()).array())
        return out.toByteArray()
    }

    /** JPEG med 1 (gråtone), 3 (farge) eller 4 (CMYK) fargekanaler, laget med JDK-ens egen JPEG-writer. */
    fun jpeg(
        bredde: Int,
        høyde: Int,
        fargekanaler: Int,
        progressiv: Boolean,
    ): ByteArray {
        val bilde =
            when (fargekanaler) {
                1 -> IIOImage(BufferedImage(bredde, høyde, BufferedImage.TYPE_BYTE_GRAY), null, null)
                3 -> IIOImage(BufferedImage(bredde, høyde, BufferedImage.TYPE_INT_RGB), null, null)
                else -> IIOImage(Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, bredde, høyde, fargekanaler, null), null, null)
            }
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val out = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(out).use { stream ->
            writer.output = stream
            val param = writer.defaultWriteParam
            if (progressiv) param.progressiveMode = ImageWriteParam.MODE_DEFAULT
            writer.write(null, bilde, param)
            writer.dispose()
        }
        return out.toByteArray()
    }

    /** Progressiv JPEG med header, men uten bildedata, slik at store dimensjoner ikke koster noe i testene. */
    fun jpegUtenBildedata(
        bredde: Int,
        høyde: Int,
        fargekanaler: Int,
    ): ByteArray {
        val komponenter = (1..fargekanaler).flatMap { listOf(it.toByte(), 0x11.toByte(), 0.toByte()) }
        val sof = byteArrayOf(8) + u16(høyde) + u16(bredde) + byteArrayOf(fargekanaler.toByte()) + komponenter
        val sos = byteArrayOf(fargekanaler.toByte()) + (1..fargekanaler).flatMap { listOf(it.toByte(), 0.toByte()) } + byteArrayOf(0, 0, 0)
        return marker(0xD8) +
            marker(0xC2) + u16(2 + sof.size) + sof +
            marker(0xDA) + u16(2 + sos.size) + sos +
            marker(0xD9)
    }

    /** Komprimerer de første [lengde] bytene av [input], enten som en uavhengig blokk (FULL_FLUSH) eller som slutten av strømmen. */
    private fun Deflater.komprimer(
        input: ByteArray,
        lengde: Int,
        avslutt: Boolean,
    ): ByteArray {
        setInput(input, 0, lengde)
        if (avslutt) finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        do {
            val antall = deflate(buffer, 0, buffer.size, if (avslutt) Deflater.NO_FLUSH else Deflater.FULL_FLUSH)
            out.write(buffer, 0, antall)
        } while (if (avslutt) !finished() else antall == buffer.size)
        return out.toByteArray()
    }

    private fun pngChunk(
        type: String,
        data: ByteArray,
    ): ByteArray {
        val typeOgData = type.toByteArray() + data
        val crc = CRC32().apply { update(typeOgData) }.value.toInt()
        return ByteBuffer.allocate(4).putInt(data.size).array() + typeOgData + ByteBuffer.allocate(4).putInt(crc).array()
    }

    private fun marker(kode: Int) = byteArrayOf(0xFF.toByte(), kode.toByte())

    private fun u16(verdi: Int) = byteArrayOf((verdi shr 8).toByte(), verdi.toByte())
}
