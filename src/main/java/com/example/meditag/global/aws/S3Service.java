package com.example.meditag.global.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;   // ✅ final 제거! (필드 주입)

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;    // 삭제용

    public String createPresignedUrl(String path) {
        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build();
        var preSignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();
        return s3Presigner.presignPutObject(preSignRequest).url().toString();
    }

    public void deleteByKey(String key) {
        if (key == null || key.isBlank()) return;
        var req = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(req);
    }

    public void deleteByUrl(String url) {
        String key = extractKeyFromUrl(url);
        deleteByKey(key);
    }

    private String extractKeyFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        int schemeIdx = url.indexOf("://");
        String path = (schemeIdx > -1) ? url.substring(url.indexOf('/', schemeIdx + 3)) : url;
        if (path.startsWith("/")) path = path.substring(1);
        int qIdx = path.indexOf('?');
        if (qIdx > -1) path = path.substring(0, qIdx);
        return path;
    }
}
