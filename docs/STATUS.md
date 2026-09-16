# Estado verificável — 16/09/2026

**Repositório exclusivo:** `SayvaBR/assistente-pedagogico`. **PR #2:** aberto e DRAFT. **Gate integral:** issue #8. Não entregar APK, release ou publicar sem todos os critérios testados e aceite expresso da titular. A data da issue #4 é meta condicional, não permissão para reduzir o produto.

## Código existente e testes que já passaram
- Kotlin/Jetpack Compose + SQLite offline; turmas e alunos com operações iniciais, chamada, observações, Agenda e Arquivos em desenvolvimento.
- Android Back com histórico, onboarding e confirmação de saída codificados. Ainda faltam testes UI/gestos físicos e safe areas atuais; nenhum gate visual aprovado.
- Turmas editar/arquivar/restaurar, limite Free de 2 turmas ativas; alunos editar/excluir com confirmação; migração v1→v2 sem perda. Testes de persistência SQLite anteriores verdes.
- Arquivos: catálogo, busca, ordenação, detalhes, renomear referência local e remover referência sem apagar original; pastas/favoritos/lixeira e tratamento integral do SAF ainda ausentes.
- Agenda: cartões abrem compromisso, edição mantém ID, exclusão confirmada e testes de persistência. CI e emulador verdes no commit `e51d9a1`: runs `35159077809` e `35159077703`.

## Nova fatia de Planejamento — ainda em validação
- Commit de implementação `2ca0fec815401f810242720beeda3fd71d2b3173`: consultas por dia/semana/mês, filtros da turma selecionada, acesso a edição de plano, arquivamento reversível com confirmação e lista de arquivados para restaurar.
- Alteração de plano preserva identidade/ID e restringe turma; mudança de data atualiza seleção após salvamento; validação `LocalTime.parse` para horário.
- Banco migra v1→v3 e v2→v3 sem destruir planos, adicionando somente a coluna `archived`; testes instrumentados novos para edição, isolamento entre turmas, arquivamento/restauração e migração v2. A criação de planos antigos também é coberta pela migração v1 existente.
- IMPORTANTE: CI no commit original do bot exibiu `action_required` (GitHub não executou PR workflows originados automaticamente). Este commit de documentação é disparado pela conta conectada para reexecutar as verificações no HEAD com o código novo. **Não atestar sucesso até jobs concluírem.**

## Não concluído / requisitos para entrega
- [ ] CI e testes Android específicos da nova fatia; teste de reinício do processo, Back/modal/teclado e safe areas reais.
- [ ] Planejamento profissional completo: campos de momentos/tempos, avaliação, adaptações, pós-aula, seleção BNCC oficial validada offline, calendário visual e fluxos aprofundados; visões recém-criadas não equivalem ao produto acabado.
- [ ] Frequência/histórico e observações totalmente editáveis; Agenda com visões semana/mês; Arquivos 100%; Mais/relatórios PDF/CSV e backup/restauração seguro.
- [ ] Splash, onboarding integral, animações, ícones Lucide/visual fiel azul e branco, acessibilidade, comparação visual em gates finais e aceite da titular.
- [ ] Privacidade/LGPD, Play Billing com verificação real, release AAB assinado, Play Console e elegibilidade.

Não gerar screenshots rotineiras nem distribuir arquivos instaláveis durante a construção. CI verde isolado não representa produto concluído.
