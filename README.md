# FA — Find Anonymity

> Scheduled connectivity control, timed reboots, and a duress panic-lock for Android — driven over a privileged backend (root **or** Shizuku). Offline, no account, no telemetry.

FA is a privacy / anti-surveillance automation tool. It runs a foreground service that
enforces your rules against wall-clock time (anchor-based, so Doze slack never compounds)
and shows a live status countdown in its notification.

## Features

- **Connectivity scheduler** — cycle **Wi‑Fi**, **mobile data**, and **airplane mode** on a
  timer (cyclical / always‑on / always‑off / unmanaged). Shrink your device's connectivity
  window to reduce trackability.
- **Timed reboots** — reboot on an interval to clear volatile state and return the device to a
  fresh, before‑first‑unlock security posture.
- **Panic‑lock (duress defense)** — a detached root daemon watches for rapid power‑button
  presses (e.g. 5× within 3 s) and instantly rotates the lock‑screen password to a long random
  string you have backed up externally. Under coercion, you press the button and lock yourself
  out — the device can no longer be unlocked on demand. Keeps working even if FA's own process
  is killed.
- **15 languages**, full RTL support, a cold **cyberpunk‑corpo** terminal UI.

## Requirements

FA needs a privileged backend to issue system commands. It supports two:

| Capability | Root | Shizuku |
|---|:---:|:---:|
| Wi‑Fi / mobile data / airplane toggles | ✅ | ✅ |
| Timed reboots | ✅ | ✅ |
| **Panic‑lock** | ✅ | ⚠️ experimental |

- **Root** (Magisk / KernelSU) enables everything, and is the recommended backend for the
  panic‑lock: the watcher runs as a detached root daemon and FA re‑arms it after a reboot.
- **Shizuku** (paired via Wireless Debugging, or root‑mode Sui) enables the connectivity and
  reboot automation, and an **experimental** panic‑lock path. Under Shizuku the guarantees are
  weaker: changing the lock‑screen password from a shell‑uid process is not dependable on every
  OEM/Android version (test on a spare device first), and protection ends at reboot with no
  auto re‑arm. Use root for a panic‑lock you can rely on.

Minimum **Android 7.0 (API 24)**; target Android 16 (API 36).

## Install

Download the signed APK from the [latest release](../../releases/latest) and install it.
Verify the download against the published `SHA-256` checksum first.

## Build from source

```bash
git clone <this-repo>
cd FindAnonymity
./gradlew assembleDebug        # debug build, installable immediately
```

To produce a **signed** release build, create a `keystore.properties` in the project root
(gitignored) pointing at your own keystore:

```properties
storeFile=keystore/your-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Then `./gradlew assembleRelease`. Without `keystore.properties` the release build stays unsigned.

## Security notes

- **No plaintext credentials at rest.** When the panic‑lock is armed, the daemon reads its two
  secrets into memory and immediately shreds + unlinks the on‑disk copies. For the rest of the
  armed lifetime the daemon holds them only in RAM.
- **Re‑arm survives reboots.** A reboot (including FA's own scheduled one) kills the detached
  daemon, so FA re‑arms it on `BOOT_COMPLETED` from Keystore‑encrypted copies of the two
  secrets. Those copies (never plaintext, Android Keystore / StrongBox‑backed, this‑app‑only)
  are what makes re‑arm possible; they are cleared on disarm. Re‑arm is fail‑closed — missing
  credentials or no root simply means no daemon and no password change.
- The next lock‑screen password is generated with `SecureRandom`.
- **Post‑trigger nuance:** if a panic had already rotated the password, the stored `old`
  credential is stale, so re‑arm restores a daemon whose next trigger is a harmless no‑op until
  you reconfigure — never a lockout.
- FA makes **no network requests.** The only privileged actions are the documented shell
  commands it runs on your behalf.

## License

[GNU GPL v3.0](LICENSE) © eVersor‑HN.
