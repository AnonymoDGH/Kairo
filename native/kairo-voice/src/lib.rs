//! Motor de voz de Kairo: envuelve `voicelab_core::StreamingVc` para Kotlin (UniFFI).
//! Todo el audio es 16 kHz mono `f32`, cruzando la frontera como bytes little-endian: UniFFI pasa
//! `Vec<u8>` como `ByteArray` sin copiar elemento a elemento (KAIRO_PLAN §5).

use std::sync::{Arc, Mutex, MutexGuard};
use std::time::Instant;

use safetensors::SafeTensors;
use safetensors::tensor::Dtype;
use voicelab_core::dsp::resample::resample_clip;
use voicelab_core::engine::vc::{BLOCK, SAMPLE_RATE, SPK_DIM};
use voicelab_core::{ModelDir, StreamingVc, Variant};

uniffi::setup_scaffolding!();

#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum VoiceError {
    #[error("{message}")]
    Engine { message: String },
}

impl From<anyhow::Error> for VoiceError {
    fn from(e: anyhow::Error) -> Self {
        VoiceError::Engine {
            message: format!("{e:#}"),
        }
    }
}

fn err(message: impl Into<String>) -> VoiceError {
    VoiceError::Engine {
        message: message.into(),
    }
}

/// Carga `libonnxruntime.so` (la que trae sherpa-onnx) una sola vez por proceso.
/// En Android basta el nombre: el enlazador la encuentra en el directorio de librerías de la app.
#[uniffi::export]
pub fn init_onnx_runtime(lib_path: String) -> Result<(), VoiceError> {
    static INIT: Mutex<bool> = Mutex::new(false);
    let mut done = INIT.lock().unwrap_or_else(|p| p.into_inner());
    if !*done {
        ort::init_from(&lib_path)
            .map_err(|e| err(format!("no se pudo cargar {lib_path}: {e}")))?
            .with_name("kairo")
            .commit();
        *done = true;
    }
    Ok(())
}

/// Remuestreo de un clip completo (p. ej. Piper 22,05 kHz → 16 kHz).
#[uniffi::export]
pub fn resample(pcm: Vec<u8>, from_hz: u32, to_hz: u32) -> Result<Vec<u8>, VoiceError> {
    Ok(to_bytes(&resample_clip(
        &from_bytes(&pcm)?,
        from_hz,
        to_hz,
    )?))
}

fn from_bytes(b: &[u8]) -> Result<Vec<f32>, VoiceError> {
    if !b.len().is_multiple_of(4) {
        return Err(err("PCM: la longitud no es múltiplo de 4 bytes"));
    }
    Ok(b.chunks_exact(4)
        .map(|c| f32::from_le_bytes([c[0], c[1], c[2], c[3]]))
        .collect())
}

fn to_bytes(pcm: &[f32]) -> Vec<u8> {
    pcm.iter().flat_map(|v| v.to_le_bytes()).collect()
}

#[uniffi::export]
pub fn engine_version() -> String {
    env!("CARGO_PKG_VERSION").to_string()
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct BenchReport {
    pub variant: String,
    pub threads: u32,
    pub blocks: u32,
    pub audio_seconds: f64,
    pub wall_seconds: f64,
    /// Tiempo de proceso / duración del audio, global.
    pub rtf: f64,
    pub rtf_p50: f64,
    pub rtf_p99: f64,
    pub block_ms_p50: f64,
    pub block_ms_p99: f64,
    pub algorithmic_latency_ms: f64,
}

struct State {
    vc: StreamingVc,
    /// Muestras recibidas y entregadas desde el último `reset`, para que `flush` no corte ni rellene.
    n_in: usize,
    n_out: usize,
}

#[derive(uniffi::Object)]
pub struct VoiceEngine {
    state: Mutex<State>,
    variant: Variant,
    threads: u32,
}

#[uniffi::export]
impl VoiceEngine {
    /// `variant`: "120ms" (Calidad) o "40ms". Requiere `init_onnx_runtime` antes.
    #[uniffi::constructor]
    pub fn new(model_dir: String, variant: String, threads: u32) -> Result<Arc<Self>, VoiceError> {
        let variant: Variant = variant.parse()?;
        if variant.uses_bwe() {
            return Err(err("la variante Ultra no está soportada en Android"));
        }
        let models = ModelDir::open(&model_dir)?;
        let vc = StreamingVc::new(&models, variant, threads.max(1) as usize)?;
        Ok(Arc::new(Self {
            state: Mutex::new(State {
                vc,
                n_in: 0,
                n_out: 0,
            }),
            variant,
            threads: threads.max(1),
        }))
    }

    /// Carga la voz destino desde el contenido de un `.vlvoice` (safetensors con `spk_emb` f32[256]).
    pub fn load_voice(&self, vlvoice: Vec<u8>) -> Result<(), VoiceError> {
        let emb = parse_vlvoice(&vlvoice)?;
        self.lock().vc.set_speaker(&emb)?;
        Ok(())
    }

    /// Streaming: entra audio, sale lo que esté listo (puede ser vacío).
    pub fn push(&self, pcm: Vec<u8>) -> Result<Vec<u8>, VoiceError> {
        let pcm = from_bytes(&pcm)?;
        let mut s = self.lock();
        s.n_in += pcm.len();
        let out = s.vc.process(&pcm)?;
        s.n_out += out.len();
        Ok(to_bytes(&out))
    }

    /// Vacía el lookahead al final de una frase: devuelve exactamente lo que faltaba por salir.
    pub fn flush(&self) -> Result<Vec<u8>, VoiceError> {
        let mut s = self.lock();
        let owed = (s.n_in + s.vc.algorithmic_latency_samples()).saturating_sub(s.n_out);
        let mut out = Vec::with_capacity(owed);
        let silence = vec![0.0; BLOCK];
        let mut guard = 0;
        while out.len() < owed && guard < 16 {
            out.extend(s.vc.process(&silence)?);
            guard += 1;
        }
        out.truncate(owed);
        s.vc.reset();
        s.n_in = 0;
        s.n_out = 0;
        Ok(to_bytes(&out))
    }

    pub fn reset(&self) {
        let mut s = self.lock();
        s.vc.reset();
        s.n_in = 0;
        s.n_out = 0;
    }

    pub fn algorithmic_latency_ms(&self) -> f64 {
        self.lock().vc.algorithmic_latency_samples() as f64 * 1000.0 / SAMPLE_RATE as f64
    }

    /// Mide el RTF procesando `seconds` de audio sintético en bloques de 160 ms, como en vivo.
    pub fn bench(&self, seconds: f32) -> Result<BenchReport, VoiceError> {
        let mut s = self.lock();
        s.vc.reset();
        let input = test_signal((seconds.max(1.0) * SAMPLE_RATE as f32) as usize);
        let block_s = BLOCK as f64 / SAMPLE_RATE as f64;
        // Calentamiento: las primeras inferencias de ONNX Runtime son más lentas.
        for block in input.chunks(BLOCK).take(3) {
            s.vc.process(block)?;
        }
        s.vc.reset();
        let mut times = Vec::with_capacity(input.len() / BLOCK + 1);
        let start = Instant::now();
        for block in input.chunks_exact(BLOCK) {
            let t = Instant::now();
            s.vc.process(block)?;
            times.push(t.elapsed().as_secs_f64());
        }
        let wall = start.elapsed().as_secs_f64();
        s.vc.reset();
        s.n_in = 0;
        s.n_out = 0;
        let audio = times.len() as f64 * block_s;
        times.sort_by(f64::total_cmp);
        let latency = s.vc.algorithmic_latency_samples() as f64 * 1000.0 / SAMPLE_RATE as f64;
        Ok(BenchReport {
            variant: self.variant.key().to_string(),
            threads: self.threads,
            blocks: times.len() as u32,
            audio_seconds: audio,
            wall_seconds: wall,
            rtf: wall / audio,
            rtf_p50: percentile(&times, 0.50) / block_s,
            rtf_p99: percentile(&times, 0.99) / block_s,
            block_ms_p50: percentile(&times, 0.50) * 1000.0,
            block_ms_p99: percentile(&times, 0.99) * 1000.0,
            algorithmic_latency_ms: latency,
        })
    }
}

impl VoiceEngine {
    fn lock(&self) -> MutexGuard<'_, State> {
        self.state.lock().unwrap_or_else(|p| p.into_inner())
    }
}

fn parse_vlvoice(bytes: &[u8]) -> Result<Vec<f32>, VoiceError> {
    let st = SafeTensors::deserialize(bytes)
        .map_err(|e| err(format!("archivo de voz no válido: {e}")))?;
    let t = st
        .tensor("spk_emb")
        .map_err(|_| err("el archivo de voz no tiene spk_emb"))?;
    if t.dtype() != Dtype::F32 || t.shape() != [SPK_DIM] {
        return Err(err(format!("spk_emb debe ser f32[{SPK_DIM}]")));
    }
    Ok(t.data()
        .chunks_exact(4)
        .map(|b| f32::from_le_bytes([b[0], b[1], b[2], b[3]]))
        .collect())
}

fn percentile(sorted: &[f64], q: f64) -> f64 {
    if sorted.is_empty() {
        return 0.0;
    }
    sorted[((sorted.len() - 1) as f64 * q).round() as usize]
}

/// Señal parecida a voz (armónicos con tono y amplitud variables + ruido): el coste del modelo
/// no depende del contenido, pero así el VAD/gate no lo trata como silencio.
fn test_signal(len: usize) -> Vec<f32> {
    let mut seed = 0x2545_f491_u32;
    let sr = SAMPLE_RATE as f32;
    let mut phase = 0.0f32;
    (0..len)
        .map(|i| {
            let t = i as f32 / sr;
            let f0 = 140.0 + 40.0 * (t * 0.7 * std::f32::consts::TAU).sin();
            phase = (phase + f0 / sr).fract();
            let harm: f32 = (1..=8)
                .map(|h| (phase * h as f32 * std::f32::consts::TAU).sin() / h as f32)
                .sum();
            seed ^= seed << 13;
            seed ^= seed >> 17;
            seed ^= seed << 5;
            let noise = seed as f32 / u32::MAX as f32 - 0.5;
            let env = 0.5 + 0.5 * (t * 3.0 * std::f32::consts::TAU).sin().abs();
            0.25 * env * (harm * 0.6 + noise * 0.1)
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use safetensors::tensor::TensorView;

    #[test]
    fn parses_vlvoice() {
        let emb: Vec<f32> = (0..SPK_DIM).map(|i| i as f32 * 0.5).collect();
        let bytes: Vec<u8> = emb.iter().flat_map(|v| v.to_le_bytes()).collect();
        let view = TensorView::new(Dtype::F32, vec![SPK_DIM], &bytes).unwrap();
        let file = safetensors::serialize([("spk_emb", view)], None).unwrap();
        assert_eq!(parse_vlvoice(&file).unwrap(), emb);
    }

    #[test]
    fn pcm_bytes_roundtrip() {
        let pcm = vec![0.0, -1.0, 0.5, f32::MIN_POSITIVE];
        assert_eq!(from_bytes(&to_bytes(&pcm)).unwrap(), pcm);
        assert!(from_bytes(&[0, 0, 0]).is_err());
    }

    #[test]
    fn rejects_garbage() {
        assert!(parse_vlvoice(b"no es una voz").is_err());
    }

    #[test]
    fn percentiles() {
        let v: Vec<f64> = (1..=100).map(f64::from).collect();
        assert_eq!(percentile(&v, 0.5), 51.0);
        assert_eq!(percentile(&v, 0.99), 99.0);
        assert_eq!(percentile(&[], 0.5), 0.0);
    }

    #[test]
    fn test_signal_is_bounded_and_not_silent() {
        let s = test_signal(SAMPLE_RATE as usize);
        assert!(s.iter().all(|v| v.abs() <= 1.0));
        let rms = (s.iter().map(|v| v * v).sum::<f32>() / s.len() as f32).sqrt();
        assert!(rms > 0.02);
    }
}
