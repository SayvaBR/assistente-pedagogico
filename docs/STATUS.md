# Estado verificável — 16/09/2026

**Repositório exclusivo:** `SayvaBR/assistente-pedagogico`. **PR #2:** aberto e DRAFT. **Gate integral:** issue #8. Não entregar APK, release ou publicar sem todos os critérios testados e aceite expresso da titular. A data da issue #4 é meta condicional, não permissão para reduzir o produto.

## Código existente e testes que já passaram
- Kotlin/Jetpack Compose + SQLite offline; turmas e alunos com operações iniciais, chamada, observações, Agenda e Arquivos em desenvolvimento.
- Android Back com histórico, onboarding e confirmação de saída codificados. Ainda faltam testes UI/gestos físicos e safe areas atuais; nenhum gate visual aprovado.
- Turmas editar/arquivar/restaurar, limite Free de 2 turmas ativas; alunos editar/excluir com confirmação; migração v1→v2 sem perda. Testes de persistência SQLite anteriores verdes.
- Arquivos: catálogo, busca, ordenação, detalhes, renomear referência local e remover referência sem apagar original; pastas/favoritos/lixeira e tratamento integral do SAF ainda ausentes.
- Agenda: cartões abrem compromisso, edição mantém ID, exclusão confirmada e testes de persistência. CI e emulador verdes no commit `e51d9a1`: runs `35159077809` e `35159077703`.

## Planejamento
- Implementação `2ca0fec815401f810242720beeda3fd71d2b3173`: consultas por dia/semana/mês, filtros da turma selecionada, edição de plano, arquivamento reversível e lista de arquivados para restaurar.
- Alteração de plano preserva identidade/ID e restringe turma; mudança de data atualiza seleção após salvamento; horário usa validação real.
- Banco v3 preserva migrações anteriores e adiciona `archived` aos planos. O planejamento profissional completo ainda exige momentos/tempos, avaliação, adaptações, pós-aula, BNCC oficial e calendário visual.

## Nova fatia de Registros — em validação no HEAD
- Commit `f2d7aa18c549863cfcee0afe96105f7c2e8d54b5`: observações recentes da turma agora são acionáveis; existe rota de edição, alteração de aluno/tipo/texto/permissão de compartilhamento e exclusão permanente com confirmação.
- `TeacherStore` valida que a observação e o aluno pertencem à turma correta; edição preserva ID e data original; exclusão é restrita ao registro selecionado.
- Teste instrumentado cobre edição persistente após reabrir banco, desvinculação de aluno, bloqueio de aluno/outra turma, exclusão isolada e ID inexistente.
- Os checks disparados automaticamente pelo commit do bot ficaram como `action_required`; esta atualização feita pela conta conectada existe para disparar CI e instrumentados sobre o mesmo código. **Não declarar esta fatia aprovada até os jobs terminarem com sucesso.**

## Não concluído / requisitos para entrega
- [ ] Validar CI e testes Android do CRUD de observações; teste de reinício do processo, Back/modal/teclado e safe areas reais.
- [ ] Planejamento profissional completo e BNCC oficial validada offline.
- [ ] Frequência com histórico e revisão completa; Agenda semana/mês; Arquivos 100%; Mais/relatórios PDF/CSV e backup/restauração seguro.
- [ ] Splash, onboarding integral, animações, ícones Lucide/visual fiel azul e branco, acessibilidade, comparação visual em gates finais e aceite da titular.
- [ ] Privacidade/LGPD, Play Billing com verificação real, release AAB assinado, Play Console e elegibilidade.

Não gerar screenshots rotineiras nem distribuir arquivos instaláveis durante a construção. CI verde isolado não representa produto concluído.
