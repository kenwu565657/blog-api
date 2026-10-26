package com.contentfarm.file.operation.springboot.starter.config;

import com.contentfarm.file.operation.springboot.starter.exception.FileOperationException;
import com.contentfarm.file.operation.springboot.starter.pojo.AsyncOperationResult;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class AmazonS3Operator implements AmazonS3Operation {
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;

    public AmazonS3Operator(AmazonS3Properties amazonS3Properties) {
        Region region = Region.of(amazonS3Properties.getRegion());
        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider(amazonS3Properties);
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider);
        S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .multipartEnabled(true);
        applyEndpointOverride(amazonS3Properties, s3ClientBuilder, s3AsyncClientBuilder);
        this.s3Client = s3ClientBuilder.build();
        this.s3AsyncClient = s3AsyncClientBuilder.build();
    }

    private static AwsCredentialsProvider resolveCredentialsProvider(AmazonS3Properties amazonS3Properties) {
        String accessKey = amazonS3Properties.getAccessKey();
        if (accessKey == null || accessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, amazonS3Properties.getSecretKey())
        );
    }

    private static void applyEndpointOverride(
            AmazonS3Properties amazonS3Properties,
            S3ClientBuilder s3ClientBuilder,
            S3AsyncClientBuilder s3AsyncClientBuilder
    ) {
        String endpoint = amazonS3Properties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        URI endpointUri = URI.create(endpoint);
        s3ClientBuilder
                .endpointOverride(endpointUri)
                .forcePathStyle(true);
        s3AsyncClientBuilder
                .endpointOverride(endpointUri)
                .forcePathStyle(true);
    }

    @Override
    public List<Bucket> listBuckets() {
        return s3Client.listBuckets().buckets();
    }

    @Override
    public Optional<Bucket> getBucket(String bucketName) {
        List<Bucket> buckets = listBuckets();
        for (Bucket bucket : buckets) {
            if (bucketName.equals(bucket.name())) {
                return Optional.of(bucket);
            }
        }
        return Optional.empty();
    }

    @Override
    public void createBucket(String bucketName) {
        s3Client.createBucket(b -> b.bucket(bucketName));
        try (S3Waiter s3Waiter = s3Client.waiter()) {
            s3Waiter.waitUntilBucketExists(b -> b.bucket(bucketName));
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
        s3Client.deleteBucket(deleteBucketRequest);
    }

    @Override
    public void uploadFile(String bucketName, String fileName, File file) {
        CompletableFuture<PutObjectResponse> response = s3AsyncClient.putObject(b -> b
                        .bucket(bucketName)
                        .key(fileName),
                Paths.get(file.getPath()));
        response.join();
    }

    @Override
    public byte[] downloadFile(String bucketName, String fileName) {
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(fileName)
                .bucket(bucketName)
                .build();

        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObject(objectRequest, ResponseTransformer.toBytes());
            return objectBytes.asByteArray();
        } catch (NoSuchKeyException e1) {
            throw FileOperationException.ofFileNameNotExist();
        } catch (NoSuchBucketException e2) {
            throw FileOperationException.ofDirectoryNotExist();
        }
    }

    @Override
    public CompletableFuture<AsyncOperationResult<byte[]>> downloadFileAsync(String bucketName, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest
                .builder()
                .key(fileName)
                .bucket(bucketName)
                .build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> getObjectResponseCompletableFuture =
                s3AsyncClient.getObject(
                        getObjectRequest,
                        AsyncResponseTransformer.toBytes()
                );
        return getObjectResponseCompletableFuture
                    .thenApply(ResponseBytes::asByteArray)
                    .handle((result, exception) -> {
                        if (exception != null) {
                            if (exception instanceof CompletionException completionException) {
                                if (completionException.getCause() instanceof NoSuchKeyException) {
                                    return AsyncOperationResult.ofFailure(FileOperationException.ofFileNameNotExist());
                                }
                                if (completionException.getCause() instanceof NoSuchBucketException) {
                                    return AsyncOperationResult.ofFailure(FileOperationException.ofDirectoryNotExist());
                                }
                                return AsyncOperationResult.ofFailure(FileOperationException.ofUnexpectedError());
                            }
                            return AsyncOperationResult.ofFailure(exception);
                        }
                        else {
                            return AsyncOperationResult.ofSuccess(result);
                        }
                    });
    }

    @Override
    public void deleteFile(String bucketName, String fileName) {
        s3Client.deleteObject(b -> b.bucket(bucketName).key(fileName));
    }
}
