## What and why

<!-- What changes, and what problem it solves. The diff already says what; explain why. -->

## Verification

<!-- Be specific about what you actually ran. "Not verified on a head unit" is a fine and
     expected answer — most of this can only be confirmed on the car or an emulator. -->

- [ ] `mise run check` passes (permission gate + lint + unit tests)
- [ ] New behaviour is covered by a unit test
- [ ] Tried on the emulator (`mise run run`, then `mise run set-home`)
- [ ] Tried on a real MG4 head unit — if yes, which firmware: <!-- e.g. SWI68 -->

## Launcher-safety checklist

- [ ] Nothing on the home path can crash: `PackageManager` results, stored package names of
      uninstalled apps, and unresolvable intents all degrade instead of throwing
- [ ] The app still holds no vehicle privileges (no `android.car.*`, no `sharedUserId`, no
      MG4Control IPC)
- [ ] No new `uses-permission` — or it is added to
      `.github/security/permission-allowlist.txt` with a justification
- [ ] Nothing network-shaped added to `src/main/`; OTA code stays in `src/unstable/` behind
      `BuildConfig.OTA_ENABLED`
- [ ] New UI uses the shared `mg4_*` tokens and the 64 dp touch target, not hard-coded
      colours or sizes

## Notes for the reviewer

<!-- Anything you are unsure about, or deliberately left out of scope. -->
