# EVLauncher

<p align="center"><img src="docs/logo.svg" width="440" alt="EVLauncher"></p>

[![Tests](https://github.com/malys/EV_Simple_Launcher/actions/workflows/tests.yml/badge.svg)](https://github.com/malys/EV_Simple_Launcher/actions/workflows/tests.yml)
[![Security](https://github.com/malys/EV_Simple_Launcher/actions/workflows/security.yml/badge.svg)](https://github.com/malys/EV_Simple_Launcher/actions/workflows/security.yml)
[![Unstable](https://github.com/malys/EV_Simple_Launcher/actions/workflows/unstable.yml/badge.svg)](https://github.com/malys/EV_Simple_Launcher/actions/workflows/unstable.yml)
[![Release](https://img.shields.io/github/v/release/malys/EV_Simple_Launcher?include_prereleases&amp;sort=semver)](https://github.com/malys/EV_Simple_Launcher/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

EVLauncher is a simple custom home launcher designed for the MG4 head
unit (1920×720, landscape). It is part of the **EVSuite** (EVProfile,
EVTasker, EVABRPUploader, EVSwipe, EVChargePilot) and shares its dark Material 3
theme, its CI/CD and security gates, and its two-channel release model.

> ⚠️ **This software runs on a vehicle head unit.** Do not interact with it while
> driving. Read [DISCLAIMER.md](DISCLAIMER.md) before installing. This independent
> project is not affiliated with or approved by SAIC Motor or MG Motor. MG and MG4 are
> third-party marks used only to identify compatibility.

---

## Contents

- [Screenshots](#screenshots)
- [⚠️ Upgrading from an earlier build — please read](#upgrading-from-an-earlier-build-please-read)
- [Features](#features)
- [Channels](#channels)
- [EVSuite releases](#evsuite-releases)
- [Changing a pinned app](#changing-a-pinned-app)
- [Second screen (metrics)](#second-screen-metrics)
- [Building](#building)
- [Project documents](#project-documents)
- [Security](#security)
- [Contributing](#contributing)
- [Legal](#legal)

## Screenshots
<p align="center">
  <img width="320" height="180" alt="ezgif-295db3ba8dbf70b5" src="https://github.com/user-attachments/assets/7a3e3bb3-c81e-41d8-ad17-c9b56d28c359" />
</p>

<p align="center">
  <img src="https://ws2.tommasovietina.it/mg4/EV_Simple_Launcher/Screenshot_1782141845.png" alt="EVLauncher — home screen" width="800" />
</p>

<p align="center">
  <img src="https://ws2.tommasovietina.it/mg4/EV_Simple_Launcher/Screenshot_1782141854.png" alt="EVLauncher — system info screen" width="800" />
</p>

## ⚠️ Upgrading from an earlier build — please read
The application id changed from `com.evsuite.launcher` to
**`com.evsuite.launcher`**, and the app is now signed with the **EVSuite platform
key** (the same key as EVProfile and EVTasker). Either change alone forces a fresh
install: **uninstall the previous version first**, then install the new one, then set it
as the default home again from Android settings. Favorites are stored per-app, so they are
reset.

## Features
- **Swipeable two-page home**: a horizontal carousel (`ViewPager2`). Swipe left/right
  between the launcher home (page 1) and a customisable, read-only **metrics** screen
  (page 2) that carries head-unit and vehicle values in one grid. A SAIC-style bar
  indicator at the bottom centre shows the current page. The home is always page 1, so
  pressing Home lands on the favourites grid whatever you last swiped to.
- **Favorite cards** (page 1): a grid of cards, each launching one app of your
  choice — up to **12**. Tap a card to open its app; **long-press** to replace or
  remove it. The last tile is always a **+**, which is how a new app is added.
  Rows and columns follow the number of cards (1 row up to 4 apps, 2 rows up to 8,
  3 rows beyond), so the tiles stay as large as the count allows.
- **Fourth column**:
  - **All apps** (top card): every launchable app, in a grid.
  - **Two fixed shortcuts** (bottom card): the Android 9 default **Files** and
    **Settings** apps, side by side as icons.
- **System apps**: inside the *All apps* drawer, the header filters system apps
  (`FLAG_SYSTEM`) and provides a **back** button to return home.
- **EVSuite manager**: the **EVSuite** button checks stable/offline GitHub releases only
  on request, shows changelogs, and downloads verified APKs for manual installation.
- **EVSuite theme**: dark Material 3 on the shared `ev_*` colour and spacing
  tokens, with the suite's 72 dp touch target. Dark is imposed rather than
  following the system: the screen faces the driver at night, and a light
  background filling the windscreen is glare, not a preference.
- **Persisted favorites**: the chosen apps are saved across reboots (a home page
  built with an older three-slot version is migrated on first launch).

## Channels
Two build flavors, like the sibling apps:

- **stable** — tagged releases with no self-update or installer capability.
- **unstable** — a pre-release published on every push to `master`, also without
  self-update or installer capability. Application id
  `com.evsuite.launcher.unstable`, so it installs beside a stable build (only one
  of the two can be the default home at a time).

Both channels update manually. The suite manager validates HTTPS and the GitHub allowlist
at every redirect, verifies package identity and the suite certificate, then asks Android
where to save the APK. It never invokes an installer, and private temporary APKs are always
deleted. See [SECURITY.md](SECURITY.md).

## EVSuite releases

From **All apps → EVSuite**, tap **Refresh** to check each app's latest stable/offline
GitHub release. Select an available version to read its changelog, then choose **Download
APK** and a save location. To install, park the car, open **Files**, select the downloaded
APK, review Android's app name and permissions, then tap **Install**. Repositories and
package names use a fixed allowlist, every APK must carry the suite signing certificate,
and the launcher cleans private EVSuite APKs after export.

## Changing a pinned app
**Long-press** a card to choose between *replace* and *remove*; replacing opens the
app picker. To **add** one, tap the trailing **+** tile and pick an app. Your choices
are saved across reboots.

## Second screen (metrics)
Swipe right from the home to reach the metrics page (`MetricsFragment` /
`res/layout/fragment_metrics.xml`). It is one customisable grid of head-unit and
vehicle values, refreshed while the page is visible — the system cards every 3 s, the
vehicle snapshot at most every 5 s.

**Customising it works exactly like the home page**: tap the trailing **+** tile to open
the full-screen metric picker, **long-press** a card to *replace* or *remove* it. Up to
twelve cards, laid out on one, two or three rows. Your choices are saved across reboots.

A fresh install shows the seven cards the two old fixed pages showed:

- **Device**: manufacturer + model, Android version (release · API), uptime, and the
  installed launcher version.
- **Memory**: used / total RAM.
- **Storage**: free / total internal storage.
- **Network**: active connection type (Wi-Fi / mobile / Ethernet / offline) and, on
  Wi-Fi, the negotiated link speed.
- **Charge**: state of charge, in percent.
- **Range**: remaining range, in kilometres.
- **Charging**: the charging state — charging on AC or DC, plugged in and not charging,
  complete, faulted, or unplugged — with whether the port is connected underneath it.

The picker offers forty-three metrics in two sections.

**Head unit** — device, chipset, CPU load, memory, storage, network, IP address, data used
today / this month / since boot, date, time, weather, uptime, Android version, kernel, screen,
launcher version.

The **weather** card is the only one that asks for a permission. It reads the head unit's own
weather service, which answers for a position, so the location permission is requested the
first time you display the card — never at launch — and the position subscription lives only
while the page is on screen. Add no weather card and the launcher never asks and never
subscribes.

**Vehicle** — every value EVHardware's read-only snapshot exposes: charge, range, charging,
charge port, speed, battery power / energy / capacity / temperature, outside and cabin
temperature, odometer, gear, the climate state (power, A/C, auto, eco, recirculation, fan,
driver and passenger targets) and the four tyre pressures. Several of those are standard AAOS
properties behind car permissions this launcher deliberately does not hold, so they read as
`—` on the MG4 — `docs/CR-011-metrics-page.md` lists which, and why adding one is a separate
boundary review.

**A value the car does not report is shown as `—`, never as zero**, with a caption saying
which kind of silence it is: *no vehicle data* when the vehicle layer is not answering at all,
*unavailable on this car* when everything else answered and this one signal did not.

Every value comes from **EVHardware**, the suite's shared vehicle layer. The launcher holds no
property id, no vendor transaction and no setter of its own, and `VehicleBoundaryTest` fails
the build if one appears. Two read-only car permissions are held —
`android.car.permission.CAR_ENERGY` and `android.car.permission.CAR_VENDOR_EXTENSION` — and
neither permits a write. Nothing is read while the page is off-screen, nothing is stored, and
a driver who never swipes this far never binds the vehicle layer at all.

Which permissions were deliberately refused, and what a now-playing card would have cost:
[docs/CR-010-vehicle-page.md](docs/CR-010-vehicle-page.md). Why the vehicle page and the
system page later became one customisable grid, and which metrics a refused permission keeps
at `—`: [docs/CR-011-metrics-page.md](docs/CR-011-metrics-page.md).

## Building
Standard Android project (Java, AGP 9.1.1, Gradle 9.3.1, `compileSdk 36`, `minSdk 28` /
`targetSdk 34`). JDK 17 is required and pinned in `mise.toml`. The `EVHardware` submodule is
included as the `:evhardware` subproject — clone with `--recurse-submodules`, or run
`git submodule update --init` before the first build.

```
mise run build            # stable debug APK
mise run build-unstable   # unstable debug APK (manual updates only)
mise run test             # JVM unit tests, both channels
mise run permissions      # permission-drift gate, same check the CI runs
```

Or directly:

```
./gradlew assembleStableDebug
./gradlew assembleUnstableDebug
```

APKs land under `app/build/outputs/apk/<channel>/debug/`.

To sign locally, in `gradle.properties` (never committed) or as environment
variables — the same EVSuite platform key used by EVProfile and EVTasker:

```
evsuite.keystore=/path/to/platform.keystore
evsuite.keystore.password=…
evsuite.key.alias=platform
evsuite.key.password=…
```


### Emulator

```
mise run emulator-setup    # one-off: SDK images + both AVDs (needs /dev/kvm)
mise run emulator-screen   # API 28 at MG4 panel geometry — the useful one for UI work
mise run emulator-car      # API 33 Automotive — automotive system UI, wrong OS version
mise run run               # build, install and start on whatever device is connected
mise run emulator-stop
```

Neither profile is faithful on both axes: the vehicle runs AAOS 9 (API 28), but Google
publishes no Automotive system image below API 33. The screen profile is the one that
matters here — the project targets 1920x1080 @ 160dpi
(`SWI68-29958-1300R69`). The `1920×720` quoted above is the usable app area left under the
system UI; set `EMU_HEIGHT=720` in `mise.toml` and re-run `emulator-setup` to model that
instead.

The AVDs are named per repo (`mg4simple-*`, `evswipe-*`), matching the `evtasker-*` /
`evabrp-*` convention used by the sibling projects.

`mise run run` starts the launcher as an ordinary activity — that does **not** make it the
default home. Use `mise run set-home` (or press Home on the emulator and pick it in the
chooser) to exercise it as the real launcher.

### CI/CD

| Workflow | Trigger | Blocking |
|---|---|---|
| `tests.yml` | push / PR | JVM unit tests, both channels |
| `security.yml` | push / PR | permission-drift gate + gitleaks; mobsfscan / semgrep / OWASP are informational SARIF |
| `unstable.yml` | push to `master` | builds and publishes the rolling `unstable` pre-release |
| `release.yml` | `v*` tag | builds, checks, and publishes the stable APK |

Every `uses-permission` must be listed with a justification in
`.github/security/permission-allowlist.txt`, or the build fails.

## Project documents
- [SECURITY.md](SECURITY.md) — threat model, what the download path guarantees, how to report
  a vulnerability privately
- [DISCLAIMER.md](DISCLAIMER.md) — no warranty, no liability, and what running this on a
  vehicle head unit means concretely
- [CONTRIBUTING.md](CONTRIBUTING.md) — ground rules and the checks to run before a PR
- [LICENSE.md](LICENSE.md) — MIT; this is a fork of an upstream project that publishes no
  licence of its own, read it before reusing anything
- [AGENTS.md](AGENTS.md) — architecture notes for contributors and coding agents
- [docs/CR-010-vehicle-page.md](docs/CR-010-vehicle-page.md) — the vehicle page: layout decision,
  the permissions taken and refused, and why there is no now-playing card
- [docs/CR-011-metrics-page.md](docs/CR-011-metrics-page.md) — merging the system and vehicle
  pages into one customisable metrics grid, and the metrics a refused permission leaves at `—`

## Security
See [SECURITY.md](SECURITY.md) for the threat model and how to report a vulnerability
privately.

## Contributing
Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. In short: this
code runs in a moving vehicle, so changes stay small, carry tests, and say in the diff
what would break without them. Anything touching the interface follows
[DESIGN.md](DESIGN.md).

## Legal

The full text lives in [DISCLAIMER.md](DISCLAIMER.md). In short:

This project is provided **for study and educational purposes only**. It is an
experimental, non-commercial project and is not affiliated with, endorsed by, or
supported by SAIC, MG, or any vehicle manufacturer.

The software is provided "as is", without warranty of any kind, express or
implied. The author accepts **no liability** for any direct, indirect, incidental,
or consequential damage of any kind — including but not limited to damage to the
vehicle, its infotainment system, software, or data, loss of functionality, or
safety-related consequences — arising from the installation or use of this app.
You use it entirely **at your own risk**. Do not interact with the app while
driving.

All graphic resources, trademarks, and brand names belong to their respective
owners and are used here for study purposes only.
