package com.karaoke.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class StorageServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private final StorageProperties properties = new StorageProperties(
            URI.create("http://10.0.2.2:9000"), "us-east-1", "access", "secret", true,
            "instrumentals", "recordings", Duration.ofMinutes(5));
    private final S3Presigner presigner = new StorageConfig().s3Presigner(properties);
    private final StorageService storage =
            new StorageService(presigner, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @AfterEach
    void close() {
        presigner.close();
    }

    @Test
    void signsInstrumentalDownloadOnPublicEndpointWithShortExpiry() {
        SignedUrl signed = storage.instrumentalDownloadUrl("demo/song one.m4a");

        URI url = URI.create(signed.url());
        assertThat(url.getHost()).isEqualTo("10.0.2.2");
        assertThat(url.getPort()).isEqualTo(9000);
        assertThat(url.getRawPath()).isEqualTo("/instrumentals/demo/song%20one.m4a");
        assertThat(url.getQuery())
                .contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
                .contains("X-Amz-Expires=300")
                .contains("X-Amz-Signature=");
        assertThat(signed.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void signsRecordingUploadIncludingContentType() {
        SignedUrl signed = storage.recordingUploadUrl("users/42/take.m4a", "audio/mp4");

        URI url = URI.create(signed.url());
        assertThat(url.getRawPath()).isEqualTo("/recordings/users/42/take.m4a");
        assertThat(url.getQuery()).contains("X-Amz-SignedHeaders=content-type;host");
        // Aucun en-tête de checksum imposé : le client n'aurait pas les valeurs à envoyer.
        assertThat(url.getQuery()).doesNotContainIgnoringCase("checksum");
    }

    @Test
    void signsRecordingDownloadInRecordingsBucket() {
        assertThat(URI.create(storage.recordingDownloadUrl("users/42/mix.m4a").url()).getRawPath())
                .isEqualTo("/recordings/users/42/mix.m4a");
    }
}
