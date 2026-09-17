# PR #10 — validação técnica e pendências de produto

Escopo: branch `feat/10-files-visual-foundation`, derivada de `feat/1-android-bootstrap`. Claude trabalha PR #9; não sobrescrever suas mudanças.

## Mudanças implementadas
- Componentes Compose azul/branco (`ApVisualComponents.kt`) e refatoração de Arquivos (`FileCatalogScreen.kt`): cards, busca, ordenação, estados vazios, detalhes e ações reais. Glifos autorais temporários, não Lucide.
- Proteção da integridade em `TeacherStore.addObservation`: aluno opcional precisa pertencer à turma da observação. A chave estrangeira do SQLite sozinha não garantia isso.
- `ObservationIsolationInstrumentedTest`: rejeita aluno de outra turma sem alterar banco; permite observação da própria turma e observação geral, verifica dados após reabrir SQLite. Só utiliza emulador descartável e dados sintéticos.
- `ObservationHistoryScreen.kt`: consulta TODAS as observações da turma em vez das cinco mais recentes; pesquisa texto/categoria/nome, filtros por tipo e ID real do aluno, exibe estado vazio e abre o registro para editar. `TeacherApp.kt` conecta histórico em Detalhe da Turma e devolve ao histórico após salvar ou excluir um registro aberto de lá. Não mexe no controle de Back/IME que está sob responsabilidade do Claude no PR #9.

## Validação e bloqueios
- Versão inicial visual no commit `b36d85c`: CI e testes SQLite Android verdes. **A nova implementação de histórico/isolamento precisa de CI, lint, compilação e instrumentados no HEAD deste registro, não herdar aprovação de commit antigo.**
- Verificar manualmente: Turmas → turma → Histórico de registros → filtro → abrir → editar/excluir → voltar à lista; reiniciar aplicativo; notas de turmas distintas não se misturam; alunos com nomes iguais devem permanecer distinguíveis pelos IDs no histórico.
- Ainda pendentes: UX visual comprovada em Android real, 360/412/480dp, TalkBack, font scaling, status/navigation bars/teclado. Não considerar CI verde como aceite visual.
- Arquivos integral: faltam pastas/favoritos/lixeira/restauração, metadados, URI/permissões e preview/captura. Não usar dados reais em prévias. Seleção de aluno no FORMULÁRIO de observações ainda usa nome; se houver homônimos pode selecionar o aluno errado — precisa correção antes do gate de produto.
- PR #9: navegação/Back/dirty state e smoke Android continuam sob Claude e revisão conjunta. PR #2 e `main` preservados; nenhum merge automático.
