# PDFTOON v0.3.9

Una aplicación moderna de lectura de PDF para Android con interfaz tipo "biblioteca digital", historial persistente y modo de lectura inmersivo.

## 🚀 ¿Qué hay de nuevo en 0.3.9?
- Rediseño Material You (Material 3) con paleta verde y degradado en modo claro.
- Compatibilidad completa tema claro/oscuro con preferencia persistente y aplicada antes de inflar la UI (sin parpadeos).
- Fondo negro puro en modo oscuro para máximo contraste y accesibilidad; ajuste automático del color de iconos en la barra de estado.
- Corrección de superposición con la barra de estado usando WindowInsets y padding dinámico.
- Optimización del APK: R8 minify + shrinkResources, exclusión de META-INF y uso de vector drawables.
- Mockups de la nueva UI incluidos en assets (no influyen en recursos de la app).

## ✨ Interfaz Material You
- Fondo (claro): degradado verde de #C8FACC a #A6ECA8; componentes en blanco/verde con bordes ≥16dp y espaciado 12–24dp.
- AppBar con icono de inicio y opciones; barra de búsqueda prominente con botón de limpiar.
- Selector desplegable (AutoCompleteTextView), reproductor simple (play/pause + slider), botones "Seleccionar" y "Guardar".
- Campos de texto con placeholder “Lorem ipsum”, interruptores y FAB "Agregar PDF" (iconografía vectorial minimalista).

## 🌓 Tema claro / oscuro
- Preferencia `pref_dark` en SharedPreferences; se aplica al iniciar con `AppCompatDelegate.setDefaultNightMode(...)` para evitar flicker.
- Claro: degradado verde; Oscuro: fondo negro (near-black) con texto de alto contraste.
- Iconos de status bar ajustados según el modo para legibilidad.

## 🧭 Status bar e insets
- Sin superposición con la barra de estado: `ViewCompat.setOnApplyWindowInsetsListener` añade padding superior dinámico al contenedor raíz.
- `android:windowLightStatusBar` y lógica en tiempo de ejecución aseguran contraste correcto de iconos.

## 📦 Optimización del APK
- R8 activado (`minifyEnabled true`) y eliminación de recursos no usados (`shrinkResources true`).
- Exclusiones de empaquetado: `META-INF/DEPENDENCIES`, `LICENSE`, `NOTICE`, etc.
- Vector drawables en lugar de PNG donde aplica; conversión a WebP innecesaria (no hay PNGs en res).
- Mockups movidos a `app/src/main/assets/mockups/` para no romper el merge de recursos.

Impacto estimado por optimización (orientativo):
- R8 + shrinkResources: -10% a -25%.
- Exclusiones META-INF: -0.2 a -1.0 MB.
- Uso de vectores vs PNG: -10% a -30% por recurso (ya aplicado).

Tamaño actual del APK release (verificado): ~31 MB (`app-release.apk`).

## 🛠️ Instalación y uso
- Android 10 (API 29) o superior; permisos modernos vía SAF.
- Biblioteca con historial persistente, saludo personalizado y tarjeta "Continuar leyendo".

### Agregar un PDF
1) Toca el FAB "+". 2) Selecciona un PDF con SAF. 3) Se guarda en la biblioteca y podrás continuar luego.

### Buscar y filtrar
- Usa la barra de búsqueda; resultados en tiempo real. Selector con "Todos / Favoritos / Recientes".

### Tema
- Cambia el switch de tema en la pantalla principal. La preferencia persiste entre ejecuciones.

## ⚙️ Compilación y firma

### Comandos rápidos
```bash
./gradlew clean assembleRelease -x lint -x test
ls -lh app/build/outputs/apk/release/
```
- Artefacto: `app/build/outputs/apk/release/app-release.apk` (≈31 MB).
- Firma: si `keystore/keystore.properties` existe y es válido, se firma con tu keystore; si no, fallback a debug keystore (para no romper el build de pruebas).

Para AAB:
```bash
./gradlew bundleRelease
ls -lh app/build/outputs/bundle/release/
```

### Configurar firma (opcional)
1) Copia tu JKS a `keystore/KEYSTORE.jks`.
2) Crea `keystore/keystore.properties` desde el ejemplo:
```properties
storeFile=keystore/KEYSTORE.jks
storePassword=TU_PASSWORD
keyAlias=TU_ALIAS
keyPassword=TU_PASSWORD_ALIAS
```
3) Vuelve a compilar con `assembleRelease`.

## 📁 Mockups
- `app/src/main/assets/mockups/mockup_ui_light.svg`
- `app/src/main/assets/mockup_ui_dark.svg`

## 🧩 Arquitectura (resumen)
- MVVM con Room, LiveData/ViewModel, SAF para acceso a archivos, y `android-pdf-viewer` para renderizado.

## 🧪 Tests y Lint
```bash
./gradlew testReleaseUnitTest
./gradlew lintVitalRelease
```

## 🛡️ Accesibilidad
- Contraste AA/AAA cuando es posible, y objetivos táctiles ≥48dp para botones.

## 📄 Licencia
MIT. Ver `LICENSE`.

## 🤝 Contribución

Las contribuciones son bienvenidas. Por favor:
1. Fork del repositorio
2. Crear una rama para tu feature
3. Commit con mensajes descriptivos
4. Push a tu rama
5. Crear Pull Request

## 📞 Soporte

Para reportar bugs o solicitar features, crear un issue en GitHub con:
- Versión de Android
- Descripción detallada del problema
- Pasos para reproducir
- Screenshots si aplica

---

**PDFTOON v0.3.9** - Una experiencia de lectura de PDF moderna y completa para Android.
