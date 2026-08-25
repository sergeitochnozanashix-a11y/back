package software.pxel.learneasy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.region}")
    private String region;

    @Value("${minio.voiceBucketName}")
    private String voiceBucketName;

    @Value("${minio.mediaBucketName}")
    private String mediaBucketName;


    @Bean
    public S3Client s3Client() {
        log.info("Initializing S3Client with URL: {}, region: {}", minioUrl, region);
        return S3Client.builder()
                .endpointOverride(URI.create(minioUrl))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .forcePathStyle(true)
                .build();
    }

    @Bean(name = "minioVoiceBucketName")
    public String minioBucketName() {
        return voiceBucketName;
    }

    @Bean(name = "minioMediaBucketName")
    public String minioMediaBucketName() {
        return mediaBucketName;
    }
}
