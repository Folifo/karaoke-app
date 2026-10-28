package com.karaoke.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.karaoke.auth.AuthDtos.TokenResponse;
import com.karaoke.config.SecurityConfig;
import com.karaoke.config.SecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class TokenServiceTest {

    private final SecurityProperties properties = new SecurityProperties(
            new SecurityProperties.Jwt("unit-test-secret-unit-test-secret-0123", "karaoke-backend",
                    Duration.ofMinutes(15)),
            Duration.ofDays(30));
    private final SecurityConfig config = new SecurityConfig();
    private final SecretKey key = config.jwtSigningKey(properties);
    private final JwtDecoder decoder = config.jwtDecoder(key, properties);
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);

    @Test
    void issuesSignedAccessTokenAndOpaqueRefreshToken() {
        UUID userId = UUID.randomUUID();
        TokenService service = new TokenService(config.jwtEncoder(key), repository, properties, Clock.systemUTC());

        TokenResponse tokens = service.issue(userId);

        Jwt jwt = decoder.decode(tokens.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("karaoke-backend");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.expiresIn()).isEqualTo(900);
        assertThat(tokens.refreshToken()).hasSize(43).doesNotContain(".");
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void rejectsExpiredAccessToken() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        TokenService service = new TokenService(config.jwtEncoder(key), repository, properties, past);

        String token = service.accessToken(UUID.randomUUID(), past.instant());

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        SecurityProperties other = new SecurityProperties(
                new SecurityProperties.Jwt("another-secret-another-secret-0123456", "karaoke-backend",
                        Duration.ofMinutes(15)),
                Duration.ofDays(30));
        TokenService forger = new TokenService(config.jwtEncoder(config.jwtSigningKey(other)), repository, other,
                Clock.systemUTC());

        String token = forger.accessToken(UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(Exception.class);
    }

    @Test
    void hashesRefreshTokensDeterministically() {
        assertThat(TokenService.hash("abc")).isEqualTo(TokenService.hash("abc")).hasSize(64);
        assertThat(TokenService.hash("abc")).isNotEqualTo(TokenService.hash("abd"));
    }
}
