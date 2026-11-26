## brik v1.0.8 — Ascended Release

### Glass Navigation

- Imported liquid glass navigation bar from Mizan/QuranReader project
- Added Glass Navigation as a third navbar style option alongside Floating and Classic
- Glass nav uses a floating translucent tab bar powered by a layered backdrop blur effect
- Unlike the original, the glass bar never collapses or hides on scroll — it stays permanently visible
- Requires Android 12+ and a non-low-RAM device; falls back to floating style on unsupported hardware
- Fixed a RenderThread stack overflow crash caused by the glass nav being placed inside the layer backdrop recording scope; moved all nav variants outside the backdrop-recorded content box

### Bottom Sheet Fixes

- Fixed a solid gap appearing at the bottom of the icon chooser and colour scheme bottom sheets in Settings
- Root cause was double-padding from both a hardcoded `padding(bottom = 36.dp)` and the system navigation bar insets; replaced with a single `navigationBarsPadding()` call
- Icon chooser sheet now opens at half screen height by default instead of expanding to full height

### Wind Down Routine Window

- Added a secondary back arrow background to the Wind Down routine window for consistent visual treatment across sub-screens

### Mode Switch Toast Removed

- Removed the toast notification that appeared when switching between Key mode and Zen mode by pulling from the top of the Key screen

### Previous Release (v1.0.7)

- Added optional key guard for DNS lock in settings bottom sheet
- Minimalist kiosk launcher for focus profiles: at any time, either target app blocking or kiosk mode can be active, not both
- Kiosk mode exit requires a 3-second press on screen, same as Zen mode
- Two kiosk sub-modes: pin to single window with system gestures and panels enabled, or pin and disable system gestures and panels entirely
- Kiosk launcher section shown when configuring a focus profile
- Categorized Wind Down routine screen and mode configuration screens with Tomato expandable sections, same visual treatment as Settings
- Any large card or content block can be smoothly expanded on tap
- Changed the routine add button from a plus circle to a rounded rectangle with an Add label
- Hid the Key mode / Zen mode pill from the top-right corner of the Key screen
- Hid the pull-to-switch-to-Zen hint text on the Key screen
- Added Material 3 Expressive rotating cookie shape on the Key home screen
- Pull-to-refresh on the Key home screen switches between Key mode and Zen mode
- On pulling down three times from the Tools window within five seconds, a bottom sheet opens to show or hide individual cards (Zen mode, Private DNS, etc.)
- Categorized Grayscale, Auto-Mode Routines, and Focus Mode configuration screens with expandable Tomato sections
- Fixed Grayscale not applying to targeted apps; added Accessibility Service integration for per-app grayscale enforcement
- Categorized Zen window contents with Tomato sections and expandable cards
- Removed the static bottom Enter Zen Mode pill; replaced with a floating pill repositioned to the bottom-right as a soft-edged rectangle
