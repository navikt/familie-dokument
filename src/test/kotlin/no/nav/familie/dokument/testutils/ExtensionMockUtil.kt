package no.nav.familie.dokument.testutils

import io.mockk.mockkStatic
import io.mockk.unmockkStatic

object ExtensionMockUtil {

    fun setUpMockHentFnr() {
        mockkStatic("no.nav.familie.dokument.storage.HentFnrKt")
    }

    fun unmockHentFnr() {
        unmockkStatic("no.nav.familie.dokument.storage.HentFnrKt")
    }
}
