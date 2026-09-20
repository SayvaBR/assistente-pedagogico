# Estado verificável — 20/09/2026

## Repositório e integração

- Repositório: `SayvaBR/assistente-pedagogico` (Kotlin, Jetpack Compose, SQLite local).
- Branch funcional atual: `feat/15-planning-polish-interactions`, HEAD `b0597bbbb352d1cc71467e19caa7a96fa50387af`; checkout limpo e sincronizado com `origin` quando verificado.
- PR #16 está **DRAFT**, empilhada sobre `feat/14-navigation-visual-integration` (PR #15). Não houve merge para `main`, release, publicação nem criação de APK para distribuição.
- CI da PR #16 no SHA `b0597bb`: `audit`, `verify` e `persistence` passaram. [Audit](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35490102573), [verify](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35490102555), [persistence](https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35490102562). Isso comprova os checks desse SHA, não aprovação visual em aparelho físico.

## Implementação observada

- Planejamento tem calendário, editor de aula, modelos, atividades, BNCC offline, backup e proteção de rascunho em evolução na cadeia de PRs.
- O primeiro uso atual começa por perfil e encaminha à criação de turma. Não há evidência no código atual de uma conta remota, catálogo de integrações externas ou jornada de assinatura implementados.
- A estrutura de tokens e componentes de identidade visual já existe; a jornada completa de onboarding e os módulos restantes ainda precisam de inventário por fluxo antes de serem chamados de prontos.
- O banco atual é SQLite versão 9. `attendance` mantém apenas marcações P/F por estudante e dia, sem snapshot de participantes/sessão. Alunos novos podem afetar a leitura de pendências antigas; exclusões podem remover marcações históricas por cascata. O fluxo de chamada precisa de correção aditiva com migração e compatibilidade do backup.

## Direção de produto

- Novo mapa de produto: [`PRODUCT_JOURNEY.md`](PRODUCT_JOURNEY.md).
- Diretriz atual registrada: offline/local-first e sem login obrigatório. A criação de conta mencionada no direcionamento mais recente ainda exige conciliação; a proposta de trabalho mantém caminho local sem conta.
- A lista de integrações do MVP não está definida; não presumir Google Calendar, Drive, login Google ou outro fornecedor.
- `BLUEPRINT.md` registra Free com até duas turmas ativas e Pro com recursos adicionais, além de R$ 29,90/mês registrado em 17/09/2026. O direcionamento mais recente pede consolidar oferta e preço; valores, trial, paywall obrigatório e plano anual não devem ser codificados até reconfirmação.

## Próximos blocos

1. Registrar esta jornada e reconciliar a documentação de produto, sem inserir cobrança ou login obrigatório.
2. Implementar sessões de chamada com snapshot de participantes/estados, migração segura e backup compatível; adicionar casos de preservação histórica e falha.
3. Só então implementar onboarding por blocos verticais: primeira abertura, uso local/conta após decisão, personalização, importação/integração opcional, primeira turma, primeira tarefa e Home baseada em dados reais.
4. Continuar Turmas e Planejamento após revisar a cadeia de PRs; executar o restante do ciclo docente conforme inventário real.
5. Implementar Professor Pro e lançamento apenas após decisões comerciais, recursos recorrentes e fluxos reais de cobrança/restauração/cancelamento.

## Gates permanentes

- Não fazer merge em `main`, publicar, distribuir APK/AAB ou ativar monetização sem autorização expressa.
- Usar apenas dados sintéticos em testes, logs, capturas e serviços externos.
- Toda mudança de UI precisa de revisão funcional, acessibilidade e evidência real quando esse for o gate; CI verde sozinho não significa aceite visual ou teste em telefone.
- O aceite estético final continua pertencendo à titular.
