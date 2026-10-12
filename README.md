# Encaja

App Android de organización familiar: turnos y disponibilidad de los
cuidadores, actividades de los niños, menú y compra. Kotlin, Jetpack
Compose, Hilt, Room y Firebase (Firestore, Auth, Functions, mensajería).

## Estructura

- `:domain` — Kotlin puro, sin Android: modelos, repositorios (interfaces)
  y casos de uso (`CalcularHuecosDelDia`, `DetectarConflictosDeAsignacion`…).
- `:data` — Room, Firestore y las implementaciones de los repositorios.
- `:app` — interfaz (Compose), ViewModels e inyección con Hilt. Cada
  pantalla vive en su paquete de `ui/`.
- `functions/` — Cloud Functions de Firebase.
- `firestore.rules` — reglas de seguridad (pendientes de desplegar; ver
  `firestore.rules.anterior` para las que hay hoy).

## Cómo abrirlo

1. Abre la carpeta con Android Studio y sincroniza Gradle.
2. Añade tu propio `app/google-services.json` (no se sube al repositorio).
3. Tests: en la vista Project, `app/src/test/kotlin` y `domain/src/test/kotlin`
   → clic derecho → *Run Tests*.

## Qué no debe entrar nunca en git

`app/google-services.json`, los `*.hprof` y `functions/node_modules`
(ya están en `.gitignore`).
