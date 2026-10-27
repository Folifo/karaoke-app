package com.karaoke.song;

import com.karaoke.common.CurrentUser;
import com.karaoke.common.PageResponse;
import com.karaoke.song.SongDtos.SongDetail;
import com.karaoke.song.SongDtos.SongSummary;
import com.karaoke.storage.SignedUrl;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping
    public PageResponse<SongSummary> search(
            @RequestParam(required = false) @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return songService.search(query, page, size);
    }

    @GetMapping("/{id}")
    public SongDetail get(@PathVariable UUID id) {
        return songService.get(id);
    }

    @GetMapping("/{id}/instrumental-url")
    public SignedUrl instrumentalUrl(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return songService.instrumentalUrl(CurrentUser.id(jwt), id);
    }
}
