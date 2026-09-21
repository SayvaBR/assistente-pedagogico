package com.sayvabr.assistentepedagogico.data

/** Parses a pasted one-column roster without merging homonyms or silently dropping fields. */
object StudentRosterImport {
    fun parse(text: String): List<String> {
        val lines = text.lineSequence().toList()
        val names = lines.mapIndexedNotNull { index, raw ->
            if (raw.isBlank()) return@mapIndexedNotNull null
            require('\t' !in raw) {
                "A linha ${index + 1} tem mais de uma coluna. Cole somente os nomes, um por linha."
            }
            val name = raw.trim()
            require(name.length >= 2) {
                "O nome da linha ${index + 1} precisa ter pelo menos 2 caracteres. Nenhum aluno foi adicionado."
            }
            name
        }
        require(names.isNotEmpty()) { "Cole pelo menos um nome de aluno, um por linha." }
        return names
    }
}
