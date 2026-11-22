# Star Flix (Compose + Media3 + TMDB)

1. Get your free "API Read Access Token" at themoviedb.org (Settings > API).
2. Push this folder to a GitHub repo.
3. Repo > Settings > Secrets and variables > Actions > New secret: `TMDB_API_KEY`.
4. Actions tab > "Build APK" > Run workflow. Download the APK artifact.

Local build: `gradle assembleDebug -PTMDB_API_KEY=yourkey`.
Playback uses `DEMO_STREAM` in MainActivity.kt. Point it at streams you have rights to.

## Adding movies & series
Edit `app/src/main/assets/catalog.json` (bundled), or host the same file online and set the repo
variable `CATALOG_URL` (Settings > Secrets and variables > Actions > Variables) so the app loads it live.
`tmdb_id` is the number in the TMDB URL (themoviedb.org/movie/10378 -> 10378). Use streams you have rights to.
