# Contributing to Transdroid 3

All code and design contributions are welcome, though sometimes we need to be strict in
what we can land in the final app, to keep the UX clear and streamlined. All code is
licensed under the GNU GPL v3 (see [COPYING](COPYING)).

- **New client adapters** are only added after explicit agreement, via a GitHub issue,
  that we want to support that client in Transdroid. Please open one before you start.
- **New client features** can be added when all or the majority of clients support them;
  typically we do not want to support a feature that only one client offers.
- **UI changes and additions** ideally update the HTML mockups in
  [`design/mockups/`](design/mockups/), in both the light and the `-dark` variant.

## Project layout

| Module | What it is |
| --- | --- |
| `:protocol` | Pure-JVM Kotlin library: torrent client (daemon) adapters, protocol models, no Android dependencies |
| `:app` | The Android app: Jetpack Compose UI, ViewModels, encrypted settings storage |

Build and test with:

```
./gradlew :protocol:test                 # protocol unit tests (run these first, they're fast)
./gradlew :app:testFullDebugUnitTest     # app unit tests
./gradlew :app:lintFullDebug             # Android lint
./gradlew :app:assembleFullDebug         # installable debug APK
```

CI runs all of the above on every push and pull request.

## Adding a torrent client adapter

Only start on an adapter once a GitHub issue has agreed the client should be added (see
above). Each adapter lives in its own package under
`protocol/src/main/kotlin/org/transdroid/protocol/` and consists of:

1. **An implementation of `DaemonAdapter`** that maps the client onto the normalized
   `Torrent`/`TorrentFile` models and throws the matching `DaemonException` subtype so
   the UI can give targeted feedback.
2. **A `DaemonType` entry** in `Models.kt` plus a branch in `DaemonAdapterFactory.create`.
3. **Fixture-based unit tests** against recorded responses in
   `protocol/src/test/resources/<client>/` (see `TransmissionAdapterTest`), covering the
   status mappings, authentication failure and any protocol quirks.
4. **UI wiring** in the app module; the compiler's exhaustive `when`s point to every spot.

Transmission (JSON-RPC), qBittorrent (REST + cookie auth), rTorrent (XML-RPC) and Deluge
(web JSON-RPC) cover most protocol shapes a new client is likely to need.

## Other extension points

- **Search providers** implement `org.transdroid.protocol.search.SearchProvider`. The
  shipped `TorznabProvider` covers Jackett/Prowlarr; a provider for another API follows
  the same pattern (pure JVM, fixture-tested, results expose a `torrentUrl` a daemon
  adapter can consume).
- **Feed handling** lives in `org.transdroid.protocol.rss.RssFetcher`; extend it there
  (with fixtures) rather than in the UI layer if a tracker's dialect needs special-casing.

Guidelines:

- Keep adapters free of Android imports; the `:protocol` module must stay pure JVM.
- Always parse responses (JSON, XML, …) with a streaming implementation rather than
  reading the body into an in-memory string first.
- Never log or embed credentials, hosts or torrent names in exception messages beyond
  what the UI needs.
- Support current client versions first; only add legacy fallbacks that you can test.

## Code style

Standard Kotlin style (`kotlin.code.style=official`). Match the surrounding code, prefer
small focused files, and let the compiler's exhaustiveness checks do the wiring work.

## Releasing (maintainers)

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add the changelog, max 500 characters (Play's limit), as
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, and the same entries to
   `ui/settings/Changelog.kt` for the in-app "What's new".
3. If the changelog mentions features Transdrone (the `lite` flavor) doesn't have, like RSS,
   search or the update check, also add
   `app/src/lite/fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. Play uses it
   for Transdrone instead of the shared one.
4. Push a `v<versionName>` tag. The Release workflow then tests, attaches the signed `full`
   APK to a GitHub release as `transdroid.apk`, and uploads the signed `lite` APK to Google
   Play. A suffixed version (`-beta1`, `-rc1`) becomes a GitHub pre-release and goes to Play's
   open testing (beta) track; otherwise it is a full release on Play's production track.

The workflow uses these repository secrets:

| Secret | Used for |
| --- | --- |
| `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Signing both APKs |
| `PLAY_SERVICE_ACCOUNT_JSON` | The Google Play upload; a Google Cloud service account key with release permissions for Transdrone in the Play Console. Without it the Play upload is skipped. |
