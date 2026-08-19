# FA — Find Anonymity

> Periodically break the continuous trail of where your phone has been — on your own schedule,
> only when you want — and save battery while you're at it. Android, offline, no account, no telemetry.

**Support development:** [Ko-fi](https://ko-fi.com/eversorhn) · [PayPal](https://paypal.me/FAMarco) · Bitcoin `bc1qv92c3eyeqvhgfnez7spfd7v2aytkhpshsl65yv`

FA is a scheduler for your phone's radios. On intervals you define, it switches **Wi-Fi**, **mobile
data**, and **airplane mode** off and back on again, so your device stops emitting a gap-free,
always-on record of your whereabouts. It is deliberately **selective and punctual** — you choose
when and how often the gaps happen — and dropping the radios also **saves energy**. It can reboot
your device on a timer too, and includes an optional root duress panic-lock.

Under the hood it runs a foreground service that enforces your rules against wall-clock time
(anchor-based, so Doze slack never compounds) and shows a live status countdown in its notification
that you can stop from there.

## Features

- **Connectivity scheduler** — control **Wi‑Fi**, **mobile data**, and **airplane mode** by a
  repeating **cycle** (e.g. on 1 min every 3 min) or a **clock schedule** (on/off between two
  times on the weekdays you pick), plus always‑on / always‑off / unmanaged. Shrink your device's
  connectivity window to reduce trackability. Edit one rule, or all three at once.
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

Minimum **Android 12 (API 31)**; target Android 16 (API 36).

## Why scheduled reboots?

A reboot isn't just cleanup — it returns the phone to the **Before-First-Unlock (BFU)** state. Until
you enter your PIN/password again, the file-based encryption keys aren't in memory, so the data is
encrypted at rest and far harder to extract with forensic tools (most only work in the After-First-
Unlock state). Yes, you have to unlock again afterwards — that's the point: the phone sits locked and
protected.

Concrete scenario: someone grabs your phone and wraps it in foil (a Faraday bag) to stop a remote
wipe or "find my device". They don't know FA is installed — so the **scheduled reboot still fires
locally**, and the device drops to BFU on its own. Combine it with a short interval and, optionally,
the **panic-lock**, and a stolen-but-unlocked phone becomes a stolen-and-locked one without you
touching it.

**Forced reboot** (a per-rule option): normally the reboot is driven by FA's foreground service, so
tapping **Stop** on the notification cancels it — convenient for you, but a thief could do the same.
Turn on *Force reboot* and the reboot instead runs from a **detached privileged daemon** that
survives FA being stopped or killed, so the protective reboot fires regardless. (Root re-arms it on
every boot; under Shizuku it fires once, since Shizuku isn't running that early.)

## Install

Download the signed APK from the [latest release](../../releases/latest) and install it.
Verify the download against the published `SHA-256` checksum first.

**Updating:** FA has no auto-update (no network access by design). To update, download the newest
signed APK from Releases and install it over the old one — same signing key, so it updates in place.
The in-app **Settings → Updates** button opens the Releases page for you.

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

## Support

FA is free and offline. If it's useful to you:

- **Ko-fi:** https://ko-fi.com/eversorhn
- **PayPal:** https://paypal.me/FAMarco
- **Bitcoin:** `bc1qv92c3eyeqvhgfnez7spfd7v2aytkhpshsl65yv`

## License

[GNU GPL v3.0](LICENSE) © Marco Aurelio Fattizzo (eVersor‑HN).
