# Pull Request

**Type of Change**
- [ ] Bug fix (non-breaking, fixes issue #_)
- [ ] Feature (non-breaking, adds functionality)
- [ ] Breaking change (fix or feature that changes existing functionality)
- [ ] Documentation update

**Related Issue**
Closes #(issue)

## Description

Please include a summary of the changes and the motivation behind them:

- What problem does this solve?
- How was this tested?
- Are there any breaking changes?

## Testing

Describe how you tested your changes (local device, emulator, specific scenarios):

- [ ] Tested on emulator (AAOS 9, MT2712)
- [ ] Tested on physical device (firmware version: _)
- [ ] Manual test steps: _
- [ ] Unit/integration tests added/updated

**Display Testing (if applicable)**
- [ ] No display changes (logic only)
- [ ] Layout renders correctly on 12.8" infotainment screen
- [ ] No text overflow or cutoff
- [ ] Touch targets remain >= 48dp
- [ ] Readable in both day and night modes (if applicable)

## Code Review Checklist

- [ ] Code follows project style and conventions
- [ ] No new permissions added without justification
- [ ] No hardcoded secrets, URLs, or credentials
- [ ] Comments explain complex logic
- [ ] Breaking changes documented in commit message
- [ ] Related documentation (README) updated

**Security Considerations**
- [ ] No prompt injection vulnerabilities (input validated)
- [ ] App shortcuts/intents validated before launching
- [ ] No crashes from malformed input
- [ ] Dependencies checked for known vulnerabilities

**Stability Considerations**
- [ ] No crashes observed during testing
- [ ] No ANR (Application Not Responding) warnings
- [ ] Launcher remains responsive (< 100ms launch time)
- [ ] No memory leaks (check with Android Profiler)
- [ ] Handles missing/uninstalled apps gracefully

## CI/CD Status

Ensure all checks pass:
- [ ] Tests pass locally (`./gradlew test`)
- [ ] No new lint errors (`./gradlew lint`)
- [ ] APK builds without warnings (`./gradlew build`)
- [ ] Security checks pass (gitleaks, mobsfscan, dependency-check)
- [ ] `mise run check` passes (permission gate + lint + tests)

## Claude-Assisted Description (Optional)

*If you used Claude AI to refine this PR description, design, or commit messages, summarize how it was improved:*
- Original issue: _
- Claude suggestions applied: _
- Confidence in description clarity: high / medium / low

---

**Note:** All contributions are subject to [CONTRIBUTING.md](CONTRIBUTING.md). Please ensure your PR maintains a responsive, safe launcher for Android Automotive drivers.
- [ ] No new `uses-permission` — or it is added to
      `.github/security/permission-allowlist.txt` with a justification
- [ ] Nothing network-shaped added to `src/main/`; OTA code stays in `src/unstable/` behind
      `BuildConfig.OTA_ENABLED`
- [ ] New UI uses the shared `mg4_*` tokens and the 64 dp touch target, not hard-coded
      colours or sizes

## Notes for the reviewer

<!-- Anything you are unsure about, or deliberately left out of scope. -->
