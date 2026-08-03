# MG4 Simple Launcher

<p align="center"><img src="docs/logo.svg" width="440" alt="MG4 Simple Launcher"></p>

[![Tests](https://github.com/malys/MG4_Simple_Launcher/actions/workflows/tests.yml/badge.svg)](https://github.com/malys/MG4_Simple_Launcher/actions/workflows/tests.yml)
[![Security](https://github.com/malys/MG4_Simple_Launcher/actions/workflows/security.yml/badge.svg)](https://github.com/malys/MG4_Simple_Launcher/actions/workflows/security.yml)
[![Unstable](https://github.com/malys/MG4_Simple_Launcher/actions/workflows/unstable.yml/badge.svg)](https://github.com/malys/MG4_Simple_Launcher/actions/workflows/unstable.yml)
[![Release](https://img.shields.io/github/v/release/malys/MG4_Simple_Launcher?include_prereleases&amp;sort=semver)](https://github.com/malys/MG4_Simple_Launcher/releases)
[![License](https://img.shields.io/badge/license-see%20LICENSE-blue.svg)](LICENSE.md)

MG4 Simple Launcher is a simple custom home launcher designed for the MG4 head
unit (1920×720, landscape). It is part of the **MG4 app suite** (MG4Control,
MG4Tasker, MG4ABRPUploader, MG4 Swipe Launcher) and shares its dark Material 3
theme, its CI/CD and security gates, and its two-channel release model.

> ⚠️ **This software runs on a vehicle head unit.** Do not interact with it while
> driving. Read [DISCLAIMER.md](DISCLAIMER.md) before installing. This independent
> project is not affiliated with SAIC Motor or MG Motor.

---

## Contents

- [Screenshots](#screenshots)
- [⚠️ Upgrading from an earlier build — please read](#upgrading-from-an-earlier-build-please-read)
- [Features](#features)
- [Channels](#channels)
- [Changing a pinned app](#changing-a-pinned-app)
- [Second screen (system info)](#second-screen-system-info)
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
  <img src="https://ws2.tommasovietina.it/mg4/MG4_Simple_Launcher/Screenshot_1782141845.png" alt="MG4 Simple Launcher — home screen" width="800" />
</p>

<p align="center">
  <img src="https://ws2.tommasovietina.it/mg4/MG4_Simple_Launcher/Screenshot_1782141854.png" alt="MG4 Simple Launcher — system info screen" width="800" />
</p>

## ⚠️ Upgrading from an earlier build — please read
The application id changed from `com.tommasov.mg4simplelauncher` to
**`com.mg4.launcher.simple`**, and the app is now signed with the **MG4 suite platform
key** (the same key as MG4Control and MG4Tasker). Either change alone forces a fresh
install: **uninstall the previous version first**, then install the new one, then set it
as the default home again from Android settings. Favorites are stored per-app, so they are
reset.

## Features
- **Swipeable two-page home**: a horizontal carousel (`ViewPager2`). Swipe left/right
  between the launcher home (page 1) and a **system-info** screen (page 2). A
  SAIC-style bar indicator at the bottom centre shows the current page.
- **Favorite cards** (page 1): a grid of cards, each launching one app of your
  choice — up to **12**. Tap a card to open its app; **long-press** to replace or
  remove it. The last tile is always a **+**, which is how a new app is added.
  Rows and columns follow the number of cards (1 row up to 4 apps, 2 rows up to 8,
  3 rows beyond), so the tiles stay as large as the count allows.
- **Fourth column**:
  - **All apps** (top card): every launchable app, in a grid.
  - **Two fixed shortcuts** (bottom card): the Android 9 default **Files** and
    **Settings** apps, side by side as icons.
- **System apps & updates**: inside the *All apps* drawer, the header carries a
  **System apps** button (only system apps, `FLAG_SYSTEM`) next to **Check for
  updates** (unstable channel only), plus a **back** button to return home.
- **MG4 suite theme**: dark Material 3 on the shared `mg4_*` colour and spacing
  tokens, with the suite's 72 dp touch target. Dark is imposed rather than
  following the system: the screen faces the driver at night, and a light
  background filling the windscreen is glare, not a preference.
- **Persisted favorites**: the chosen apps are saved across reboots (a home page
  built with an older three-slot version is migrated on first launch).

## Channels
Two build flavors, like the sibling apps:

- **stable** — tagged releases, **no self-update**. The updater class is not in the
  APK and the manifest declares no `INTERNET` permission. Installed offline, from a
  USB stick.
- **unstable** — a pre-release published on every push to `master`, with OTA so
  testers stay current without manual work. Application id
  `com.mg4.launcher.simple.unstable`, so it installs beside a stable build (only one
  of the two can be the default home at a time).

The unstable updater accepts an APK only over https from an allowlisted GitHub host,
and only if it is signed with the same certificate as the running app — otherwise it
deletes the file. Install is still a manual tap: the app does not hold
`REQUEST_INSTALL_PACKAGES`. See [SECURITY.md](SECURITY.md).

## Changing a pinned app
**Long-press** a card to choose between *replace* and *remove*; replacing opens the
app picker. To **add** one, tap the trailing **+** tile and pick an app. Your choices
are saved across reboots.

## Second screen (system info)
Swipe right from the home to reach the system-info page (`SystemInfoFragment` /
`res/layout/fragment_system.xml`). It shows live, permission-free stats that refresh
while the page is visible:

- **Device**: manufacturer + model, Android version (release · API), uptime, and the
  installed launcher version.
- **Memory**: used / total RAM.
- **Storage**: free / total internal storage.
- **Network**: active connection type (Wi-Fi / mobile / Ethernet / offline) and, on
  Wi-Fi, the negotiated link speed.

## Building
Standard Android project (Java + Kotlin, AGP 8.6, Gradle 8.7, `minSdk 28` /
`targetSdk 34`). JDK 17 is required and pinned in `mise.toml`.

```
mise run build            # stable debug APK
mise run build-unstable   # unstable debug APK (OTA enabled)
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
variables — the same MG4 suite platform key used by MG4Control and MG4Tasker:

```
mg4.keystore=/path/to/platform.keystore
mg4.keystore.password=…
mg4.key.alias=platform
mg4.key.password=…
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

The AVDs are named per repo (`mg4simple-*`, `mg4swipe-*`), matching the `mg4tasker-*` /
`mg4abrp-*` convention used by the sibling projects.

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
- [SECURITY.md](SECURITY.md) — threat model, what the OTA path guarantees, how to report
  a vulnerability privately
- [DISCLAIMER.md](DISCLAIMER.md) — no warranty, no liability, and what running this on a
  vehicle head unit means concretely
- [CONTRIBUTING.md](CONTRIBUTING.md) — ground rules and the checks to run before a PR
- [LICENSE.md](LICENSE.md) — this is a fork of an **unlicensed** upstream project; read it
  before reusing anything
- [AGENTS.md](AGENTS.md) — architecture notes for contributors and coding agents

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
