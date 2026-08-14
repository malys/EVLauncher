# AGENTS.md — EVLauncher

Custom home launcher for the SAIC MG4 head unit (1920×720, landscape). Part of
**EVSuite** alongside [EVProfile](../EVProfile), [EVTasker](../EVTasker),
[EVABRPUploader](../EVABRPUploader) and [EVSwipe](../EVSwipe).

The workspace `AGENTS.md` and normative workspace `DESIGN.md` apply; this file contains
only launcher-specific additions.

Fork of [Tommasov/EV_Simple_Launcher](https://github.com/Tommasov/EV_Simple_Launcher) —
see [`LICENSE.md`](LICENSE.md), the licence situation is not the usual one.

Commit author: malys.training@gmail.com

## The one rule that shapes everything

**This app never touches the vehicle.** No `android.car.*` permission, no `sharedUserId`,
no IPC to EVProfile. It is an ordinary app that happens to be `CATEGORY_HOME`. Vehicle
reads and writes belong in EVProfile; automation belongs in EVTasker.

The second rule follows from the first: **it must not strand the driver**. A launcher that
crashes on the home path leaves the head unit with no home screen. Every
`PackageManager` lookup, every persisted package name (the app may have been uninstalled)
and every `Intent` resolution is a failure path that has to degrade, not throw.

## Two channels, separated by source set

| | stable | unstable |
|---|---|---|
| Application id | `com.evsuite.launcher` | `com.evsuite.launcher.unstable` |
| Published by | `v*` tag → `release.yml` | push to `master` → `unstable.yml`, rolling `unstable` tag |
| `INTERNET` | manual EVSuite checks only | manual EVSuite checks only |
| Self-updater | absent | absent |

Neither channel checks or installs updates automatically. The EVSuite screen checks only
after an explicit Refresh, shows release notes, and downloads only after confirmation.
It verifies the fixed package and suite certificate, exports through Android's document
picker, and deletes every private temporary APK. Installation remains a separate manual
action in Files. Neither channel uses `sharedUserId` or installer permissions.

## Permission allowlist is enforced, not documented

`.github/security/permission-allowlist.txt` lists every allowed `uses-permission` with the
reason it exists. `check-permissions.sh` fails the build on anything else, and it runs in
`security.yml`, in `release.yml` before publishing, and locally via `mise run permissions`.
Adding a permission means editing the allowlist **with a justification** in the same PR.

Current surface: `QUERY_ALL_PACKAGES` (drawer), `ACCESS_NETWORK_STATE` +
`ACCESS_WIFI_STATE` (system-info page, read-only, never SSID/BSSID/MAC),
and `INTERNET` only for the user-initiated EVSuite release manager. The manager
uses fixed GitHub repositories and fails closed on URL, identity or signature.
It follows the stable/offline releases of all five suite applications.

## Layout of the code

`MainActivity` hosts the `ViewPager2` carousel: `HomeFragment` (a runtime-built grid of
favourite cards, up to `PreferencesManager.MAX_FAVORITES`, plus the trailing "add" tile +
all-apps / shortcut column) and `SystemInfoFragment` (device, memory, storage, network —
all permission-free reads, refreshed only while visible). `AppDrawerActivity` is the full
grid plus the system-apps filter (`FLAG_SYSTEM`).
`PreferencesManager` persists the chosen packages as one ordered, hole-free list, and
migrates the old three-slot keys on first read; `AppLauncher`/`AppInfo`/
`AppListAdapter` are the shared launch-and-list plumbing.

## Reference patterns (shared with the suite)

- **Signing**: the EVSuite platform key, path + passwords from env vars (CI) or
  `gradle.properties` (local); the `signingConfig` is created only if the keystore file
  exists. Never a literal secret in a build file.
- **Security CI**: `.github/workflows/security.yml` — blocking permission-drift gate +
  gitleaks, plus informational SARIF from mobsfscan / semgrep / dependency-check.
- **Theme**: dark Material 3 on the shared `ev_*` colour and spacing tokens, 72 dp touch
  targets. Dark is imposed, not system-following — glare on a windscreen at night.
- **Language**: English by default (code, comments, commits, docs).

## Build

`mise run build | build-unstable | test | check | permissions | run`. JDK 17, AGP 8.6,
Gradle 8.7, `minSdk 28` / `targetSdk 34`. Emulator: `mise run emulator-setup` then
`emulator-screen` (API 28 at panel geometry) or `emulator-car` (API 33 Automotive); AVDs
are named `mg4simple-*`, per-repo like the sibling projects. `mise run run` starts the app
as an ordinary activity — `mise run set-home` is what makes it the default home.
