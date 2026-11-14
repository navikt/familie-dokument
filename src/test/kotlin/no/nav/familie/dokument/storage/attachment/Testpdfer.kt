package no.nav.familie.dokument.storage.attachment

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.DeflaterOutputStream

internal object Testpdfer {
    const val KATALOG = "<< /Type /Catalog /Pages 3 0 R >>"
    const val SIDER = "<< /Type /Pages /Kids [] /Count 0 >>"

    /** Objektene får nummer fortløpende etter objektstrømmene. Strømmen fylles med mellomrom opp til [utpakketStørrelse]. */
    class Objektstrøm(
        val objekter: List<String>,
        val utpakketStørrelse: Int = 0,
        val filter: String? = "/FlateDecode",
        val kod: (ByteArray) -> ByteArray = ::deflate,
        val lengde: Long? = null,
    )

    /**
     * Xref-strømmen fylles med nuller opp til [utpakketStørrelse]. Uten xref må PDFBox lete seg fram til objektene.
     * [objekterSomMangler] er objekter som xref sier ligger i den første objektstrømmen, men som ikke finnes der.
     */
    class Xref(
        val utpakketStørrelse: Int = 0,
        val dekodeparametre: String = "",
        val radbredder: String = "[1 4 2]",
        val finnes: Boolean = true,
        val objekterSomMangler: Int = 0,
    )

    /** Lager en PDF der alle objekter ligger i objektstrømmer. Det første objektet i den første strømmen er katalogen. */
    fun pdf(
        objektstrømmer: List<Objektstrøm>,
        xref: Xref = Xref(),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val xrefRader = mutableListOf(xrefRad(0, 0, 65535))
        val pakkedeObjekter = mutableListOf<ByteArray>()
        var nesteObjektnummer = objektstrømmer.size + 1
        out.write("%PDF-1.5\n")

        objektstrømmer.forEachIndexed { indeks, objektstrøm ->
            val strømnummer = indeks + 1
            val innhold = objektstrøm.objekter.joinToString("\n")
            var posisjon = 0
            val header =
                objektstrøm.objekter.joinToString("") { objekt ->
                    "${nesteObjektnummer++} $posisjon ".also { posisjon += objekt.length + 1 }
                }
            val utpakket = (header + innhold).toByteArray(Charsets.ISO_8859_1)
            val fylt = utpakket.copyOf(maxOf(utpakket.size, objektstrøm.utpakketStørrelse))
            fylt.fill(' '.code.toByte(), utpakket.size)

            xrefRader += xrefRad(1, out.size(), 0)
            objektstrøm.objekter.indices.forEach { pakkedeObjekter += xrefRad(2, strømnummer, it) }
            val innholdKodet = objektstrøm.kod(fylt)
            out.skrivStrøm(
                strømnummer,
                "/Type /ObjStm /N ${objektstrøm.objekter.size} /First ${header.length}" +
                    objektstrøm.filter?.let { " /Filter $it" }.orEmpty(),
                innholdKodet,
                objektstrøm.lengde ?: innholdKodet.size.toLong(),
            )
        }

        if (xref.finnes) {
            val xrefNummer = nesteObjektnummer + xref.objekterSomMangler
            val xrefPosisjon = out.size()
            xrefRader += pakkedeObjekter
            repeat(xref.objekterSomMangler) { xrefRader += xrefRad(2, 1, pakkedeObjekter.size + it) }
            xrefRader += xrefRad(1, xrefPosisjon, 0)
            val rader = xrefRader.reduce(ByteArray::plus)
            out.skrivStrøm(
                xrefNummer,
                "/Type /XRef /Size ${xrefNummer + 1} /W ${xref.radbredder} /Root ${objektstrømmer.size + 1} 0 R " +
                    "/Filter /FlateDecode ${xref.dekodeparametre}",
                deflate(rader.copyOf(maxOf(rader.size, xref.utpakketStørrelse))),
            )
            out.write("startxref\n$xrefPosisjon\n")
        }
        out.write("%%EOF\n")
        return out.toByteArray()
    }

    fun deflate(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    fun asciiHex(bytes: ByteArray): ByteArray {
        val siffer = "0123456789abcdef"
        val hex = ByteArray(bytes.size * 2 + 1)
        bytes.forEachIndexed { indeks, byte ->
            hex[2 * indeks] = siffer[(byte.toInt() shr 4) and 0xF].code.toByte()
            hex[2 * indeks + 1] = siffer[byte.toInt() and 0xF].code.toByte()
        }
        hex[hex.lastIndex] = '>'.code.toByte()
        return hex
    }

    private fun xrefRad(
        type: Int,
        felt2: Int,
        felt3: Int,
    ): ByteArray =
        ByteBuffer
            .allocate(7)
            .put(type.toByte())
            .putInt(felt2)
            .putShort(felt3.toShort())
            .array()

    private fun ByteArrayOutputStream.write(tekst: String) = write(tekst.toByteArray(Charsets.ISO_8859_1))

    private fun ByteArrayOutputStream.skrivStrøm(
        objektnummer: Int,
        ordbok: String,
        innhold: ByteArray,
        lengde: Long = innhold.size.toLong(),
    ) {
        write("$objektnummer 0 obj\n<< $ordbok /Length $lengde >>\nstream\n")
        write(innhold)
        write("\nendstream\nendobj\n")
    }
}
