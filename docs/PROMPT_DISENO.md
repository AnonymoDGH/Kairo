# Prompt para generar las pantallas de Kairo

Pensado para generadores de imagen (ChatGPT/GPT-image, Gemini, Midjourney) o de UI (Google Stitch,
v0, Figma Make). Va en inglés porque estos modelos lo siguen mejor; el texto de la interfaz va en
español. Pega el bloque entero. Si solo genera una imagen por vez, pide primero la pantalla 1 y
luego: *"Same exact style, now screen N"*.

Cuando tengas la que te guste, pásame la imagen: copio colores, radios, tipografía y espaciado.

---

```
Design a high-fidelity mobile app UI for "Kairo", a real-time voice assistant for Android that
answers with the user's own cloned voice. Render FLAT, front-facing screenshots at 1080×2400
(phone portrait), no device frame, no perspective, no hands, no 3D mockup, no background scene —
just the screen, edge to edge, so it can be reproduced pixel-accurately in code. All UI text
must be in Spanish, sharp and legible.

VISUAL STYLE
- Dark, minimal, premium. Mix of Cursor (code editor) cleanliness and Apple "liquid glass".
- Background #07070B with a soft "aurora": large blurred blobs of violet #7C5CFF, cyan #22D3EE
  and magenta #F472B6 at ~40% opacity, very smooth gradients, no noise, no stars.
- Glass panels: translucent white fill 6–10%, strong background blur, 1px border with a gradient
  from white 28% (top-left) to white 4% (bottom-right), a thin specular highlight on the top
  edge, subtle refraction at the corners. No drop shadows, no neon glow on text.
- Typography: Geist (sans) for UI, Geist Mono for numbers, metrics and code. Large clean titles,
  generous spacing, 16dp side margins, 8dp grid.
- Text colors: primary #F5F5F7, secondary #A1A1AA. Accents: cyan #22D3EE (listening),
  violet #7C5CFF (thinking), amber #F59E0B (errors/warnings), green only for "ok" checkmarks.
- Corner radii: cards 24dp, bottom dock 28dp, chips and buttons fully rounded (pill).
- Icons: thin line icons (1.5px stroke), monochrome.

THE ORB (hero element)
A glass sphere ~55% of screen width: fresnel rim light, slowly flowing colored liquid inside,
soft caustics, subtle reflection. It changes with state: idle (cool dim colors), listening
(cyan, soft concentric ripples), thinking (violet inner swirl), speaking (pulses with the voice).

SCREENS (same style for all)
1. Inicio — aurora background, orb centered in the upper half, below it two lines of live
   transcript ("¿Qué tiempo hace mañana en Madrid?" in gray, "Mañana tendrás 24 grados y sol."
   in white). Bottom floating glass dock: big round mic button in the center, keyboard button,
   a chip "Claude Haiku" (model) and a chip with a small avatar + "Mi voz".
2. Conversación — glass sheet with a Cursor-style message timeline: no chat bubbles, clean
   blocks; user messages have a thin cyan left border; one code block in Geist Mono with a
   "Copiar" button; text field at the bottom "Escribe a Kairo…".
3. Paleta de comandos — glass search field "Buscar comando" over a dimmed screen, list with
   shortcuts: "Nueva conversación", "Cambiar voz", "Cambiar modelo", "Silenciar mi voz (voz
   Kairo)", "Diagnóstico".
4. Voces — 2-column grid of glass cards with illustrated portrait, name (Álvaro, Clara, Diego,
   Elena…) and a "Probar" button; a highlighted card "Voz de Kairo"; top buttons
   "Importar .vlvoice" and "Clonar mi voz".
5. Ajustes — grouped glass list: Cerebro, Voz, Audio, Modelos, Diagnóstico, Acerca de; minimal
   toggles and sliders ("Sensibilidad de interrupción", "Manos libres").
6. Diagnóstico — top bar with back arrow, title "Diagnóstico" and a "Copiar informe" text
   button. Sections: "Modelos" (rows: name, size "177 MB", license in small gray text, progress
   bar "84 / 177 MB", buttons "Descargar" / "Borrar"), "Dispositivo" (key-value list in Geist
   Mono: "Redmi Note 14 4G", "Helio G99", "Android 14 · API 34", "AEC: sí"), and benchmark
   cards ("VoiceLab (Calidad) · 1 hilo" → "RTF p99 0,48 ✓", "Piper" → "RTF 0,21 ✓",
   "STT Whisper base" → "620 ms · WER 9 % ✓") each with a small "Ejecutar" pill button.
7. Bienvenida — 3-step onboarding on glass: "Conecta OpenRouter" → "Descarga los modelos"
   (with sizes) → "Permisos de micrófono y notificaciones", with a step indicator.

DELIVER 4 VISUAL DIRECTIONS of screen 1 (Inicio) side by side, same content, labeled on top:
A. "Fiel" — exactly as described.
B. "Minimal" — almost monochrome, faint aurora, the orb is the only color.
C. "Vibrante" — intense aurora, thicker glass, iridescent highlights, more refraction.
D. "Terminal" — closer to Cursor: more Geist Mono, hairline dividers, subtle grid, tag-like chips.
```
