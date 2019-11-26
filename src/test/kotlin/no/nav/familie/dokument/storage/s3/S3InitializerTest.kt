package no.nav.familie.dokument.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
class S3InitializerTest {

    private val SIZE_MB = 20

    @Container
    var localStackContainer = LocalStackContainer().withServices(S3)

    private var s3: AmazonS3? = null

    @BeforeEach
    fun setUp() {
        s3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(S3))
                .withCredentials(localStackContainer.defaultCredentialsProvider)
                .enablePathStyleAccess()
                .build()
    }

    @Test
    fun bucketIsCreatedwhenMissing() {
        Assert.assertEquals(0, s3!!.listBuckets().size)

        S3Storage(s3!!, SIZE_MB)
        Assert.assertEquals(1,s3!!.listBuckets().size)

        S3Storage(s3!!, SIZE_MB)
        Assert.assertEquals(1,s3!!.listBuckets().size)

    }
}
