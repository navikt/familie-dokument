package no.nav.familie.dokument.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter
import org.slf4j.LoggerFactory

internal class S3Initializer(private val s3: AmazonS3) {

    fun initializeBucket(bucketName: String) {

        if (s3.listBuckets().stream().noneMatch({ bucket -> bucket.getName().equals(bucketName) })) {
            createBucket(bucketName)
        }

        log.info("Initializing bucket {}", bucketName)
        s3.setBucketLifecycleConfiguration(bucketName,
                BucketLifecycleConfiguration().withRules(
                        BucketLifecycleConfiguration.Rule()
                                .withId("soknad-retention-policy-1")
                                .withFilter(LifecycleFilter())
                                .withStatus(BucketLifecycleConfiguration.ENABLED)
                                .withExpirationInDays(1)
                ))

    }

    private fun createBucket(bucketName: String) {
        log.info("Bucket {} doesn't exist. Creating", bucketName)
        s3.createBucket(CreateBucketRequest(bucketName).withCannedAcl(CannedAccessControlList.Private))
    }

    companion object {

        private val log = LoggerFactory.getLogger(S3Initializer::class.java)
    }

}
