# Estado verificável — 18/09/2026

**Repositório exclusivo:** `SayvaBR/assistente-pedagogico` (Kotlin/Jetpack Compose, SQLite offline). **Frente ativa:** [PR #15](https://github.com/SayvaBR/assistente-pedagogico/pull/15), aberta como **DRAFT**, empilhada sobre PR #13. PR #2 e o estado de 16/09 não representam o HEAD atual. Não fazer merge para `main`, gerar APK/AAB/release, publicar na Play Store nem capturas de tela automáticas sem autorização expressa da titular. Dados de teste exclusivamente sintéticos; nenhum banco de escola ou aluno real em dispositivos de teste.

## Planejamento — situação e evidência

- Disponíveis: calendário por dia, semana e mês, seleção de turma e data; agenda e compromissos CRUD; planos de aula com campos profissionais e momentos/tempos; busca BNCC offline; atividades reutilizáveis e vinculadas; estados rascunho/pronto/concluído/arquivado; duplicação transacional de plano e atividades; controle de colisão entre planos e compromissos; backup/restauração SAF com prévia e confirmação explícita.
- A base `5e917a2` passou nos três gates: [Android CI](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35375700683), [Quality](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35375700885) e [emulador](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35375700738). Esses resultados cobrem restauração dos editores, recriação real da MainActivity e leitura/gravação em documento sintético via ContentResolver, mas **não** a interação visual do seletor de arquivos.
- Após a base aprovada: corrigido padding de barra inferior em todas as rotas (`8b5c13d`); preservadas seleções BNCC provisórias na rotação e isolado o filtro de ano por etapa (`2165891`); adicionados testes para seleção BNCC e alcance dos botões de salvar após digitação (`75eae3b`, `71e1995`). **Verificar CI e emulador destes commits antes de declarar aprovada a rodada nova.**
- Fonte BNCC: snapshot terceirizado `dados-2026.07.1`, 1.721 entradas (141 Computação). Contagem e estrutura automatizadas; textos e metadados ainda **não auditados integralmente** contra MEC/CNE. Veja `docs/PLANEJAMENTO_BNCC_AUDITORIA.md`. A UI não deve anunciar homologação MEC.
- Estimativas internas anteriores (não métricas): **82% funcional, 40% visual**. Não alterá-las sem novos gates, verificação de aparelho e aceite funcional.

## Pendências críticas do Planejamento (sem maquiagem de 100%)

- [ ] Gates completos para o HEAD da rodada atual; resolver falhas e registrar número de testes e evidência.
- [ ] Homologar teclado real, rotação, área segura superior/inferior, navegação gestual/Voltar, retomada e encerramento do processo em Android físico — sem capturas automáticas.
- [ ] Validar escolha visual `CreateDocument`/`OpenDocument`, permissões, cancelamento e ausência de sobrescrita sem consentimento. O round-trip por URI já é coberto separadamente.
- [ ] Conferir códigos, texto, etapas, anos, Computação e localizadores de todas as 1.721 entradas BNCC contra documentos oficiais; documentar diferenças antes de anunciar dataset verificado.
- [ ] Aceite da titular do fluxo real Planejamento. Depois, executar refinamento visual e Lucide seguindo design azul/branco.

## Demais áreas e lançamento

Turmas/alunos, frequência, observações, Arquivos e Mais permanecem frentes de produto separadas; a aprovação do Planejamento não equivale ao aplicativo pronto. Segurança/privacidade, eventuais assinaturas/Play Billing, geração e assinatura de release, revisão comercial e publicação na Play Store exigem gates próprios e autorização. **Auditoria específica de acessibilidade foi retirada do escopo desta rodada a pedido da titular**; requisitos essenciais de uso Android como botão Voltar, teclado e áreas seguras permanecem.

Os links acima são evidências de um commit anterior; a PR e seus checks atuais são a fonte de verdade para o HEAD. CI verde isolado não representa produto concluído.
