package com.karaoke.song;

import com.karaoke.common.ApiException;
import com.karaoke.common.ErrorCode;
import com.karaoke.common.PageResponse;
import com.karaoke.entitlement.EntitlementService;
import com.karaoke.song.SongDtos.SongDetail;
import com.karaoke.song.SongDtos.SongSummary;
import com.karaoke.storage.SignedUrl;
import com.karaoke.storage.StorageService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SongService {

    private static final Sort CATALOG_ORDER = Sort.by("artist", "title", "id");

    private final SongRepository songs;
    private final EntitlementService entitlements;
    private final StorageService storage;

    public SongService(SongRepository songs, EntitlementService entitlements, StorageService storage) {
        this.songs = songs;
        this.entitlements = entitlements;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public PageResponse<SongSummary> search(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, CATALOG_ORDER);
        Page<Song> result = query == null || query.isBlank()
                ? songs.findAll(pageRequest)
                : songs.search("%" + escapeLike(query.strip()) + "%", pageRequest);
        return PageResponse.of(result, SongSummary::of);
    }

    @Transactional(readOnly = true)
    public SongDetail get(UUID id) {
        return SongDetail.of(find(id));
    }

    /** Vérifie les droits (et décompte la lecture gratuite) avant de signer l'URL de l'instrumental. */
    @Transactional
    public SignedUrl instrumentalUrl(UUID userId, UUID songId) {
        Song song = find(songId);
        entitlements.authorizePlay(userId, song);
        return storage.instrumentalDownloadUrl(song.getInstrumentalKey());
    }

    private Song find(UUID id) {
        return songs.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Chanson introuvable."));
    }

    /** Les caractères spéciaux de LIKE saisis par l'utilisateur sont cherchés littéralement. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
