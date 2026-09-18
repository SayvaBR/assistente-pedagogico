# Planejamento — contrato do construtor flexível (18/09/2026)

## Estado atual: implementação integrada, validação em andamento

O construtor está conectado ao editor real `LessonEditorV6.kt` por `PlanComposerPanel.kt`. O domínio (`PlanComposition.kt`), codec (`PlanLayoutV9.kt`), persistência SQLite v9, migração aditiva v8→v9, duplicação, backup/restauração e exportação ordenada estão implementados. **Não confundir implementação com aceite final:** os testes do HEAD da PR #15 e a experiência em aparelho físico ainda devem ser avaliados. O documento anterior dizia que o editor era fixo e a migração v9 não existia; isso ficou obsoleto.

## Objetivo funcional

O professor escolhe as seções que compõem o plano, adiciona várias seções personalizadas, renomeia por diálogo com confirmação, escreve diretamente nas seções, reorganiza por Subir/Descer e salva modelos próprios de escola sem copiar conteúdo de aulas anteriores. Planos já criados permanecem independentes dos modelos posteriormente excluídos. O fluxo funciona offline.

## Três camadas de dados

1. **Campos canônicos:** ID, turma, data, disciplina, horário/duração, estado, códigos BNCC, objetivo, conteúdo, metodologia e demais propriedades de `LessonPlanV6`. O calendário não depende da posição ou visibilidade de cartões; a BNCC é uma referência ao catálogo independente, sem gerar códigos ou descrições fictícios.
2. **Composição do plano:** IDs estáveis dos blocos, tipo semântico, título, ordem e texto de seções `CUSTOM`. Dados canônicos não são duplicados no JSON. O layout e os campos são gravados na mesma transação quando a edição é salva.
3. **Modelo reutilizável:** nome e cópia da estrutura sem conteúdo livre nem tempos. Aplicar modelo substitui a organização e o texto das seções personalizadas após confirmação, preservando os campos canônicos da aula. Excluir modelo não modifica planos existentes.

## Regras e comportamento implementados

- `PlanComposition.standard()` contempla Identificação, Objetivos, Conteúdo, BNCC, Contextualização, Metodologia, Abertura, Desenvolvimento, Fechamento, Avaliação e Adaptações.
- `add`, `remove`, `move(±1)` e `update` devolvem nova composição. A Identificação é obrigatória no domínio; a interface preserva também os blocos nativos essenciais Objetivos, Conteúdo e Metodologia. Demais blocos são opcionais e campos canônicos ocultados não são apagados.
- Blocos nativos não podem se repetir; blocos personalizados podem se repetir com IDs únicos. Limites: 1 a 40 seções; título com 2 a 100 caracteres; texto próprio até 20.000 caracteres. Renomeação ocorre em diálogo validado para permitir substituir integralmente o nome sem rejeitar o primeiro caractere intermediário; cancelar não altera o plano.
- Layout de plano legado usa fallback `standard()`; v8→v9 adiciona tabelas sem reescrever registros existentes. O backup v1 antigo continua aceito com fallback, e novos layouts/modelos malformados são rejeitados antes da restauração apagar dados.
- A duplicação cria cópia independente do plano, layout e atividades vinculadas. A prévia de compartilhamento percorre a ordem configurada; compartilhar não salva um rascunho automaticamente.
- A organização não salva usa estado recuperável na rotação; voltar de editor modificado exige confirmação de descarte. Modelos não copiam conteúdo anterior por padrão.

## BNCC: requisito funcional, não auditoria exaustiva

O catálogo é um snapshot offline atribuído à fonte independente e fornece orientação pedagógica. Por decisão da titular em 18/09/2026, a conferência editorial individual das 1.719 entradas restantes não bloqueia a conclusão. Permanecem obrigatórios seleção, salvamento, integridade, busca e tratamento de indisponibilidade. Não afirmar API ao vivo ou homologação MEC. Ver `docs/PLANEJAMENTO_BNCC_AUDITORIA.md`.

## Evidências e critérios de aceite

- `PlanLayoutV9InstrumentedTest` exercita persistência, reabertura, duplicação independente, modelo sem conteúdo, backup, rejeição de corrupção e migração.
- `PlanComposerPanelInstrumentedTest` exercita bloco personalizado, movimento, modelo sem texto, restauração de rascunho e renomeação validada com cancelamento.
- `PlanningComposerEndToEndInstrumentedTest` percorre a UI com plano sintético: editar seção personalizada, salvar, reabrir e duplicar. Considerar aprovado **somente após workflow do HEAD concluir com sucesso**.
- Aceite manual ainda necessário no Android físico: criação, edição, reabertura, duplicação, modelo, restauração SAF, Voltar, rotação, teclado, insets e compartilhamento. Não usar dados de alunos reais nos testes.
- Após estabilizar a função, melhorar visual azul/branco e iconografia Lucide, sem capturas automáticas recorrentes. PR #15 permanece DRAFT até decisão expressa.
