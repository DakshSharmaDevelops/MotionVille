package org.example.motionville.services.video;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Lazy
public class R2StorageService {

    private final S3Client client;
    private final S3Client cdnClient;
    private final S3Presigner presigner;
    private final String bucket;
    private final String cdnBucket;
    private final String cdnBaseUrl;
    private final String cloudflareZoneId;
    private final String cloudflareApiToken;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public R2StorageService(
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.access-key}") String accessKey,
            @Value("${r2.secret-key}") String secretKey,
            @Value("${r2.bucket}") String bucket,
            @Value("${r2.cdn-bucket:}") String cdnBucket,
            @Value("${r2.cdn-base-url:}") String cdnBaseUrl,
            @Value("${cloudflare.zone-id:}") String cloudflareZoneId,
            @Value("${cloudflare.api-token:}") String cloudflareApiToken
    ) {
        this.bucket = bucket;
        this.cdnBucket = cdnBucket.trim();
        this.cdnBaseUrl = trimTrailingSlash(cdnBaseUrl.trim());
        this.cloudflareZoneId = cloudflareZoneId.trim();
        this.cloudflareApiToken = cloudflareApiToken.trim();
        boolean cdnConfigured = !this.cdnBucket.isEmpty()
                && !this.cdnBaseUrl.isEmpty()
                && !this.cloudflareZoneId.isEmpty()
                && !this.cloudflareApiToken.isEmpty();
        boolean cdnPartiallyConfigured = !this.cdnBucket.isEmpty()
                || !this.cdnBaseUrl.isEmpty()
                || !this.cloudflareZoneId.isEmpty()
                || !this.cloudflareApiToken.isEmpty();
        if (cdnPartiallyConfigured && !cdnConfigured) {
            throw new IllegalArgumentException(
                    "Configure R2_CDN_BUCKET, R2_CDN_BASE_URL, CLOUDFLARE_ZONE_ID, and CLOUDFLARE_API_TOKEN together"
            );
        }
        if (cdnConfigured) {
            URI cdnUri = URI.create(this.cdnBaseUrl);
            if (!"https".equalsIgnoreCase(cdnUri.getScheme())
                    || cdnUri.getHost() == null
                    || cdnUri.getUserInfo() != null
                    || cdnUri.getQuery() != null
                    || cdnUri.getFragment() != null) {
                throw new IllegalArgumentException("R2_CDN_BASE_URL must be an HTTPS origin URL");
            }
        }

        URI endpointUri = URI.create(endpoint);
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        this.client = createClient(endpointUri, credentials);
        this.cdnClient = cdnConfigured ? createClient(endpointUri, credentials) : null;

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .region(Region.of("auto"))
                .credentialsProvider(credentials)
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }

    private S3Client createClient(URI endpoint, StaticCredentialsProvider credentials) {
        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of("auto"))
                .credentialsProvider(credentials)
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .chunkedEncodingEnabled(false)
                                .build()
                )
                .build();
    }

    public String createUploadUrl(String key, String mimeType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimeType)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(objectRequest)
                        .build();

        return presigner.presignPutObject(presignRequest)
                .url()
                .toString();
    }

    public String createPlaybackUrl(String key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .getObjectRequest(objectRequest)
                        .build();

        return presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    public HeadObjectResponse headObject(String key) {
        return client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public String upload(Path file, String key, String mimeType) {
        return upload(file, key, mimeType, null);
    }

    public String upload(Path file, String key, String mimeType, String cacheControl) {
        var requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(mimeType);
        if (cacheControl != null) {
            requestBuilder.cacheControl(cacheControl);
        }
        PutObjectRequest request = requestBuilder.build();

        client.putObject(request, RequestBody.fromFile(file));

        return objectLocator(key);
    }

    public String uploadToCdnBucket(Path file, String key, String mimeType, String cacheControl) {
        requireCdnConfigured();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(cdnBucket)
                .key(key)
                .contentType(mimeType)
                .cacheControl(cacheControl)
                .build();
        cdnClient.putObject(request, RequestBody.fromFile(file));
        return cdnObjectUrl(key);
    }

    public void download(String key, Path destination) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        client.getObject(request, destination);
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        client.deleteObject(request);
    }

    public void deleteAfterCommit(Collection<String> locators) {
        String prefix = objectLocator("");
        Set<String> keys = new LinkedHashSet<>();
        for (String locator : locators) {
            if (locator != null && locator.startsWith(prefix)) {
                String key = locator.substring(prefix.length());
                if (!key.isBlank()) {
                    keys.add(key);
                }
            }
        }
        if (keys.isEmpty()) {
            return;
        }
        Runnable deleteObjects = () -> {
            RuntimeException failure = null;
            for (String key : keys) {
                try {
                    delete(key);
                } catch (RuntimeException exception) {
                    if (failure == null) {
                        failure = exception;
                    } else {
                        failure.addSuppressed(exception);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteObjects.run();
                }
            });
        } else {
            deleteObjects.run();
        }
    }

    public void deleteOriginPrefixAfterCommit(String prefix) {
        runAfterCommit(() -> {
            for (S3Object object : listObjects(client, bucket, prefix)) {
                delete(object.key());
            }
        });
    }

    public boolean cdnConfigured() {
        return cdnClient != null;
    }

    public String cdnObjectUrl(String key) {
        requireCdnConfigured();
        return cdnBaseUrl + "/" + key;
    }

    public boolean cdnObjectExists(String key) {
        requireCdnConfigured();
        try {
            cdnClient.headObject(HeadObjectRequest.builder()
                    .bucket(cdnBucket)
                    .key(key)
                    .build());
            return true;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    public void publishCdnPrefixAfterCommit(String prefix) {
        requireCdnConfigured();
        runAfterCommit(() -> publishCdnPrefix(prefix));
    }

    public void deleteCdnPrefixAfterCommit(String prefix) {
        if (!cdnConfigured()) {
            return;
        }
        runAfterCommit(() -> deleteCdnPrefix(prefix));
    }

    public void publishCdnPrefix(String prefix) {
        requireCdnConfigured();
        List<S3Object> objects = listObjects(client, bucket, prefix);
        List<String> urls = new ArrayList<>(objects.size());
        for (S3Object object : objects) {
            String encodedSource = URLEncoder.encode(
                    bucket + "/" + object.key(), StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%2F", "/");
            cdnClient.copyObject(CopyObjectRequest.builder()
                    .copySource(encodedSource)
                    .bucket(cdnBucket)
                    .key(object.key())
                    .metadataDirective(MetadataDirective.COPY)
                    .build());
            urls.add(cdnObjectUrl(object.key()));
        }
        purgeCdnUrls(urls);
    }

    public void deleteCdnPrefix(String prefix) {
        requireCdnConfigured();
        List<S3Object> cdnObjects = listObjects(cdnClient, cdnBucket, prefix);
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(listObjects(client, bucket, prefix).stream().map(S3Object::key).toList());
        keys.addAll(cdnObjects.stream().map(S3Object::key).toList());
        List<String> urls = keys.stream()
                .map(this::cdnObjectUrl)
                .toList();
        purgeCdnUrls(urls);
        for (S3Object object : cdnObjects) {
            cdnClient.deleteObject(DeleteObjectRequest.builder()
                    .bucket(cdnBucket)
                    .key(object.key())
                    .build());
        }
    }

    public void purgeCdnUrls(Collection<String> urls) {
        requireCdnConfigured();
        List<String> pending = new ArrayList<>(urls);
        for (int start = 0; start < pending.size(); start += 30) {
            List<String> batch = pending.subList(start, Math.min(start + 30, pending.size()));
            String files = batch.stream()
                    .map(url -> "\"" + escapeJson(url) + "\"")
                    .collect(java.util.stream.Collectors.joining(","));
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            "https://api.cloudflare.com/client/v4/zones/"
                                    + cloudflareZoneId + "/purge_cache"))
                    .header("Authorization", "Bearer " + cloudflareApiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"files\":[" + files + "]}"))
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300
                        || !response.body().matches("(?s).*\\\"success\\\"\\s*:\\s*true.*")) {
                    throw new IllegalStateException(
                            "Cloudflare CDN purge failed with HTTP " + response.statusCode());
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not reach Cloudflare CDN purge API", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Cloudflare CDN purge was interrupted", exception);
            }
        }
    }

    private List<S3Object> listObjects(S3Client storageClient, String storageBucket, String prefix) {
        List<S3Object> objects = new ArrayList<>();
        String continuationToken = null;
        do {
            var response = storageClient.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(storageBucket)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build());
            objects.addAll(response.contents());
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);
        return objects;
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception ex) {
                        log.warn("Failed to execute storage cleanup after commit: {}", ex.getMessage());
                    }
                }
            });
        } else {
            try {
                action.run();
            } catch (Exception ex) {
                log.warn("Failed to execute storage cleanup: {}", ex.getMessage());
            }
        }
    }

    private void requireCdnConfigured() {
        if (!cdnConfigured()) {
            throw new IllegalStateException("Cloudflare CDN storage is not configured");
        }
    }

    private String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String objectLocator(String key) {
        return "r2://" + bucket + "/" + key;
    }

    public String resolveUrl(String locator) {
        if (locator == null || !locator.startsWith("r2://")) {
            return locator;
        }
        String prefix = "r2://" + bucket + "/";
        if (locator.startsWith(prefix)) {
            return createPlaybackUrl(locator.substring(prefix.length()));
        }
        int slashIndex = locator.indexOf('/', "r2://".length());
        if (slashIndex >= 0 && slashIndex + 1 < locator.length()) {
            return createPlaybackUrl(locator.substring(slashIndex + 1));
        }
        return locator;
    }

    public String bucketName() {
        return bucket;
    }

    @PreDestroy
    public void close() {
        client.close();
        if (cdnClient != null) {
            cdnClient.close();
        }
        presigner.close();
    }
}