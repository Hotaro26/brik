## brik v2.0.0 — Major Release

### Assistive Quick Controls (iOS AssistiveTouch Style)

- Fluid Morphing Container: Floating control ball smoothly expands and morphs into a Material 3 Expressive quick controls card with spring physics.
- Edge Snapping: Draggable freely across the display; automatically glides and docks to the nearest screen edge on release.
- Inactivity Auto-Hide: Fades and dims down after an idle period, with configurable timeout intervals (2s, 3s, 5s, or Always On).
- Dedicated Settings Window: Accessible via Miscellaneous settings to configure visible quick tiles (Wi-Fi, Mobile Data, Hotspot, Flashlight, Bluetooth, Do Not Disturb, and Phone Caller).
- Notification Shade Access: Tapping the notification pill directly expands the system notification shade.
- Phone Caller Tile: Added a phone caller tile replacing emergency calls, with an editable visibility switch in Assistive Settings.
- LockTask Isolation: Interactions on the floating ball and expanded card consume touch events, preventing accidental kiosk exit triggers.
- Redrawn Vector Drawables: Clean 24x24 Material vector icons for Wi-Fi, Hotspot, and Flashlight.

### Kiosk Launcher Configuration

- Dedicated Kiosk Settings Window: Configure lockdown parameters and call handling directly from Miscellaneous settings.
- Incoming Calls Window: Allow incoming call overlay window without breaking kiosk lockdown.
- Auto-Resume Session: Automatically re-locks into kiosk mode once a phone call concludes.
- Configurable Exit Hold Timer: Customize required background hold duration (1s, 2s, 3s, or 5s) before revealing exit controls.
- Lockdown Policies: Configure restrictions for notification shades and system navigation gestures.

### Routines & Time Display

- Global Clock Format: Consistent 12-hour and 24-hour clock formatting across routines, limits, and kiosk clocks.
- Routine Bottom Sheet: Routine creation and detail editors respect the selected clock format, pairing dynamic time formatting with an adaptive Material 3 TimePicker.
- Smaller AM/PM Indicator: Kiosk modes format 12-hour clocks with large digits and reduced-size uppercase AM/PM indicators aligned at the baseline.

### Miscellaneous & UI Polish

- Multi-Style Pull-Down Animations: Selectable Canvas vector pull-refresh indicators including Smiley, Cat, Butterfly, Books, Man in Lotus, or Random Mixed mode, accompanied by a live animated preview card.
- Nested Back Navigation Architecture: Implemented an intermediate background sub-screen layer for multi-level settings stacks, delivering seamless predictive back transitions without exposing the root Settings tab.

### App Manager & Bags

- Full-Width Search: Header search bar spans full width with pull-to-refresh system apps toggle.
- Custom Limits: Direct numeric input field for app usage limits with suspension or hiding enforcement.
- Two-Step Bag Wizard: Extended floating action button in Bags window leading to a streamlined two-step creation bottom sheet.
