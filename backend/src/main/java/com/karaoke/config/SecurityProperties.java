package com.karaoke.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("karaoke.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt, @NotNull Duration refreshTokenTtl) {

    public record Jwt(
            @NotBlank(message = "doit être défini (variable JWT_SECRET)")
            @Size(min = 32, message = "doit faire au moins 32 caractères") String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl) {
    }
}
