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

**This app is read-only on the vehicle.** It may present vehicle telemetry, but only from
EVHardware's typed read-only telemetry API. It never writes a vehicle setting, never owns a
property id or vendor transaction, never reopens `CarPropertyManager`, and never uses IPC to
EVProfile. No `sharedUserId` and no setter-capable adapter belong here. Vehicle writes belong
in EVProfile; automation belongs in EVTasker.

RAM, storage, uptime and network state on the existing system-information page are device
reads, not vehicle telemetry. A vehicle-information page may add SOC, range and other
available read-only values, but it must preserve that distinction, render unreadable values
as unavailable rather than zero, and consume the shared EVHardware fallback/provenance rules.

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
`ACCESS_WIFI_STATE` (system-info page, read-only, never SSID/BSSID/MAC), `INTERNET` only for
the user-initiated EVSuite release manager, and `CAR_ENERGY` + `CAR_VENDOR_EXTENSION` for the
read-only vehicle page. The manager uses fixed GitHub repositories and fails closed on URL,
identity or signature. It follows the stable/offline releases of all five suite applications.

The two car permissions are the minimum for the vehicle values the metrics page can show, and
neither permits a write. `CAR_SPEED`, `CAR_EXTERIOR_ENVIRONMENT` and `CONTROL_CAR_CLIMATE`
are held by EVChargePilot and deliberately **not** here. `VehicleBoundaryTest` enforces the
rule in the test suite as well as in the CI gate: it fails the build on a direct vehicle API,
on an EVHardware import outside the read-only telemetry surface, on `sharedUserId`, and on any
car permission the boundary review did not name. Reasoning: `docs/CR-010-vehicle-page.md`.

## Layout of the code

`MainActivity` hosts the `ViewPager2` carousel over two pages: `HomeFragment` (a
runtime-built grid of favourite cards, up to `PreferencesManager.MAX_FAVORITES`, plus the
trailing "add" tile + all-apps / shortcut column) and `MetricsFragment` (a runtime-built grid
of up to `PreferencesManager.MAX_METRICS` cards, head-unit and vehicle metrics in one list,
customised with the same add / replace / remove gestures as the favourites). It merges the old
`SystemInfoFragment` and `VehicleInfoFragment`: system values are permission-free reads
refreshed every 3 s while visible, vehicle values come only through EVHardware's read-only
snapshot at most every 5 s, and the telemetry reader is built only once a vehicle card is
actually on the page. `Metric` is the catalogue (18 head-unit + 25 vehicle entries) — its constant name is the
persisted key, so renaming one drops the card from every driver who chose it.
`MetricPickerActivity` is the full-screen picker: a sectioned grid, not a dialog, and like
`AppDrawerActivity` it writes the choice itself and finishes rather than returning a result.
The non-car permissions this added are `PACKAGE_USAGE_STATS` for the data-usage cards and
`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` for the weather card — the launcher's first
runtime permission, requested only when a weather card is first displayed, with the position
subscription held only while the page is visible.
Reasoning: `docs/CR-011-metrics-page.md`. The home stays at position 0 whatever is added
after it. `AppDrawerActivity` is the full
grid plus the system-apps filter (`FLAG_SYSTEM`).
`PreferencesManager` persists both the chosen packages and the chosen metrics as ordered,
hole-free lists, and migrates the old three-slot favourite keys on first read;
`AppLauncher`/`AppInfo`/`AppListAdapter` are the shared launch-and-list plumbing.

## Reference patterns (shared with the suite)

- **Signing**: the EVSuite platform key, path + passwords from env vars (CI) or
  `gradle.properties` (local); the `signingConfig` is created only if the keystore file
  exists. Never a literal secret in a build file.
- **Security CI**: `.github/workflows/security.yml` — blocking permission-drift gate +
  gitleaks, plus informational SARIF from mobsfscan / semgrep / dependency-check.
- **Theme**: dark Material 3 on the shared `ev_*` colour and spacing tokens, 72 dp touch
  targets. Dark is imposed, not system-following — glare on a windscreen at night.
- **Language**: English by default (code, comments, commits, docs).

## Consuming EVHardware

Submodule at `./EVHardware`; `settings.gradle.kts` includes `EVHardware/lib` as
`:evhardware`. It tracks the HEAD of `master` like every other consumer — never pin an older
commit. The launcher consumes the **read-only telemetry surface only**
(`telemetry.EnergyTelemetryReader`, `telemetry.EnergySnapshot`, `catalog.VehicleEnums`, and
the library's `R` for the shared charging vocabulary); `VehicleBoundaryTest` fails the build
on anything else.

## Build

`mise run build | build-unstable | test | check | permissions | run`. JDK 17, AGP 9.1.1,
Gradle 9.3.1, `compileSdk 36`, `minSdk 28` / `targetSdk 34`. AGP 9 is not cosmetic here: it
provides the built-in Kotlin compilation `EVHardware/lib` relies on, which is why the separate
Kotlin plugin is gone. Emulator: `mise run emulator-setup` then
`emulator-screen` (API 28 at panel geometry) or `emulator-car` (API 33 Automotive); AVDs
are named `mg4simple-*`, per-repo like the sibling projects. `mise run run` starts the app
as an ordinary activity — `mise run set-home` is what makes it the default home.
