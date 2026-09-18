package com.sayvabr.assistentepedagogico.data

/**
 * Immutable domain model for the v9 plan composer. The enum defines semantic identity,
 * not a mandatory display order. Canonical lesson fields still live in LessonPlanV6.
 */
enum class PlanBlockKind(val defaultTitle: String) {
    IDENTIFICATION("Identificação"),
    OBJECTIVES("Objetivos"),
    CONTENT("Conteúdo / objeto de conhecimento"),
    BNCC("Habilidades BNCC"),
    CONTEXT("Justificativa / contextualização"),
    METHODOLOGY("Metodologia"),
    OPENING("Abertura"),
    DEVELOPMENT("Desenvolvimento"),
    CLOSING("Fechamento"),
    ASSESSMENT("Avaliação"),
    ADAPTATIONS("Adaptações"),
    RESOURCES("Recursos"),
    CUSTOM("Seção personalizada"),
}

/** Stable block IDs are unrelated to the current position; moving a block never changes its data. */
data class PlanBlock(
    val id: String,
    val kind: PlanBlockKind,
    val title: String = kind.defaultTitle,
    /** Only custom sections retain arbitrary text; no BNCC text is generated. */
    val body: String = "",
    val minutes: Int? = null,
)

/** A template has no teacher/student-specific content. */
data class PlanTemplate(val name: String, val blocks: List<PlanBlock>) {
    init {
        require(name.trim().length in 2..80) { "Informe o nome do modelo (2–80 caracteres)." }
        PlanComposition(blocks)
        require(blocks.all { it.body.isEmpty() && it.minutes == null }) {
            "Modelos não podem copiar conteúdo de aulas sem consentimento explícito."
        }
    }

    fun instantiate(): PlanComposition = PlanComposition(blocks.map { it.copy() })
}

/** Immutable editing operations preserve section identity across movement and saves. */
data class PlanComposition(val blocks: List<PlanBlock>) {
    init {
        require(blocks.size in 1..40) { "O plano deve conter de 1 a 40 seções." }
        require(blocks.count { it.kind == PlanBlockKind.IDENTIFICATION } == 1) {
            "A identificação é necessária para manter turma, data e calendário vinculados ao plano."
        }
        require(blocks.map { it.id }.distinct().size == blocks.size) { "Seções com identificadores repetidos." }
        require(blocks.filter { it.kind != PlanBlockKind.CUSTOM }.map { it.kind }.distinct().size ==
            blocks.count { it.kind != PlanBlockKind.CUSTOM }) { "Uma seção nativa não pode ser duplicada." }
        blocks.forEach { block ->
            require(block.id.matches(Regex("[a-zA-Z0-9_-]{1,80}"))) { "Identificador de seção inválido." }
            require(block.title.trim().length in 2..100) { "O título de uma seção deve ter de 2 a 100 caracteres." }
            require(block.body.length <= 20_000) { "Conteúdo da seção muito extenso." }
            require(block.minutes == null || (block.kind in MOMENTS && block.minutes in 0..480)) {
                "Somente momentos de aula aceitam tempos entre 0 e 480 minutos."
            }
        }
    }

    /** Always return a new layout. Moving beyond an edge is deliberately a no-op. */
    fun move(id: String, offset: Int): PlanComposition {
        require(offset == -1 || offset == 1) { "Mova uma seção por vez." }
        val from = blocks.indexOfFirst { it.id == id }
        require(from >= 0) { "Seção não encontrada." }
        val to = from + offset
        if (to !in blocks.indices) return this
        val next = blocks.toMutableList()
        val item = next.removeAt(from)
        next.add(to, item)
        return PlanComposition(next)
    }

    fun add(section: PlanBlock, afterId: String? = null): PlanComposition {
        val at = if (afterId == null) blocks.size else blocks.indexOfFirst { it.id == afterId }
            .also { require(it >= 0) { "Seção de referência não encontrada." } } + 1
        return PlanComposition(blocks.toMutableList().apply { add(at, section) })
    }

    fun remove(id: String): PlanComposition {
        val section = blocks.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Seção não encontrada.")
        require(section.kind != PlanBlockKind.IDENTIFICATION) {
            "A identificação pode ser reposicionada, mas seus dados não podem ser apagados."
        }
        return PlanComposition(blocks.filterNot { it.id == id })
    }

    fun update(section: PlanBlock): PlanComposition {
        require(blocks.any { it.id == section.id }) { "Seção não encontrada." }
        val old = blocks.first { it.id == section.id }
        require(old.kind == section.kind) { "O tipo de uma seção existente não pode mudar silenciosamente." }
        return PlanComposition(blocks.map { if (it.id == section.id) section else it })
    }

    /** Avoid accidental copying of previous pupils' work, personal notes, or BNCC free text. */
    fun saveAsTemplate(name: String): PlanTemplate = PlanTemplate(
        name.trim(), blocks.map { it.copy(body = "", minutes = null) }
    )

    companion object {
        private val MOMENTS = setOf(PlanBlockKind.OPENING, PlanBlockKind.DEVELOPMENT, PlanBlockKind.CLOSING)

        fun standard(): PlanComposition = PlanComposition(
            listOf(
                PlanBlock("identification", PlanBlockKind.IDENTIFICATION),
                PlanBlock("objectives", PlanBlockKind.OBJECTIVES),
                PlanBlock("content", PlanBlockKind.CONTENT),
                PlanBlock("bncc", PlanBlockKind.BNCC),
                PlanBlock("context", PlanBlockKind.CONTEXT),
                PlanBlock("methodology", PlanBlockKind.METHODOLOGY),
                PlanBlock("opening", PlanBlockKind.OPENING),
                PlanBlock("development", PlanBlockKind.DEVELOPMENT),
                PlanBlock("closing", PlanBlockKind.CLOSING),
                PlanBlock("assessment", PlanBlockKind.ASSESSMENT),
                PlanBlock("adaptations", PlanBlockKind.ADAPTATIONS),
            )
        )
    }
}
