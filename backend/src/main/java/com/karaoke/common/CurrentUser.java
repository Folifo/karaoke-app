package com.karaoke.common;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** Identifiant de l'utilisateur authentifié (claim {@code sub} de l'access token). */
    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
