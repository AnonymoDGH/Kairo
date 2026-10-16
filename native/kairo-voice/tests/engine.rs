//! Prueba con modelos reales. Se salta si faltan `KAIRO_MODELS` (carpeta con manifest.json y los
//! .onnx de 120 ms), `KAIRO_ORT_LIB` (libonnxruntime) o `KAIRO_VOICE` (un .vlvoice).

use kairo_voice::{VoiceEngine, init_onnx_runtime};

fn env(k: &str) -> Option<String> {
    std::env::var(k).ok().filter(|v| !v.is_empty())
}

#[test]
fn streaming_flush_and_bench() {
    let (Some(models), Some(ort_lib), Some(voice)) = (
        env("KAIRO_MODELS"),
        env("KAIRO_ORT_LIB"),
        env("KAIRO_VOICE"),
    ) else {
        eprintln!("sin modelos: prueba saltada");
        return;
    };
    init_onnx_runtime(ort_lib).unwrap();
    let engine = VoiceEngine::new(models, "120ms".into(), 1).unwrap();
    engine.load_voice(std::fs::read(voice).unwrap()).unwrap();

    // Trozos de tamaño irregular, como los que da Piper tras remuestrear.
    let input: Vec<f32> = (0..16_000 * 2)
        .map(|i| (i as f32 * 0.03).sin() * 0.2)
        .collect();
    let bytes = |pcm: &[f32]| {
        pcm.iter()
            .flat_map(|v| v.to_le_bytes())
            .collect::<Vec<u8>>()
    };
    let mut out = Vec::new();
    for chunk in input.chunks(1_234) {
        out.extend(engine.push(bytes(chunk)).unwrap());
    }
    out.extend(engine.flush().unwrap());
    let out: Vec<f32> = out
        .chunks_exact(4)
        .map(|c| f32::from_le_bytes([c[0], c[1], c[2], c[3]]))
        .collect();
    let latency = (engine.algorithmic_latency_ms() * 16.0).round() as usize;
    assert_eq!(out.len(), input.len() + latency);
    assert!(out.iter().all(|v| v.is_finite()));

    let report = engine.bench(5.0).unwrap();
    eprintln!("{report:#?}");
    assert!(report.blocks > 0 && report.rtf > 0.0);
}
