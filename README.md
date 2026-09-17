# Encaja

Proyecto Android de la app "Encaja" — coordinación familiar de turnos,
cuidado de niños, menú y compra. Arquitectura en capas: UI Compose →
ViewModel → dominio → repositorios (Room + Firestore).

## Estado actual

### `:domain` — ✅ compilado y verificado
Kotlin puro, sin Android. Modelos, casos de uso (`CalcularHuecosDelDia`,
`ProponerCuidadores`, `CalcularRepartoSemanal`) y tests. Compilado y
ejecutado en este entorno con `kotlinc`: **17 comprobaciones en verde**
entre el dominio y el mapper de UI (ver más abajo).

### `:app` — parcialmente verificado
- `SemaforoUiState.kt`, `SemaforoUiStateMapper.kt`, `DatosEjemploFamilia.kt`
  → **✅ Kotlin puro, compilado y testeado aquí mismo** (11 comprobaciones
  en verde), con los datos de ejemplo de la familia Oliver Izquierdo.
- `SemaforoViewModel.kt`, `SemanaScreen.kt`
  → **⚠️ código completo para Android Studio, NO compilado aquí.** Este
  entorno no tiene acceso al repositorio de Google ni a Maven Central
  (solo a un puñado de dominios permitidos), así que no puede descargar
  AndroidX, Compose, Hilt ni coroutines para comprobarlo. Sintácticamente
  correcto y fiel a la maqueta acordada, pero la primera compilación real
  ocurrirá en tu máquina, dentro de Android Studio.

### `:data` — todavía no existe
Siguiente pieza: entidades Room, mappers de Firestore, implementaciones
de repositorio que sustituyan a `DatosEjemploFamilia` por datos reales.

## Cómo abrirlo

1. Descomprime y abre la carpeta `encaja/` con Android Studio (necesitas
   conexión a internet para que Gradle descargue las dependencias de
   Android/Compose/Hilt la primera vez — aquí no ha podido hacerse).
2. Sincroniza Gradle.
3. Ejecuta los tests de `:domain` y de `:app` (el paquete
   `com.encaja.app.ui.semana`, salvo el ViewModel y el Composable, que no
   llevan test unitario propio todavía).
4. Lanza la app en un emulador o dispositivo para ver `SemanaScreen`.

## Siguiente paso

Módulo `:data`: entidades Room + mappers de Firestore + repositorios,
para que `SemaforoViewModel` deje de usar `DatosEjemploFamilia` y reciba
datos reales inyectados con Hilt.
