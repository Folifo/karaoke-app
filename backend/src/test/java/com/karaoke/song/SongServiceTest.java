package com.karaoke.song;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SongServiceTest {

    @Test
    void escapesLikeWildcards() {
        assertThat(SongService.escapeLike("100% _rock_ \\o/")).isEqualTo("100\\% \\_rock\\_ \\\\o/");
        assertThat(SongService.escapeLike("Édith Piaf")).isEqualTo("Édith Piaf");
    }
}
