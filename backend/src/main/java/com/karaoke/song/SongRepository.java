package com.karaoke.song;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SongRepository extends JpaRepository<Song, UUID> {

    /** @param pattern motif LIKE déjà échappé (caractère d'échappement : {@code \}) */
    @Query("""
            select s from Song s
            where lower(s.title) like lower(:pattern) escape '\\'
               or lower(s.artist) like lower(:pattern) escape '\\'
            """)
    Page<Song> search(String pattern, Pageable pageable);
}
