# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.4.1] - 2026-09-17

### Changed

- Updated EVHardware to the shared `master` catalogue and telemetry implementation.

## [2.4.0] - 2026-09-07

### Removed

- **Twelve vehicle metrics that could never answer on this car**: speed, cabin temperature,
  odometer, charge port, the four tyre pressures, and battery power / energy / capacity /
  temperature. Each is a standard AAOS property behind a car permission this launcher does not
  hold — and, for the `dangerous` ones it does declare, does not request at runtime either, so
  a manifest line was never a grant. They were offered on the argument that a dash saying
  *unavailable on this car* is an honest answer; on the vehicle it is noise, since nothing in
  the picker told the driver which entries can never fill in. EVTasker's diagnostic reports the
  same signals unreadable there, for the same reason. Reasoning:
  `docs/CR-011-metrics-page.md`.
- The charging card's **port-flag caption**. It read `EV_CHARGE_PORT_CONNECTED`, which needs
  `CAR_ENERGY_PORTS` — held by no app in the suite — so the flag was never once non-null and
  the caption always fell back.

### Changed

- The picker now offers **thirty-one metrics**: the eighteen head-unit ones, unchanged, and
  thirteen vehicle ones that answer through the SAIC vendor services — charge, range, charging,
  outside temperature, the eight climate signals — plus the gear position, which EVHardware
  reads through the vendor condition manager rather than `CarPropertyManager`. None of them
  needs a car permission, which is why they work.
- A metric card a driver had already chosen and that this build dropped is skipped on the next
  page build, as any unknown key already was. Nothing else on the page changes.

Car permissions are **unchanged**: still `CAR_ENERGY` and `CAR_VENDOR_EXTENSION`, read-only.

## [2.3.0] - 2026-09-04

### Changed

- The system-information page and the vehicle page are now **one customisable metrics page**
  (`MetricsFragment`). Cards are added, replaced and removed with the same gestures as the
  favourite apps: tap the trailing **+** tile, long-press a card. Up to twelve cards over one
  to three rows. A fresh install shows the same seven values the two fixed pages showed.
  The carousel is two pages instead of three. Reasoning: `docs/CR-011-metrics-page.md`.

### Added

- **Forty-three metrics in the picker**, in two sections. Head unit: device, chipset, CPU
  load, memory, storage, network, IP address, data used today / this month / since boot,
  date, time, weather, uptime, Android version, kernel, screen, launcher version. Vehicle:
  every value of EVHardware's read-only snapshot — charge, range, charging, charge port,
  speed, battery power / energy /
  capacity / temperature, outside and cabin temperature, odometer, gear, the eight climate
  signals and the four tyre pressures.
- The metric picker is a **full screen** (`MetricPickerActivity`) with a sectioned grid of
  touch-target-sized cells, replacing the list dialog the first cut used — forty entries in a
  dialog is the smallest touch target in the app.
- The **weather** card, from the head unit's own weather service. It needs a position, so
  `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` are declared and requested at runtime the
  first time a weather card is displayed — never at launch. The position subscription is held
  only while the page is visible and only while a weather card is on it. Without the
  permission the card reads `—` and says so.
- `android.permission.PACKAGE_USAGE_STATS` for the data-usage cards: read-only, device-wide,
  not a car permission. Car permissions are unchanged; vehicle signals behind one the launcher
  deliberately does not hold still read as `—` with *unavailable on this car*.

## [2.2.0] - 2026-08-29

### Added

- **EVChargePilot appears in the suite manager.** The read-only energy dashboard joins the
  fixed catalogue of suite applications, so the manager reports whether it is installed, which
  version, and offers the stable APK from its own GitHub releases. The catalogue stays an
  allowlist: adding an application is a code change here, never something a downloaded
  manifest can do.

## [2.1.0] - 2026-08-29

### Added

- **A third home page, showing what the car itself is doing.** Swipe once more past the system
  information and the launcher shows state of charge, remaining range and charging state. It is
  a page of its own rather than three more cards beside the RAM and storage figures for two
  reasons: seven cards across the panel leaves about 253 dp each, which ellipsizes exactly the
  two numbers the page exists for, and the head unit's memory and the car's battery are not the
  same kind of fact. The reasoning, the wireframe and the rejected alternatives are in
  `docs/CR-010-vehicle-page.md`.
  **A value the car does not report shows as `—`, never as `0`**, with a caption distinguishing
  "the vehicle layer is not answering" from "this car does not publish that one". Nothing is
  read while the page is off-screen, nothing is written to disk, and a driver who never swipes
  that far never binds the vehicle layer at all.
- **The suite's shared vehicle layer, as a submodule.** `EVHardware` is now included as the
  `:evhardware` subproject and is the *only* way this app reaches the car. Clone with
  `--recurse-submodules`, or run `git submodule update --init` before the first build.

### Security

- **Two read-only car permissions, and a test that keeps them the only ones.**
  `android.car.permission.CAR_ENERGY` and `android.car.permission.CAR_VENDOR_EXTENSION` are the
  minimum the three displayed values need, and neither permits a write. `CAR_SPEED`,
  `CAR_EXTERIOR_ENVIRONMENT` and `CONTROL_CAR_CLIMATE` — all held by EVChargePilot — were
  deliberately refused: this page shows no speed, no outside temperature and no climate state,
  and the app that owns the home screen holds no permission whose name contains `CONTROL`.
  `VehicleBoundaryTest` now fails the build on a direct `android.car` reference, on a
  `CarPropertyManager`, on an EVHardware import outside the read-only telemetry surface, on
  `sharedUserId`, and on any car permission the boundary review did not name.
- **No now-playing card, and the reason is written down.** Reading what another app is playing
  needs `MEDIA_CONTENT_CONTROL` (signature/privileged) or notification-listener access, which
  would let the launcher read every notification on the head unit. Both are far larger grants
  than a track title is worth, so media was left out rather than paid for with a permission.

### Changed

- **AGP 9.1.1 / Gradle 9.3.1, `compileSdk 36`, and the Kotlin plugin is gone.** Not
  housekeeping: AGP 9's built-in Kotlin compilation is what `EVHardware/lib` relies on, and it
  is the toolchain every other EVSuite app already runs. The launcher's own sources remain
  Java.

## [2.0.4] - 2026-08-22

### Fixed

- A suite update signed with the very same key could still be refused as "APK rejected:
  package or signature mismatch". The check read the archive's certificates from
  `signingInfo` alone and demanded set equality with the launcher's own: a head unit that
  fills only the legacy `signatures` array on an archive produced an empty set, and an
  installed app carrying a signing history could never match a single-certificate APK.
  The certificates of both are now collected from `signingInfo` **and** the legacy array,
  and the APK is accepted when it shares one of them — the rule Android itself applies to
  an update. A refusal now names the two short fingerprints so it can be reported.

## [2.0.3] - 2026-08-22

### Changed

- A failed suite download now names its cause instead of showing one generic "Download
  failed" line: an unreachable GitHub, a refused file (with the HTTP status), a full app
  storage, an over-sized asset, a blocked URL, or an APK whose package or signature does
  not match the suite key. Without that distinction the failure could not be reported from
  the car, and the export step was fixed in 2.0.2 while the real failure was earlier.

## [2.0.2] - 2026-08-22

### Fixed

- Downloading a suite update now saves the APK straight into the public **Downloads**
  folder through MediaStore instead of depending on a document picker. Head units that
  ship no picker used to fail at the export step; the picker is still used as a fallback
  on Android 9 or when MediaStore refuses the write, and the missing "no file manager"
  message was added in all five languages.
- The verified APK is no longer deleted while an export is still pending: it survives the
  activity being recreated behind the picker, and the cache purge run by a refresh now
  spares it.

## [2.0.1] - 2026-08-21

### Fixed

- EVSuite now detects an installed **EVProfile**, shows its version and offers its
  updates. The suite catalogue still looked for the legacy `com.evsuite.profile.offline`
  application id, which EVProfile stopped publishing when it moved to
  `com.evsuite.profile`: the entry could never match an install, and its release check
  only accepted an APK asset whose name contained `offline` — no release ships one.

## [2.0.0] - 2026-08-15

### ⚠️ Breaking — existing users must install once more

- Renamed from MG4SimpleLauncher to **EVLauncher**, and the application id changed from
  `com.mg4.launcher.simple` to **`com.evsuite.launcher`**. Android treats this as a
  different app, so it does not update an existing install: it is added next to it and
  starts with no favourites. Install this one, set it as the home app again, check it, and
  only then uninstall the old app.

## [1.5.1] - 2026-08-10

### Changed

- Clarified the MIT licensing terms and updated the README licence badge.

## [1.5.0] - 2026-08-09

### Added

- Added a user-initiated EVSuite release checker for the stable/offline applications.
- Added changelog review and verified APK export through Android's document picker.
- Added manual installation guidance in English, French, German, Spanish and Italian.

### Changed

- Removed automatic launcher updates from both channels.
- Removed the privileged UID and silent `pm install` path. Installation is always manual.
- Private temporary EVSuite APKs are cleaned after saving, failure or cancellation.
- Moved the About button from the System page to the app drawer header, next to
  All apps / EVSuite.

## [1.4.0] - 2026-08-02

### Changed

- Adopted the EVSuite design system. Colour, spacing, type and component styles now
  come from shared tokens (`values/colors.xml`, `values/dimens.xml`,
  `values/styles_ev.xml`), generated by `tools/sync-tokens.mjs` and specified in
  [DESIGN.md](DESIGN.md).
- Theme is now `Theme.Material3.DayNight.NoActionBar`: the app follows the vehicle's
  day/night setting, and the light palette is held to the same 7:1 contrast floor as
  the dark one.
- `ev_outline` raised from `#4A525B` to `#7A8492`. The old value was 2.25:1 against
  `ev_surface`, below the 3:1 floor for non-text UI, so the card border it was meant
  to provide effectively was not there.
- Launcher icon replaced with the EVSuite adaptive icon: charcoal tile, white glyph
  with a single red accent, product caption. Vector only — the five legacy density
  bitmap buckets are gone.
- README restructured to the shared EVSuite skeleton; table of contents is now
  generated by `tools/sync-docs.mjs`.
