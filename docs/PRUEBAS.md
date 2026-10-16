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
