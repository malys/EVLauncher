# AGENTS.md — MG4 Simple Launcher

Custom home launcher for the SAIC MG4 head unit (1920×720, landscape). Part of the MG4 app
suite alongside [MG4Control](../MG4Control), [MG4Tasker](../MG4Tasker),
[MG4ABRPUploader](../MG4ABRPUploader) and [MG4 Swipe Launcher](../MG4SwipeLauncher).

Fork of [Tommasov/MG4_Simple_Launcher](https://github.com/Tommasov/MG4_Simple_Launcher) —
see [`LICENSE.md`](LICENSE.md), the licence situation is not the usual one.

Commit author: malys.training@gmail.com

## The one rule that shapes everything

**This app never touches the vehicle.** No `android.car.*` permission, no `sharedUserId`,
no IPC to MG4Control. It is an ordinary app that happens to be `CATEGORY_HOME`. Vehicle
reads and writes belong in MG4Control; automation belongs in MG4Tasker.

The second rule follows from the first: **it must not strand the driver**. A launcher that
crashes on the home path leaves the head unit with no home screen. Every
`PackageManager` lookup, every persisted package name (the app may have been uninstalled)
and every `Intent` resolution is a failure path that has to degrade, not throw.

## Two channels, separated by source set

| | stable | unstable |
|---|---|---|
| Application id | `com.mg4.launcher.simple` | `com.mg4.launcher.simple.unstable` |
| Published by | `v*` tag → `release.yml` | push to `master` → `unstable.yml`, rolling `unstable` tag |
| `INTERNET` | absent from the manifest | declared in `src/unstable/AndroidManifest.xml` |
| `BuildConfig.OTA_ENABLED` | `false` | `true` |
| Updater | `src/stable/.../UpdateHook.kt` — a no-op | `src/unstable/.../{UpdateHook,OtaUpdater,ApkSignature}.kt` |

`UpdateHook` is the flavour-aware seam: `MainActivity` and `AppDrawerActivity` call it
without knowing which channel they were built into, and the stable variant does nothing.
The stable APK does not merely disable the updater — the code is not in it. Keep it that
way: nothing network-shaped in `src/main/`.

The OTA path is `https` only, GitHub host allowlist, and the downloaded APK must be signed
with the same certificate as the running app (`ApkSignature`) or it is deleted. No
`REQUEST_INSTALL_PACKAGES`: the file lands in public Downloads and the user taps it.
`OtaUpdaterTest` covers those gates and runs in CI.

## Permission allowlist is enforced, not documented

`.github/security/permission-allowlist.txt` lists every allowed `uses-permission` with the
reason it exists. `check-permissions.sh` fails the build on anything else, and it runs in
`security.yml`, in `release.yml` before publishing, and locally via `mise run permissions`.
Adding a permission means editing the allowlist **with a justification** in the same PR.

Current surface: `QUERY_ALL_PACKAGES` (drawer), `ACCESS_NETWORK_STATE` +
`ACCESS_WIFI_STATE` (system-info page, read-only, never SSID/BSSID/MAC), and `INTERNET` in
the unstable flavour only.

## Layout of the code

`MainActivity` hosts the `ViewPager2` carousel: `HomeFragment` (a runtime-built grid of
favourite cards, up to `PreferencesManager.MAX_FAVORITES`, plus the trailing "add" tile +
all-apps / shortcut column) and `SystemInfoFragment` (device, memory, storage, network —
all permission-free reads, refreshed only while visible). `AppDrawerActivity` is the full
grid plus the system-apps filter (`FLAG_SYSTEM`) and, on unstable, the update button.
`PreferencesManager` persists the chosen packages as one ordered, hole-free list, and
migrates the old three-slot keys on first read; `AppLauncher`/`AppInfo`/
`AppListAdapter` are the shared launch-and-list plumbing.

## Reference patterns (shared with the suite)

- **Signing**: the MG4 suite platform key, path + passwords from env vars (CI) or
  `gradle.properties` (local); the `signingConfig` is created only if the keystore file
  exists. Never a literal secret in a build file.
- **Security CI**: `.github/workflows/security.yml` — blocking permission-drift gate +
  gitleaks, plus informational SARIF from mobsfscan / semgrep / dependency-check.
- **Theme**: dark Material 3 on the shared `mg4_*` colour and spacing tokens, 72 dp touch
  targets. Dark is imposed, not system-following — glare on a windscreen at night.
- **Language**: English by default (code, comments, commits, docs).

## Build

`mise run build | build-unstable | test | check | permissions | run`. JDK 17, AGP 8.6,
Gradle 8.7, `minSdk 28` / `targetSdk 34`. Emulator: `mise run emulator-setup` then
`emulator-screen` (API 28 at panel geometry) or `emulator-car` (API 33 Automotive); AVDs
are named `mg4simple-*`, per-repo like the sibling projects. `mise run run` starts the app
as an ordinary activity — `mise run set-home` is what makes it the default home.
