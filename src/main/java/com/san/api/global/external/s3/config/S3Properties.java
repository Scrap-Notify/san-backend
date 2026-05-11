package com.san.api.global.external.s3.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/** S3 연동 설정 값 */
@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotBlank String bucket,
        @NotBlank String region,
        @Positive long presignedUrlExpirationMinutes,
        @Positive long maxFileSizeBytes,
        Set<@NotBlank String> allowedExtensions,
        Set<@NotBlank String> allowedContentTypes
) {
}
