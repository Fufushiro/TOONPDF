# Compatibilidad con Páginas de 16 KB - PDFTOON

## ⚠️ Requisito Importante

A partir del **1 de noviembre de 2025**, todas las aplicaciones nuevas y actualizaciones dirigidas a Android 15+ deben ser compatibles con dispositivos que usan páginas de memoria de 16 KB.

## 🔧 Soluciones Implementadas

### 1. **NDK Actualizado**
- **Versión NDK**: `27.0.12077973`
- Esta versión incluye soporte nativo para alineación de 16 KB en bibliotecas nativas (.so)

### 2. **Configuración de Packaging**
```kotlin
packaging {
    jniLibs {
        useLegacyPackaging = false  // CRÍTICO para alineación de 16 KB
        keepDebugSymbols.addAll(listOf("**/*.so"))
    }
}
```

### 3. **Filtros de ABI**
Se configuraron los ABIs necesarios para optimizar el tamaño del APK:
- `armeabi-v7a` (ARM 32-bit)
- `arm64-v8a` (ARM 64-bit) - **Arquitectura principal para 16 KB**
- `x86` (Intel 32-bit)
- `x86_64` (Intel 64-bit)

## 📋 Bibliotecas Nativas Afectadas

Las siguientes bibliotecas del visor PDF ahora están correctamente alineadas:
- `libc++_shared.so`
- `libjniPdfium.so`
- `libmodft2.so`
- `libmodpdfium.so`
- `libmodpng.so`

## ✅ Verificación

Para verificar la compatibilidad de tu APK:

```bash
# Compilar la aplicación
./gradlew clean assembleRelease

# Verificar alineación (requiere Android SDK build-tools)
# El APK debe estar alineado a 16 KB para todas las bibliotecas .so
```

## 🎯 Beneficios

1. **Compatibilidad futura**: Listo para Google Play a partir de nov 2025
2. **Mejor rendimiento**: En dispositivos con páginas de 16 KB (nuevos ARM)
3. **Sin rechazos**: Google Play no rechazará la app por incompatibilidad

## 📱 Dispositivos Afectados

Los dispositivos que más se benefician incluyen:
- Google Pixel 9 y posteriores
- Samsung Galaxy con Exynos recientes
- Dispositivos con ARM v9+ architecture
- ChromeOS con arquitecturas modernas

## 🔗 Referencias

- [Android 16 KB Page Size Guide](https://developer.android.com/16kb-page-size)
- [NDK r27 Release Notes](https://developer.android.com/ndk/downloads)

