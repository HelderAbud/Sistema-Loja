package com.lojapp.service;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(name = "lojapp.nfe.storage.backend", havingValue = "s3")
public class S3NfeRawXmlStorage implements NfeRawXmlStorage {

    private final S3Client s3;
    private final String bucket;
    private final String keyPrefix;

    public S3NfeRawXmlStorage(
            @Value("${lojapp.nfe.storage.s3.endpoint:}") String endpoint,
            @Value("${lojapp.nfe.storage.s3.region:us-east-1}") String region,
            @Value("${lojapp.nfe.storage.s3.access-key:}") String accessKey,
            @Value("${lojapp.nfe.storage.s3.secret-key:}") String secretKey,
            @Value("${lojapp.nfe.storage.s3.bucket}") String bucket,
            @Value("${lojapp.nfe.storage.s3.key-prefix:nfe}") String keyPrefix) {
        var builder = S3Client.builder().region(Region.of(region));

        boolean hasStaticCreds = !accessKey.isBlank() && !secretKey.isBlank();
        if (hasStaticCreds) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey.trim(), secretKey.trim())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (!endpoint.isBlank()) {
            // Endpoint customizado (ex.: MinIO local) exige path-style.
            builder.endpointOverride(java.net.URI.create(endpoint.trim())).forcePathStyle(true);
        }

        this.s3 = builder.build();
        this.bucket = bucket;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public StoredRawXml persist(long userId, String rawXml) {
        String key = "%s/%d/%s.xml".formatted(keyPrefix, userId, UUID.randomUUID());
        PutObjectRequest request =
                PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/xml").build();
        s3.putObject(request, RequestBody.fromString(rawXml, StandardCharsets.UTF_8));
        return new StoredRawXml(null, key);
    }

    @Override
    public String retrieve(String rawXml, String rawXmlKey) {
        // Fallback transitório para linhas legadas ainda com raw_xml no banco.
        if (rawXml != null && !rawXml.isBlank()) {
            return rawXml;
        }
        if (rawXmlKey == null || rawXmlKey.isBlank()) {
            throw new LojappDomainException(
                    ApiErrorCode.INTERNAL_ERROR,
                    "XML bruto da NFe não está disponível para esta entrada");
        }
        try {
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(rawXmlKey).build();
            ResponseBytes<?> bytes = s3.getObjectAsBytes(request);
            return bytes.asString(StandardCharsets.UTF_8);
        } catch (NoSuchKeyException ex) {
            throw new LojappDomainException(
                    ApiErrorCode.NOT_FOUND,
                    "XML bruto da NFe não encontrado no storage externo");
        }
    }
}
