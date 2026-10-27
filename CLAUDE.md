## 1. Vision

Application mobile iOS et Android qui permet de chanter sur des instrumentaux avec les paroles synchronisées, d'enregistrer sa voix, de la mixer avec la musique et de partager le résultat.

**Public** : particuliers qui chantent seuls ou entre amis, sur téléphone.
**Modèle économique** : abonnement mensuel/annuel via App Store et Google Play, avec un accès gratuit limité.
**Langue de l'interface** : français (anglais en option plus tard).

## 2. Périmètre du MVP

### Inclus
1. Création de compte, connexion, déconnexion.
2. Catalogue de chansons avec recherche (titre, artiste).
3. Lecteur karaoké : instrumental + paroles synchronisées défilantes.
4. Enregistrement de la voix pendant la lecture, avec faible latence.
5. Mixage voix + instrumental, réécoute, export en fichier audio.
6. Liste « Mes enregistrements » (écouter, partager, supprimer).
7. Abonnement via achats intégrés, avec vérification côté serveur.
8. Accès gratuit limité (exemple : 3 chansons par jour, à confirmer).

### Exclus du MVP (versions suivantes)
Duos en ligne, notation de la justesse, effets de voix (reverb, autotune), mode vidéo, réseau social et classements, mode hors ligne complet.

## 3. Architecture

```
┌────────────────────────┐        HTTPS / JSON        ┌─────────────────────────┐
│  App Flutter (Dart)    │ ─────────────────────────► │  Backend Spring Boot    │
│  iOS + Android         │ ◄───────────────────────── │  (Java 21)              │
│                        │                            │                         │
│  Plugin audio natif    │                            │  PostgreSQL             │
│  Kotlin/Oboe | Swift   │   URLs signées             │  Stockage objet (S3)    │
└──────────┬─────────────┘ ◄────────────────────────► │  CDN                    │
           │                                          └─────────────────────────┘
           └─ Achats intégrés (App Store / Google Play) ──► validation côté backend
```

### Choix techniques
| Couche | Technologie |
|---|---|
| Mobile | Flutter (Dart), Riverpod pour l'état, go_router pour la navigation |
| Audio natif Android | Kotlin + Oboe (faible latence) |
| Audio natif iOS | Swift + AVAudioEngine |
| Pont Flutter ↔ natif | Plugin Flutter local avec platform channels |
| Backend | Java 21, Spring Boot 3, Spring Security, Spring Data JPA |
| Base de données | PostgreSQL, migrations Flyway |
| Stockage | S3 ou compatible (MinIO en local), URLs signées |
| Paiement | package `in_app_purchase`, validation des reçus côté backend |
| Authentification | JWT (access + refresh token) |
| Documentation API | OpenAPI / Swagger |
| Infra locale | Docker Compose (PostgreSQL + MinIO + backend) |
| CI | GitHub Actions |

## 4. Structure du dépôt

```
/
├── CLAUDE.md
├── docker-compose.yml
├── docs/
│   └── openapi.yaml
├── backend/            # Spring Boot
├── mobile/             # Flutter
│   ├── lib/
│   └── plugins/
│       └── karaoke_audio/   # plugin audio natif (android/ et ios/)
└── .github/workflows/
```

## 5. Modèle de données (PostgreSQL)

- **users** : id, email (unique), password_hash, display_name, created_at
- **songs** : id, title, artist, duration_ms, instrumental_key (clé de stockage), lyrics_lrc (texte LRC), language, cover_url, is_free, created_at
- **recordings** : id, user_id, song_id, voice_key, mixed_key, duration_ms, created_at
- **subscriptions** : id, user_id, platform (APPLE | GOOGLE), product_id, original_transaction_id, status (ACTIVE | EXPIRED | CANCELED), expires_at, updated_at
- **usage_daily** : user_id, day, songs_played (pour la limite gratuite)

## 6. API REST (v1)

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Inscription |
| POST | `/api/v1/auth/login` | Connexion, retourne access + refresh token |
| POST | `/api/v1/auth/refresh` | Renouvelle le token |
| GET | `/api/v1/songs?query=&page=` | Catalogue et recherche paginée |
| GET | `/api/v1/songs/{id}` | Détail, paroles LRC |
| GET | `/api/v1/songs/{id}/instrumental-url` | URL signée de l'instrumental (vérifie l'abonnement ou la limite gratuite) |
| POST | `/api/v1/recordings/upload-url` | URL signée pour envoyer un enregistrement |
| POST | `/api/v1/recordings` | Enregistre les métadonnées d'un enregistrement |
| GET | `/api/v1/recordings` | Mes enregistrements |
| DELETE | `/api/v1/recordings/{id}` | Supprime un enregistrement |
| POST | `/api/v1/subscriptions/verify` | Vérifie un reçu Apple/Google et active l'abonnement |
| GET | `/api/v1/me` | Profil et statut d'abonnement |

Règles : mots de passe hachés (BCrypt), validation des entrées, codes d'erreur cohérents, accès aux fichiers uniquement par URL signée à durée courte.

## 7. Application Flutter

### Écrans
1. **Connexion / inscription**
2. **Accueil / catalogue** : liste paginée, barre de recherche
3. **Détail d'une chanson** : pochette, titre, bouton « Chanter »
4. **Lecteur karaoké** : paroles défilantes, ligne courante mise en évidence, bouton enregistrer/pause/stop, indicateur de niveau micro, demande de permission micro, conseil « utilise des écouteurs »
5. **Réécoute et mixage** : curseurs de volume voix et musique, bouton enregistrer/partager
6. **Mes enregistrements**
7. **Abonnement** : offres, achat, restauration des achats
8. **Profil / réglages**

### Fonctionnalités clés
- Parseur LRC (`[mm:ss.xx] texte`) et synchronisation avec la position de lecture.
- Gestion des états de chargement, d'erreur et d'absence de réseau.
- Stockage sécurisé des tokens (flutter_secure_storage).

## 8. Plugin audio natif (`karaoke_audio`)

**Interface Dart exposée :**
- `prepare(instrumentalPath)`
- `startSession()` : lance en même temps la lecture de l'instrumental et l'enregistrement du micro
- `stopSession()` : retourne le chemin du fichier voix et la latence estimée
- `positionStream` : position de lecture en millisecondes
- `levelStream` : niveau du micro
- `mix(voicePath, instrumentalPath, voiceGain, musicGain, offsetMs)` : produit un fichier mixé (AAC/M4A)

**Android** : Kotlin, Oboe (flux d'entrée et de sortie à faible latence), gestion des permissions et du focus audio.
**iOS** : Swift, AVAudioEngine, session `playAndRecord`, gestion des interruptions (appel, casque débranché).

**Exigence** : compensation de la latence (décalage voix/musique) lors du mixage, avec un réglage manuel de l'offset en secours.

## 9. Paiement

- Deux produits : abonnement mensuel et annuel (identifiants à créer dans App Store Connect et Google Play Console).
- Flux : achat dans l'app → envoi du reçu au backend → vérification auprès d'Apple (App Store Server API) ou de Google (Play Developer API) → mise à jour de la table `subscriptions`.
- Le backend est la source de vérité sur les droits d'accès.
- Bouton « Restaurer mes achats » obligatoire sur iOS.

## 10. Stockage

- Buckets séparés : `instrumentals` (lecture seule pour les utilisateurs, via URL signée) et `recordings` (envoi et lecture par l'utilisateur propriétaire).
- URLs signées valables quelques minutes.
- CDN devant les instrumentaux en production.

## 11. Qualité et livraison

- Backend : tests unitaires (JUnit) et d'intégration (Testcontainers pour PostgreSQL).
- Mobile : tests unitaires et tests de widgets.
- CI : build et tests à chaque pull request.
- Livraison : builds Android (AAB) et iOS (IPA), fiches des stores en français.

## 12. Ordre de réalisation demandé

1. Structure du dépôt, Docker Compose, contrat OpenAPI.
2. Backend : authentification, puis catalogue, puis stockage.
3. Backend : enregistrements, puis abonnements.
4. Flutter : structure, thème, navigation, écrans de connexion et catalogue.
5. Plugin audio Android, puis iOS, avec un écran de test de latence.
6. Flutter : lecteur karaoké, paroles synchronisées, enregistrement, mixage.
7. Flutter : mes enregistrements, abonnement et achats intégrés.
8. Tests, CI, préparation des builds.

**À chaque étape** : code, tests, instructions pour lancer, et liste de ce qui reste à valider manuellement.
