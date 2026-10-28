package com.karaoke.auth;

import com.karaoke.common.Utf8MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) @Utf8MaxBytes(72) String password,
            @NotBlank @Size(max = 50) String displayName) {

        public RegisterRequest {
            email = strip(email);
            displayName = strip(displayName);
        }
    }

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password) {

        public LoginRequest {
            email = strip(email);
        }
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    /** Les espaces autour de l'email (fréquents avec l'autocomplétion mobile) sont ignorés. */
    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
