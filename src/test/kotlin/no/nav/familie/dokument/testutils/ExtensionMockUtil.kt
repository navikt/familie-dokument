package no.nav.familie.dokument.testutils

import io.mockk.mockkStatic

object ExtensionMockUtil {

    fun setUpMockHentFnr() {
        mockkStatic("no.nav.familie.dokument.storage.ExtensionsKt")
    }
}