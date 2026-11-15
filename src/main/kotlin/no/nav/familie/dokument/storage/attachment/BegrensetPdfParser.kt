package no.nav.familie.dokument.storage.attachment

import no.nav.familie.dokument.InvalidDocumentFormat
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
 * Leser en PDF slik Loader.loadPDF gjør, men avviser den når strømmene pakkes ut til mer enn [Utpakkingsbudsjett] tillater.
 * PDFBox pakker ellers ut hver strøm i sin helhet i minnet, og en liten PDF kan pakkes ut til flere GB.
 */
internal class BegrensetPdfParser(
    input: ByteArray,
    maksBytes: Long,
) : PDFParser(RandomAccessReadBuffer(input)) {
    init {
        // Dokumentet lager alle strømmene i PDF-en, også når PDFBox må lete seg fram i en ødelagt fil
        document = BegrensetDokument(this, Utpakkingsbudsjett(maksBytes))
    }
}

private const val MAKS_GJENTATTE_UTPAKKINGER = 32L

private fun avvis(grunn: String): Nothing = throw InvalidDocumentFormat("PDF-en ble avvist. $grunn")

/**
 * Hver strøm teller mot [maksBytes] med det meste den er pakket ut til. PDFBox pakker ut samme strøm flere ganger,
 * for eksempel en innebygd skrift én gang per skjemafelt, så alt som pakkes ut teller i tillegg mot en grense som er
 * [MAKS_GJENTATTE_UTPAKKINGER] ganger så stor.
 */
private class Utpakkingsbudsjett(
    private val maksBytes: Long,
) {
    private var utpakket = 0L
    private var utpakketMedGjentakelser = 0L

    fun bruk(
        antallBytes: Long,
        nyeBytes: Long,
    ) {
        utpakket += nyeBytes
        utpakketMedGjentakelser += antallBytes
        if (utpakket > maksBytes) avvis("Strømmene pakkes ut til mer enn ${maksBytes / (1024 * 1024)} MiB")
        if (utpakketMedGjentakelser > maksBytes * MAKS_GJENTATTE_UTPAKKINGER) avvis("Strømmene pakkes ut for mange ganger")
    }
}

private class BegrensetDokument(
    private val parser: ICOSParser,
    private val budsjett: Utpakkingsbudsjett,
) : COSDocument(parser) {
    override fun createCOSStream(
        dictionary: COSDictionary,
        startPosition: Long,
        streamLength: Long,
    ): COSStream {
        if (streamLength < 0) avvis("En strøm oppgir negativ lengde")
        return BegrensetStrøm(budsjett, parser.createRandomAccessReadView(startPosition, streamLength)).apply {
            dictionary.forEach { navn, verdi -> setItem(navn, verdi) }
            // Med feil /Length kan PDFBox 3.0.3 bli stående i en evig løkke når strømmen pakkes ut
            if (getLong(COSName.LENGTH) != streamLength) setLong(COSName.LENGTH, streamLength)
            key = dictionary.key
        }
    }
}

private class BegrensetStrøm(
    private val budsjett: Utpakkingsbudsjett,
    kilde: RandomAccessReadView,
) : COSStream(null, kilde) {
    private var størsteUtpakking = 0L

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
        // PDFBox leser en strøm med /First som objektstrøm. Den blir til nye objekter i minnet hver gang, så den teller fullt hver gang
        val erObjektstrøm = containsKey(COSName.FIRST)
        var utpakketNå = 0L
        val tell = { antallBytes: Long ->
            utpakketNå += antallBytes
            budsjett.bruk(antallBytes, nyeBytes = if (erObjektstrøm) antallBytes else maxOf(0, utpakketNå - størsteUtpakking))
            størsteUtpakking = maxOf(størsteUtpakking, utpakketNå)
        }
        val vekt = vekt()
        val filtre = filtre()
        if (filtre.isEmpty()) tell(vekt * length)
        tell(radbuffere())
        var input: InputStream = createRawInputStream()
        filtre.forEachIndexed { indeks, filter ->
            val erSiste = indeks == filtre.lastIndex
            val mellomresultat = ByteArrayOutputStream()
            val output =
                TellendeOutputStream(if (erSiste) OutputStream.nullOutputStream() else mellomresultat, tell, if (erSiste) vekt else 1)
            input.use { filter.decode(it, output, this, indeks, DecodeOptions.DEFAULT) }
            input = ByteArrayInputStream(mellomresultat.toByteArray())
        }
    }

    // En xref-strøm blir til én rad i minnet per objekt, så smale rader teller som om de var MINSTE_XREF_RADBREDDE brede
    private fun vekt(): Long {
        val bredder = getCOSArray(COSName.W) ?: return 1
        val radbredde = (0 until bredder.size()).sumOf { bredder.getInt(it, 0).toLong() }
        if (radbredde !in 1..STØRSTE_XREF_RADBREDDE) avvis("Xref-strømmen har ugyldig radbredde")
        return (MINSTE_XREF_RADBREDDE + radbredde - 1) / radbredde
    }

    // Samme filtre som PDFBox bruker, som hopper over et filter som er oppgitt flere ganger.
    // Bildefiltre setter av minne ut fra bildets egne mål og brukes ikke på det vi leser.
    private fun filtre(): List<Filter> {
        val navn =
            when (val filtre = filters) {
                is COSName -> listOf(filtre)
                is COSArray -> filtre.toList()
                else -> emptyList()
            }
        if (!TILLATTE_FILTRE.containsAll(navn)) avvis("En strøm som må pakkes ut, har et filter som ikke støttes")
        return navn.map { FilterFactory.INSTANCE.getFilter(it as COSName) }.distinct()
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
        if (radlengde <= 0) avvis("En strøm som må pakkes ut, har ugyldig radlengde")
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
    private val tell: (Long) -> Unit,
    private val vekt: Long,
) : OutputStream() {
    override fun write(byte: Int) {
        tell(vekt)
        output.write(byte)
    }

    override fun write(
        bytes: ByteArray,
        fra: Int,
        antall: Int,
    ) {
        tell(vekt * antall)
        output.write(bytes, fra, antall)
    }
}
