package com.karaoke.song;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.karaoke.IntegrationTest;
import com.karaoke.subscription.Subscription;
import com.karaoke.subscription.SubscriptionRepository;
import com.karaoke.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InstrumentalUrlIT extends IntegrationTest {

    @Autowired
    private SubscriptionRepository subscriptions;

    @Autowired
    private UserRepository users;

    @Test
    void freeUserPlaysFreeSongsWithinDailyLimit() throws Exception {
        Session session = register();
        Song song = song("Gratuite", "Artiste", true);
        String path = "/api/v1/songs/" + song.getId() + "/instrumental-url";

        for (int i = 0; i < 3; i++) {
            getAs(session, path)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value(startsWith("http://localhost:9000/instrumentals/test/")))
                    .andExpect(jsonPath("$.url").value(containsString("X-Amz-Signature=")))
                    .andExpect(jsonPath("$.expiresAt").isString());
        }
        getAs(session, path)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("FREE_LIMIT_REACHED"));

        getAs(session, "/api/v1/me").andExpect(jsonPath("$.freeQuota.usedToday").value(3));
    }

    @Test
    void freeUserCannotPlayPremiumSong() throws Exception {
        Session session = register();
        Song song = song("Premium", "Artiste", false);

        getAs(session, "/api/v1/songs/" + song.getId() + "/instrumental-url")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_REQUIRED"));
        getAs(session, "/api/v1/me").andExpect(jsonPath("$.freeQuota.usedToday").value(0));
    }

    @Test
    void subscriberPlaysEverythingWithoutLimit() throws Exception {
        Session session = register();
        subscribe(session, Subscription.Status.ACTIVE, Instant.now().plus(Duration.ofDays(30)));
        Song premium = song("Premium", "Artiste", false);
        Song free = song("Gratuite", "Artiste", true);

        for (int i = 0; i < 5; i++) {
            getAs(session, "/api/v1/songs/" + free.getId() + "/instrumental-url").andExpect(status().isOk());
        }
        getAs(session, "/api/v1/songs/" + premium.getId() + "/instrumental-url").andExpect(status().isOk());
        getAs(session, "/api/v1/me")
                .andExpect(jsonPath("$.subscription.active").value(true))
                .andExpect(jsonPath("$.subscription.platform").value("APPLE"))
                .andExpect(jsonPath("$.subscription.productId").value("karaoke.monthly"))
                .andExpect(jsonPath("$.freeQuota.usedToday").value(0));
    }

    @Test
    void expiredSubscriptionGivesNoAccess() throws Exception {
        Session session = register();
        subscribe(session, Subscription.Status.ACTIVE, Instant.now().minus(Duration.ofDays(1)));
        Song premium = song("Premium", "Artiste", false);

        getAs(session, "/api/v1/songs/" + premium.getId() + "/instrumental-url")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_REQUIRED"));
        getAs(session, "/api/v1/me").andExpect(jsonPath("$.subscription.active").value(false));
    }

    @Test
    void unknownSong() throws Exception {
        getAs(register(), "/api/v1/songs/" + UUID.randomUUID() + "/instrumental-url")
                .andExpect(status().isNotFound());
    }

    private void subscribe(Session session, Subscription.Status status, Instant expiresAt) {
        UUID userId = users.findByEmail(session.email()).orElseThrow().getId();
        subscriptions.save(new Subscription(userId, Subscription.Platform.APPLE, "karaoke.monthly",
                "tx-" + UUID.randomUUID(), status, expiresAt));
    }
}
