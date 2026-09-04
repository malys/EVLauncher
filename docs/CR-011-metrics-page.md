# CR-011 — merging the system and vehicle pages into one customisable metrics page

## The decision

**One carousel page, built at runtime from the driver's own list.** The fixed system page
(page 2) and the fixed vehicle page (page 3) become a single page 2 whose cards the driver
adds, replaces and removes — exactly the gestures the favourite apps already use on the home
page. `MetricsFragment` replaces `SystemInfoFragment` and `VehicleInfoFragment`, which are
deleted.

This supersedes the layout half of [CR-010](CR-010-vehicle-page.md). Its boundary half —
the launcher reads the car only through EVHardware's typed, nullable telemetry API, owns no
property id, no vendor transaction and no setter, and writes nothing — is unchanged and still
enforced by `VehicleBoundaryTest`.

## Why CR-010's arithmetic no longer forces a third page

CR-010 rejected merging because seven fixed cards on a 1920 dp panel leaves ~253 dp each,
below what a 28 sp label over a 32 sp value can carry. That arithmetic assumed **one row**.

The home page had already solved the same problem for favourites: past four tiles it wraps to
a second and then a third row. Reusing that banding here keeps every card at least a quarter
of the panel wide:

```
1 row  : up to  4 cards  ≈ 460 dp each
2 rows : up to  8 cards  ≈ 460 dp × 2 rows
3 rows : up to 12 cards  ≈ 460 dp × 3 rows
```

`PreferencesManager.MAX_METRICS` is 12 for the same reason `MAX_FAVORITES` is: a 4×3 grid is
the last one whose cards are still readable at a glance from the driver's seat.

## What a fresh install shows

The seven cards the two old pages showed, in their old order: device, memory, storage,
network, charge, range, charging. Merging two pages must not silently take a value away from
a driver who never opens the picker, so the default is the union, not a new opinion.

An explicitly emptied page stays empty — the stored value is an empty string, which is
distinct from "never customised" (unset).

## Which metrics are on the menu

Forty-three entries in two sections. The enum constant name in `Metric` is the persisted key,
so a label change belongs in `strings.xml` and a rename would drop the card from the page of
every driver who chose it.

**Head unit (18).** Device, chipset, CPU load, memory, storage, network, IP address, data
today / this month / since boot, date, time, weather, uptime, Android version, kernel, screen,
launcher version. All permission-free `/proc`, `/sys` and framework reads except the two data
windows and the weather (below). CPU load is a ratio between two ticks, so the first refresh
after the page opens primes the counters and shows the dash. Date and time follow the system's
own locale and 12/24-hour setting rather than a format this app picks.

**Vehicle (25).** Every field of EVHardware's `EnergySnapshot`: charge, range, charging,
charge port, speed, battery power / energy / capacity / temperature, outside and cabin
temperature, odometer, gear, the eight climate signals and the four tyre pressures.

## The picker is a screen, not a dialog

The first cut used an `AlertDialog.setItems` list. Forty entries in a dialog is a scrolling
column of small rows on a panel the driver reaches across — the wrong component for the
number of choices, and the smallest touch target in the app.

`MetricPickerActivity` is a full screen with the same header + grid shape as
`AppDrawerActivity`, so the launcher's two pickers behave alike: back button, title, and a
grid of 128 dp cells under a **Head unit** / **Vehicle** section header. Like the app picker
it writes the choice itself and finishes; `MetricsFragment` rebuilds from preferences in
`onResume`, so there is one owner of the write and no result to plumb back. Metrics already on
the page are left out, except the one in the slot being replaced.

The long-press menu (*replace* / *remove*) stays a dialog: two items is what a dialog is for.

## Data usage needs one non-car permission

The data-today and data-this-month cards go through `EVHardware.DataUsage`, which reads
Android's own `NetworkStatsManager` counters — the numbers the Settings screen shows. That
needs `android.permission.PACKAGE_USAGE_STATS`, which is **added to the manifest** by this
change. It is read-only, device-wide (no per-app breakdown is queried), and it is not a car
permission, so the vehicle boundary is untouched. The since-boot card uses `TrafficStats` and
needs nothing; the two windowed cards fall back to `—` rather than to zero when the counters
do not answer.

`DataUsage` is added to `VehicleBoundaryTest`'s EVHardware allowlist for the same reason: it
names no vehicle API at all, and lives in the library only because every app in the suite has
to read data usage the same awkward way (the head unit's modem presents as Ethernet, so the
public `querySummaryForDevice(int, …)` template answers zero).

## Weather is the one card that costs a runtime permission

`SaicWeather` answers **for a position**, so a weather card needs one. That is a real cost in
an app whose selling point is that its stats are permission-free, and it is confined as
tightly as the feature allows:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` are declared, but **requested at runtime
  only the first time a weather card is displayed** — never at launch. A driver who does not
  add the card is never asked, and a driver who declines is not asked again on every swipe
  back.
- Both grades are declared because since API 31 the user may grant the approximate one when
  the precise one was asked for, and a city's weather does not need the precise one.
- The position comes from a **subscription held only while the page is visible** and only
  while a weather card is on it, at 60 s / 1 km. It is dropped in `onPause`. A subscription
  rather than `getLastKnownLocation` alone because on a head unit with no other GPS client
  that cache is empty and stays empty — the same finding that made EVTasker's vehicle service
  hold one.
- The query blocks up to two seconds waiting on the service's callback binder, so it runs on
  a single background thread and is re-read at most every ten minutes. Two seconds on the tick
  would be a frozen home screen.
- Three silences, and the card names which: *needs the location permission*, *waiting for a
  position*, *weather unavailable*. None of them is a zero degrees.

`SaicWeather` joins `VehicleBoundaryTest`'s allowlist: it is an app service on the head unit's
map stack with one query and no setter, and it reaches no car API — unlike `SaicClimate` and
`SaicCharging`, which stay forbidden. Forecasts are not offered: the service declares no
transaction that answers one.

## Vehicle signals the launcher holds no permission for

The **car** permissions are deliberately unchanged: `CAR_ENERGY` and `CAR_VENDOR_EXTENSION`,
the two read-only permissions CR-010 reviewed. Several of the offered vehicle metrics are
standard AAOS properties gated behind permissions this app does **not** hold, and therefore
read as `—` with *unavailable on this car*:

| Metric | AAOS property | Permission not held here |
|---|---|---|
| Speed | `PERF_VEHICLE_SPEED` | `CAR_SPEED` |
| Outside temperature | `ENV_OUTSIDE_TEMPERATURE` | `CAR_EXTERIOR_ENVIRONMENT` |
| Cabin temperature | `HVAC_TEMPERATURE_CURRENT` | `CONTROL_CAR_CLIMATE` |
| Odometer | `PERF_ODOMETER` | `CAR_MILEAGE` |
| Charge port | `EV_CHARGE_PORT_CONNECTED` | `CAR_ENERGY_PORTS` |
| Tyre pressures | `TIRE_PRESSURE` | `CAR_TIRES` |

Listing them anyway is deliberate. The page's contract is already that a value the car does
not answer is a dash with a caption, never a zero, and a permission the app does not hold is
one more reason a car does not answer. Offering the metric costs nothing and makes the gap
legible; hiding it would make the catalogue depend on a permission set that is a boundary
decision, not a UI one.

Adding any of those permissions is a **separate** change: it needs a boundary review, a new
entry in `VehicleBoundaryTest`'s manifest allowlist, and a line in `AGENTS.md`, which today
says `CAR_SPEED` and `CAR_EXTERIOR_ENVIRONMENT` are held by EVChargePilot and deliberately not
here. The vendor-service signals (state of charge, range, charging status, climate) go through
`CAR_VENDOR_EXTENSION` and are unaffected.

## Refresh

Two cadences on one ticker, unchanged from the pages they replace: the system cards tick every
3 s, the vehicle snapshot is re-read at most every 5 s — state of charge and range move over
minutes, and that read reaches the vehicle layer rather than `/proc`.

The reader is built on the first refresh that actually needs it, so **a page holding only
system cards binds no vehicle layer at all** — the same property the standalone vehicle page
had, now conditioned on the card list instead of on which page was swiped to. The ticker runs
only between `onResume` and `onPause`, and EVHardware keeps owning the binding and its
reconnection watchdog, so nothing here tears a bound service down underneath it.

## Consequences

- The carousel is two pages: `HomePagerAdapter.PAGE_COUNT` is 2, and `activity_main.xml` has
  two pagination bars.
- `PreferencesManager` now persists two ordered, hole-free lists under one prefs file; the
  favourites API and its legacy three-slot migration are untouched.
- `ConsumptionCalculator.instantaneous` is deliberately **not** offered as a card: it derives
  kWh/100 km from battery power and speed, and speed needs `CAR_SPEED`, so the card could
  never show a number. Offering it would mean widening the EVHardware allowlist for a
  permanent dash.
- A stored key this build does not know (a downgrade, or a metric later dropped) is skipped
  rather than shown as an empty card.
