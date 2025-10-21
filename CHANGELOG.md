# Changelog

All notable changes to this project will be documented in this file.

## [5.0.0] - 2025-10-20

### Added
- **Soporte completo de internacionalización (i18n):**
  - Todos los textos de la interfaz ahora soportan español e inglés.
  - Agregados 14 nuevos strings traducidos en `values/strings.xml` y `values-en/strings.xml`.
  - La aplicación cambia automáticamente de idioma según la configuración del sistema.

### Changed
- **Actualización de información del desarrollador:**
  - El nombre del desarrollador en la sección "Acerca de" ahora muestra "fufushiro" en lugar de "PDFTOON Team".
  - Actualizado en ambos idiomas (español e inglés).
- **Mejoras en el toolbar:**
  - Eliminado el icono de navegación de inicio (casa) del toolbar para una interfaz más limpia.
  - El toolbar ahora muestra solo el título y el menú de opciones.
- Reemplazados todos los textos hardcodeados en layouts XML por referencias a recursos de strings.
- Mejorada la accesibilidad con `contentDescription` consistentes usando recursos de strings.
- Versión estable con todas las características implementadas.

### Removed
- **Limpieza de código - eliminados 6 archivos XML innecesarios:**
  - `fragment_home.xml` (reemplazado por `fragment_new_home.xml`)
  - `fragment_pdfs.xml` (no usado)
  - `activity_changelog.xml` (no usado)
  - `activity_pdf_viewer.xml` (no usado)
  - `view_card_last_pdf.xml` (no usado)
  - `item_changelog_entry.xml` (no usado)
- Reducción del tamaño del código y mejora en mantenibilidad.

### Fixed
- Corregido uso de `app:tint` en lugar de `android:tint` para compatibilidad con Material Design.
- Agregado namespace `xmlns:tools` donde era necesario para herramientas de desarrollo.

### Technical
- Actualización de recursos de strings en `values/strings.xml` y `values-en/strings.xml`.
- Simplificación del layout del MaterialToolbar en `activity_main.xml`.

## [4.9.9] - 2025-10-18

### Added
- Hoja inferior de opciones para PDFs (bottom sheet):
  - Nuevos layouts y drawables para fondo y tirador.
- Diálogo de información del PDF con metadatos clave.
- Nuevos recursos de iconos adaptativos (anydpi-v26) para launcher.

### Changed
- Interfaz y lógica actualizadas en Activities, ViewModels y Repositorios para soportar las nuevas vistas y flujos.
- Sustitución de iconos legacy PNG por recursos modernos WebP y XML vectoriales/adaptativos.
- Ajustes de estilos/temas, arrays y colores para una apariencia más consistente.
- Mejora de documentación en `README.md`.

### Removed
- Eliminados recursos PNG legacy de mipmap (hdpi, xhdpi, xxhdpi, xxxhdpi) y definiciones antiguas anydpi.

### Fixed
- Ajustes en configuración de build (ProGuard/R8, `gradle.properties`) para builds más estables y predecibles.
- Correcciones menores en preferencias y configuración de firma.

### Build
- `versionName` establecido a 4.9.9 y artefactos de release generados (universal y por ABI).

## [4.5.1] - 2025-10-17

### Added
- **Nueva pantalla de inicio rediseñada:**
  - Tarjeta destacada de última lectura con vista previa del PDF, progreso visual y metadatos.
  - Saludo de bienvenida personalizado ("Bienvenido de vuelta").
  - Sección de PDFs recientes con lista compacta para acceso rápido.
  - Estado vacío mejorado con instrucciones claras.
  - Diseño Material You con tarjetas de bordes redondeados (16dp) y degradados modernos.
- **Compatibilidad con Android 15 y páginas de 16 KB:**
  - Actualización a NDK r27 para soporte de dispositivos con páginas de 16 KB.
  - Alineación correcta de librerías nativas (.so) según requisitos de Google Play.
  - Compatibilidad garantizada con Pixel 9, Samsung Galaxy con Exynos recientes y ARM v9+.
- **Mejoras de rendimiento:**
  - Optimización de compilación con Gradle (30-50% más rápido).
  - Reducción del tamaño del APK (10-15% menor).
  - Configuración mejorada de R8 y ProGuard.

### Changed
- La interfaz principal ahora muestra directamente las tarjetas de PDFs en lugar de usar ViewPager por defecto.
- El ViewPager2 se mantiene disponible pero oculto, reservado para navegación de historial.
- Mejoras en la experiencia de usuario con animaciones y transiciones más fluidas.

### Fixed
- Corrección de APIs deprecadas en MainActivity usando WindowCompat moderno.
- Resolución de advertencias de compilación para build limpio (0 warnings).
- Mejoras en el manejo de errores para archivos no encontrados o corruptos.

## [4.3.4] - 2025-10-17

### Added
- **Rediseño completo del visor de PDF:**
  - Controles de interfaz que se ocultan automáticamente para una experiencia de lectura inmersiva.
  - Navegación mediante desplazamiento vertical.
  - Gesto de un toque para mostrar/ocultar la interfaz.
  - Tarjeta de navegación al final del documento para volver al listado o abrir el siguiente archivo.
- **Nueva barra de navegación inferior:**
  - Diseño minimalista con acceso rápido a "Home", "Historial" y un botón central prominente para reanudar la última lectura.
- **Historial de lectura:**
  - Guarda automáticamente el progreso de cada archivo.
  - Permite reanudar la lectura desde la última posición.
  - Gestión de entradas individuales y opción para limpiar el historial.
- **Mejoras de interacción y gestos:**
  - Doble toque para alternar el zoom.
  - Menús contextuales al mantener pulsado un archivo.
- **Personalización y accesibilidad:**
  - Bloqueo de rotación configurable.
  - Mejoras de contraste y etiquetas para accesibilidad.
- **Privacidad y gestión de datos:**
  - Todos los datos se guardan localmente.
  - Funcionalidad para exportar e importar el historial de lectura en formato JSON.
- **Estética moderna:**
  - Interfaz con esquinas redondeadas, sombras sutiles y tipografía actualizada.

### Changed
- La versión de la aplicación se ha actualizado a 4.3.4.
- El comportamiento de toque en los laterales del visor ha sido eliminado en favor del desplazamiento vertical.

### Fixed
- Mejorado el manejo de errores para archivos no encontrados o corruptos.

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
