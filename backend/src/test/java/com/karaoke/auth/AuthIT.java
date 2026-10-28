package com.karaoke.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.karaoke.IntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIT extends IntegrationTest {

    @Test
    void registerOpensSessionUsableOnProtectedRoutes() throws Exception {
        String email = "Nouveau-" + UUID.randomUUID() + "@Example.COM";

        JsonNode tokens = read(postJson("/api/v1/auth/register",
                Map.of("email", "  " + email + " ", "password", "motdepasse", "displayName", " Léa "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900)));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.displayName").value("Léa"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.subscription.active").value(false))
                .andExpect(jsonPath("$.freeQuota.dailyLimit").value(3))
                .andExpect(jsonPath("$.freeQuota.usedToday").value(0));
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCase() throws Exception {
        Session existing = register();

        postJson("/api/v1/auth/register",
                Map.of("email", existing.email().toUpperCase(), "password", "motdepasse", "displayName", "Autre"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        postJson("/api/v1/auth/register",
                Map.of("email", "pas-un-email", "password", "court", "displayName", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("email", "password", "displayName")));

        // 36 caractères mais 72+ octets : refusé (limite BCrypt).
        postJson("/api/v1/auth/register",
                Map.of("email", "a" + UUID.randomUUID() + "@example.com", "password", "é".repeat(37),
                        "displayName", "Léa"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void malformedJsonIsReported() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void loginWithValidAndInvalidCredentials() throws Exception {
        Session session = register();

        postJson("/api/v1/auth/login", Map.of("email", session.email().toUpperCase(), "password", "motdepasse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        postJson("/api/v1/auth/login", Map.of("email", session.email(), "password", "mauvais-mdp"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        postJson("/api/v1/auth/login", Map.of("email", "inconnu-" + UUID.randomUUID() + "@example.com",
                "password", "motdepasse"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        postJson("/api/v1/auth/login", Map.of("email", session.email(), "password", "x".repeat(100)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesTokens() throws Exception {
        Session session = register();

        JsonNode renewed = read(postJson("/api/v1/auth/refresh", Map.of("refreshToken", session.refreshToken()))
                .andExpect(status().isOk()));
        assertThat(renewed.get("refreshToken").asText()).isNotEqualTo(session.refreshToken());

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + renewed.get("accessToken").asText()))
                .andExpect(status().isOk());

        postJson("/api/v1/auth/refresh", Map.of("refreshToken", renewed.get("refreshToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void reusedRefreshTokenRevokesAllSessions() throws Exception {
        Session session = register();
        JsonNode renewed = read(postJson("/api/v1/auth/refresh", Map.of("refreshToken", session.refreshToken()))
                .andExpect(status().isOk()));

        // Le token d'origine est rejoué (vol présumé) : rejet, et la session légitime est fermée aussi.
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", session.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", renewed.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRefreshTokenIsRejected() throws Exception {
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", "inconnu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshIgnoresStaleAuthorizationHeader() throws Exception {
        Session session = register();

        mvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer jeton.expire.invalide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", session.refreshToken()))))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Session session = register();

        postJson("/api/v1/auth/logout", Map.of("refreshToken", session.refreshToken()))
                .andExpect(status().isNoContent());
        postJson("/api/v1/auth/logout", Map.of("refreshToken", session.refreshToken()))
                .andExpect(status().isNoContent());
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", session.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRequireValidAccessToken() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer pas.un.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
