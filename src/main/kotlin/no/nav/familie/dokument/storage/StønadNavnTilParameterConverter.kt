package no.nav.familie.dokument.storage

import no.nav.familie.dokument.UkjentStønadParameter
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StønadNavnTilParameterConverter : Converter<String, StonadController.StønadParameter> {
    override fun convert(stønadNavn: String): StonadController.StønadParameter =
        StonadController.StønadParameter.entries.find { it.name == stønadNavn || it.stønadKey == stønadNavn }
            ?: throw UkjentStønadParameter(stønadNavn)
}
