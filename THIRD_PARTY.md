# Software de terceros

| Componente | Licencia | Uso |
|---|---|---|
| [Geist / Geist Mono](https://github.com/vercel/geist-font) (Vercel) | SIL OFL 1.1 — [texto](docs/licenses/Geist-OFL.txt) | Tipografía, empaquetada en `core/ui/src/main/res/font/` |
| [Haze](https://github.com/chrisbanes/haze) (Chris Banes) | Apache-2.0 | Desenfoque y cristal líquido |
| Jetpack Compose, AndroidX | Apache-2.0 | UI |
| Kotlin, kotlinx.coroutines | Apache-2.0 | Lenguaje y concurrencia |
| [VoiceLab](https://github.com/AnonymoDGH/voicelab) (`voicelab-core`) | Apache-2.0 | Motor de conversión de voz (en `native/`) |
| [MeanVC2](https://github.com/ASLP-lab/MeanVC2) | Apache-2.0 | Modelos de VoiceLab (se descargan) |
| [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) | Apache-2.0 | Piper, VAD y STT; trae ONNX Runtime |
| [ONNX Runtime](https://github.com/microsoft/onnxruntime) | MIT | Inferencia (dentro del AAR de sherpa-onnx) |
| [ort](https://github.com/pykeio/ort) | MIT / Apache-2.0 | Enlace Rust con ONNX Runtime |
| [UniFFI](https://github.com/mozilla/uniffi-rs) | MPL-2.0 | Bindings Rust ↔ Kotlin |
| [JNA](https://github.com/java-native-access/jna) | Apache-2.0 / LGPL-2.1 | Llamadas nativas de UniFFI |
| [OkHttp](https://github.com/square/okhttp) | Apache-2.0 | Descargas |
| [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) | Apache-2.0 | Descomprimir modelos `.tar.bz2` |

### Modelos (se descargan en el móvil, no van en el APK)

| Modelo | Licencia |
|---|---|
| VoiceLab / MeanVC2 (120 ms y 40 ms) | Apache-2.0 |
| Piper `es_ES-davefx-medium` | Dataset CC0 (NabuCasa); Piper MIT |
| Silero VAD | MIT |
| Whisper base / small (int8) | MIT |
| NeMo FastConformer CTC español | CC-BY-4.0 |
| Zipformer streaming español (Kroko) | CC-BY-SA (modelos comunitarios de Banafo) |


