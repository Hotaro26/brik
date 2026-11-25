# Brik

Brik is a privileged Android digital enforcement and focus application designed to eliminate digital distractions through immutable commitment. Rather than relying on easily bypassable timers, accessibility services, or overlay permissions, Brik interfaces directly with Android platform management APIs--including Device Policy Manager (Device Owner), Dhizuku, Shizuku, and Root--to enforce package suspension, app concealment, and uninstallation protection.

---

## Philosophy & Motivation

Most mobile blockers fail because they rely on self-regulation at the exact moment when self-regulation is weakest. Accessibility-based blockers can be turned off in two taps, overlays can be swiped away, and standard applications can simply be uninstalled when an impulse strikes.

Brik was built out of willpower: the realization that true focus in an attention-economy environment requires absolute, physical friction. By establishing an unyielding commitment through Android enterprise management APIs, Brik treats distractions as non-negotiable boundaries. Once a bag of applications is locked with a physical key or an external QR token, the friction required to reverse the decision forces intentionality and breaks compulsive dopamine feedback loops.

## Screenshots
| | | | |
| :---: | :---: | :---: | :---: |
| <img width="160" alt="Screenshot_20261125-155044_Brik" src="https://github.com/user-attachments/assets/a65f9566-9260-4f36-8bfb-3154e36b4757" /> | <img width="160" alt="Screenshot_20261125-163327_Brik" src="https://github.com/user-attachments/assets/55d545e4-a27b-47b2-95f4-ccc6ad2b1fb4" /> | <img width="160" alt="Screenshot_20261125-164329_Brik" src="https://github.com/user-attachments/assets/16dcb4bb-ddb9-41d0-a722-84cd935c6fd4" /> | <img width="160" alt="Screenshot_20261125-164333_Brik" src="https://github.com/user-attachments/assets/17f6fc91-6078-4506-b029-31dd9445877c" /> |
| <img width="160" alt="Screenshot_20261125-164338_Brik" src="https://github.com/user-attachments/assets/95cc7e78-4cb9-49af-9a92-3ea3be04ac61" /> | <img width="160" alt="Screenshot_20261125-164722_Brik" src="https://github.com/user-attachments/assets/e08147d6-336c-4b13-b309-c256819c4060" /> | <img width="160" alt="Screenshot_20261125-164903_Brik" src="https://github.com/user-attachments/assets/1af601d8-429f-4605-860f-618c6089d623" /> | <img width="160" alt="Screenshot_20261125-165011_Brik" src="https://github.com/user-attachments/assets/ad04ea08-53b6-42fc-8eb9-b2c654ee9db9" /> |

## Privileged Architecture

Brik operates across a four-tier privilege hierarchy, automatically selecting the highest capability bridge available while gracefully degrading based on device configuration:

1. **Device Owner (Device Policy Manager - DPM)**
   The highest privilege level on non-rooted Android. Provides kernel-level package suspension, silent app hiding, single-task kiosk mode, and anti-tamper uninstallation prevention. Brik cannot be uninstalled or force-stopped while Device Owner is active.
2. **Dhizuku Bridge**
   Leverages an existing Device Owner proxy to grant Brik enterprise policy enforcement capabilities without requiring direct Device Owner status.
3. **Shizuku Bridge**
   Utilizes user-space ADB permission delegation to enforce package state changes, hiding, and management without root access.
4. **Root Shell (libsu)**
   Direct root execution utilizing SuperSU/Magisk/KernelSU for rooted environments where Device Owner provisioning is undesirable.

---

## Core Capabilities

### 1. App Bags & Targeted Grouping
- Create custom application bundles (e.g., "Distractions", "Social Media", "Gaming").
- Bulk-select installed third-party and system applications using a fast package scanner.
- Manage multiple independent bags with distinct locking states.

### 2. Flexible Commitment Mechanisms
- **Alphanumeric Password**: Protect bags with a secret passphrase (self-imposed or entrusted to an accountability partner).
- **Random QR Token**: Generates a cryptographically random UUID token rendered as a QR code. The token can be exported, stored on a separate physical machine, or printed, preventing impulsive unlocking.
- **External Physical QR Scanner**: Pair an app bag with any arbitrary barcode or physical QR code existing in the real world (e.g., a barcode on a book or an object in another room). The bag cannot be unlocked without physically walking over and scanning that real-world object.

### 3. Policy Enforcement Engines
- **Package Suspension (App Blocking)**: Freezes packages at the OS level. Suspended applications have their icons dimmed by the launcher and cannot be launched by the user or background tasks.
- **App Hiding (Launcher Invisibility)**: Completely unregisters application launcher activities. Target apps disappear entirely from the home screen and app drawer.
- **Single-App Kiosk Mode**: Locks the device into a designated productive application, disabling home navigation and recents.
- **Anti-Tamper Uninstallation Lockout**: Revoking Device Owner status requires verifying a pre-configured security key (password or QR code). Non-DPM privilege switches are automatically concealed when Device Owner is active.

### 4. Privacy & Network Tools
- **Private DNS (DNS over TLS)**: In-app management of Android global private DNS settings.
- **Preset Providers**: One-tap configuration for AdGuard (ad/tracker blocking), Cloudflare, NextDNS, Quad9, Google, and Mullvad.
- **Custom DoT Hostname**: Specify custom TLS endpoints for private filtering profiles.

### 5. Stealth Mode & Dialer Code Recovery
- Completely conceal Brik from the Android launcher and home screen.
- Reappear and launch the application on demand by typing a secret code sequence into the native phone dialer (`*#*#<dialer_code>#*#*`, default: `*#*#2745#*#*`).
- Configurable custom PIN for dialer execution.

### 6. Interface & Customization
- **Mizan Detaching Rounded Windows**: Fluid surface detaching motion with pull-to-refresh spinner and tactile haptic tick feedback.
- **Material 3 Expressive UI**: Clean segmented card structures and standardized icon containers.
- **Pure AMOLED Black Dark Theme**: Pitch black surfaces (`#000000`) across all dark palettes for minimal battery draw on OLED displays.
- **10 Curated Color Palettes**: Dynamic Material You, Brik Terracotta, Classic Green, Cherry Red, Navy Blue, Lavender, Pink, Mocha, Catppuccin, and Monochrome.
- **Color-Harmonized Status Bar**: Edge-to-edge status bar background matching the active secondary container tone.
- **Predictive Back Navigation**: Custom predictive gesture transitions featuring slide scaling, dynamic corner radius rounding, and realistic shadow projection.

---

## Setup & Installation

### Prerequisites
- Android 8.0 (API level 26) or higher.
- A computer with `adb` (Android Debug Bridge) installed, or an active Shizuku/Dhizuku installation.

### Method A: Provisioning as Device Owner (Recommended)
Device Owner provides the strongest enforcement and prevents Brik from being uninstalled.

1. Install the Brik release APK on your device:
   ```bash
   adb install -r Brik-v1.0.0.apk
   ```
2. Remove all existing user accounts from the device (Settings -> Passwords & Accounts). Android restricts Device Owner provisioning if user accounts already exist. You can re-add your accounts immediately after provisioning.
3. Remove any existing Work Profile or Dual App profiles.
4. Execute the Device Owner provisioning command via ADB:
   ```bash
   adb shell dpm set-device-owner com.hotaro.brik/.admin.BrikAdminReceiver
   ```
5. Open Brik. In the Settings tab, the bridge indicator will display "Device Owner". Re-add your accounts.

### Method B: Dhizuku Setup
If you already use Dhizuku to manage device owner permissions:
1. Open Dhizuku.
2. Grant authorization to Brik.
3. Open Brik -> Settings -> select Dhizuku Bridge.

### Method C: Shizuku Setup
1. Start Shizuku via wireless debugging or ADB.
2. Launch Brik and authorize the Shizuku permission prompt.
3. Brik will immediately use Shizuku for package suspension and concealment.

---

## Day-to-Day Workflow

### Locking an App Bag
1. Open Brik and navigate to the **Tools** tab.
2. Select **Bags** -> Create Bag.
3. Choose the packages to group into the bag.
4. From the **Key** screen or **Bags** manager, tap **Lock**.
5. Select your locking mechanism:
   - **Password**: Enter an alphanumeric key.
   - **QR Key (Random)**: Copy or save the generated QR code to a secondary device.
   - **QR Key (Scan External)**: Scan an existing barcode or physical QR code in your environment.
6. Confirm the lock. The applications in the bag are immediately suspended or hidden.

### Unlocking an App Bag
1. Open Brik -> **Key** screen.
2. Tap the central Key icon to reveal the **Unlock** action.
3. Select the locked bag.
4. Supply the password or scan the corresponding QR key using the integrated CameraX scanner.
5. Upon verification, the applications are instantly restored.

### Configuring Stealth Mode
1. Navigate to **Tools** -> **Stealth Mode**.
2. Set your desired secret dialer code (numeric digits only).
3. Toggle "Hide Brik from Launcher".
4. To open Brik subsequently, open your phone's dialer app and dial:
   ```text
   *#*#<code>#*#*
   ```

---

## Technical Specifications

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3
- **Local Persistence**: SharedPreferences and Room Database
- **Networking**: DevicePolicyManager Private DNS integration
- **Camera Engine**: CameraX with ZXing barcode decoding
- **Privilege Interfacing**: Android DPM, Rikka Shizuku API, Rosan Dhizuku API, TopJohnWu libsu
- **Minification**: R8 optimizer with specialized keep rules for IPC interfaces

---

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.

## Star History

<a href="https://www.star-history.com/?repos=hotaro26%2Fbrik&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=hotaro26/brik&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=hotaro26/brik&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=hotaro26/brik&type=date&legend=top-left" />
 </picture>
</a>
