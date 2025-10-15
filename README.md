# PDFTOON v0.1.3

Una aplicación moderna de lectura de PDF para Android con interfaz tipo "biblioteca digital", historial persistente y modo de lectura inmersivo.

## 🚀 Características Principales

### ✨ Nueva Interfaz Biblioteca Digital
- **Pantalla principal moderna y limpia** con diseño minimalista
- **Saludo personalizado** basado en la hora del día
- **Barra de búsqueda** para encontrar documentos rápidamente
- **Sección "Continuar leyendo"** que destaca el último PDF abierto
- **Lista de documentos recientes** con información detallada de progreso

### 📚 Historial Persistente
- **Base de datos local** que guarda automáticamente todos los PDFs abiertos
- **Seguimiento de progreso** con última página leída y porcentaje completado
- **Fecha de última lectura** para cada documento
- **Continuación automática** desde donde se quedó la lectura

### 📖 Visor de PDF Avanzado
- **Modo de pantalla completa inmersivo** que oculta completamente las barras del sistema
- **Barra de progreso visual** dentro y fuera del visor
- **Controles intuitivos** para navegación entre páginas
- **Información en tiempo real** de página actual y progreso total
- **Tap para mostrar/ocultar controles** en modo normal

### 🔧 Funcionalidades Técnicas
- **Storage Access Framework (SAF)** para selección segura de archivos
- **Arquitectura MVVM** con Room Database para persistencia
- **Renderizado optimizado** con manejo en segundo plano
- **Verificación de archivos** con manejo de errores si el PDF fue movido o eliminado

## 📱 Capturas de Pantalla

### Pantalla Principal
- Interfaz limpia con saludo personalizado
- Sección "Continuar leyendo" destacada
- Lista de documentos recientes con progreso visual

### Visor de PDF
- Modo normal con controles y barra de progreso
- Modo pantalla completa inmersivo
- Información detallada de páginas y porcentaje

## 🛠️ Instalación y Uso

### Requisitos del Sistema
- Android 10.0 (API nivel 29) o superior
- Espacio de almacenamiento para la base de datos local

### Cómo Usar la Aplicación

#### 📂 Agregar un PDF
1. Toca el botón flotante **"+"** en la esquina inferior derecha
2. Selecciona un archivo PDF desde tu almacenamiento
3. El PDF se abrirá automáticamente y se guardará en tu biblioteca

#### 📖 Continuar Leyendo
1. En la pantalla principal, encuentra la tarjeta **"Continuar leyendo"**
2. Toca **"Continuar"** para reanudar desde la última página leída
3. El progreso se actualiza automáticamente mientras lees

#### 🔍 Buscar Documentos
1. Usa la barra de búsqueda en la parte superior
2. Escribe el nombre del archivo que buscas
3. Los resultados se filtran en tiempo real

#### 🖥️ Modo Pantalla Completa
1. Mientras lees un PDF, toca el botón **"Pantalla completa"**
2. La aplicación ocultará todas las barras del sistema
3. Toca la pantalla para mostrar temporalmente los controles
4. Usa el botón atrás o el mismo botón para salir del modo pantalla completa

#### 📊 Ver Progreso
- **En la lista principal**: Cada PDF muestra una barra de progreso y porcentaje
- **Dentro del visor**: La barra inferior muestra página actual, total y porcentaje
- **Información detallada**: "Página X / Y • Z%" en tiempo real

#### 🗑️ Eliminar del Historial
1. En la lista de documentos, toca el icono de **eliminar** (🗑️)
2. El PDF se eliminará del historial (no del almacenamiento)
3. Puedes volver a agregarlo seleccionándolo nuevamente

## 🔄 Cambios en la Versión 0.1.3

### 🎨 Interfaz Completamente Rediseñada
- **Nueva pantalla principal** con estilo biblioteca digital minimalista
- **Colores y tipografía modernos** con jerarquía visual clara
- **Tarjetas con sombras suaves** y bordes redondeados
- **Sección "Continuar leyendo"** con fondo coral destacado

### 🗄️ Historial Persistente
- **Base de datos Room** para almacenamiento local confiable
- **Seguimiento automático** de progreso de lectura
- **Información detallada** por cada PDF: nombre, páginas, progreso, fecha

### 📈 Sistema de Progreso Avanzado
- **Cálculo automático** de porcentaje leído
- **Barra de progreso visual** en lista principal y visor
- **Persistencia de última página** leída por documento
- **Actualización en tiempo real** durante la lectura

### 🖥️ Modo Inmersivo Completo
- **Ocultación total** de barras de estado y navegación del sistema
- **Controles por tap** para mostrar/ocultar elementos temporalmente
- **Experiencia de lectura sin distracciones**
- **Mantenimiento de pantalla activa** durante la lectura

### 🔧 Mejoras Técnicas
- **Arquitectura MVVM** con separación clara de responsabilidades
- **Storage Access Framework** para manejo moderno de archivos
- **Manejo de errores robusto** para archivos inexistentes
- **Tests unitarios** para validación de cálculos y operaciones

### 🚀 Rendimiento Optimizado
- **Renderizado en segundo plano** para evitar bloqueos de UI
- **Carga lazy** de elementos de lista
- **Gestión eficiente de memoria** durante la lectura
- **Animaciones suaves** en transiciones de interfaz

## ⚙️ Arquitectura Técnica

### 🏗️ Patrón MVVM
```
MainActivity ↔ MainViewModel ↔ PdfRepository ↔ PdfHistoryDao ↔ Room Database
PdfViewerActivity ↔ MainViewModel
```

### 🗃️ Base de Datos
- **Room Database** con entidad `PdfHistoryEntity`
- **Campos**: URI, nombre, páginas totales, última página, fecha, tamaño
- **Operaciones**: CRUD completo con LiveData para actualizaciones reactivas

### 🎯 Componentes Principales
- **MainActivity**: Pantalla biblioteca con lista de PDFs
- **PdfViewerActivity**: Visor con modo pantalla completa
- **PdfHistoryAdapter**: RecyclerView adapter con DiffUtil
- **MainViewModel**: Lógica de negocio y manejo de datos

## 🧪 Testing

### ✅ Tests Unitarios
- **Cálculo de porcentajes** de progreso de lectura
- **Validación de entidades** PDF con diferentes escenarios
- **Casos edge** como documentos vacíos o páginas únicas

### 🔬 Tests Instrumentados
- **Operaciones de base de datos** con Room testing
- **Inserción, actualización y eliminación** de registros
- **Ordenamiento por fecha** y recuperación de datos
- **Casos de reemplazo** y manejo de duplicados

## 🔒 Permisos

La aplicación utiliza permisos modernos y seguros:
- `READ_MEDIA_DOCUMENTS` (Android 13+)
- `READ_EXTERNAL_STORAGE` (hasta Android 12)

## 🏗️ Compilación

### Requisitos de Desarrollo
- Android Studio Hedgehog o superior
- Kotlin 1.9+
- Gradle 8.0+
- SDK mínimo: Android 10 (API 29)
- SDK objetivo: Android 14 (API 36)

### Dependencias Principales
- **Material Design 3** para UI moderna
- **Room Database** para persistencia local
- **Lifecycle & ViewModel** para arquitectura MVVM
- **android-pdf-viewer** para renderizado de PDF

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver `LICENSE` para más detalles.

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

**PDFTOON v0.1.3** - Una experiencia de lectura de PDF moderna y completa para Android.
