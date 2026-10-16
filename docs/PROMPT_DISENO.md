# Prompt para generar el diseño de Kairo con IA

Pégalo entero en ChatGPT (imagen), Gemini, Midjourney o Google Stitch. Genera 4 direcciones
de arte muy distintas para la pantalla de Inicio. Elige una, pide el resto de pantallas en ese
estilo y pásame las imágenes: copio el estilo exacto.

```
You are an award-winning product designer (think Teenage Engineering, Nothing, Rabbit r1,
Linear, Arc, Braun). Design the home screen of "Kairo", an Android voice assistant you talk to in
real time and that answers WITH YOUR OWN CLONED VOICE. The name comes from Greek "kairos": the
right moment. The screen must feel like a crafted object, not an app template.

HARD RULES — avoid the generic AI look:
- NO purple/blue/cyan gradients, NO neon glow, NO glowing blue orb, NO aurora blobs, NO
  sparkles/stars, NO robot or brain icons, NO glassmorphism cards floating on a gradient.
- NO stock chat bubbles, NO default Material/iOS components, NO generic line icons.
- Max 3 colors + neutrals. Every color must have a job.
- Typography must carry the design: confident sizes, tight tracking, real hierarchy.

OUTPUT: 4 separate images, each a FLAT front-facing screenshot 1080×2400, edge to edge, no phone
frame, no perspective, no hands, no mockup scene. All UI text in Spanish, crisp and legible.
Same content in all 4; only the art direction changes. Label each image with its name.

CONTENT OF THE HOME SCREEN
- The voice visual as the hero: a living shape that reacts to sound (state shown: "Escuchando").
- Live transcript, 2 lines: what I said ("¿Me recuerdas llamar a Marta a las 6?") and what Kairo
  answers ("Hecho. A las 18:00 te aviso.").
- Small status line: "Mi voz · Claude Haiku · local".
- Bottom controls: big talk button, keyboard button, and access to "Conversación" and "Ajustes".

THE 4 DIRECTIONS

1. "OBSIDIANA" — volcanic black glass, warm and heavy.
   Palette: #0E0C0B obsidian, #1A1715 raised surface, #E8E1D7 bone text, #C8743A copper accent,
   #6B635B muted text. The voice visual is a ring of molten copper light, like a solar eclipse
   corona, that ripples with the voice. Glass is dark and smoky (polished stone, not frosted
   white). Type: a high-contrast editorial serif for the big words (e.g. "Instrument Serif") +
   a refined mono for data. Feels like a luxury watch at night.

2. "PAPEL" — light, tactile, editorial; the opposite of every AI app.
   Palette: #F1ECE3 warm paper, #161412 ink, #E5482B vermilion accent, #8C857B pencil gray.
   The voice visual is an ink waveform drawn like a brush stroke / seismograph line across the
   page. Swiss grid, big Grotesk type ("Neue Haas"/"Inter Display" feel), thin hairline rules,
   numbered sections, subtle paper grain. Feels like a printed magazine that listens.

3. "FÓSFORO" — a precision instrument / oscilloscope.
   Palette: #0A0B0A near black, #121412 panel, #FFB000 phosphor amber, #3A3F38 grid lines,
   #D9D4C7 text. The voice visual is a Lissajous figure / oscilloscope trace drawn live in
   amber, with a faint measurement grid and tick marks. Mono type everywhere ("JetBrains Mono"/
   "Berkeley Mono" feel), labels in small caps like hardware silkscreen, physical-looking
   buttons. Feels like Teenage Engineering made a voice assistant.

4. "MAREA" — calm, organic, premium. Real liquid glass done right.
   Palette: #0B1416 deep sea ink, #13211F surface, #CFE3DA sea-glass text, #7FD1B9 sea-glass
   accent, #F2C14E single warm sun accent used once. The voice visual is a drop of liquid glass
   (clear, refractive, with real caustic light on the background, NOT glowing) that deforms
   with the voice like water surface tension. Soft rounded sans ("Satoshi"/"General Sans" feel).
   Feels like sea glass on a beach at dusk.

Make each one feel designed by a different studio. Prioritize originality, restraint and craft.
```

Cuando elijas una dirección, pide las demás pantallas con:

```
Same exact art direction "<NOMBRE>", same palette, type and components. Now design these
screens, each as a separate flat 1080×2400 screenshot with Spanish text:
1) "Conversación": message history (no chat bubbles), one code block with "Copiar", text input.
2) "Voces": grid of voices (Álvaro, Clara, Diego, Elena, "Voz de Kairo"), buttons
   "Importar .vlvoice" and "Clonar mi voz", each voice with "Probar".
3) "Ajustes": sections Cerebro, Voz, Audio, Modelos, Diagnóstico, Acerca de; toggles/sliders
   "Manos libres" and "Sensibilidad de interrupción".
4) "Diagnóstico": back button, title, "Copiar informe"; section "Modelos" (name, "177 MB",
   license, progress "84 / 177 MB", "Descargar"/"Borrar"); section "Dispositivo" (Redmi Note 14
   4G · Helio G99 · Android 14 · AEC: sí); benchmark rows ("VoiceLab · 1 hilo — RTF p99 0,48",
   "Piper — RTF 0,21", "Whisper base — 620 ms · WER 9 %") with "Ejecutar".
5) "Bienvenida": 3 steps "Conecta OpenRouter" → "Descarga los modelos" → "Permisos".
```
