package com.pickview.media;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaStorage {
    private final Path mRoot;
    private final String mMode;
    private final String mBucket;
    private final MinioClient mClient;

    public MediaStorage(@Value("${pickview.storage.path}") String root, @Value("${pickview.storage.mode}") String mode,
                        @Value("${pickview.storage.endpoint}") String endpoint, @Value("${pickview.storage.access-key}") String accessKey,
                        @Value("${pickview.storage.secret-key}") String secretKey, @Value("${pickview.storage.bucket}") String bucket) throws IOException {
        mRoot = Path.of(root).toAbsolutePath().normalize(); Files.createDirectories(mRoot);
        mMode = mode; mBucket = bucket;
        mClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    public void storeFile(String key, Path source, String contentType) throws Exception {
        if (mMode.equals("s3")) {
            if (!mClient.bucketExists(BucketExistsArgs.builder().bucket(mBucket).build())) {
                mClient.makeBucket(MakeBucketArgs.builder().bucket(mBucket).build());
            }
            try (InputStream stream = Files.newInputStream(source)) {
                mClient.putObject(PutObjectArgs.builder().bucket(mBucket).object(key).stream(stream, Files.size(source), -1).contentType(contentType).build());
            }
            return;
        }
        Files.copy(source, resolvePath(key), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public Path getFile(String key) throws Exception {
        Path file = resolvePath(key);
        if (mMode.equals("s3") && !Files.exists(file)) {
            try (InputStream stream = mClient.getObject(GetObjectArgs.builder().bucket(mBucket).object(key).build())) {
                Files.copy(stream, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return file;
    }

    private Path resolvePath(String key) {
        if (!key.matches("[a-zA-Z0-9_.-]+")) { throw new IllegalArgumentException("Invalid media key"); }
        Path file = mRoot.resolve(key).normalize();
        if (!file.startsWith(mRoot)) { throw new IllegalArgumentException("Invalid media path"); }
        return file;
    }
}
