# Kairo

Asistente de voz para Android con el que hablas en tiempo real y que **te responde con tu propia
voz**, clonada en local con el motor de [VoiceLab](https://github.com/AnonymoDGH/voicelab).
Interfaz limpia estilo Cursor con *liquid glass* y un orbe animado.

> **Estado:** Fase 0 (esqueleto y CI). El plan completo está en [KAIRO_PLAN.md](KAIRO_PLAN.md);
> las pruebas en el móvil, en [docs/PRUEBAS.md](docs/PRUEBAS.md).

## Compilar

```sh
./gradlew test assembleDebug          # APK en app/build/outputs/apk/debug/
(cd native && cargo test)             # motor Rust en el host
```

Requisitos: JDK 21, Android SDK (API 37). CI (GitHub Actions) sube el APK como artefacto en cada PR.

## Resumen

- **Android nativo:** Kotlin + Jetpack Compose; motor de voz en Rust (VoiceLab) por UniFFI.
- **Escucha local:** VAD Silero + STT en el móvil.
- **Cerebro:** OpenRouter (inicias sesión con OAuth o pegas tu clave) o cualquier API compatible con OpenAI.
- **Habla local:** Piper genera la voz y VoiceLab la convierte a tu timbre. El audio nunca sale del móvil; al LLM solo va texto.
- **Puedes interrumpirle** mientras habla.

## Uso responsable

Clona solo tu voz o la de personas que te hayan dado permiso.

## Licencia

[Apache-2.0](LICENSE)
