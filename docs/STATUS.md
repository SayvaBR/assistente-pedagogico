# Estado verificável — Planejamento, 18/09/2026

**Repositório:** `SayvaBR/assistente-pedagogico` (Kotlin/Jetpack Compose, SQLite offline). **Trabalho ativo:** [PR #15](https://github.com/SayvaBR/assistente-pedagogico/pull/15), aberta como **DRAFT** e empilhada sobre PR #13. Não fazer merge para `main`, gerar APK/AAB/release, publicar na Play Store nem capturas automáticas sem autorização. Testes que reiniciam banco rodam apenas em emulador descartável com dados sintéticos.

## Funcionalidade implementada

- Planejamento Dia/Semana/Mês, agenda, planos de aula, atividades vinculadas, estados rascunho/pronto/concluído/arquivado, duplicação com atividades, backup/restauração por SAF e proteção de navegação/áreas seguras.
- Construtor de planos v9 funcional na interface: seções nativas ordenáveis, seções personalizadas com texto próprio, inclusão/remoção de blocos opcionais e modelos reutilizáveis que não copiam o texto de aulas anteriores. Campos canônicos e estrutura são gravados na mesma transação SQLite; a migração v8→v9 é aditiva e o backup mantém compatibilidade.
- A renomeação das seções usa confirmação explícita: permite apagar o nome antigo e digitar outro, mas impede salvar títulos com menos de 2 caracteres. O teste específico verifica validação e cancelamento.
- A seleção BNCC utiliza um **snapshot offline de fonte independente**, não uma API remota em tempo real. Por decisão da titular, serve de referência pedagógica; auditoria editorial individual das 1.719 habilidades restantes não é bloqueio. Permanecem testes de seleção, persistência, integridade de códigos e indisponibilidade. Não afirmar homologação MEC; detalhes em `docs/PLANEJAMENTO_BNCC_AUDITORIA.md`.

## Evidências e verificação

- O HEAD `d2287b1bc99b3e142edfb25ee554d4c162275add` passou [Android CI](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35394835459), [Quality](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35394835441) e [emulador/persistência](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35394835493). Estes links **não** comprovam o estado de commits posteriores.
- Rodada posterior adicionou a janela de renomeação, ajustou testes de navegação/Voltar/teclado, adicionou teste integrado que edita, salva, reabre e duplica um plano com seção personalizada (`PlanningComposerEndToEndInstrumentedTest.kt`) e atualizou a comunicação BNCC e a documentação. Verificar os workflows do HEAD final desta rodada antes de declarar os novos testes aprovados; não tratar execução em andamento como sucesso.
- Testes em emulador não substituem teste no aparelho da titular, especialmente o seletor de documentos do sistema e teclado real.

## Critérios restantes para concluir esta frente

1. Obter os três gates verdes no HEAD final da rodada, investigar falhas reais e corrigir antes do aceite.
2. Validar em Android físico, somente com dados fictícios: criar/editar/reabrir/duplicar plano, salvar e descartar rascunho, backup/exportação/importação SAF, seleção BNCC, Voltar, rotação, teclado e insets superior/inferior. Sem afirmar aceite sem teste da titular.
3. Refinar as telas de Planejamento segundo a identidade azul/branco aprovada, ícones Lucide e qualidade de interação, preservando a funcionalidade. Capturas apenas quando expressamente necessárias/autorizadas.
4. Play Store, cobrança, segurança/privacidade e lançamento têm gates próprios fora desta PR. O restante do produto não se torna pronto automaticamente quando o Planejamento é aprovado.

**Sem merge e sem percentual calculado por número de commits.** Os checks correntes da PR são a fonte da verdade para o HEAD.
