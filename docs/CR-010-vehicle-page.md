# CR-010 — the read-only vehicle page: layout decision

## The decision

**A third carousel page.** Not a section appended to the existing system-information page.

The ticket asks for the smaller solution unless the second page becomes crowded. It does.

## Why the second page could not take it

`fragment_system.xml` is four equal-weight cards laid out horizontally across the whole
panel. The usable app area is **1920 × 720** at 160 dpi, so each card is currently about
**460 dp** wide before padding, and each one carries a 28 sp label over a 32 sp value.

Adding state of charge, range and charging state makes it seven cards:

```
1920 dp − (2 × 24 dp screen padding) − (6 × 16 dp gaps) = 1776 dp / 7 ≈ 253 dp per card
```

253 dp holds a 32 sp value and a 28 sp label only if nothing beside them ever grows. "Not
charging" and a three-digit range with its unit are both wider than that at those sizes, so
the first thing the layout would do on a real car is ellipsize the two values the page exists
to show. Dropping the type sizes to fit is the other way to lose: 16 sp is the floor in
`DESIGN.md` precisely because contrast stops compensating for reflection and reading distance
below it, and this is a screen read at 70 cm, in sun, at speed.

There is a second reason, and it outranks the arithmetic. `AGENTS.md` requires the launcher to
**preserve the distinction between device reads and vehicle telemetry**. RAM, storage, uptime
and network state are facts about the head unit; state of charge is a fact about the car. Seven
cards in one row states that they are the same kind of thing. Two pages state that they are not,
and the page is the cheapest possible way to say it — no new component, no explanatory label.

## What the third page costs

| Concern | Answer |
| --- | --- |
| Glanceability at 1920×720 | Three cards, ~576 dp each. The value can be `text_display` (32 sp) with room for a unit and a caption underneath. |
| Touch targets | None. The page is read-only and has no controls, so the 72 dp minimum has nothing to apply to. |
| Information density | Three values on a panel sized for four. Deliberate headroom: battery power, temperature, odometer and tyre pressures are the next candidates and they fit without a redesign. |
| Swipe discoverability | The pagination indicator already exists and already generalises — a third bar, same drawable, same behaviour. Drivers on this head unit already swipe the SAIC home. |
| Home-path reliability | Page 0 is still `HomeFragment`. Pressing Home lands on the favourites grid whatever the vehicle page is doing, and `ViewPager2` restores the last page without the vehicle layer being consulted. |

## Wireframe

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                                                                                │
│  ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐    │
│  │ Charge               │ │ Range                │ │ Charging             │    │
│  │                      │ │                      │ │                      │    │
│  │                      │ │                      │ │                      │    │
│  │        67 %          │ │       243 km         │ │      Charging        │    │
│  │                      │ │                      │ │                      │    │
│  │   state of charge    │ │     remaining        │ │      AC · plugged in │    │
│  │                      │ │                      │ │                      │    │
│  └──────────────────────┘ └──────────────────────┘ └──────────────────────┘    │
│                                                                                │
│                                  ▬▬  ▬  ▬                                      │
└────────────────────────────────────────────────────────────────────────────────┘

Unavailable state — never a zero:

  ┌──────────────────────┐
  │ Charge               │
  │                      │
  │          —           │
  │                      │
  │  no vehicle data     │
  └──────────────────────┘
```

The caption line under each value is where the reason lives when the value is `—`: *no vehicle
data* when the car reports nothing at all, *unavailable on this car* when the rest of the
snapshot answered and this signal did not. A zero is never rendered for an absent reading, and
the two cases are distinguished because they mean different things to whoever is diagnosing it.

## Media: found, and deliberately omitted

The ticket makes the now-playing card conditional on reaching it *without* a new privileged
capability. It cannot be reached that way.

Reading what another application is playing means `MediaSessionManager.getActiveSessions()`,
and that call is gated on one of two things: `android.permission.MEDIA_CONTENT_CONTROL`, which
is signature/privileged, or an enabled **notification listener**, which is a special access the
driver has to grant in Settings and which lets the holder read *every* notification on the head
unit. Neither is minimal, and the second is a far larger grant than "show the track title".
EVHardware's own `SaicMediaPlayer.sessionCommand` already documents this from the other side —
it catches the `SecurityException` and gives up the session path rather than requiring the
permission.

There is no unprivileged read for it. `AudioManager.isMusicActive()` says only that *something*
is audible, and on this car it is famously false while the radio plays, its stream not being the
music one. That is a boolean about the amplifier, not a now-playing card.

So media is **omitted from this slice**, per the ticket's own instruction to document the
finding rather than silently widen permissions. If it is ever wanted, it is an owner decision
about notification-listener access, taken with a security review, and not a side effect of a
telemetry page.

## Permissions actually added

Two, both read-only:

| Permission | Why |
| --- | --- |
| `android.car.permission.CAR_ENERGY` | Range remaining and charge-port connected, the standard AAOS reads behind `EnergyTelemetryReader`. |
| `android.car.permission.CAR_VENDOR_EXTENSION` | The SWI68 vendor fallbacks for state of charge and range. The vendor property space cannot be read at all without it. |

Deliberately **not** added, though `EVChargePilot` holds them: `CAR_SPEED` (the launcher shows
no speed), `CAR_EXTERIOR_ENVIRONMENT` (no outside temperature in this slice) and
`CONTROL_CAR_CLIMATE` (reads HVAC state, and this page shows none — its name alone is a reason
not to hold it in the app that owns the home screen). Every EVHardware getter returns null when
a permission is missing rather than throwing, so the omitted ones cost exactly the readings that
were not wanted.

The state of charge and the charging status come from the SAIC vendor **services**, which are
bound AIDL interfaces and need no manifest permission at all.

## Failure behaviour

- **No EVHardware service, no vehicle layer, unreadable signal** — every value is nullable and
  renders `—` with its reason. `EnergyTelemetryReader` returns a snapshot of nulls rather than
  throwing, and the refresh tick is wrapped the same way `SystemInfoFragment`'s already is.
- **Page not visible** — the ticker starts in `onResume` and stops in `onPause`, so nothing is
  read while the driver is on the home page or in another app. Nothing is written to disk, no
  boot receiver, no overlay, no network.
- **Binding, and the one thing this page does not do.** The ticket asks for a deterministic
  unbind. EVHardware exposes none, and that is a decision rather than an omission: it owns the
  Car service lifecycle behind an unbounded reconnection watchdog, so a consumer tearing the
  binding down between two swipes would be fighting the component whose job is to keep it up,
  and the next page-in would read nulls until the backoff caught up. What is deterministic
  here is the acquisition: `EnergyTelemetryReader` is constructed the first time this page
  becomes visible, so **a driver who never swipes to it never binds the vehicle layer at
  all**, and the reads stop the moment the page leaves the screen. Adding an unbind belongs in
  EVHardware, with the watchdog, if it is ever wanted — not in the launcher.
- **Fragment recreation** — the fragment holds no state beyond the views; the reader is rebuilt
  from the application context.
