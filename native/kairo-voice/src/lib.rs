//! Esqueleto de Fase 0: solo comprueba que la cadena Rust → Android (cargo-ndk) funciona en CI.
//! En Fase 1 este crate expone `VoiceEngine` por UniFFI (KAIRO_PLAN §5).

pub fn engine_version() -> &'static str {
    env!("CARGO_PKG_VERSION")
}

#[cfg(test)]
mod tests {
    #[test]
    fn version_is_set() {
        assert!(!super::engine_version().is_empty());
    }
}
