# Changelog

All notable changes to this project will be documented in this file.

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

### Changed
- Replaced legacy storage permission prompts with the official Android system dialogs.
- Improved main UI contrast (dark text on light background) and ensured text uses theme colors.
- Greeting now includes the user’s configured name (e.g., "Buenos días, Ana").

### Fixed
- Storage permission issues where acceptance was not persisted or visible in system settings.
- Inability to modify the home/start photo after adding it once.
- Several minor stability and accessibility issues when opening or relocating PDFs.

### Compatibility
- Verified behavior against Android 11 (API 30) through Android 15 (API 35) using SAF.
- Avoids Play Store-restricted storage permissions; no legacy READ_EXTERNAL_STORAGE required.

### Developer Notes
- DataStore keys added: `app_theme`, `user_name`, `user_avatar_uri`, `storage_tree_uri`.
- Theme is applied early in Activities via `ThemeUtils.applyTheme(...)`.

## [0.2.4] - 2025-09-XX
- Previous minor fixes and improvements.


