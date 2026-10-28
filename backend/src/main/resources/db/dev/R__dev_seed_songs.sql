-- Données de démonstration (profil dev uniquement) : chansons traditionnelles du domaine public.
-- Les fichiers instrumentaux correspondants doivent être déposés dans le bucket « instrumentals »
-- sous les clés indiquées (voir backend/README.md).

INSERT INTO songs (id, title, artist, duration_ms, instrumental_key, lyrics_lrc, language, cover_url, is_free)
VALUES
('00000000-0000-0000-0000-000000000001', 'Au clair de la lune', 'Traditionnel', 32000,
 'demo/au-clair-de-la-lune.m4a', $lrc$[ti:Au clair de la lune]
[ar:Traditionnel]
[00:05.00] Au clair de la lune
[00:08.00] Mon ami Pierrot
[00:11.00] Prête-moi ta plume
[00:14.00] Pour écrire un mot
[00:17.00] Ma chandelle est morte
[00:20.00] Je n'ai plus de feu
[00:23.00] Ouvre-moi ta porte
[00:26.00] Pour l'amour de Dieu
$lrc$, 'fr', NULL, TRUE),

('00000000-0000-0000-0000-000000000002', 'Frère Jacques', 'Traditionnel', 24000,
 'demo/frere-jacques.m4a', $lrc$[ti:Frère Jacques]
[ar:Traditionnel]
[00:04.00] Frère Jacques, frère Jacques
[00:08.00] Dormez-vous ? Dormez-vous ?
[00:12.00] Sonnez les matines, sonnez les matines
[00:16.00] Ding, dang, dong ! Ding, dang, dong !
$lrc$, 'fr', NULL, TRUE),

('00000000-0000-0000-0000-000000000003', 'Sur le pont d''Avignon', 'Traditionnel', 28000,
 'demo/sur-le-pont-d-avignon.m4a', $lrc$[ti:Sur le pont d'Avignon]
[ar:Traditionnel]
[00:04.00] Sur le pont d'Avignon
[00:07.50] L'on y danse, l'on y danse
[00:11.00] Sur le pont d'Avignon
[00:14.50] L'on y danse tous en rond
$lrc$, 'fr', NULL, TRUE),

('00000000-0000-0000-0000-000000000004', 'Alouette', 'Traditionnel', 30000,
 'demo/alouette.m4a', $lrc$[ti:Alouette]
[ar:Traditionnel]
[00:03.00] Alouette, gentille alouette
[00:07.00] Alouette, je te plumerai
[00:11.00] Je te plumerai la tête
[00:14.00] Je te plumerai la tête
[00:17.00] Et la tête, et la tête
[00:20.00] Alouette, alouette
$lrc$, 'fr', NULL, FALSE),

('00000000-0000-0000-0000-000000000005', 'Il était un petit navire', 'Traditionnel', 30000,
 'demo/il-etait-un-petit-navire.m4a', $lrc$[ti:Il était un petit navire]
[ar:Traditionnel]
[00:04.00] Il était un petit navire
[00:08.00] Il était un petit navire
[00:12.00] Qui n'avait ja-ja-jamais navigué
[00:17.00] Qui n'avait ja-ja-jamais navigué
[00:22.00] Ohé ! Ohé !
$lrc$, 'fr', NULL, FALSE)

ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    artist = EXCLUDED.artist,
    duration_ms = EXCLUDED.duration_ms,
    instrumental_key = EXCLUDED.instrumental_key,
    lyrics_lrc = EXCLUDED.lyrics_lrc,
    language = EXCLUDED.language,
    cover_url = EXCLUDED.cover_url,
    is_free = EXCLUDED.is_free;
