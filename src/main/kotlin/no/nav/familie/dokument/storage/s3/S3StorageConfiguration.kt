package no.nav.familie.dokument.storage.s3

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class S3StorageConfiguration {

    @Bean
    fun s3(
            endpointConfiguration: AwsClientBuilder.EndpointConfiguration,
            credentialsProvider: AWSCredentialsProvider): AmazonS3 {

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(credentialsProvider)
                .enablePathStyleAccess()
                .build()

    }

    @Bean
    fun enpointConfiguration(
            @Value("\${FAMILIE_DOKUMENT_S3_ENDPOINT}") endpoint: String,
            @Value("\${FAMILIE_DOKUMENT_S3_REGION}") region: String): AwsClientBuilder.EndpointConfiguration {

        log.info("Initializing s3 endpoint configuration with enpoint {} and region {}", endpoint, region)

        return AwsClientBuilder.EndpointConfiguration(endpoint, region)
    }

    @Bean
    fun credentialsProvider(
            @Value("\${FAMILIE_DOKUMENT_S3_ACCESSKEY}") accessKey: String,
            @Value("\${FAMILIE_DOKUMENT_S3_SECRETKEY}") secretKey: String): AWSCredentialsProvider {

        val awsCredentials = BasicAWSCredentials(accessKey, secretKey)

        return AWSStaticCredentialsProvider(awsCredentials)
    }


    @Profile("!dev")
    @Bean
    fun storage(s3: AmazonS3, @Value("\${attachment.max.size.mb}") maxFileSizeMB: Int): S3Storage {
        return S3Storage(s3, maxFileSizeMB)
    }

    companion object {

        private val log = LoggerFactory.getLogger(S3StorageConfiguration::class.java)
    }

}
