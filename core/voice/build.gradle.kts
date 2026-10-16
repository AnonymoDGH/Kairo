plugins {
    alias(libs.plugins.kairo.library)
}

android {
    namespace = "io.github.anonymodgh.kairo.core.voice"
    ndkVersion = libs.versions.ndk.get()
}

// Motor Rust (native/kairo-voice): cargo-ndk → jniLibs, y bindings Kotlin de UniFFI.
val nativeDir = rootProject.layout.projectDirectory.dir("native")
val rustSources = fileTree(nativeDir) {
    include("**/*.rs", "**/Cargo.toml", "Cargo.lock", "**/uniffi.toml")
    exclude("target/**")
}

val cargoNdkBuild = tasks.register<CargoNdkBuildTask>("cargoNdkBuild") {
    sources.from(rustSources)
    workspace.set(nativeDir)
    crateName.set("kairo-voice")
    abis.set(listOf("arm64-v8a"))
    ndkDirectory.set(androidComponents.sdkComponents.ndkDirectory.map { it.asFile.absolutePath })
    outputDir.set(layout.buildDirectory.dir("rust/jniLibs"))
}

val uniffiBindgen = tasks.register<UniffiBindgenTask>("uniffiBindgen") {
    library.set(cargoNdkBuild.flatMap { it.outputDir.file("arm64-v8a/libkairo_voice.so") })
    config.from(nativeDir.file("kairo-voice/uniffi.toml"))
    workspace.set(nativeDir)
    outputDir.set(layout.buildDirectory.dir("generated/uniffi"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(cargoNdkBuild, CargoNdkBuildTask::outputDir)
        variant.sources.kotlin?.addGeneratedSourceDirectory(uniffiBindgen, UniffiBindgenTask::outputDir)
    }
}

dependencies {
    api(libs.sherpa.onnx) { artifact { type = "aar" } }
    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(libs.kotlinx.coroutines.android)
}
