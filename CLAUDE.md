# Kairo — instrucciones para agentes

- **El encargo completo está en [KAIRO_PLAN.md](KAIRO_PLAN.md).** Léelo entero antes de empezar,
  empezando por §0 (cómo trabajar) y §12 (riesgos). Las decisiones de §2 ya están tomadas con el dueño.
- Trabaja **fase a fase** (§10): rama `fase-N-<tema>`, PR por fase, APK de depuración como
  artefacto de GitHub Actions y `docs/PRUEBAS.md` con lo que el dueño debe comprobar en su
  Redmi Note 14 4G. No empieces una fase sin que la anterior compile y pase sus tests.
- Si te desvías del plan, anótalo con el porqué en `docs/DECISIONES.md`.
- Nunca subas claves, tokens ni modelos `.onnx`.
- Texto visible para el usuario en español; identificadores en inglés; commits en español.
