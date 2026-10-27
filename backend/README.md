# Backend

Spring Boot 3.5, Java 21, PostgreSQL (Flyway), stockage S3/MinIO par URLs signées, JWT.

## Lancer

**Avec Docker Compose** (depuis la racine) : `docker compose up --build`.

**En local, hors Docker** (Java 21 requis) :

```bash
docker compose up -d postgres minio minio-init   # dépendances seulement
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test     # tests unitaires (rapides, sans Docker)
./mvnw verify   # + tests d'intégration *IT (Testcontainers : Docker doit tourner)
```

Les tests d'intégration démarrent un PostgreSQL éphémère et appellent l'API de bout en bout
(MockMvc + vrais JWT). `OpenApiContractIT` échoue si une route implémentée manque dans `docs/openapi.yaml`.

## Organisation du code

| Paquet | Rôle |
|---|---|
| `auth` | inscription, connexion, refresh (rotation), déconnexion |
| `user` | entité utilisateur, `GET /me` |
| `song` | catalogue, recherche, détail, URL de l'instrumental |
| `entitlement` | droits d'accès : abonnement actif ou limite gratuite quotidienne |
| `subscription` | entité abonnement (alimentée à l'étape 3) |
| `storage` | signature des URLs S3 |
| `common` | erreurs problem+json, pagination, utilitaires |
| `config` | sécurité (JWT HS256, BCrypt), horloge |

## Configuration

Variables d'environnement principales (valeurs par défaut dans `application.yml`) :

| Variable | Rôle |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | connexion PostgreSQL |
| `JWT_SECRET` | secret HMAC des access tokens, ≥ 32 caractères. **Obligatoire hors profil `dev`** (l'application refuse de démarrer sans) |
| `STORAGE_PUBLIC_ENDPOINT` | URL du stockage vue par le téléphone ; les URLs signées pointent dessus |
| `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`, `STORAGE_REGION` | identifiants S3/MinIO |
| `STORAGE_PATH_STYLE` | `true` pour MinIO, `false` possible pour AWS S3 |

Autres réglages (`karaoke.*` dans `application.yml`) : durée des tokens (15 min / 30 jours),
durée des URLs signées (5 min), limite gratuite (3 chansons/jour, fuseau Europe/Paris).

## Comportements à connaître

- **Tokens** : access token JWT de 15 min ; refresh token opaque de 30 jours, stocké haché (SHA-256),
  à usage unique. Rejouer un refresh token déjà utilisé ferme toutes les sessions de l'utilisateur.
  L'application mobile doit donc **sérialiser les renouvellements** (un seul `/auth/refresh` à la fois).
- **Droits sur les instrumentaux** : abonné actif → tout le catalogue ; sinon chansons `isFree`
  uniquement, et chaque URL délivrée consomme une lecture du quota du jour. L'application doit
  réutiliser l'URL tant qu'elle est valable plutôt que la redemander.
- **Erreurs** : format RFC 9457 avec un champ `code` stable (liste dans `docs/openapi.yaml`).
- **Données de démo** (profil `dev`) : `src/main/resources/db/dev/R__dev_seed_songs.sql`.
  Les fichiers audio ne sont pas fournis : déposer un fichier `.m4a` dans le bucket `instrumentals`
  sous la clé indiquée (ex. `demo/au-clair-de-la-lune.m4a`) via la console MinIO
  (http://localhost:9001). Pour générer un son de test : `ffmpeg -f lavfi -i "sine=frequency=440:duration=32" -c:a aac test.m4a`.
