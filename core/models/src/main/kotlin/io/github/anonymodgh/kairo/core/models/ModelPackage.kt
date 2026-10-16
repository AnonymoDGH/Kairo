package io.github.anonymodgh.kairo.core.models

enum class ArchiveType { TarBz2 }

/** [path] es relativo al directorio del paquete; en archivos comprimidos, el directorio donde extraer ("" = raíz). */
data class ModelFile(
    val url: String,
    val path: String,
    val sha256: String,
    val bytes: Long,
    val archive: ArchiveType? = null,
    val stripComponents: Int = 1,
)

data class ModelPackage(
    val id: String,
    val title: String,
    val description: String,
    val license: String,
    val files: List<ModelFile>,
) {
    val totalBytes: Long get() = files.sumOf { it.bytes }
}

sealed interface InstallState {
    data object NotInstalled : InstallState
    data class Downloading(val downloaded: Long, val total: Long) : InstallState
    data object Verifying : InstallState
    data object Extracting : InstallState
    data object Installed : InstallState
    data class Failed(val message: String) : InstallState
}

class ModelStoreException(message: String) : Exception(message)
