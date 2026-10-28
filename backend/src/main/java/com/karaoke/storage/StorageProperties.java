package com.karaoke.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param publicEndpoint URL du stockage vue par l'application mobile ; les URLs signées pointent dessus
 * @param signedUrlTtl   durée de validité des URLs signées (quelques minutes)
 */
@Validated
@ConfigurationProperties("karaoke.storage")
public record StorageProperties(
        @NotNull URI publicEndpoint,
        @NotBlank String region,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        boolean pathStyleAccess,
        @NotBlank String instrumentalsBucket,
        @NotBlank String recordingsBucket,
        @NotNull Duration signedUrlTtl) {
}
