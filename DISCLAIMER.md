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
  `sharedUserId`, no bridge to EVProfile. It cannot read or change a vehicle setting, and
  a bug in it cannot write to the car. What it *can* do is start other apps and occupy the
  screen the driver looks at.
- **Compatibility is inferred, not certified.** The panel geometry and Android version
  are project compatibility targets, not a vendor specification. A firmware update can
  change the system UI, the available apps, or the
  launcher selection screen.
- Neither channel self-updates or silently installs APKs. The EVSuite screen can check
  allowlisted GitHub releases and download a package after confirmation; installation is
  always a separate manual action. See [`SECURITY.md`](SECURITY.md).
- Release builds are signed with the **EVSuite platform key**. Installing them replaces
  any earlier build signed differently — you must uninstall first, and your favorites are
  reset.

## Not affiliated

This project is **not affiliated with, endorsed by, or supported by** SAIC Motor, MG Motor,
or Google. MG, MG4 and related names and logos are trademarks of their respective owners.
They are used solely to identify compatibility with certain vehicles; no official origin,
certification or approval is claimed. Other marks belong to their respective owners.

## Contributors

Contributors provide their work on the same terms: no warranty, and no liability for how
anyone uses it.
