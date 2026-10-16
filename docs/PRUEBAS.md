# Pruebas en el Redmi Note 14 4G

Cada fase deja aquí lo que hay que comprobar en el móvil. Si algo falla, pega lo que veas
(o una captura) en el PR de la fase.

## Fase 0 — Esqueleto y CI

APK: pestaña **Actions** del PR → última ejecución de *Android* → artefacto `kairo-debug-apk`
(descomprime el zip y abre el `.apk`; hace falta permitir "instalar apps desconocidas").

- [ ] El APK se instala y la app abre sin cerrarse.
- [ ] Se ve el **fondo aurora** (manchas violeta, cian y magenta sobre negro) moviéndose muy despacio.
- [ ] En el centro hay una **tarjeta de cristal** con "Kairo": el fondo se ve **desenfocado** a
      través de ella, con borde fino brillante arriba a la izquierda.
- [ ] La última línea de la tarjeta dice `API 34 · cristal con refracción` (o la API de tu HyperOS).
      Fíjate si los bordes de la tarjeta **deforman** ligeramente el fondo (refracción).
- [ ] El texto usa la tipografía Geist (no la del sistema) y la línea de versión es monoespaciada.
- [ ] La animación va fluida (sin tirones) a 120 Hz. Si notas tirones, dilo.
- [ ] El icono del launcher es un orbe de color.

> Si cada APK nuevo pide desinstalar el anterior ("conflicto de firma"), es porque falta el
> secreto `KAIRO_DEBUG_KEYSTORE_B64` (ver `docs/DECISIONES.md`).

## Fase 1 — Rendimiento en el dispositivo

Instala el APK nuevo y en Inicio pulsa **"Abrir diagnóstico"**. Con **Wi-Fi** y el móvil enchufado
o con batería > 50 %, **sin ahorro de batería**:

1. En **Modelos**, descarga: *Voz VoiceLab (Calidad)*, *Voz base Piper*, *Silero VAD* y los STT
   que quieras probar (mínimo *Whisper base* y *NeMo CTC español*; *Whisper small* pesa ~375 MB).
2. Pulsa, de uno en uno y esperando a que acabe cada uno:
   - [ ] VoiceLab (Calidad) · 1 hilo — objetivo **RTF p99 < 0,6**
   - [ ] VoiceLab (Calidad) · 2 hilos (sobre todo si el anterior da 0,6–0,9)
   - [ ] Piper — objetivo **RTF < 0,3**
   - [ ] VoiceLab + Piper a la vez — objetivo **RTF combinado < 0,8**
   - [ ] STT — objetivo **≤ 700 ms para 4 s y WER ≤ 15 %**
   - [ ] Latencia de un turno (sin LLM)
   - [ ] Fluidez de la UI (fps)
3. Pulsa **"Copiar informe"** y pégalo en el PR de la Fase 1.

Si algo se cierra de golpe o sale un error en rojo, pégalo también (o una captura).
