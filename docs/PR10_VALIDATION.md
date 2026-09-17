# PR #10 — validação técnica e pendências de produto

Branch `feat/10-files-visual-foundation`, derivada da integração PR #2; Claude trabalha separadamente PR #9 (Back/IME/rascunho). Ambos exigem revisão conjunta antes de integrar.

## Implementado em código
- `ApVisualComponents.kt`: botões táteis com borda inferior sólida, cartões e glifos vetoriais autorais, azul/branco, sem fingir que são Lucide.
- `FileCatalogScreen.kt`: biblioteca com destaque contextual, quantidade real de documentos, busca/ordenação, estados vazios, detalhes e ações existentes de importar/abrir/renomear referência/remover referência.
- `TeacherStore.addObservation`: impede associar observação a aluno de turma alheia; foreign key SQLite sozinha não garantiria o escopo. `ObservationIsolationInstrumentedTest` verifica recusa sem inserção e persistência dos caminhos legítimos após reabrir banco em emulador descartável.
- `ObservationHistoryScreen.kt`: mostra TODAS as observações da turma, busca por conteúdo/nome, filtros de categoria e aluno por ID, vazio contextual, abre observação para editar. `TeacherApp.kt` liga a tela ao detalhe da turma, inclusive retorno ao histórico após criar/editar/excluir.
- Formulário de observações: seleção de aluno agora usa ID e dropdown; homônimos exibem identificador interno quando necessário, evitando associar registros ao primeiro nome coincidente.

## Evidências necessárias
- CI+Android instrumentado no HEAD deste commit; compilações anteriores do UI (`b36d85c`) passaram, mas não cobrem estas alterações. Não atestar sucesso antes do check específico.
- Caminho real no dispositivo: turma → histórico → filtro → editar/excluir → lista → reiniciar; testar homônimos, turma alheia, todos os tipos e busca vazia.
- Revisão visual de screenshots reais 360/412/480dp, Android 16/IME/status bar, fonte ampliada e TalkBack não realizada. Integrar PR #9 somente após teste conjunto de rotas e modificações não salvas.
- Arquivos ainda parcial: pastas, favoritos, lixeira/restaurar, metadados/URI/permissões e prévia/captura faltam. Não usar dados reais em prévias.
- Não mesclar `main`, lançar Play/RevenueCat ou declarar UX/produto pronto automaticamente.
