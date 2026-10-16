# Decisiones y desviaciones del plan

Registro de lo que se aparta de `KAIRO_PLAN.md` o lo concreta, con el porqué.

## Fase 0

- **Rama de trabajo.** El entorno del agente solo permite empujar a `claude/determined-sagan-9saqyb`;
  la Fase 0 vive ahí en lugar de `fase-0-esqueleto`. El PR es el mismo.
- **Versiones (oct. 2026).** AGP 9.4.1 (Kotlin integrado en AGP, sin plugin `kotlin-android`),
  Kotlin 2.4.21, Gradle 9.8.1, Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 31, JDK 21
  en CI (bytecode Java 17).
- **Convenciones Gradle** en `build-logic/` (`kairo.android.application`, `kairo.android.library`,
  `kairo.android.compose`) para no repetir configuración en los 12 módulos.
- **Cristal: Haze 2.0.1 (`haze-glass`).** De las dos librerías del plan (§9), Haze 2 ya incluye un
  material *liquid glass* con refracción AGSL en API 33+ y degradado automático a solo desenfoque
  en 31–32, así que no hace falta Backdrop ni un shader propio por ahora. La API de glass es
  `@ExperimentalHazeApi`; está aislada en `core/ui/GlassSurface.kt` para poder cambiarla.
  Se fuerza la respuesta "oscura" del material (Kairo siempre es oscuro) porque Haze la elige según
  el modo noche del sistema y en modo claro el cristal salía lechoso.
- **Fondo aurora** con degradados radiales dibujados en la fase de *draw* (no recompone). Se graba
  una vez como fuente de Haze y lo comparten todas las superficies de cristal.
- **Motor Rust.** En Fase 0 `native/kairo-voice` es un esqueleto sin UniFFI: CI solo valida
  `cargo test` y `cargo ndk -t arm64-v8a build`. La integración con Gradle (`jniLibs`) llega en Fase 1.
- **Firma del APK de depuración.** Cada runner de CI genera un keystore de depuración distinto y
  Android exigiría desinstalar en cada actualización. Si existe el secreto
  `KAIRO_DEBUG_KEYSTORE_B64` (keystore de depuración en base64, alias `androiddebugkey`,
  contraseñas `android`), CI lo usa. Para crearlo:
  `keytool -genkeypair -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -validity 10000 -dname "CN=Android Debug"`
  y `base64 -w0 debug.keystore` → *Settings → Secrets → Actions*. Nunca se sube al repo.
- **Test de humo con Robolectric** (`app/src/test`): compone la pantalla de inicio en API 31 y 35
  con gráficos nativos, para detectar fallos de arranque sin tener el móvil.
