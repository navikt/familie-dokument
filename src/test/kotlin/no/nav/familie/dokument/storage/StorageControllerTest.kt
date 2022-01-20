package no.nav.familie.dokument.storage

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.dokument.pdf.PdfService
import no.nav.familie.dokument.storage.attachment.AttachmentStorage
import no.nav.familie.dokument.storage.encryption.Hasher
import no.nav.familie.dokument.virusscan.VirusScanService
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

internal class StorageControllerTest {

    lateinit var storageController: StorageController
    val storageMock = mockk<AttachmentStorage>()
    val dokument1 = UUID.randomUUID()
    val dokument2 = UUID.randomUUID()
    val dokument3 = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {

        val virusScanMock = mockk<VirusScanService>()
        val contextHolderMock = mockk<TokenValidationContextHolder>()
        val pdfServiceMock = PdfService()

        storageController =
                StorageController(storageMock, virusScanMock, contextHolderMock, 10, Hasher("Hemmelig salt"), pdfServiceMock)

        every { contextHolderMock.hentFnr() } returns "12345678910"
        every { storageMock.get(any(), dokument1.toString()) } returns leseVedlegg("vedlegg", "gyldig-0.8m.pdf")
        every { storageMock.get(any(), dokument2.toString()) } returns leseVedlegg("pdf", "eksempel1.pdf")
        every { storageMock.get(any(), dokument3.toString()) } returns leseVedlegg("vedlegg", "gyldig-0.8m.pdf")


    }

    @Test
    internal fun `skal slå sammen en liste av innsendte dokumenter og lagre som et dokument`() {
        val dokumentListe = listOf(dokument1, dokument2, dokument3)
        val dokumentIdSlot = slot<String>()
        val mergetDokumentSlot = slot<ByteArray>()
        every { storageMock.put(any(), capture(dokumentIdSlot), capture(mergetDokumentSlot)) } just Runs
        val response = storageController.mergeAndStoreDocuments("familievedlegg", dokumentListe)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body.get("dokumentId")).isEqualTo(dokumentIdSlot.captured)
        Files.createDirectories(Paths.get("target/mergetpdf"))
        File("target/mergetpdf", "mergetfil.pdf").writeBytes(mergetDokumentSlot.captured)
    }

    private fun leseVedlegg(mappeNavn: String, navn: String): ByteArray {
        return StorageControllerTest::class.java.getResource("/${mappeNavn}/${navn}").readBytes()
    }

}