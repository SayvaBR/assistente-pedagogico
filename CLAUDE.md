# Claude — Assistente Pedagógico (repo NOVO)

Você trabalha exclusivamente em `SayvaBR/assistente-pedagogico`. **NUNCA** consulte ou copie `SayvaBR/assistente-ped` (legado). Antes de agir leia `AGENTS.md`, `docs/BLUEPRINT.md`, `docs/STATUS.md`, `docs/DESIGN_SYSTEM.md` e `docs/AGENT_WORKFLOW.md`; confirme o estado atual do GitHub, pois alguns status históricos podem estar desatualizados.

Sua frente atual é o PR #9, navegação/Back, teclado/IME, safe area e proteção de alterações não salvas, inclusive troca de data/aba na chamada. ChatGPT trabalha PR #10 (Arquivos, histórico de observações, integridade) e PR #11 (agentes/Design System). Antes de modificar arquivo compartilhado, compare os diffs, declare o que vai alterar e preserve as duas soluções; não faça push/force push na branch de outro agente.

Toda correção exige: cenário reproduzível, análise de alternativas e falhas futuras, teste de regressão apropriado, evidência do HEAD exato (CI/emulador/UI conforme escopo), descrição de limitações e PR DRAFT. Não marque como concluído com build verde apenas, não crie botões fictícios, não destrua dados/migrações, não publique dados reais de alunos, não exponha segredos e não faça merge na main nem publique APK/release. A titular suspendeu novo APK durante esta rodada; o CI pode compilar internamente, mas não deve distribuir artefatos de instalação.

Ouça sugestões de outros agentes, registre riscos e faça handoff transparente. Design System em `docs/DESIGN_SYSTEM.md` e tokens Kotlin em `ApDesignTokens.kt`. Receitas e divisão: `docs/AGENT_WORKFLOW.md`.
