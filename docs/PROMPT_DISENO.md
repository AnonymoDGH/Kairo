# Prompt para generar diseños de Kairo con una IA

Pensado para Google Stitch, v0, Figma Make, Galileo o un generador de imágenes (GPT-image,
Midjourney). Pégalo tal cual. Si la herramienta solo genera una pantalla por vez, pide primero
"Inicio" y luego el resto con "mismo estilo, ahora la pantalla X".

---

Diseña la interfaz móvil (Android, 393×873 dp, pantalla AMOLED) de **Kairo**, un asistente de voz
con el que hablas en tiempo real y que te responde con tu propia voz clonada. Todo el texto de la
interfaz en **español**.

**Estilo:** oscuro, limpio y con mucho aire, como el editor Cursor, mezclado con el *liquid glass*
de Apple: paneles de cristal translúcido con desenfoque fuerte del fondo, bordes finos que brillan
(degradado blanco 28 % → 4 % de arriba-izquierda a abajo-derecha), un reflejo especular en el borde
superior y una ligera refracción en las esquinas. Nada de decoración gratuita, ni sombras duras,
ni burbujas de chat de colores.

**Paleta:** fondo `#07070B`. Sobre él, una *aurora* de manchas grandes y difusas en violeta
`#7C5CFF`, cian `#22D3EE` y magenta `#F472B6` al 35–45 %. Texto principal `#F5F5F7`, secundario
`#A1A1AA`. Acentos: cian cuando escucha, violeta cuando piensa y ámbar `#F59E0B` para errores.
**Tipografía:** Geist, y Geist Mono para código y datos técnicos. **Radios:** tarjetas 24 dp,
dock 28 dp y chips con forma de píldora.

**El protagonista es un orbe**: una esfera de cristal de unos 55 % del ancho, con fresnel en el
borde, un fluido de color que se mueve dentro y cáusticas suaves. Muéstralo en sus estados:
reposo (colores fríos tenues), escuchando (cian, ondas concéntricas suaves), pensando (violeta,
remolino interno) y hablando (late con la voz).

**Pantallas:**
1. **Inicio:** aurora de fondo y orbe centrado. Debajo, 1–2 líneas de transcripción en vivo: lo
   que dice el usuario en gris y lo que responde Kairo en blanco. Abajo, un **dock de cristal**
   con un botón de micrófono grande, un botón de teclado, un chip con el modelo de IA
   (p. ej. "Claude Haiku") y un chip con la voz activa (avatar pequeño + "Mi voz").
2. **Conversación:** hoja de cristal con el historial al estilo Cursor, en bloques limpios y sin
   burbujas. Los mensajes del usuario llevan un borde izquierdo de color de acento. Incluye un
   bloque de código en Geist Mono con botón "Copiar" y un campo de texto abajo.
3. **Paleta de comandos:** buscador de cristal con atajos: "Nueva conversación", "Cambiar voz",
   "Cambiar modelo", "Silenciar mi voz (voz Kairo)" y "Diagnóstico".
4. **Voces:** cuadrícula de tarjetas de cristal con retratos ilustrados y nombre, una tarjeta
   "Voz de Kairo" y botones "Importar .vlvoice" y "Clonar mi voz".
5. **Ajustes:** lista agrupada en cristal (Cerebro, Voz, Audio, Modelos, Diagnóstico, Acerca de)
   con interruptores y deslizadores minimalistas.
6. **Bienvenida:** 3 pasos en cristal: conectar OpenRouter → descargar modelos (con tamaños en MB)
   → permisos de micrófono y notificaciones, con indicador de progreso.

**Entrega 4 direcciones visuales distintas** de la pantalla de Inicio, con el mismo contenido y
estas variaciones:
- **A. Fiel:** exactamente lo descrito arriba.
- **B. Minimal:** casi monocromo, la aurora apenas visible y el orbe como único color.
- **C. Vibrante:** aurora intensa, más refracción y cristal más grueso, con brillos iridiscentes.
- **D. Terminal:** más cercana a Cursor, con más Geist Mono, líneas finas, rejilla sutil y chips
  tipo etiqueta.

Pon el nombre de cada dirección encima de su pantalla. Después, para la dirección que elija,
genera las otras 5 pantallas con el mismo estilo.
