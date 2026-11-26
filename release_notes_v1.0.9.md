## brik v1.0.9 — Ascended Release

### Assistive Quick Controls (iOS AssistiveTouch Style)

- Fluid Morphing Container: The floating AssistiveTouch ball now smoothly morphs with spring physics from a 56dp circular disc into the 290x266dp Material 3 Expressive quick controls card and back on dismiss.
- Smooth Edge Docking: When dragged and released anywhere away from the screen borders, the ball automatically springs to the nearest horizontal edge (left or right).
- Inactivity Auto-Hide: Floating ball automatically dims down to 20% opacity after an idle period. Configurable delay options (2s, 3s, 5s, Off) are available in settings.
- Movable Quick Controls Card: The expanded 2x2 Bento card can be dragged and repositioned anywhere on the screen.
- Kiosk Exit Isolation: Touch interactions on the floating ball and expanded card consume gestures, preventing accidental activation of the 3-second kiosk exit timer.
- Dedicated Assistive Vector: Custom concentric vector icon for AssistiveTouch displayed across settings cards and headers.
- Available across all kiosk modes: Zen Mode, Wind Down bedtime desktop, and Focus Mode kiosk.

### App Manager Improvements

- Full-Width Search Bar: Removed the visibility toggle pill beside the searchbar, allowing search input to span the full header width.
- Pull to Show/Hide System Apps: Added pull-to-refresh gesture on the app list that toggles system app visibility with toast feedback.
- Custom Limit Placeholder: Replaced preset limit chips and sliders with a direct numeric input field, allowing custom daily minute limits.
- Card Expansion: Tap any app card to inspect today's usage and configure daily limits with suspension or hiding enforcement.

### Bags Window Wizard

- Extended Floating Action Button: Added a rounded-rectangle Add action button to the Bags window.
- Two-Step Creation Wizard: Step 1 provides app selection with search and a system apps visibility toggle (system apps hidden by default); Step 2 provides bag naming with quick suggestions.

### Kiosk Clock Formatting

- Smaller AM/PM Indicator: Kiosk modes (Zen Mode, Wind Down, and Focus Mode kiosk) now format 12-hour clocks with large time digits and a reduced-size uppercase AM/PM indicator aligned at the baseline.

### Settings

- Added Miscellaneous section in Settings with 12-hour / 24-hour clock format preference.
