# Karaoké

Application mobile (iOS/Android) pour chanter sur des instrumentaux avec paroles synchronisées,
s'enregistrer, mixer et partager. Spécification complète : [`CLAUDE.md`](CLAUDE.md).

## Structure

```
├── CLAUDE.md               spécification et ordre de réalisation
├── docker-compose.yml      infra locale : PostgreSQL + MinIO + backend
├── .env.example            variables surchargeables (copier en .env)
├── docs/openapi.yaml       contrat de l'API REST v1 (source de vérité)
├── backend/                Spring Boot 3 / Java 21 — voir backend/README.md
├── mobile/                 Flutter (à partir de l'étape 4)
│   └── plugins/karaoke_audio/
└── .github/workflows/      CI
```

## Démarrage rapide

Prérequis : Docker avec Docker Compose.

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080/api/v1 |
| Swagger UI (contrat) | http://localhost:8080/swagger-ui.html |
| Console MinIO | http://localhost:9001 (minioadmin / minioadmin) |
| PostgreSQL | localhost:5432 (karaoke / karaoke) |

Le backend tourne avec le profil `dev` : secret JWT de développement et 5 chansons de démonstration
(domaine public). Les buckets `instrumentals` et `recordings` sont créés automatiquement, privés.

Essai rapide :

```bash
curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"moi@example.com","password":"motdepasse","displayName":"Moi"}'
# puis, avec l'accessToken reçu :
curl -s localhost:8080/api/v1/songs?query=lune -H "Authorization: Bearer <accessToken>"
```

**Téléphone ou émulateur** : les URLs signées pointent vers `STORAGE_PUBLIC_ENDPOINT`
(par défaut `http://localhost:9000`). Pour l'émulateur Android, mettre `http://10.0.2.2:9000` dans `.env` ;
pour un appareil physique, `http://<IP du poste>:9000`.

## Avancement

| Étape (CLAUDE.md §12) | État |
|---|---|
| 1. Structure, Docker Compose, contrat OpenAPI | fait |
| 2. Backend : authentification, catalogue, stockage | fait |
| 3. Backend : enregistrements, abonnements | contrat écrit, à implémenter |
| 4 à 8 | à faire |
