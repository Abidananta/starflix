# Star Flix (Compose + Media3 + TMDB)

1. Get your free "API Read Access Token" at themoviedb.org (Settings > API).
2. Push this folder to a GitHub repo.
3. Repo > Settings > Secrets and variables > Actions > New secret: `TMDB_API_KEY`.
4. Actions tab > "Build APK" > Run workflow. Download the APK artifact.

Local build: `gradle assembleDebug -PTMDB_API_KEY=yourkey`.
Playback uses `DEMO_STREAM` in MainActivity.kt. Point it at streams you have rights to.
