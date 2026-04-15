package no.nav.familie.dokument.storage

import no.nav.familie.dokument.UkjentStønadParameter
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StønadParameterConverter : Converter<String, StonadController.StønadParameter> {
    override fun convert(kilde: String): StonadController.StønadParameter =
        StonadController.StønadParameter.entries.find { it.name == kilde || it.stønadKey == kilde }
            ?: throw UkjentStønadParameter(kilde)
}
