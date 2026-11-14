package no.nav.familie.dokument.storage.attachment

import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSDocument
import org.apache.pdfbox.cos.COSInputStream
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSStream
import org.apache.pdfbox.cos.ICOSParser
import org.apache.pdfbox.filter.DecodeOptions
import org.apache.pdfbox.filter.Filter
import org.apache.pdfbox.filter.FilterFactory
import org.apache.pdfbox.io.RandomAccessRead
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.io.RandomAccessReadView
import org.apache.pdfbox.pdfparser.PDFParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

/**
 * Leser en PDF slik Loader.loadPDF gjør, men stopper når strømmene i PDF-en til sammen pakkes ut til mer enn budsjettet.
 * PDFBox pakker ellers ut hver strøm i sin helhet i minnet, og en liten PDF kan pakkes ut til flere GB.
 */
internal class BegrensetPdfParser(
    input: ByteArray,
    budsjett: Utpakkingsbudsjett,
) : PDFParser(RandomAccessReadBuffer(input), "", null, null, null) {
    init {
        // Dokumentet lager alle strømmene i PDF-en, også når PDFBox må lete seg fram i en ødelagt fil
        document = BegrensetDokument(this, budsjett)
    }
}

/**
 * Hvor mye PDFBox får lese fra strømmene i én PDF til sammen. En strøm som leses flere ganger, teller hver gang,
 * og en strøm uten filter teller med sin egen størrelse.
 */
internal class Utpakkingsbudsjett(
    private val maksBytes: Long,
) {
    private var brukt = 0L

    var avvisningsgrunn: String? = null
        private set

    fun bruk(antallBytes: Long) {
        brukt += antallBytes
        if (avvisningsgrunn != null || brukt > maksBytes) avvis("Strømmene pakkes ut til mer enn ${maksBytes / (1024 * 1024)} MiB")
    }

    fun avvis(grunn: String): Nothing {
        avvisningsgrunn = avvisningsgrunn ?: grunn
        throw PdfAvvist(grunn)
    }
}

// RuntimeException fordi PDFBox fanger IOException flere steder og leser videre
internal class PdfAvvist(
    grunn: String,
) : RuntimeException(grunn)

private class BegrensetDokument(
    private val parser: ICOSParser,
    private val budsjett: Utpakkingsbudsjett,
) : COSDocument(parser) {
    override fun createCOSStream(
        dictionary: COSDictionary,
        startPosition: Long,
        streamLength: Long,
    ): COSStream =
        BegrensetStrøm(budsjett, parser.createRandomAccessReadView(startPosition, streamLength)).apply {
            dictionary.forEach { navn, verdi -> setItem(navn, verdi) }
            // PDFBox 3.0.3 setter av minne ut fra /Length, også når lengden er feil og PDFBox har funnet den riktige selv
            if (getLong(COSName.LENGTH) != streamLength) setLong(COSName.LENGTH, streamLength)
            key = dictionary.key
        }
}

private class BegrensetStrøm(
    private val budsjett: Utpakkingsbudsjett,
    kilde: RandomAccessReadView,
) : COSStream(null, kilde) {
    override fun createInputStream(options: DecodeOptions): COSInputStream {
        kontrollerUtpakking()
        return super.createInputStream(options)
    }

    override fun createView(): RandomAccessRead {
        kontrollerUtpakking()
        return super.createView()
    }

    // Pakker ut strømmen uten å ta vare på resultatet, og trekker størrelsen fra budsjettet, før PDFBox pakker den ut
    private fun kontrollerUtpakking() {
        val vekt = vekt()
        val filtre = filtre()
        if (filtre.isEmpty()) budsjett.bruk(vekt * length)
        budsjett.bruk(radbuffere())
        var input: InputStream = createRawInputStream()
        filtre.forEachIndexed { indeks, filter ->
            val erSiste = indeks == filtre.lastIndex
            val utpakket = ByteArrayOutputStream()
            val output =
                TellendeOutputStream(if (erSiste) OutputStream.nullOutputStream() else utpakket, budsjett, if (erSiste) vekt else 1)
            input.use { filter.decode(it, output, this, indeks, DecodeOptions.DEFAULT) }
            input = ByteArrayInputStream(utpakket.toByteArray())
        }
    }

    // En xref-strøm blir til én rad i minnet per objekt, så smale rader teller som om de var MINSTE_XREF_RADBREDDE brede
    private fun vekt(): Long {
        val bredder = getCOSArray(COSName.W) ?: return 1
        val radbredde = (0 until bredder.size()).sumOf { bredder.getInt(it, 0).toLong() }
        if (radbredde !in 1..STØRSTE_XREF_RADBREDDE) budsjett.avvis("Xref-strømmen har ugyldig radbredde")
        return (MINSTE_XREF_RADBREDDE + radbredde - 1) / radbredde
    }

    // Samme filtre som PDFBox bruker. Bildefiltre setter av minne ut fra bildets egne mål og brukes ikke på det vi leser.
    private fun filtre(): List<Filter> {
        val navn =
            when (val filtre = filters) {
                is COSName -> listOf(filtre)
                is COSArray -> filtre.toList()
                else -> emptyList()
            }
        if (!TILLATTE_FILTRE.containsAll(navn)) budsjett.avvis("En strøm som må pakkes ut, har et filter som ikke støttes")
        return navn.map { FilterFactory.INSTANCE.getFilter(it as COSName) }
    }

    // Med predictor setter PDFBox av to rader i minnet før utpakkingen starter, med en bredde PDF-en selv oppgir
    private fun radbuffere(): Long {
        val parametre =
            when (val parametre = getDictionaryObject(COSName.DP, COSName.DECODE_PARMS)) {
                is COSDictionary -> listOf(parametre)
                is COSArray -> (0 until parametre.size()).mapNotNull { parametre.getObject(it) as? COSDictionary }
                else -> emptyList()
            }
        return parametre.sumOf { 2L * radlengde(it) }
    }

    // Regner med Int som Predictor.calculateRowLength i PDFBox, slik at overflow gir samme radlengde som PDFBox setter av
    private fun radlengde(parametre: COSDictionary): Int {
        if (parametre.getInt(COSName.PREDICTOR) <= 1) return 0
        val bitsPerPiksel = min(parametre.getInt(COSName.COLORS, 1), 32) * parametre.getInt(COSName.BITS_PER_COMPONENT, 8)
        val radlengde = (parametre.getInt(COSName.COLUMNS, 1) * bitsPerPiksel + 7) / 8
        // Med en rad uten lengde blir PDFBox stående og lese samme rad om igjen
        if (radlengde <= 0) budsjett.avvis("En strøm som må pakkes ut, har ugyldig radlengde")
        return radlengde
    }

    companion object {
        private const val MINSTE_XREF_RADBREDDE = 8L
        private const val STØRSTE_XREF_RADBREDDE = 20L
        private val TILLATTE_FILTRE =
            setOf(
                COSName.FLATE_DECODE,
                COSName.FLATE_DECODE_ABBREVIATION,
                COSName.LZW_DECODE,
                COSName.LZW_DECODE_ABBREVIATION,
                COSName.ASCII_HEX_DECODE,
                COSName.ASCII_HEX_DECODE_ABBREVIATION,
                COSName.ASCII85_DECODE,
                COSName.ASCII85_DECODE_ABBREVIATION,
                COSName.RUN_LENGTH_DECODE,
                COSName.RUN_LENGTH_DECODE_ABBREVIATION,
                COSName.CRYPT,
            )
    }
}

private class TellendeOutputStream(
    private val output: OutputStream,
    private val budsjett: Utpakkingsbudsjett,
    private val vekt: Long,
) : OutputStream() {
    override fun write(byte: Int) {
        budsjett.bruk(vekt)
        output.write(byte)
    }

    override fun write(
        bytes: ByteArray,
        fra: Int,
        antall: Int,
    ) {
        budsjett.bruk(vekt * antall)
        output.write(bytes, fra, antall)
    }
}
