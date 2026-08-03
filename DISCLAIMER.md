# Disclaimer — no warranty, no liability

**Use this software entirely at your own risk.**

This project is provided **"as is"**, without warranty of any kind, express or implied,
including but not limited to the warranties of merchantability, fitness for a particular
purpose and non-infringement. In no event shall the authors or contributors be liable for
any claim, damages or other liability, whether in an action of contract, tort or otherwise,
arising from, out of or in connection with the software or its use.

It is an experimental, non-commercial project provided for **study and educational
purposes**.

## What that means concretely

- The app runs on a **vehicle head unit** and **replaces the home screen**. If it crashes,
  hangs or fails to start after a firmware update, the driver is left without a launcher
  until the stock one is restored from Android settings. Know how to get back to the stock
  launcher *before* you set this one as default.
- Installing it is your decision and your responsibility, including any effect on the head
  unit's stability, your warranty, your insurance, or your vehicle's roadworthiness.
- **Do not configure the launcher while driving.** Assigning favorites, opening the app
  drawer or browsing system info is parked-only work. Nothing here needs attention on the
  move.
- The launcher **holds no vehicle privileges**: no `android.car.*` permission, no
  `sharedUserId`, no bridge to MG4Control. It cannot read or change a vehicle setting, and
  a bug in it cannot write to the car. What it *can* do is start other apps and occupy the
  screen the driver looks at.
- **Compatibility is inferred, not certified.** The panel geometry and Android version
  are project compatibility targets, not a vendor specification. A firmware update can
  change the system UI, the available apps, or the
  launcher selection screen.
- The **stable channel has no network access at all** — no `INTERNET` permission, no
  updater code in the APK. The **unstable channel self-updates** from GitHub pre-releases
  over https, with a host allowlist and a signature check, and is meant for testers, not
  for a car you depend on. See [`SECURITY.md`](SECURITY.md).
- Release builds are signed with the **MG4 suite platform key**. Installing them replaces
  any earlier build signed differently — you must uninstall first, and your favorites are
  reset.

## Not affiliated

This project is **not affiliated with, endorsed by, or supported by** SAIC Motor, MG Motor,
or Google. All trademarks, brand names and graphic resources belong to their respective
owners and are used only to identify the vehicle the software targets.

## Contributors

Contributors provide their work on the same terms: no warranty, and no liability for how
anyone uses it.
