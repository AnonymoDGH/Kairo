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

## Fase 1

- **VoiceLab para Android: PR en lugar de vendorizado.** PR
  [AnonymoDGH/voicelab#5](https://github.com/AnonymoDGH/voicelab/pull/5) añade a `voicelab-core`
  las features `live`, `download`, `ort-download` (las tres por defecto, así el escritorio no
  cambia) y `load-dynamic`. Kairo depende de la rama por **commit fijo**
  (`7c944754…`). Cuando se fusione, cambiar `rev` al commit de `main`.
  `ort-download` sigue activada por defecto en el core (y no solo en CLI/app) porque
  `cargo test -p voicelab-core` necesita ORT sin depender de otros crates del workspace.
- **Una sola `libonnxruntime.so` (§5.3, opción a).** La trae el AAR de sherpa-onnx 1.13.8
  (ORT 1.28.2). El motor Rust usa `ort` con `load-dynamic` y la abre por nombre
  (`init_onnx_runtime("libonnxruntime.so")`, tras `System.loadLibrary("onnxruntime")`).
  `ort 2.0.0-rc.13` sin features de API usa la API 17 (ORT ≥ 1.17), pero `GraphOptimizationLevel::Level3`
  de VoiceLab es `ORT_ENABLE_LAYOUT`, que **exige ORT ≥ 1.23** (probado: 1.22 falla, 1.24 va).
- **sherpa-onnx desde las releases de GitHub** con un repositorio *ivy* en `settings.gradle.kts`
  (no está en Maven Central). El AAR trae 4 ABIs; `abiFilters` deja solo `arm64-v8a` en el APK.
- **PCM como bytes en UniFFI.** `Vec<f32>` se convertiría en `List<Float>` (un objeto por muestra);
  el motor recibe y devuelve `f32` little-endian en `Vec<u8>` ↔ `ByteArray`.
- **`flush()`** no existe en `StreamingVc`: se implementa empujando silencio hasta haber entregado
  exactamente `entrada + latencia algorítmica` muestras (probado en host con modelos reales).
- **Prueba en host con modelos reales** (`native/kairo-voice/tests/engine.rs`, se salta sin
  `KAIRO_MODELS`/`KAIRO_ORT_LIB`/`KAIRO_VOICE`). Referencia en x86 (contenedor, 1 hilo):
  RTF p50 0,12 / p99 0,19 en *Calidad*.
- **Catálogo de modelos en Kotlin** (`core/models/ModelCatalog.kt`) en vez de `catalog.json`:
  mismo contenido (URL, sha256, tamaño), comprobado por el compilador y por tests. Los sha256
  salen del `manifest.json` de VoiceLab, de los punteros LFS de Hugging Face o calculados al fijarlos.
- **ModelStore mínimo** en Fase 1 (descarga reanudable con `Range`, sha256, tar.bz2, espacio
  libre) en corrutinas, sin WorkManager ni restricción de Wi-Fi: llega completo en Fase 3.
- **Candidatos STT** (todos con sherpa-onnx, un solo runtime): Whisper base y small (int8),
  NeMo FastConformer CTC español (int8) y Zipformer *streaming* español de Kroko (da parciales).
  **Vosk se descarta** de momento: traería otro runtime nativo (Kaldi) y su modelo pequeño de
  español es antiguo; se reconsidera si ningún candidato cumple §6.
  Ojo: Kroko es **CC-BY-SA** (comunitario); si gana, revisar si encaja con la licencia de la app.
- **Frases de prueba del STT sintetizadas con Piper** en el propio móvil (no se suben audios al
  repo). El WER con voz real será algo peor; el dueño puede repetir con su voz más adelante.
- **Voz Piper `es_ES-davefx-medium`** (dataset CC0). Alternativa permisiva: `es_MX-ald-medium` (Unlicense).
- **`.gitignore`: `/models/` anclado a la raíz.** El patrón `models/` también ignoraba el paquete
  `core/models/src/.../models/`. Los modelos siguen fuera del repo por `*.onnx` y porque se
  descargan a `filesDir`.

## Diseño (cambio pedido por el dueño)

- **Cristal líquido monocromo estilo iOS 26 en lugar de la aurora violeta/cian de §9.** El dueño
  eligió una referencia (panel de cristal, esfera de cristal transparente, barra en píldora con
  teclado · micro · ajustes). Un solo acento: azul `#0A84FF`. Se eliminan la aurora y Haze.
- **Sin fondo propio: la ventana de Kairo es translúcida** y el sistema desenfoca lo que haya
  detrás (`FLAG_BLUR_BEHIND`, API 31+), sea el launcher u otra app. Android no deja leer los
  píxeles de otras apps, así que la refracción real de lo de detrás no es posible; el cristal y la
  esfera se dibujan encima. Si el sistema desactiva el desenfoque (ahorro de batería, ajustes de
  HyperOS), se usa un gris sólido `#3A3B3E`.
- **Invocable fuera de la app:** Kairo se registra como *Asistente digital* (`VoiceInteractionService`
  + `ACTION_ASSIST`). Android exige además un `RecognitionService`; el de Kairo no reconoce nada
  para otras apps (su STT es propio). Adelanta parte del subsistema 2, sin herramientas aún.
