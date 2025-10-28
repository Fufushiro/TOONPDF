# PDFTOON v5.1.2

Una aplicación moderna de lectura de PDF para Android con interfaz tipo "biblioteca digital", historial persistente y modo de lectura inmersivo.

## 📱 Capturas de Pantalla

<p align="center">
  <img src="screenshots/home-screen.png" alt="Pantalla Principal" width="300"/>
</p>

## 🆕 ¿Qué hay de nuevo en 5.1.2?

### 🎨 Visor de PDF con Diseño Material You Mejorado
- **Toolbars semi-transparentes**: Efecto glassmorphism elegante con fondo blanco translúcido (#CCFFFFFF) que deja ver el PDF detrás
- **Diseño compacto y redondeado**: Barras con esquinas completamente redondeadas (24dp) y márgenes flotantes de 12dp
- **Elevación sutil**: Elevación de 6dp para un efecto de profundidad moderno
- **Tipografía optimizada**: Tamaños de fuente reducidos (16sp para páginas, 14sp para porcentaje) para mayor densidad
- **Colores Material You**: Uso consistente de la paleta de colores primaria y variantes de superficie

### ⚡ Animación Avanzada de Progreso de Lectura
- **Animación suave del progreso**: La barra de progreso se anima con interpolación suave al cambiar de página
- **Cambio de color gradual**: Transición progresiva de verde a rojo entre 85% y 100%
  - Verde primario (< 85%)
  - Transición gradual verde → rojo (85-99%)
  - Rojo intenso (#D32F2F) al 100%
- **Efecto de "temblor" (shake)**: La barra comienza a temblar al pasar del 80% de lectura
- **Velocidad del temblor aumenta**: El temblor se acelera gradualmente mientras te acercas al 100%
## ✨ Características Principales
- **Sincronización del color del texto**: El porcentaje cambia de color junto con la barra
### 📖 Visor de PDF Rediseñado (v5.1.2)
- **Toolbars semi-transparentes**: Efecto glassmorphism moderno con fondo blanco translúcido
- **Diseño Material You compacto**: Esquinas redondeadas (24dp), márgenes flotantes y elevación sutil
- **Animación avanzada de progreso**: Cambio de color gradual y efecto de temblor al acercarse al 100%
- **Modo de lectura inmersivo**: Los controles se ocultan automáticamente tras 2.5 segundos de inactividad
- **Navegación vertical**: Desplazamiento continuo por el documento (sin toques laterales para cambiar página)
- **Controles minimalistas**: Barra superior translúcida que aparece/desaparece con un toque
- **Bloqueo de rotación**: Orientación vertical por defecto para lectura cómoda
- **Doble toque para zoom**: Alterna entre ajuste de ancho y zoom 150%
### 🎯 Mejoras de Usabilidad
- **Barra de progreso más visible**: Altura aumentada de 8dp a 10dp (ahora reducida a 8dp para diseño compacto)
- **Espaciado optimizado**: Márgenes y padding ajustados para mejor aprovechamiento del espacio
- **Controles más accesibles**: Mayor área táctil y mejor visibilidad
- **Gestión inteligente de animaciones**: Cancelación automática en onPause y limpieza en onDestroy para evitar fugas de memoria



### 🏠 Nueva Pantalla de Inicio Rediseñada
- **Tarjeta destacada de última lectura**: Muestra el último PDF abierto con vista previa, progreso visual y acceso rápido
- **Saludo personalizado**: Mensaje de bienvenida dinámico en la pantalla principal
- **Sección de recientes**: Lista compacta de PDFs recientes con acceso directo
- **Estado vacío mejorado**: Indicaciones claras cuando no hay PDFs en el historial
- **Diseño Material You**: Tarjetas con bordes redondeados (16dp), elevación sutil y degradados modernos

### 📱 Compatibilidad Android 15 y Dispositivos de 16 KB
- **Soporte completo para páginas de 16 KB**: Cumple con los requisitos de Google Play (obligatorio desde Nov 2025)
- **NDK r27 actualizado**: Garantiza compatibilidad con Pixel 9 y dispositivos ARM v9+
- **Optimización de librerías nativas**: Todas las bibliotecas .so correctamente alineadas

### 📖 Visor de PDF Rediseñado
- **Modo de lectura inmersivo**: Los controles se ocultan automáticamente tras 2.5 segundos de inactividad
- **Navegación vertical**: Desplazamiento continuo por el documento (sin toques laterales para cambiar página)
- **Controles minimalistas**: Barra superior translúcida que aparece/desaparece con un toque
- **Bloqueo de rotación**: Orientación vertical por defecto para lectura cómoda
- **Doble toque para zoom**: Alterna entre ajuste de ancho y zoom 150%

### 📚 Sistema de Historial Avanzado
- **Posición exacta guardada**: Guarda no solo la página, sino el scroll exacto dentro de la página
- **Reanudar lectura**: Al reabrir cualquier PDF, continúa exactamente donde lo dejaste
- **Metadata completa**: Fecha de última lectura, progreso porcentual, páginas totales
- **Favoritos**: Marca tus PDFs importantes para acceso rápido
- **Gestión de historial**: Borra entradas individuales o limpia todo el historial

### 💾 Privacidad y Exportación
- **Datos 100% locales**: Todo se guarda en tu dispositivo, sin telemetría
- **Exportar/Importar historial**: Respalda tu progreso de lectura en formato JSON
- **Accesibilidad inteligente**: La app detecta si un archivo ha sido movido o eliminado

### 🎨 Interfaz Material You
- Fondo (claro): degradado verde de #C8FACC a #A6ECA8
- Componentes en blanco/verde con bordes ≥16dp y espaciado 12–24dp
- AppBar con icono de inicio y opciones; barra de búsqueda prominente
- FAB "Agregar PDF" con iconografía vectorial minimalista

### 🌓 Tema claro / oscuro
- Preferencia persistente entre sesiones
- Claro: degradado verde; Oscuro: fondo negro (near-black) con texto de alto contraste
- Iconos de status bar ajustados según el modo para legibilidad

## ✨ Características Principales

### Visor de PDF
- **Controles auto-ocultables**: Interfaz limpia que desaparece automáticamente
- **Scroll vertical continuo**: Navegación natural página por página
- **Guardado automático de progreso**: No pierdas nunca tu lugar de lectura
- **Pantalla completa**: Modo inmersivo que mantiene la pantalla encendida
- **Título abreviado**: Muestra el nombre del archivo sin extensión

### Gestión de Archivos
- **Storage Access Framework (SAF)**: Acceso moderno a archivos sin permisos invasivos
- **Compatibilidad Android 10-15**: Funciona en todas las versiones modernas
- **Detección de archivos movidos**: Notificación clara si un archivo cambió de ubicación
- **Búsqueda en tiempo real**: Filtra tu biblioteca instantáneamente

### Agregar un PDF
1. Toca el FAB "+" en la esquina inferior derecha
2. Selecciona un PDF usando el selector de archivos del sistema
3. El PDF se añade automáticamente a tu biblioteca

### Leer un PDF
1. Toca cualquier PDF de tu biblioteca
2. Lee con scroll vertical natural
3. Toca la pantalla para mostrar/ocultar controles
4. Doble toque para hacer zoom
5. Tu progreso se guarda automáticamente al salir


## 🛡️ Privacidad y Seguridad
- ✅ Sin telemetría ni analytics
- ✅ Sin conexión a internet requerida
- ✅ Datos almacenados localmente
- ✅ Exportación cifrable del historial (JSON local)
- ✅ Sin permisos de almacenamiento invasivos (usa SAF)


