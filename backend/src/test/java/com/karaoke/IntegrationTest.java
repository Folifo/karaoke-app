package com.karaoke;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karaoke.song.Song;
import com.karaoke.song.SongRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base des tests d'intégration : application complète sur un PostgreSQL éphémère (Testcontainers),
 * partagé par toutes les classes de test. Chaque test crée ses propres utilisateurs et chansons.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected SongRepository songs;

    protected record Session(String email, String accessToken, String refreshToken) {
    }

    protected Session register() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        JsonNode body = read(postJson("/api/v1/auth/register",
                Map.of("email", email, "password", "motdepasse", "displayName", "Chanteur"))
                .andExpect(status().isCreated()));
        return new Session(email, body.get("accessToken").asText(), body.get("refreshToken").asText());
    }

    protected Song song(String title, String artist, boolean free) {
        return songs.save(new Song(title, artist, 180_000, "test/" + UUID.randomUUID() + ".m4a",
                "[00:01.00] la la la", "fr", null, free));
    }

    protected ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    protected ResultActions getAs(Session session, String path) throws Exception {
        return mvc.perform(authenticated(get(path), session));
    }

    protected static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request,
            Session session) {
        return request.header("Authorization", "Bearer " + session.accessToken());
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
