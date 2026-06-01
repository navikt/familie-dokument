package no.nav.familie.dokument.storage

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import no.nav.familie.dokument.pdf.PdfService
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.virusscan.VirusScanService
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.familie.sikkerhet.EksternBrukerUtils.hentFnrFraToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

internal class StorageControllerTest {
    private val storageMock = mockk<AttachmentStorage>()
    private val virusScanMock = mockk<VirusScanService>()
    private val pdfServiceMock = PdfService()
    private val storageController = StorageController(storageMock, virusScanMock, 10, Hasher("Hemmelig salt"), pdfServiceMock)

    private val dokument1 = UUID.randomUUID()
    private val dokument2 = UUID.randomUUID()
    private val dokument3 = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        mockkObject(EksternBrukerUtils)
        every { hentFnrFraToken() } returns "12345678910"
        every { storageMock.get(any(), dokument1.toString()) } returns leseVedlegg("vedlegg", "gyldig-0.8m.pdf")
        every { storageMock.get(any(), dokument2.toString()) } returns leseVedlegg("pdf", "eksempel1.pdf")
        every { storageMock.get(any(), dokument3.toString()) } returns leseVedlegg("vedlegg", "gyldig-0.8m.pdf")
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(EksternBrukerUtils)
    }

    @Test
    internal fun `skal slå sammen en liste av innsendte dokumenter og lagre som et dokument`() {
        val dokumentListe = listOf(dokument1, dokument2, dokument3)
        val dokumentIdSlot = slot<String>()
        val mergetDokumentSlot = slot<ByteArray>()
        every { storageMock.put(any(), capture(dokumentIdSlot), capture(mergetDokumentSlot)) } just Runs
        val response = storageController.mergeAndStoreDocuments("familievedlegg", dokumentListe)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.get("dokumentId")).isEqualTo(dokumentIdSlot.captured)
        Files.createDirectories(Paths.get("target/mergetpdf"))
        File("target/mergetpdf", "mergetfil.pdf").writeBytes(mergetDokumentSlot.captured)
    }

    private fun leseVedlegg(
        mappeNavn: String,
        navn: String,
    ): ByteArray = StorageControllerTest::class.java.getResource("/$mappeNavn/$navn").readBytes()
}
