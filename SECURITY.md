# Security policy

This app replaces the home screen of a car. It holds no credentials and, on the stable
channel, contacts no server — but it runs on every boot, sees every installed package, and
is the first thing the driver touches. Security reports are welcome and taken seriously.

## Reporting a vulnerability

Please **do not** open a public issue for a vulnerability. Use GitHub's
[private vulnerability reporting](../../security/advisories/new) instead.

Include what you were able to do and on which firmware generation. A proof of concept
helps; a working exploit is not required.

## What is in scope

- Anything that lets another app on the head unit make this launcher **start a component it
  was not asked to start**, or launch with attacker-controlled extras.
- Anything that gets the manual downloader to accept an APK that is **not** served over https
  from an allowlisted GitHub host, or that is **not** signed with the running app's
  certificate.
- Anything that makes the suite manager accept an APK outside its fixed package
  allowlist or signed with a different certificate.
- Path traversal or overwrite through a downloaded APK file name, which is derived from a
  remote asset name.
- Anything that makes the drawer leak the installed-package list off-device.

## What is not in scope

- Requiring physical access to an unlocked head unit with developer mode enabled.
- `QUERY_ALL_PACKAGES`. A launcher that cannot enumerate launchable apps has no drawer. The
  list never leaves the device.
- A user deliberately downloading and manually installing a correctly signed suite release.
- Vulnerabilities in the OEM firmware itself.

## Design decisions you should know about

- **Neither channel self-updates.** There is no background release check, `pm install`
  path, privileged UID, or installer permission. Network activity starts only when the
  user opens EVSuite and refreshes or confirms a download.
- **The manual downloader fails closed, twice.** The APK URL comes from a remote JSON
  document and is never trusted: https only, **exact-match** host allowlist (never a suffix
  test, so `github.com.attacker.net` is rejected), re-checked immediately before the URL
  reaches the system downloader. The downloaded APK must then be signed by the same
  certificate as the running app, or it is deleted rather than offered for install. An
  unreadable archive or a failed API call counts as a mismatch. Both gates are unit-tested.
- **Installation is outside the launcher.** The manager shows the changelog, asks before
  download, verifies the fixed package and certificate, and exports through Android's
  document picker. The user later opens the APK in Files and accepts Android's installer.
  Private temporary APKs are deleted after success, failure, cancellation, and startup.
- **The remote version string never reaches a path raw.** It is reduced to `[a-z0-9._-]`
  before becoming a file name, and there is a unit test for `../../etc/passwd`.
- **The previous third-party update server is gone.** Earlier versions fetched a manifest
  from a single personal domain and verified the APK against a SHA-256 published by that
  same host — an integrity check against a corrupt transfer, not against a compromised
  server. It has been replaced by the EVSuite's GitHub + signature-match model.
- **Permission drift is a blocking CI gate.** Every `uses-permission` in every manifest must
  appear in `.github/security/permission-allowlist.txt`, which carries the justification
  for each one. Adding a permission without editing that file fails the build.
- **Signed with the EVSuite platform key.** The launcher claims no privileged permission
  of its own; the shared key is what makes the suite one installable set, and it is what
  the manual download signature check compares an incoming APK against.
- **The APK is not minified.** R8 is off on release, so a published APK stays verifiable
  line-by-line against this source.
