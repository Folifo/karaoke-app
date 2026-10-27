package com.karaoke.entitlement;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Compteur quotidien de chansons jouées (table usage_daily). */
@Repository
public class UsageRepository {

    private final JdbcClient jdbc;

    public UsageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Incrémente le compteur du jour seulement s'il reste sous la limite, en une seule requête
     * atomique (pas de dépassement en cas d'appels simultanés).
     *
     * @return {@code true} si la lecture a été comptée, {@code false} si la limite est atteinte
     */
    public boolean incrementIfBelow(UUID userId, LocalDate day, int limit) {
        if (limit <= 0) {
            return false;
        }
        int updated = jdbc.sql("""
                        INSERT INTO usage_daily (user_id, day, songs_played) VALUES (:userId, :day, 1)
                        ON CONFLICT (user_id, day) DO UPDATE SET songs_played = usage_daily.songs_played + 1
                        WHERE usage_daily.songs_played < :limit
                        """)
                .param("userId", userId)
                .param("day", day)
                .param("limit", limit)
                .update();
        return updated == 1;
    }

    public int songsPlayed(UUID userId, LocalDate day) {
        return jdbc.sql("SELECT songs_played FROM usage_daily WHERE user_id = :userId AND day = :day")
                .param("userId", userId)
                .param("day", day)
                .query(Integer.class)
                .optional()
                .orElse(0);
    }
}
