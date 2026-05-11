package com.san.api.global.external.s3.service;

import com.san.api.global.external.s3.config.S3Properties;
import com.san.api.global.external.s3.dto.request.S3PresignedUrlRequest;
import com.san.api.global.external.s3.dto.response.S3PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/** S3 Presigned URL 발급 Service */
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private static final String SCRAP_IMAGE_PREFIX = "scrap/images/";

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * S3 업로드용 Presigned URL 발급
     *
     * @param request Presigned URL 발급 요청
     * @return Presigned URL 발급 응답
     */
    public S3PresignedUrlResponse createPresignedUrl(UUID userId, S3PresignedUrlRequest request) {
        String objectKey = createObjectKey(userId, request.fileName());
        Duration signatureDuration = Duration.ofMinutes(s3Properties.presignedUrlExpirationMinutes());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .contentType(request.contentType())
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new S3PresignedUrlResponse(
                presignedRequest.url().toString(),
                objectKey,
                signatureDuration.toSeconds()
        );
    }

    private String createObjectKey(UUID userId, String fileName) {
        // 사용자별 파일 정리와 원본 파일명 충돌 방지를 위해 사용자 ID와 UUID로 S3 저장 경로 생성
        return SCRAP_IMAGE_PREFIX + userId + "/" + UUID.randomUUID() + extractExtension(fileName);
    }

    private String extractExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0) {
            return "";
        }

        return fileName.substring(extensionIndex);
    }

}
