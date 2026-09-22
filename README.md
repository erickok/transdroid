Transdroid 3
============

[www.transdroid.org](https://www.transdroid.org/) - [transdroid@2312.nl](mailto:transdroid@2312.nl)

Manage torrents from your Android device.

> **Branch notice** — `master` now contains **Transdroid 3** (currently in alpha), a
> ground-up rewrite following the [Transdroid 3 plan](transdroid3_plan.md). The final
> Transdroid 2 code is preserved at the [`transdroid2-final`](../../tree/transdroid2-final)
> tag and receives no further development.

What has been done so far
=========================

This branch replaces the entire Transdroid 2 code base (Java, Apache HTTP legacy,
AndroidAnnotations, ORMLite, XML layouts) with a new app built from scratch. Status by
area:

* **Project foundations** — new Gradle Kotlin DSL build with a version catalog, minSdk 29
  / targetSdk 36, the `full`/`lite` product flavor split carried over from v2, and a
  GitHub Actions CI pipeline that runs all tests and lint on every push and uploads an
  installable debug APK as a build artifact (see the
  [Actions tab](../../actions), bottom of the latest run, artifact
  `transdroid-full-debug`).
* **Protocol layer** (plan Phases 1 and 4) — a new pure-JVM `:protocol` module defines one
  normalized `DaemonAdapter` interface plus torrent/file models, with working adapters for
  **Transmission** (JSON-RPC, 409 session-id handshake, basic auth), **qBittorrent**
  (Web API v2 cookie auth, compatible with both 4.x `pause/resume` and 5.x `stop/start`
  endpoints), **rTorrent** (XML-RPC over HTTP with a hardened minimal codec) and
  **Deluge** (Web UI JSON-RPC with session re-authentication). All protocol behavior is
  unit-tested against recorded fixture responses; no emulator or real daemon needed.
* **App UI** (plan Phase 2 + Phase 3) — Jetpack Compose with Material 3 in the classic
  grey-green Transdroid identity: torrent list with automatic 5-second refresh, status
  filters and pull-to-refresh; torrent details with start/pause/remove (optionally
  deleting data) and per-file progress; add-torrent by magnet link or URL, including
  handling magnet links opened from other apps; server settings with a connection test
  button. On tablets/foldables the list and details show side by side.
* **Security** — server credentials are stored AES-256-GCM encrypted using a
  hardware-backed Android Keystore key and are excluded from cloud backup and device
  transfer; passwords never leave the device.
* **Verified** — protocol and app unit tests pass, Android lint is clean, and debug plus
  minified R8 release builds succeed for both flavors.

* **Search** (plan Phase 5) — in-app torrent search built as an extension point: a
  `SearchProvider` interface in the protocol module with a **Torznab** implementation, so
  one Jackett or Prowlarr endpoint unlocks hundreds of indexers. Providers (endpoint +
  API key) are stored encrypted; results sort by seeders and send straight to the active
  server. Gated to the `full` flavor via `search_available`.
* **RSS feeds** — subscribe to torrent RSS/Atom feeds, see new items highlighted, and
  send entries to your client with one tap. Feed URLs (which often embed private
  passkeys) live in the same encrypted store as server credentials. `full` flavor only.
* **Notifications** — an opt-in background check (WorkManager, ~15 min interval) that
  notifies when torrents finish, with proper Android 13+ notification-permission
  handling.
* **Home screen widget** — a Glance widget with the active server's torrent counts and
  total speeds, refreshed by both foreground use and the background check.
* **More ways to add** — open or share magnet links, open `.torrent` files from file
  managers and browsers, pick a `.torrent` file in-app, or paste a URL. Plus torrent
  list sorting (date added, name, download speed, ratio).
* **Self-signed HTTPS** — seedboxes and home servers with self-signed certificates are
  supported securely: the app shows the server's certificate fingerprint and, once
  accepted, pins exactly that certificate for that server (no "trust everything" toggle).
* **Local network discovery** — adding a server automatically scans your Wi-Fi/Ethernet
  subnet for Transmission, qBittorrent and Deluge daemons and offers what it finds with
  one tap to fill in the connection details.
* **Labels and file priorities** — labels/categories from all four clients appear as
  filter chips and in the details view, and per-file download priorities can be changed
  by tapping a file.
* **Release pipeline** — tag-triggered GitHub Releases with signed APKs (signing via
  repository secrets, unsigned fallback), `dependenciesInfo` and VCS metadata stripped
  from APKs per F-Droid reproducible-build requirements, plus fastlane store metadata.
* **Contributor docs** (plan Phase 6) — [CONTRIBUTING.md](CONTRIBUTING.md) documents the
  build, the module layout and the adapter interface as the extension point for adding
  more torrent clients.

Not yet done: F-Droid inclusion (metadata is ready; store screenshots and the fdroiddata
merge request remain), translations, and the remaining Transdroid 2 client adapters. See
the [roadmap](transdroid3_plan.md#roadmap).

About the rewrite
=================

Transdroid 3 is a fresh start on the same mission: manage your torrents from your Android
device. The rewrite replaces the legacy Apache HTTP networking, AndroidAnnotations, ORMLite
and XML layouts of Transdroid 2 with a modern, testable stack:

* **Kotlin** everywhere, with coroutines and Flow for concurrency
* **Jetpack Compose** with Material 3, themed with the classic Transdroid grey-green identity
* **OkHttp** + kotlinx.serialization for the daemon protocols
* A pure-JVM **`:protocol` module** containing all client adapters, unit-tested against
  recorded fixture responses — no Android dependency, no emulator needed
* **Encrypted server profiles**: connection credentials are stored AES-256-GCM encrypted
  with a hardware-backed Android Keystore key, and excluded from device backups
* **DataStore** for preferences; no SQL database, no ORM
* minSdk 29 (Android 10) and up, edge-to-edge, adaptive two-pane layout on large screens

Supported clients
=================

| Client | Status |
| --- | --- |
| Transmission | ✅ Supported (RPC over JSON, session-id handshake, basic auth) |
| qBittorrent | ✅ Supported (Web API v2, works with qBittorrent 4.1+ and 5.x) |
| rTorrent | ✅ Supported (XML-RPC over HTTP, e.g. /RPC2 behind a web server or ruTorrent) |
| Deluge | ✅ Supported (Web UI JSON-RPC, Deluge 1.3 and 2.x) |

The remaining Transdroid 2 adapters are out of scope for the initial v3 release; community
contributions can revive them once the adapter interface stabilizes — see
[CONTRIBUTING.md](CONTRIBUTING.md) for how to add an adapter.

Building
========

```
./gradlew :protocol:test        # protocol layer unit tests (pure JVM)
./gradlew :app:assembleFullDebug
```

Two product flavors exist, carried over from Transdroid 2: `full` (transdroid.org, F-Droid)
and `lite` (Google Play). Feature differences are driven purely by flavor resources.

Contributions
=============

Code and design contributions are very welcome.
Please note that all code will be licensed in GNU GPLv3.

Developed By
============

Designed and developed by [Eric Kok](mailto:eric@2312.nl) of [2312 development](https://2312.nl/).
Contributions by various others (see commit log).

License
=======

    Copyright 2010-2026 Eric Kok et al.

    Transdroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Transdroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Transdroid.  If not, see <https://www.gnu.org/licenses/>.

Libraries used in the project:
*  [Android Jetpack (AndroidX)](https://developer.android.com/jetpack), including Compose —
   The Android Open Source Project, Apache License 2.0
*  [Kotlin and kotlinx libraries](https://kotlinlang.org/) —
   JetBrains and contributors, Apache License 2.0
*  [OkHttp](https://square.github.io/okhttp/) —
   Square, Inc., Apache License 2.0
*  [MaxMind DB Reader for Java](https://github.com/maxmind/MaxMind-DB-Reader-java) —
   MaxMind, Inc., Apache License 2.0

Peer country flags can optionally use the [IP66](https://ip66.dev/) GeoIP database. It is not
bundled: it is only downloaded when the user asks for it in Settings, and looked up on-device.
