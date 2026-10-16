# Kairo — plan completo (núcleo de voz)

> **Para el agente que lea esto:** este documento es tu encargo. Contiene las decisiones ya
> tomadas con el dueño del proyecto (no las vuelvas a discutir salvo que algo sea imposible),
> la arquitectura, el diseño visual y un plan por fases con criterios de aceptación.
> Lee primero **§0 Cómo trabajar** y **§12 Riesgos**.

---

## 0. Cómo trabajar (instrucciones para el agente en la nube)

1. **Repo:** trabaja en `AnonymoDGH/kairo` (créalo si está vacío). Licencia **Apache-2.0**, como VoiceLab.
2. **No tienes el móvil.** El dispositivo de pruebas es un **Redmi Note 14 4G** (MediaTek Helio
   G99-Ultra: 2× Cortex-A76 2,2 GHz + 6× A55, Mali-G57 MC2, 6–8 GB RAM, Android 14/HyperOS,
   pantalla AMOLED 120 Hz). Por eso:
   - Cada fase termina con un **APK de depuración** generado por GitHub Actions y subido como
     artefacto (y en un pre-release `vX.Y.Z-faseN`), para que el dueño lo instale.
   - Todo lo que haya que medir en el móvil se mide **dentro de la app** (pantalla
     *Diagnóstico*, §9) y se puede **copiar como texto** para que el dueño te lo pegue.
   - Al final de cada fase, deja en `docs/PRUEBAS.md` una lista corta de lo que el dueño
     tiene que comprobar en el móvil.
3. **Commits pequeños**, uno por cambio con sentido, en español, en una rama por fase con su PR.
4. **Nunca** subas claves, tokens ni modelos `.onnx` al repo (los modelos se descargan en tiempo de
   ejecución; ver §7). Añade al `.gitignore` `local.properties`, `*.keystore`, `models/`.
5. **Tests:** lógica pura (troceador de frases, parser SSE, máquina de estados, PKCE, verificación
   sha256) con tests unitarios JVM. El motor Rust conserva los tests de paridad de VoiceLab en host.
6. **Si algo del plan resulta imposible** (p. ej. una librería no existe o no compila), elige la
   alternativa más sencilla que cumpla el criterio de aceptación, y apúntalo en
   `docs/DECISIONES.md` con el porqué. No te quedes bloqueado.
7. Idioma de la app y del código visible al usuario: **español**. Identificadores en inglés.

---

## 1. Qué es Kairo

Un **asistente de voz para Android** con el que hablas en tiempo real, como el modo voz de Grok
o el orbe de ChatGPT, con dos rasgos propios:

- **Te responde con tu propia voz**, clonada con el motor de
  [VoiceLab](https://github.com/AnonymoDGH/voicelab) (del mismo autor), que corre **en local**,
  sin API.
- **Estética moderna y limpia, tipo Cursor, con *liquid glass*** (cristal líquido con
  desenfoque, refracción y brillos) y un orbe animado como protagonista.

**Éxito del núcleo:** abro Kairo, hablo con naturalidad, a los ~2 s empieza a contestar con mi
voz, puedo interrumpirle a mitad de frase y se calla y me escucha; todo el audio se procesa en el
móvil y solo el texto va al LLM.

### Alcance de este documento (subsistema 1 de 4)

| # | Subsistema | Estado |
|---|---|---|
| **1** | **Núcleo de voz**: orbe, conversación en tiempo real con interrupción, tu voz clonada, cerebro en OpenRouter | **Este plan** |
| 2 | Herramientas del móvil (alarmas, mensajes, abrir apps, web, memoria) | Futuro. Dejar el `Brain` preparado para *tool calling* |
| 3 | Modo programador estilo Cursor (GitHub, repos) | Futuro |
| 4 | Puente con el PC: `kairo-bridge` que ejecuta **Claude Code** del usuario | Futuro (ver §11) |

LLM local en el móvil: **descartado por ahora** (decisión del dueño).

---

## 2. Decisiones ya tomadas

| Tema | Decisión | Por qué |
|---|---|---|
| Plataforma | **Kotlin + Jetpack Compose nativo**, motor de voz en **Rust por JNI (UniFFI)** | Mejor rendimiento de audio y shaders; servicios en primer plano y accesibilidad (subsistemas 2–4) son nativos |
| Cerebro | **OpenRouter** (API compatible con OpenAI, streaming SSE), con login **OAuth PKCE** y opción de pegar clave; campo "URL base" para otras APIs compatibles | Una integración, todos los modelos (Claude, Grok, GPT, Gemini, gratuitos `:free`) |
| Login con suscripción de Claude | **No** en la app | Las condiciones de Anthropic no permiten que apps de terceros ofrezcan login con Claude.ai ni usen esos tokens. Claude Code con la suscripción del usuario entra en el subsistema 4 vía el binario oficial sin modificar |
| Voz base (TTS) | **Piper** (VITS) en español, vía **sherpa-onnx** | Muy rápido en CPU móvil; pocket-tts probablemente no llega a tiempo real junto a VoiceLab en 2 núcleos grandes |
| Tu voz | Piper → **VoiceLab (MeanVC2, modo *Calidad* 120 ms)** con el `.vlvoice` del usuario | VoiceLab convierte voz→voz; no es TTS. Ultra (48 kHz) no cabe en el G99 |
| Escucha | **VAD Silero** + **STT local**; candidato elegido por medición (Fase 1) | Privacidad y sin coste |
| Clonado | Importar `.vlvoice` (clonado en PC con VoiceLab) **y** clonar en el móvil con descarga opcional | El encoder de voz pesa 386 MB |
| Nombre | **Kairo** | Elegido por el dueño |

---

## 3. Restricciones

- **Hardware:** solo **2 núcleos rápidos**. VoiceLab + Piper + STT compiten por ellos. Nada de
  modo Ultra. El LLM va en la nube.
- **GPU modesta (Mali-G57 MC2):** el cristal se dosifica (≤ 2 capas desenfocadas a la vez en
  pantalla); objetivo 120 Hz, aceptable 60 Hz estable.
- **minSdk 31** (Android 12, `RenderEffect`), **targetSdk** el último estable; el shader AGSL
  (`RuntimeShader`) requiere API 33 → en 31–32 usar fallback sin refracción.
- **Solo `arm64-v8a`** para el motor nativo (añadir `x86_64` solo si hace falta para el emulador de CI).
- **Uso responsable:** como en VoiceLab, clonar exige marcar "Es mi voz o tengo permiso de la persona".
- **Licencias:** VoiceLab, MeanVC2, sherpa-onnx: Apache-2.0. **Cada voz de Piper tiene su
  propia licencia** (mira el `MODEL_CARD` de la voz elegida; evita las no comerciales). Recoge
  todo en `THIRD_PARTY.md`.

---

## 4. Arquitectura

```
 🎙 AudioRecord (VOICE_COMMUNICATION) + AcousticEchoCanceler + NoiseSuppressor
   │ PCM 16 kHz mono
   ▼
 Listener: VAD Silero ──► STT local ──► texto final (y parciales para la UI)
   ▲                                          │
   │ barge-in: voz del usuario > 250 ms       ▼
   │ mientras habla Kairo → cancelar todo  Brain: OpenRouter /chat/completions (stream)
   │                                          │ tokens
   │                                          ▼
   │                                     SentenceChunker → frases completas
   │                                          │
   │                                          ▼
   │                                     Piper (sherpa-onnx) 22,05 kHz → remuestreo 16 kHz
   │                                          │
   │                                          ▼
   │                                     VoiceEngine (Rust/VoiceLab, tu .vlvoice) 16 kHz
   │                                          │   (o se salta si "Voz de Kairo")
   │                                          ▼
   └───────────────────────────────────  AudioTrack (USAGE_ASSISTANT, baja latencia)
```

### Módulos Gradle

```
kairo/
├─ app/                 Activity, navegación, DI manual (sin Hilt para empezar), tema
├─ core/ui/             Sistema de diseño: tokens, GlassSurface, Orb, componentes
├─ core/audio/          AudioIO (captura/reproducción, AEC, niveles RMS)
├─ core/conversation/   ConversationService + máquina de estados
├─ core/listen/         VAD + STT (interfaz SpeechToText con implementaciones)
├─ core/brain/          LlmProvider, OpenRouter, SSE, PKCE, SentenceChunker, historial
├─ core/voice/          Tts (Piper), VoiceEngine (binding Rust), VoicePipeline
├─ core/models/         ModelStore: catálogo, descarga reanudable, sha256, espacio
├─ feature/home/        Orbe + transcripción + dock
├─ feature/voices/      Biblioteca, importar .vlvoice, clonar
├─ feature/settings/    Cerebro, voz, audio, modelos
├─ feature/diagnostics/ Bancos de rendimiento y log copiable
└─ native/              Workspace Rust: crate `kairo-voice` (UniFFI) → libkairo_voice.so
```

### Interfaces clave (Kotlin)

```kotlin
interface LlmProvider {
    fun stream(messages: List<ChatMessage>, params: LlmParams): Flow<LlmEvent>
    // LlmEvent: Delta(text) | Done(usage) | Error(kind, message)
    // Preparado para subsistema 2: params.tools + LlmEvent.ToolCall (no implementar aún)
}

interface SpeechToText {           // una implementación por candidato
    suspend fun transcribe(pcm16k: FloatArray): String
    val supportsPartials: Boolean
}

interface Tts { fun synthesize(text: String): Flow<FloatArray> /* 16 kHz tras remuestreo */ }

interface VoiceConverter {         // envoltorio del motor Rust
    fun loadVoice(vlvoice: ByteArray)
    fun push(pcm16k: FloatArray): FloatArray   // streaming, devuelve lo que haya listo
    fun flush(): FloatArray                    // vacía el lookahead al final de una frase
    fun reset()
}
```

### Máquina de estados (`ConversationService`, servicio en primer plano tipo `microphone`)

```
Idle ──(tap orbe / modo manos libres)──► Listening
Listening ──(VAD fin de habla + STT)──► Thinking
Thinking ──(primera frase sintetizada)──► Speaking
Speaking ──(cola vacía y stream terminado)──► Listening (manos libres) | Idle
Speaking/Thinking ──(barge-in)──► Listening   // cancela HTTP, vacía colas, fade-out 60 ms
cualquier estado ──(error)──► Error(mensaje) ──(tap)──► Idle
```

- La UI observa un `StateFlow<ConversationState>` con: estado, nivel RMS de mic/salida (para el
  orbe), transcripción parcial y la última respuesta.
- **Barge-in:** el micro sigue abierto mientras Kairo habla; la AEC del sistema elimina el eco.
  Disparar solo si el VAD detecta voz ≥ 250 ms **y** la energía supera un umbral calibrable
  (Ajustes → Audio → Sensibilidad de interrupción). Si en el Redmi la AEC no basta, fallback:
  modo "pulsar para interrumpir" (tap en el orbe).

### Presupuesto de latencia (objetivo, a medir)

| Etapa | Objetivo |
|---|---|
| Fin de habla detectado (VAD, cola de silencio) | 400–500 ms |
| STT de la frase | ≤ 700 ms |
| Primer token del LLM (modelo rápido en OpenRouter) | 300–800 ms |
| Primera frase: Piper + VoiceLab (latencia algorítmica ~305 ms) | ≤ 500 ms |
| **Total hasta oír la primera palabra** | **≈ 1,5–2,5 s** |

Optimización obligatoria: **trocear por frases** y empezar a sintetizar la primera mientras el LLM
sigue escribiendo; sintetizar la frase N+1 mientras suena la N.

---

## 5. Motor de voz: integrar VoiceLab en Android

### Lo que hay en `voicelab-core` (v0.2.0)

- API pública útil: `engine::{ModelDir, StreamingVc, SpeakerEncoder, Variant}`, `voice::Voice`,
  `dsp::*` (fbank, remuestreo, iSTFT), `audio::processor::Processor` (bloques + gate + efectos).
- Dependencias que **no sirven o sobran en Android**: `cpal` + `rtrb` (audio en vivo de escritorio),
  `ureq` + `dirs` (descargas y rutas), `ort` con `download-binaries`/`copy-dylibs` (no hay binarios
  precompilados de ONNX Runtime para Android por esa vía).

### Plan

1. **PR a `AnonymoDGH/voicelab`** que añada features en `voicelab-core`:
   - `live` (por defecto activada): `cpal`, `rtrb`, módulos `audio::live`, `audio::devices`, `audio::record`.
   - `download` (por defecto activada): `ureq`, `dirs`, `engine::download`, `paths`.
   - Que `ort` no fuerce `download-binaries` desde el core (moverlo a la CLI y la app de
     escritorio) y que exista una ruta para `load-dynamic`.
   - Verificar que el escritorio sigue compilando y los tests pasan (`cargo test -p voicelab-core`).
   - **Si no tienes permiso de escritura en voicelab:** depende de un fork o copia solo los módulos
     necesarios a `native/third_party/voicelab-core/` con su `LICENSE` y `NOTICE` (Apache-2.0 lo
     permite) y anota el commit de origen en `docs/DECISIONES.md`.
2. **Crate `native/kairo-voice`** (`crate-type = ["cdylib"]`) con **UniFFI**, que expone:
   - `VoiceEngine::new(model_dir, variant="120ms", threads=1)`
   - `load_voice(bytes)`, `push(pcm) -> pcm`, `flush() -> pcm`, `reset()`
   - `bench(seconds) -> BenchReport` (RTF p50/p99, ms por bloque) para Diagnóstico
   - `clone_voice(pcm16k) -> bytes(.vlvoice)` (Fase 5; necesita `spk_encoder.int8.onnx`)
3. **ONNX Runtime en Android:** usa `ort` con `load-dynamic` y carga `libonnxruntime.so` en tiempo
   de ejecución. **Ojo al conflicto:** sherpa-onnx también trae su `libonnxruntime.so`. Debe haber
   **una sola** copia en el APK. Opciones, en orden:
   a) usar la `libonnxruntime.so` que trae sherpa-onnx para ambos, si su versión es compatible con
      la que exige `ort 2.0.0-rc.13`;
   b) compilar sherpa-onnx contra la versión de ORT que pida `ort`;
   c) usar el AAR oficial `onnxruntime-android` para ambos.
   Documenta lo elegido.
4. **Compilación:** `cargo-ndk` (target `aarch64-linux-android`), integrado en Gradle (tarea
   previa a `mergeJniLibs`) o un plugin equivalente. `profile.release` con `lto="thin"`,
   `codegen-units=1`, `opt-level=3`. Comprueba que NEON está activo (por defecto en aarch64).
5. **Hilo:** el `VoiceEngine` corre en su propio hilo con prioridad de audio
   (`Process.THREAD_PRIORITY_URGENT_AUDIO`), fuera del hilo de `AudioTrack`.

### Formato de entrada y salida

- VoiceLab trabaja a **16 kHz mono float**. Piper da 22,05 kHz → remuestrear a 16 kHz (en Rust con
  `rubato`, ya es dependencia).
- Salida a `AudioTrack` a 16 kHz (o remuestrear a 48 kHz si el dispositivo lo prefiere; medir
  latencia con ambos).
- Al final de cada frase, `flush()` para vaciar el lookahead (40 ms) y no cortar la última sílaba.

---

## 6. Escucha (VAD + STT)

- **VAD:** Silero VAD vía sherpa-onnx. Fin de habla: 500 ms de silencio (ajustable).
- **STT: elegir por medición en Fase 1.** Candidatos (todos locales):
  1. **Whisper** base o small (int8) vía sherpa-onnx, idioma forzado `es`.
  2. **Vosk** modelo pequeño de español (streaming, da parciales).
  3. Cualquier otro modelo en español disponible para sherpa-onnx que encuentres
     (p. ej. streaming zipformer/paraformer en español), si existe.
- **Criterio:** frase de 4 s transcrita en **≤ 700 ms** en el Redmi y **WER ≤ 15 %** en las 10
  frases de prueba de `feature/diagnostics/assets/stt/` (graba o genera 10 frases variadas en
  español con su texto de referencia; incluye números, nombres propios y una pregunta con `¿`).
- Si ninguno cumple ambos, gana el de menor WER que tarde ≤ 1,2 s, y se anota.
- **No** usar `SpeechRecognizer` de Android: compite por el micro con nuestra captura (necesaria
  para el barge-in) y puede ir a la nube.

---

## 7. Modelos y descargas (`ModelStore`)

| Paquete | Archivos | Tamaño | Cuándo |
|---|---|---|---|
| **Voz VoiceLab (Calidad)** | `asr_120ms.onnx`, `dit_120ms.onnx`, `gtm_120ms.onnx`, `vocos.onnx`, `manifest.json` | ~177 MB | Primer arranque (si el usuario activa "Mi voz") |
| **Voz base** | Piper en español (elegir una voz `es_ES` o `es_MX` de calidad *medium* con licencia permisiva) | ~60 MB | Primer arranque |
| **Escucha** | Silero VAD + STT ganador | ~2 MB + 40–150 MB | Primer arranque |
| **Clonado (opcional)** | `spk_encoder.int8.onnx` | ~386 MB | Solo si el usuario quiere clonar en el móvil |
| Voces incluidas | `voices/*.vlvoice` de VoiceLab (16 voces, 1 KB c/u) + retratos `.svg` | <1 MB | Empaquetadas en el APK |

- **Origen de VoiceLab:** assets del release `v0.2.0` de `AnonymoDGH/voicelab`
  (`https://github.com/AnonymoDGH/voicelab/releases/download/v0.2.0/<archivo>`). El
  `manifest.json` describe variantes y ficheros; verifica **sha256** de cada archivo (si el manifest
  no trae hashes por archivo, calcula y fija los esperados en un `catalog.json` dentro de Kairo).
- Descargas con **WorkManager**, **reanudables** (HTTP Range), solo Wi-Fi por defecto (opción
  para datos), progreso visible en Ajustes → Modelos, comprobación de espacio libre previa.
- Guardar en `context.filesDir/models/<paquete>/`. Botón para borrar cada paquete.

---

## 8. Cerebro: OpenRouter

- **Endpoint:** `POST https://openrouter.ai/api/v1/chat/completions` con `stream: true` (SSE,
  formato OpenAI). Cabeceras: `Authorization: Bearer <key>`, y opcionales de atribución
  `HTTP-Referer` / `X-Title: Kairo`.
- **Cliente HTTP:** OkHttp + parser SSE propio (pequeño y testeado) o `okhttp-sse`. Cancelación
  real del stream en barge-in (`call.cancel()`).
- **Login OAuth PKCE** (oficial de OpenRouter):
  1. Generar `code_verifier` (aleatorio, 64 caracteres) y `code_challenge = base64url(sha256(verifier))`.
  2. Levantar un mini servidor HTTP en `127.0.0.1:<puerto libre>` (solo durante el login, ~2 min
     máx.) y abrir una **Custom Tab** a
     `https://openrouter.ai/auth?callback_url=http://localhost:<puerto>/callback&code_challenge=…&code_challenge_method=S256&key_label=Kairo`.
  3. Recibir `?code=…` en el callback, responder una página "Ya puedes volver a Kairo" y traer la app al frente.
  4. `POST https://openrouter.ai/api/v1/auth/keys` con `{code, code_verifier, code_challenge_method:"S256"}` → `key`.
  5. Alternativas en la misma pantalla: **"Pegar código"** (modo headless de OpenRouter) y **"Pegar API key"**.
- **Guardado de la clave:** cifrada con una clave AES-GCM del **Android Keystore**, en DataStore.
  Nunca en logs, nunca en el log copiable de Diagnóstico, nunca en backups (`allowBackup=false` o
  regla de exclusión).
- **Modelos:** lista desde `GET https://openrouter.ai/api/v1/models`, con buscador, precio y
  marca de gratis. Recomendar en la UI modelos rápidos para voz. No inventes un slug por defecto:
  en el primer arranque, pide elegir y sugiere un modelo rápido y barato de la lista (p. ej. el
  Haiku más reciente de Anthropic o uno `:free`).
- **URL base editable** (Ajustes avanzados) para cualquier API compatible con OpenAI.
- **Prompt de sistema** (editable en Ajustes → Personalidad), por defecto:
  *"Eres Kairo, un asistente de voz. Respondes en español, en frases cortas y naturales, como en una
  conversación hablada. Nada de markdown, listas, emojis ni bloques de código salvo que te lo pidan;
  si hay código, di que lo dejas en pantalla. Si no sabes algo, dilo."*
- **Historial:** Room. Conversaciones con título automático; se envían los últimos N turnos
  (configurable, por defecto 20).
- **`SentenceChunker`:** corta en `.`, `?`, `!`, `…`, `:` y saltos de línea, respetando `¿…?` y
  `¡…!`, números decimales (`3.5`), abreviaturas comunes (`Sr.`, `Dra.`, `etc.`, `p. ej.`) y URLs;
  primera frase con mínimo ~4 palabras para no arrancar con un "Bueno."; máximo ~25 palabras
  (cortar en coma si se pasa). Quitar markdown antes de pasar al TTS, pero mostrarlo tal cual en pantalla.
- **Errores:** sin red, 401 (clave inválida → volver a conectar), 402 (sin saldo), 429 (límite),
  5xx. Mensaje hablado corto + tarjeta en pantalla.

---

## 9. Diseño visual: *liquid glass* estilo Cursor

### Principios

- **Oscuro por defecto**, limpio, mucho aire; el **orbe** es el protagonista; el texto es secundario
  y aparece en tarjetas de cristal.
- Estilo Cursor: tipografía sobria, monoespaciada para código, chips compactos, una **paleta de
  comandos** y un dock inferior; nada de decoración gratuita.

### Tokens

| Token | Valor |
|---|---|
| Fondo base | `#07070B` |
| Aurora (manchas animadas lentas, 20–30 s de ciclo) | violeta `#7C5CFF`, cian `#22D3EE`, magenta `#F472B6` al 35–45 % |
| Cristal: relleno | blanco 6–10 % sobre desenfoque 24–32 dp |
| Cristal: borde | degradado blanco 28 % → 4 % (arriba-izquierda → abajo-derecha), 1 dp |
| Cristal: brillo especular | franja superior blanca 12 %, se mueve sutilmente con el giroscopio (opcional) |
| Texto primario / secundario | `#F5F5F7` / `#A1A1AA` |
| Acento | cian `#22D3EE` (escuchando), violeta `#7C5CFF` (pensando), color de la voz activa (hablando) |
| Error | ámbar `#F59E0B` |
| Radios | tarjetas 24 dp, chips 999, dock 28 dp |
| Tipografía | **Geist** y **Geist Mono** (OFL, las mismas que VoiceLab) |
| Movimiento | `spring(dampingRatio 0.8, stiffness 300)`; transiciones 220–320 ms |
| Tema claro | variante "cristal de día" (fondo `#F4F4F8`, aurora pastel) — Fase 5 |

### Implementación del cristal

- `GlassSurface` composable: desenfoque del fondo (`RenderEffect`, API 31+), capa de relleno, borde
  degradado y, en **API 33+**, un **shader AGSL** de refracción en los bordes (desplazamiento de
  UV según la distancia al borde) + brillo especular.
- Evalúa estas librerías antes de escribir la tuya: **Haze** (`dev.chrisbanes.haze`, desenfoque de
  fondo en Compose) y **Backdrop / AndroidLiquidGlass** de Kyant0 (refracción tipo liquid glass en
  Compose). Usa la que funcione con Compose actual y rinda en la Mali-G57; si ninguna sirve,
  `RenderEffect` + AGSL propio.
- **Presupuesto de GPU:** máximo 2 superficies desenfocadas visibles a la vez; el fondo aurora se
  dibuja una vez en una capa y se reutiliza; desactivar refracción y animación de aurora en
  **ahorro de batería** y si Diagnóstico mide < 55 fps.

### El orbe

Shader AGSL (con fallback a degradados animados en Canvas para API < 33): una esfera de cristal
con fresnel, ruido interno que fluye y caústicas suaves. Reacciona a `level` (RMS 0..1) y a `state`:

| Estado | Comportamiento |
|---|---|
| Idle | Respira lento (escala 0,98↔1,02, 4 s), colores fríos tenues |
| Listening | Cian; se **expande con tu voz** (RMS del micro); ondas concéntricas suaves |
| Thinking | Violeta; remolino interno que gira; ligera pulsación |
| Speaking | Color de la voz activa; **late con la amplitud de salida** |
| Error | Destello ámbar breve y vuelve a Idle |

Háptica ligera al cambiar a Listening y al ser interrumpido.

### Pantallas

1. **Inicio:** fondo aurora; orbe centrado (≈ 55 % del ancho); debajo, 1–2 líneas de la
   transcripción en vivo (lo que dices en gris, lo que responde en blanco); **dock de cristal**
   inferior con: botón micro (tap = hablar, mantener = manos libres), botón teclado (escribir), chip
   del modelo actual, chip de la voz actual. Deslizar hacia arriba abre la conversación.
2. **Conversación:** hoja de cristal con la línea temporal de mensajes (estilo Cursor: sin
   burbujas, bloques limpios; el usuario con borde izquierdo de acento; bloques de código con Geist
   Mono y botón copiar). Campo de texto opcional.
3. **Paleta de comandos** (deslizar abajo o botón ⌘): "Nueva conversación", "Cambiar voz",
   "Cambiar modelo", "Silenciar mi voz (voz Kairo)", "Diagnóstico".
4. **Voces:** cuadrícula de tarjetas de cristal con retrato (las 16 de VoiceLab + las tuyas),
   botón "Importar .vlvoice", botón "Clonar mi voz" (3 pasos como VoiceLab: audio 10–20 s grabado o
   de archivo → nombre y foto → casilla de permiso), "Probar" (dice una frase) y la opción
   **"Voz de Kairo"** (Piper sin conversión).
5. **Ajustes:** *Cerebro* (Conectar OpenRouter / pegar clave, modelo, URL base, personalidad,
   turnos de memoria) · *Voz* (mi voz o voz de Kairo, velocidad de Piper) · *Audio* (sensibilidad
   de interrupción, silencio de fin de habla, manos libres) · *Modelos* (paquetes, tamaño,
   descargar/borrar) · *Diagnóstico* · *Acerca de* (licencias).
6. **Diagnóstico:** botones que ejecutan y muestran: RTF de VoiceLab (p50/p99), RTF de Piper, tiempo
   y WER de cada STT candidato, latencia de extremo a extremo de un turno simulado, fps de la UI,
   y si el dispositivo tiene AEC. Botón **"Copiar informe"** (texto plano sin claves).
7. **Bienvenida (primer arranque):** 3 pasos en cristal: conectar OpenRouter → descargar modelos
   (con tamaños) → permiso de micrófono y notificaciones.

---

## 10. Plan por fases

Cada fase: rama propia, PR, APK en Actions y `docs/PRUEBAS.md` actualizado. **No empieces la
siguiente sin que la anterior compile y pase sus tests.**

### Fase 0 — Esqueleto y CI
- Proyecto Gradle multi-módulo (§4), Kotlin y Compose estables actuales con version catalog,
  minSdk 31, solo `arm64-v8a`.
- Workflow de GitHub Actions: JDK, Android SDK, NDK, Rust + `cargo-ndk`, caché; `./gradlew test
  assembleDebug`; sube el APK como artefacto. Release en tags `v*`.
- Tema base, fuentes Geist, fondo aurora y un `GlassSurface` mínimo.
- **Aceptación:** el APK se instala en el Redmi y muestra el fondo aurora con una tarjeta de cristal.

### Fase 1 — Prueba de rendimiento en el dispositivo (la más importante)
- PR a voicelab (o vendorizado) con las features de §5; crate `kairo-voice` con UniFFI y `bench()`.
- sherpa-onnx integrado (Piper + VAD + candidatos STT), resolviendo el conflicto de
  `libonnxruntime.so` (§5.3).
- Pantalla **Diagnóstico** con los bancos de §9.6. Los modelos para el banco se descargan desde la propia pantalla.
- **Aceptación (medido por el dueño en el Redmi):**
  - VoiceLab *Calidad*, 1 hilo: **RTF p99 < 0,6** (si da 0,6–0,9, probar 2 hilos y anotar).
  - Piper: **RTF < 0,3**.
  - VoiceLab + Piper a la vez: **RTF combinado < 0,8**.
  - STT: según §6.
- **Puerta:** si VoiceLab no llega a tiempo real ni con 2 hilos, para y propón en `docs/DECISIONES.md`
  opciones (cuantizar el DiT a int8 o fp16, variante 40 ms, bloque mayor) antes de seguir.

### Fase 2 — Cerebro y chat por texto con la UI de cristal
- OpenRouter: PKCE + pegar código + pegar clave; Keystore; lista de modelos; streaming;
  `SentenceChunker`; historial Room; prompt de sistema.
- Pantalla Inicio con orbe (estados Idle y Thinking), Conversación, Paleta de comandos, Ajustes → Cerebro.
- **Tests:** parser SSE (trozos partidos, `[DONE]`, comentarios `:`), PKCE (vectores conocidos), chunker (≥ 25 casos en español).
- **Aceptación:** conecto OpenRouter, elijo modelo, escribo y la respuesta aparece en streaming;
  cerrar y abrir conserva el historial.

### Fase 3 — Habla: Piper + mi voz
- `VoicePipeline`: frases → Piper → remuestreo → VoiceLab → AudioTrack, con cola y síntesis
  adelantada de la siguiente frase. Interruptor "Mi voz / Voz de Kairo".
- Voces: biblioteca con las 16 incluidas, importar `.vlvoice`, probar voz.
- `ModelStore` completo (WorkManager, Range, sha256, solo Wi-Fi) y Bienvenida.
- Orbe en estado Speaking con la amplitud de salida.
- **Aceptación:** escribo una pregunta y Kairo la responde **hablando con la voz elegida**, sin
  cortes ni chasquidos entre frases; la primera palabra suena < 1,5 s después del primer token.

### Fase 4 — Escucha y conversación en tiempo real
- `AudioIO` con AEC/NS, VAD, STT ganador, `ConversationService` en primer plano con notificación
  (botones Pausar/Terminar), máquina de estados (§4), **barge-in**, modo manos libres, transcripción en vivo.
- **Aceptación:** conversación de 5 turnos solo con la voz; interrumpo a Kairo a mitad de frase y
  se calla en < 300 ms y me escucha; con la pantalla apagada la conversación sigue; latencia total
  medida en Diagnóstico ≈ 1,5–2,5 s.

### Fase 5 — Clonar en el móvil y pulido
- Descarga opcional del encoder; grabar 10–20 s o elegir archivo (`symphonia` en Rust ya decodifica
  wav/mp3/flac/ogg); casilla de permiso; foto; generar `.vlvoice`.
- Tema claro, accesibilidad (TalkBack en controles, contraste ≥ 4,5:1 en texto, tamaño de texto del
  sistema), modo ahorro (sin refracción), pantalla Acerca de con licencias.
- **Aceptación:** me clono en el móvil y Kairo me responde con mi voz; TalkBack puede usar el dock.

---

## 11. Futuro (no implementar ahora, pero no cerrar la puerta)

- **Subsistema 2 (herramientas del móvil):** `LlmProvider` ya admite `tools`; las herramientas
  serán clases `Tool` con esquema JSON y confirmación del usuario para acciones con efectos
  (enviar mensajes, llamar).
- **Subsistema 4 (`kairo-bridge` en el PC):** binario Rust que lanza el **`claude` oficial sin
  modificar** en modo headless (`claude -p --input-format stream-json --output-format stream-json`)
  con la sesión que el propio usuario inició con `/login`, y expone un WebSocket en la red local
  (emparejamiento con código QR; Tailscale para fuera de casa). Kairo **nunca** maneja credenciales
  de Claude. Aparecerá como otro `LlmProvider`. Así el usuario usa su suscripción de forma
  permitida (las condiciones de Claude Code permiten que el usuario inicie sesión con su suscripción
  en el binario sin modificar).
- **Subsistema 3:** se apoya en el 4 (Claude Code ya edita repos) más una vista de diffs estilo Cursor.

---

## 12. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| VoiceLab no llega a tiempo real en el G99 | Fase 1 lo mide primero; opciones: 2 hilos, int8/fp16 del DiT, variante 40 ms, o "voz de Kairo" sin conversión |
| Dos `libonnxruntime.so` (ort y sherpa-onnx) chocan | Una sola copia compartida (§5.3), decidir en Fase 1 |
| La AEC del Redmi no evita que Kairo se interrumpa a sí mismo | Umbral de energía + VAD ≥ 250 ms; fallback "tocar para interrumpir" |
| STT local lento o malo en español | Medir 2–3 candidatos; aceptar ≤ 1,2 s si el WER es mejor |
| Cristal con tirones en la Mali-G57 | Límite de 2 capas, fallback sin refracción, medir fps en Diagnóstico |
| Calor y batería en conversaciones largas | Liberar sesiones ONNX tras 2 min en Idle; aviso si la temperatura sube (`PowerManager.getThermalHeadroom`) |
| Licencia de la voz Piper | Elegir una con licencia permisiva y documentarla |
| Callback localhost de OpenRouter bloqueado por la Custom Tab | Alternativas "Pegar código" y "Pegar clave" ya en la misma pantalla |

---

## 13. Fuera de alcance de este plan

LLM local, palabra de activación ("Oye Kairo"), widgets, Wear OS, iOS, sincronización en la nube,
publicación en Google Play (se decidirá aparte), subsistemas 2–4.

---

## 14. Referencias

- VoiceLab: <https://github.com/AnonymoDGH/voicelab> (motor en `crates/voicelab-core`, modelos en el release `v0.2.0`)
- MeanVC2: <https://github.com/ASLP-lab/MeanVC2>
- sherpa-onnx: <https://github.com/k2-fsa/sherpa-onnx>
- Piper: <https://github.com/rhasspy/piper> (voces: <https://huggingface.co/rhasspy/piper-voices>)
- OpenRouter API: <https://openrouter.ai/docs> · OAuth PKCE: <https://openrouter.ai/docs/guides/overview/auth/oauth>
- UniFFI: <https://github.com/mozilla/uniffi-rs> · cargo-ndk: <https://github.com/bbqsrc/cargo-ndk>
- Condiciones de autenticación de Claude Code: <https://code.claude.com/docs/en/legal-and-compliance>
