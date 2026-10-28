package com.karaoke.song;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karaoke.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SongIT extends IntegrationTest {

    @Test
    void searchesTitleAndArtistIgnoringCaseSortedByArtistThenTitle() throws Exception {
        Session session = register();
        String tag = "zx" + UUID.randomUUID().toString().substring(0, 8);
        song("Bal " + tag, "Zoé", true);
        song("Aube", "Groupe " + tag.toUpperCase(), false);
        song("Chanson " + tag, "Zoé", false);
        song("Sans rapport", "Personne", true);

        getAs(session, "/api/v1/songs?query=" + tag.toUpperCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[*].title").value(contains("Aube", "Bal " + tag, "Chanson " + tag)))
                .andExpect(jsonPath("$.items[0].isFree").value(false))
                .andExpect(jsonPath("$.items[1].isFree").value(true))
                .andExpect(jsonPath("$.items[0].durationMs").value(180000))
                .andExpect(jsonPath("$.items[0].language").value("fr"))
                .andExpect(jsonPath("$.items[0].lyricsLrc").doesNotExist());
    }

    @Test
    void paginates() throws Exception {
        Session session = register();
        String tag = "pg" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 5; i++) {
            song(tag + " " + i, "Artiste", true);
        }

        getAs(session, "/api/v1/songs?query=" + tag + "&page=1&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items[*].title").value(contains(tag + " 2", tag + " 3")));
    }

    @Test
    void likeWildcardsAreSearchedLiterally() throws Exception {
        Session session = register();
        String tag = "wc" + UUID.randomUUID().toString().substring(0, 8);
        song(tag + " 100% live", "Artiste", true);
        song(tag + " 1000 live", "Artiste", true);

        mvc.perform(authenticated(get("/api/v1/songs").param("query", tag + " 100%"), session))
                .andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(authenticated(get("/api/v1/songs").param("query", tag + " 100_"), session))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void validatesPagingParameters() throws Exception {
        Session session = register();

        getAs(session, "/api/v1/songs?size=51")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        getAs(session, "/api/v1/songs?page=-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        getAs(session, "/api/v1/songs?page=abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void returnsSongDetailWithLyrics() throws Exception {
        Session session = register();
        Song song = song("Détail", "Artiste", true);

        getAs(session, "/api/v1/songs/" + song.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(song.getId().toString()))
                .andExpect(jsonPath("$.lyricsLrc").value(startsWith("[00:01.00]")))
                .andExpect(jsonPath("$.instrumentalKey").doesNotExist());
    }

    @Test
    void unknownOrMalformedSongId() throws Exception {
        Session session = register();

        getAs(session, "/api/v1/songs/" + UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        getAs(session, "/api/v1/songs/pas-un-uuid")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void catalogRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/songs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        getAs(register(), "/api/v1/inexistant")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
