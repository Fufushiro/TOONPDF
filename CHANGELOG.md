# Changelog

All notable changes to this project will be documented in this file.

## [0.3.9] - 2025-10-16

### Added
- 

### Changed
- 

### Fixed
- 

## [0.3.0] - 2025-10-15

### Added
- Modern, persistent storage access using Android Storage Access Framework (SAF):
  - Open documents with persistable URI permissions (works on Android 11–15).
  - Optional folder access via "OpenDocumentTree" toggle in Ajustes to grant long-term access.
- Settings screen (Ajustes) with:
  - Username input to personalize the greeting.
  - Theme selector: System, Light, Dark, and AMOLED (pure black) with persistent preference.
  - Avatar picker: change the home/start photo at any time; selection persists across restarts.
- AMOLED dark theme with pure black backgrounds and high-contrast text.
- Material You (Material 3) redesign for the main screen: green palette, rounded corners (≥16dp), generous spacing, and updated components (AppBar, search bar, dropdown selector, simple media controls, action buttons, text fields, storage switch, and Add PDF FAB).

### Changed
- Replaced legacy storage permission prompts with the official Android system dialogs.
- Improved main UI contrast and ensured usage of theme colors across components.
- Greeting now includes the user’s configured name (e.g., "Buenos días, Ana").
- Light mode uses a green gradient background; dark mode uses pure black background for accessibility.

### Fixed
- Theme toggle now correctly switches between light/dark, persists across restarts, and is applied before inflating UI to avoid flicker.
- Status bar no longer overlaps content; dynamic top padding via WindowInsets applied to the root container.
- Status bar icon/text color automatically adjusted for readability depending on theme.
- Several minor stability and accessibility issues when opening or relocating PDFs.

### Performance/Build
- Enabled R8 minification and resource shrinking for release builds; removed unused META-INF entries.
- Prefer vector drawables; moved mockup SVGs to assets so they don’t affect resource merging.
- Resulting release APK size observed ≈ 31 MB (may vary by device/ABI).

### Compatibility
- Verified behavior against Android 11 (API 30) through Android 15 (API 35) using SAF.
- Avoids Play Store-restricted storage permissions; no legacy READ_EXTERNAL_STORAGE required.

### Developer Notes
- DataStore/SharedPreferences keys: `app_theme`, `user_name`, `user_avatar_uri`, `storage_tree_uri`, `pref_dark`.
- Theme is applied early in Activities via `ThemeUtils.applyTheme(...)` and `AppCompatDelegate.setDefaultNightMode(...)`.

## [0.2.4] - 2025-09-XX
- Previous minor fixes and improvements.
