# Estado verificável — 16/09/2026

**Repositório certo:** `SayvaBR/assistente-pedagogico` (não o legado). **PR de implementação:** #2, aberto e DRAFT. **Gate integral de aceite e proibição de entrega antecipada de APK:** issue #8; issue #4 representa prazo-alvo condicional, não garantia de publicação.

## Evidências concluídas
- [x] Projeto nativo Kotlin/Jetpack Compose e SQLite executável; build anterior, lint e testes unitários passaram no GitHub: run 35117001439.
- [x] Cinco testes instrumentados do SQLite passaram em emulador descartável: run 35129412901 (reabertura de conexão, edição, limite e restauração de turmas, integridade de frequência/observações, rollback de chamada e migração v1→v2).
- [x] Handler para Voltar Android/gestos e histórico interno codificado, onboarding com Voltar e confirmação de saída; código compilado no run 35130797904. **Ainda requer teste UI e inspeção gestual física**, portanto bug não encerrado.
- [x] CI alterado para manter builds técnicos efêmeros, mas parar de publicar APKs instaláveis como artefato. Nenhum APK deve ser entregue antes de completar issue #8 e aceite da titular.
- [x] Gerenciamento de turmas/alunos com edição e arquivamento/restauração; migration não destrutiva.
- [x] Primeira fatia de Arquivos introduzida no branch: nova tela de catálogo com pesquisa, ordenação, detalhes, renomeação e remoção **apenas do vínculo local**; nunca apaga o documento original. Novo teste de persistência correspondente criado. Validar CI específico mais recente antes de aceitar.

## Não concluído / bloqueadores do produto
- [ ] Suíte UI automatizada de navegação e tecla Voltar (inclusive teclado, dialogs, telas vazias, restauração de estado); safe areas precisam de confirmação visual em dispositivo.
- [ ] Arquivos **não está completo**: faltam pastas, favoritos, lixeira e restauração, ordenação/filtros avançados, captura e visualização confiável, permission loss e compatibilidade.
- [ ] Planejamento dia/semana/mês, detalhes de plano/momentos/tempos/BNCC validada, CRUD agenda e registros, relatórios/backup/restauração, todas as ações reais.
- [ ] Splash, onboarding pleno, animações, assets/Lucide, fidelidade às referências, acessibilidade e revisão UI/UX integral com aceite.
- [ ] Testes E2E criar→encerrar processo→reabrir por cada jornada, privacidade, Play Billing verdadeiro, AAB assinado e Play Console/política/qualificação.
- [ ] Gradle wrapper versionado e blueprint longo versionado integralmente.

Últimos CI relevantes: Android CI `https://github.com/SayvaBR/assistente-pedagogico/actions/runs/35130797904` passou antes da nova fatia de Arquivos. **Não considerar o código subsequente validado até CI e instrumentados correspondentes concluírem.** Prints anteriores (run 35078346930) são de uma versão antiga com falhas conhecidas; não validam o produto atual. Não disponibilizar APK, release, AAB nem screenshots de rotina; capturas pontuais só em gates de design indispensáveis.
