# Assistente Pedagógico — diretrizes clean-room (síntese operacional)

**Aviso de integridade:** este arquivo é uma síntese do blueprint detalhado original fornecido pelo proprietário; não é transcrição integral. O documento completo deverá ser versionado em PR próprio. Na dúvida, não inventar uma regra pedagógica; criar issue e registrar a decisão.

## Missão
Android-only, local-first, privacy-first, sem obrigação de login nem rede para as rotinas pedagógicas. Professor registra cada informação uma vez, reaproveitando-a ao longo do ciclo: planejamento → aula → frequência → atividade → avaliação → desempenho → registros → relatórios → reflexão/próximo planejamento. Não criar ERP, formulários burocráticos ou chatbot como estrutura do produto.

## Navegação e domínio
Cinco áreas: Hoje, Turmas, Planejamento, Arquivos, Mais. Três etapas visíveis: Educação Infantil, Ensino Fundamental e Ensino Médio; 1º–9º ano é atributo de turma. Centralizar diferenças em capabilities e políticas de avaliação configuráveis; não usar valores fixos de média, recuperação, pesos ou períodos. Cadastro de aluno: nome e turma obrigatórios, fotos, contatos e adaptações opcionais; dados de apoio jamais expostos como badges públicos.

## Fluxos essenciais
Onboarding guiado sem conta: boas-vindas, perfil, etapa, primeira turma, privacidade e Home personalizada. Turmas/alunos; chamada em lote com alteração de exceções, histórico e autosave; planos de aula em blocos (identificação, pedagógico, metodologia, momentos com duração, avaliação, adaptações e pós-aula); BNCC oficial versionada e pesquisável offline incluindo Computação, sem gerar códigos por IA; atividades por aluno; avaliação por nota, conceito ou qualitativa com fórmulas transparentes; biblioteca pedagógica e relatórios PDF/CSV profissionais sem publicidade no documento.

## Dados
Room para dados estruturados, DataStore para preferências, Storage Access Framework/armazenamento privado para arquivos. UUID em entidades. Datas pedagógicas locais separadas de instantes. Respeitar relações entre turma/aluno e garantir que não vazem entre contextos. Backup `.apbackup` versionado, criptografado quando transportável, com verificação e restauração atômica testada; Free e Pro mantêm backup/restauração.

## Privacidade
Sem telemetria remota na primeira versão por padrão. Não enviar nomes, notas, frequência, observações, arquivos, contatos ou diagnósticos a terceiros. Usar dados sintéticos em testes e prints. Permissões mínimas, logs saneados, credenciais e assinatura fora do Git. Revisar LGPD e políticas da Play antes do release.

## Monetização
Sem anúncios. Gratuito: até 2 turmas ativas, alunos ilimitados, chamada, BNCC, planejamento essencial, arquivos e backup; Pro: turmas ilimitadas, relatórios, sequências e reutilização avançada. **Preço mensal definido pela titular em 17/09/2026: R$ 29,90. Preço anual ainda não decidido:** não presumir o antigo preço mensal de R$ 19,90 nem o antigo anual de R$ 149,90. Os valores comerciais mostrados na interface deverão vir da loja em tempo de execução, jamais de textos fixos de preço; habilitar pagamento apenas após integração real de cobrança. Compra, restauração e downgrade devem ser testados; expiração jamais apaga nem oculta os dados criados.

## Visual
Identidade visual definida em `docs/DESIGN_SYSTEM.md` v2, ancorada nas cinco imagens AP-01 a AP-05: azul/ciano, fundos claros, títulos arredondados, ilustrações de menina/menino, ícones de domínio com volume e cartões suaves. Não há mascote; livro é símbolo gráfico. Gradientes locais e sombras frias discretas são permitidos. Preservar acessibilidade, contraste, fonte ampliada e movimento reduzido. Código atual ainda precisa ser migrado; aceite visual exige evidência Android real.

## Engenharia e publicação
Kotlin + Compose; arquitetura UI → ViewModel → domínio → repositório → fonte local/plataforma, fluxo unidirecional. Em M0 usar módulo `app` único, evoluir por necessidade. Issues/branches/PRs pequenos, testes unitários, instrumentados e E2E conforme escopo, CI e screenshots. Gates: M0 bootstrap; M1 persistência; M2 Design System/onboarding; M3 turmas/alunos; M4 chamada/registros; M5 planejamento/BNCC; M6 atividades/avaliações; M7 arquivos/relatórios; M8 Pro; M9 hardening; M10 Play. Zero P0 conhecido no release. Não declarar entrega pronta sem evidência.
