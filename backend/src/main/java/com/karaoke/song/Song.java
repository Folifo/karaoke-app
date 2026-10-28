package com.karaoke.song;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    /** Clé de l'objet dans le bucket des instrumentaux. */
    @Column(name = "instrumental_key", nullable = false)
    private String instrumentalKey;

    @Column(name = "lyrics_lrc", nullable = false)
    private String lyricsLrc;

    @Column(nullable = false)
    private String language;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "is_free", nullable = false)
    private boolean free;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Song() {
    }

    public Song(String title, String artist, long durationMs, String instrumentalKey, String lyricsLrc,
            String language, String coverUrl, boolean free) {
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
        this.instrumentalKey = instrumentalKey;
        this.lyricsLrc = lyricsLrc;
        this.language = language;
        this.coverUrl = coverUrl;
        this.free = free;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getInstrumentalKey() {
        return instrumentalKey;
    }

    public String getLyricsLrc() {
        return lyricsLrc;
    }

    public String getLanguage() {
        return language;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public boolean isFree() {
        return free;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
