package com.karaoke.song;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public final class SongDtos {

    private SongDtos() {
    }

    public record SongSummary(UUID id, String title, String artist, long durationMs, String language,
            String coverUrl, @JsonProperty("isFree") boolean isFree) {

        static SongSummary of(Song song) {
            return new SongSummary(song.getId(), song.getTitle(), song.getArtist(), song.getDurationMs(),
                    song.getLanguage(), song.getCoverUrl(), song.isFree());
        }
    }

    public record SongDetail(UUID id, String title, String artist, long durationMs, String language,
            String coverUrl, @JsonProperty("isFree") boolean isFree, String lyricsLrc) {

        static SongDetail of(Song song) {
            return new SongDetail(song.getId(), song.getTitle(), song.getArtist(), song.getDurationMs(),
                    song.getLanguage(), song.getCoverUrl(), song.isFree(), song.getLyricsLrc());
        }
    }
}
