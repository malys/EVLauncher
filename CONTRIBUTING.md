# Contributing

This app is the **home screen of a car**. That single fact drives everything below.

## Ground rules

1. **The launcher holds no vehicle privileges.** No `android.car.*` permission, no
   `sharedUserId`, no IPC to MG4Control. A patch that reaches the vehicle from here will be
   rejected regardless of how useful it is — vehicle work belongs in MG4Control, and
   automation in MG4Tasker.
2. **Never leave the driver without a home screen.** A crash on the home path is not a
   normal bug: it strands the head unit. Guard every `PackageManager` result, every stored
   package name that may have been uninstalled, and every intent that may resolve to
   nothing.
3. **Stable stays offline.** The `stable` flavor declares no `INTERNET` permission and
   contains no updater code, by construction. Anything network-shaped goes in
   `src/unstable/` behind `BuildConfig.OTA_ENABLED`, or it does not go in.
4. **Say what you did not verify.** Most of this can only be confirmed on a head unit.
   "Builds, unit tests pass, tried on the emulator, not on the car" is a good PR note.
   Silence implying it ran in a vehicle is not.

## Before opening a PR

```bash
mise run check      # permission gate + lint + unit tests
```

or, without mise:

```bash
bash .github/security/check-permissions.sh
./gradlew testStableDebugUnitTest testUnstableDebugUnitTest lintStableDebug
```

New behaviour needs a unit test. Logic that can be tested without Android — update-gate
decisions, version comparison, host allowlisting — belongs in a plain class, not in an
activity or a fragment.

## Permissions

Any new `uses-permission` fails CI until it is added to
`.github/security/permission-allowlist.txt` **with a justification comment**. A launcher
sees every installed package and starts on every boot; it should not gain capabilities
quietly. `REQUEST_INSTALL_PACKAGES` was deliberately dropped — re-adding it is a security
change and needs its own discussion.

## The two channels

| | stable | unstable |
|---|---|---|
| Published by | `v*` tag | every push to `master` |
| Application id | `com.mg4.launcher.simple` | `com.mg4.launcher.simple.unstable` |
| `INTERNET` | absent | declared in `src/unstable/` |
| Updater code | not in the APK | https + host allowlist + signature check |

Both install side by side; only one can be the default home at a time. A change that blurs
that separation — an updater class reachable from `src/main/`, a permission moved up out of
`src/unstable/` — will be rejected.

## UI

Dark Material 3 on the shared `mg4_*` colour and spacing tokens, 64 dp touch targets. Dark
is imposed rather than following the system: the screen faces the driver at night, and a
light background filling the windscreen is glare, not a preference. Keep new surfaces on
the tokens instead of hard-coded colours or dimensions.

## Language and licence

English by default — code, comments, commit messages, docs. Read
[`LICENSE.md`](LICENSE.md) before contributing: this is a fork of an unlicensed upstream
project, and the licence situation is genuinely unusual.

## Commit messages

Explain **why**, not what — the diff already says what. If you fixed something subtle, say
what the failure looked like on the head unit, so the next person recognises it.
