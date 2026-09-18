package com.sayvabr.assistentepedagogico.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Immutable, bundled catalogue. Never sends a teacher's searches or plans to a third party. */
data class BnccLearning(
    val code: String,
    val text: String,
    val stage: String,
    val component: String,
    val years: List<Int>,
    val ageGroup: String,
    val supplement: Boolean,
    val document: String,
    val sourceFile: String,
    val sourcePage: String,
    val status: String,
)

class BnccCatalog private constructor(
    val version: String,
    val attribution: String,
    val entries: List<BnccLearning>,
) {
    private val byCode = entries.associateBy { it.code }
    init {
        require(version.isNotBlank() && attribution.contains("bncc.dev")) { "Fonte BNCC não identificada." }
        require(entries.size == 1721 && byCode.size == 1721) { "Catálogo BNCC incompleto ou com códigos duplicados." }
        require(entries.count { it.supplement } == 141) { "Complemento de Computação incompleto." }
        require(entries.all { it.code.isNotBlank() && it.text.isNotBlank() && it.sourceFile.isNotBlank() }) {
            "Uma aprendizagem está sem código, texto ou proveniência."
        }
    }

    fun find(code: String): BnccLearning? = byCode[code.trim().uppercase(Locale.ROOT)]

    fun unknownCodes(input: String): List<String> = parseCodes(input).filter { find(it) == null }

    /** Accent-insensitive on-device search; no remote API call or account is needed. */
    fun search(
        query: String = "",
        stage: String? = null,
        year: Int? = null,
        supplementOnly: Boolean = false,
        limit: Int = 80,
    ): List<BnccLearning> {
        val term = fold(query.trim())
        return entries.asSequence()
            .filter { stage == null || it.stage == stage }
            .filter { year == null || year in it.years }
            .filter { !supplementOnly || it.supplement }
            .filter { term.isBlank() || fold("${it.code} ${it.text} ${it.component} ${it.ageGroup}").contains(term) }
            .take(limit.coerceIn(1, 500))
            .toList()
    }

    companion object {
        const val ASSET = "bncc/catalog.json"
        const val EXPECTED_COUNT = 1721
        const val EXPECTED_SUPPLEMENT_COUNT = 141

        fun parseCodes(value: String): List<String> = value.split(',', ';', '\n')
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter(String::isNotEmpty)
            .distinct()

        private fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)

        fun load(context: Context): BnccCatalog = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use {
            fromJson(it.readText())
        }

        fun fromJson(raw: String): BnccCatalog {
            val document = JSONObject(raw)
            val list = document.getJSONArray("aprendizagens")
            require(document.getInt("quantidade") == EXPECTED_COUNT && list.length() == EXPECTED_COUNT) {
                "Snapshot BNCC não está completo."
            }
            val rows = ArrayList<BnccLearning>(list.length())
            for (index in 0 until list.length()) {
                val item = list.getJSONObject(index)
                val years = item.optJSONArray("anos")
                rows += BnccLearning(
                    code = item.getString("codigo"),
                    text = item.getString("texto"),
                    stage = item.getString("etapa"),
                    component = item.optString("componente"),
                    years = if (years == null) emptyList() else (0 until years.length()).mapNotNull {
                        years.optString(it).toIntOrNull()
                    },
                    ageGroup = item.optString("grupo_etario"),
                    supplement = item.getBoolean("complemento"),
                    document = item.optString("documento"),
                    sourceFile = item.getString("arquivo_fonte"),
                    sourcePage = item.optString("localizador_pdf"),
                    status = item.optString("vigencia"),
                )
            }
            return BnccCatalog(document.getString("versao"), document.getString("atribuicao"), rows)
        }
    }
}
