package com.sayvabr.assistentepedagogico.ui

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Works with the user-selected SAF URI. No filesystem paths, network, or external PDF bytes. */
internal object PlanningBackupDocumentIo {
    const val MAX_BYTES = 10 * 1024 * 1024

    fun write(resolver: ContentResolver, uri: Uri, payload: String) {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Backup excede o limite de 10 MB." }
        val stream = resolver.openOutputStream(uri, "wt")
            ?: error("Não foi possível abrir o destino selecionado.")
        stream.use { output -> output.write(bytes); output.flush() }
    }

    fun read(resolver: ContentResolver, uri: Uri): String {
        val input = resolver.openInputStream(uri)
            ?: error("Não foi possível ler o documento selecionado.")
        val buffer = ByteArray(8192)
        val contents = ByteArrayOutputStream()
        input.use { source ->
            while (true) {
                val count = source.read(buffer)
                if (count < 0) break
                require(contents.size() + count <= MAX_BYTES) { "Backup excede o limite de 10 MB." }
                contents.write(buffer, 0, count)
            }
        }
        return contents.toString(Charsets.UTF_8.name())
    }
}
