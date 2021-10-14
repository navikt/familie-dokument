package no.nav.familie.dokument.virusscan

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VirusScanServiceTest {

    private val virusScanClient: VirusScanClient = mockk()
    private val virusScanService = VirusScanService(virusScanClient, mockk(relaxed = true))

    @Test
    internal fun ok() {
        every { virusScanClient.scan(any()) } returns listOf(ScanResult(Result.OK))
        virusScanService.scan(byteArrayOf(12), "navn")
    }

    @Test
    internal fun `skal kaste exception når klienten returnerer tom liste med result`() {
        every { virusScanClient.scan(any()) } returns listOf()
        assertThat(Assertions.catchThrowable { virusScanService.scan(byteArrayOf(12), "navn") })
                .isInstanceOf(VirusScanException::class.java)
    }

    @Test
    internal fun `skal kaste exception når klienten returnerer fler enn 1 result`() {
        every { virusScanClient.scan(any()) } returns listOf(ScanResult(Result.OK), ScanResult(Result.OK))
        assertThat(Assertions.catchThrowable { virusScanService.scan(byteArrayOf(12), "navn") })
                .isInstanceOf(VirusScanException::class.java)
    }

    @Test
    internal fun `skal kaste exception når klienten finner virus`() {
        every { virusScanClient.scan(any()) } returns listOf(ScanResult(Result.OK), ScanResult(Result.FOUND))
        assertThat(Assertions.catchThrowable { virusScanService.scan(byteArrayOf(12), "navn") })
                .isInstanceOf(VirusScanException::class.java)
    }
}
