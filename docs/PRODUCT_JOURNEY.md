# Jornada do produto — Assistente Pedagógico

## Objetivo

Construir uma experiência que cumpra a promessa **“Menos tempo organizando. Mais clareza para ensinar.”** O produto deve entregar utilidade docente real antes de pedir esforço excessivo ou apresentar a assinatura; preservar autonomia, dados e a possibilidade de aprofundamento.

Este documento orienta a sequência de produto e engenharia. Ele não substitui decisões pedagógicas nem autoriza publicação, integração de cobrança ou coleta remota de dados.

## Princípios de experiência

- **Valor antes de cadastro e cobrança:** permitir que o professor compreenda e experimente o núcleo do produto antes de pedir conta ou assinatura.
- **Local-first e autonomia:** as rotinas pedagógicas continuam disponíveis sem rede. O blueprint atual não exige login; uma conta obrigatória é uma mudança de produto que precisa de decisão explícita.
- **Personalização com consequência:** perguntar etapa, perfil ou preferência somente quando a resposta adaptar termos, opções ou a próxima ação.
- **Primeiro resultado real:** confirmar sucesso somente após persistir a turma, chamada ou planejamento.
- **Divulgação progressiva:** deixar tarefas frequentes visíveis; agrupar aprofundamentos sem escondê-los em menus difíceis.
- **Confiança:** explicar o que será salvo, conectado, compartilhado ou cobrado; permitir voltar, pular configurações opcionais e cancelar ações.
- **Psicologia sem coerção:** apoiar autonomia e sensação de competência. Não usar urgência falsa, culpa, sequências punitivas ou obstáculos para escapar da oferta.
- **Privacidade docente e estudantil:** nenhum nome, nota, frequência, observação, plano ou arquivo de aluno em analytics, testes, exemplos ou serviços externos.

## Jornada proposta

1. **Splash:** identidade reconhecível, inicialização breve e recuperação do ponto apropriado; não usar uma tela de espera como anúncio.
2. **Boas-vindas:** explicar em uma frase o trabalho que o app facilita e oferecer um próximo passo claro.
3. **Conta ou uso local:** manter “Continuar sem conta” como caminho funcional. Se houver conta, explicar o benefício concreto e permitir entrar/criar conta sem descartar os dados locais.
4. **Personalização essencial:** coletar etapa de ensino e, se útil, nome profissional opcional. Não exigir escola, cadastro completo ou dados de alunos para entrar.
5. **Integração/importação contextual:** oferecer somente integrações já implementadas e explicar o efeito de cada uma. A pessoa pode pular e continuar; solicitar permissões no momento da tarefa que as necessita.
6. **Primeiro espaço de trabalho:** criar a primeira turma com os campos mínimos válidos; alunos podem ser cadastrados depois. O estado e o avanço do onboarding sobrevivem a interrupção e reinício.
7. **Primeira ação:** escolher entre tarefas disponíveis e relevantes, inicialmente chamada ou planejamento. Não conduzir a um fluxo ainda não implementado.
8. **Primeiro resultado:** mostrar confirmação textual após gravação bem-sucedida e levar a uma Home que reflita os dados reais, com uma próxima ação útil.
9. **Professor Pro:** apresentar valor após o primeiro resultado ou quando a pessoa solicitar um recurso Pro. A versão gratuita precisa continuar útil e a saída deve permanecer visível.
10. **Uso contínuo:** retomar atividades recentes, mostrar ações úteis para o dia e oferecer histórico, arquivos, relatórios e configurações sem transformar a Home em painel de ansiedade.
11. **Gestão da conta e assinatura:** tornar restauração, gerenciamento/cancelamento, exportação/backup e explicação de downgrade fáceis de localizar; preservar os dados após expiração.

### Estados exigidos nos fluxos iniciais

- Primeira instalação, instalação retomada e retorno de usuário.
- Sem rede, integração indisponível, permissão recusada e importação cancelada.
- Dados vazios, salvamento em andamento, sucesso confirmado e falha preservando o que foi digitado.
- Voltar, pular uma etapa, retomar depois, fechar teclado e reiniciar o app.
- Fonte ampliada, leitores de tela, áreas seguras e tamanhos comuns de aparelho.

## Limites e decisões comerciais

### Base registrada no blueprint atual

- Free: até duas turmas ativas, alunos ilimitados nessas turmas, chamada, BNCC, planejamento essencial, arquivos e backup/restauração.
- Pro: mais turmas e recursos avançados de produtividade, relatórios, sequências e reutilização.
- O blueprint registra R$ 29,90 por mês como decisão de 17/09/2026 e não define o anual.

### Decisões a reconfirmar antes de implementar conta ou paywall

O direcionamento mais recente pede consolidar o modelo de cobrança, preço, teste e bloqueio. Portanto, as informações acima são referência existente, não autorização para fixar preço na interface. Confirmar:

1. Conta: somente local, conta opcional para sincronização, ou conta obrigatória? Recomendação de produto: manter acesso local sem cadastro obrigatório.
2. Integrações do MVP: serviço, dado, tarefa docente, comportamento offline, permissões e alternativa manual para cada conexão. Não presumir calendário, Drive, login Google ou outro fornecedor.
3. Free/Pro: limites exatos, benefícios recorrentes já implementados e comportamento ao ultrapassar o limite e ao fazer downgrade.
4. Oferta: hard paywall ou oferta voluntária/contextual? Recomendação de produto: acesso gratuito útil e paywall contextual depois de demonstrar valor.
5. Comercial: preços mensais/anuais, trial, elegibilidade e promoções configuradas na Play. Mostrar preço e termos carregados da oferta da loja; nunca usar preço promocional fictício ou urgência artificial.

Não implementar cobrança até haver produto Pro real, estados de compra/restituição cobertos e fluxo verificável de restauração e cancelamento.

## Ordem de execução

### A. Fechar o contrato do produto

Atualizar o status técnico; manter mapa de jornadas, decisões, escopo de lançamento e matriz de funcionalidades Free/Pro coerentes. UX participa antes de cada módulo. Referências visuais são estudo de padrões, não telas do aplicativo.

### B. Proteger persistência e histórico

Corrigir a representação de sessões de chamada para que a lista de participantes e estados daquele dia não seja recalculada pela turma atual. Preservar histórico em remoção/arquivamento de aluno, tratar chamadas sem marcação sem inventar informação histórica e manter migração/backup compatíveis.

### C. Entregar a primeira experiência

Implementar a jornada acima em blocos verticais: entrada e retomada; escolha de uso local/conta; personalização; primeira turma; primeira ação; confirmação persistida; Home personalizada. Cada bloco deve funcionar por si e permitir pular/retomar onde aplicável.

### D. Completar o ciclo docente

Priorizar chamadas e Turmas; manter o Planejamento estabilizado; concluir atividades, avaliações, registros, arquivos e relatórios conforme inventário do código e especificação pedagógica. Reaproveitar dados entre etapas e não criar um segundo modelo para um modo guiado.

### E. Monetizar e preparar lançamento

Só após definir os limites e benefícios do Pro: integrar cobrança Play, tratar compra pendente/confirmada/renovação/cancelamento/expiração, restaurar acesso e testar downgrade. Depois realizar beta fechado, hardening, acessibilidade, privacidade, suporte e material da loja.

## Critérios de aceite para cada bloco de tela/fluxo

- A tarefa principal é identificável e os dados necessários são validados sem formulários excessivos.
- Estados de vazio, carregamento, sucesso e erro são claros; sucesso depende de persistência real.
- Voltar, cancelar, teclado, retomada e rede ausente não descartam dados inesperadamente.
- A ação funciona com fonte ampliada, TalkBack, áreas de toque adequadas e diferentes tamanhos de tela.
- Persistência, migração, isolamento por turma e falha que preserva dados são cobertos por testes adequados ao risco.
- Evidência visual vem do app executado; aprovação estética final continua sendo da titular.
- Movimento explica uma mudança de estado, respeita redução de movimento e não segura uma ação rápida.

## Medidas de validação

Quando houver autorização e desenho de privacidade para analytics, medir apenas eventos de produto minimizados: conclusão do onboarding, tempo até primeiro resultado, etapa de abandono, descoberta de opção avançada, visualização voluntária da oferta, resultado da compra e retorno de uso. Não enviar conteúdo pedagógico nem identificadores de estudantes. Complementar com sessões de usabilidade com professores; métricas não provam causalidade nem substituem observação.

## Referências para estudo e adaptação

Aplicar padrões somente depois de observar o fluxo completo e o problema resolvido. Registrar padrão, problema que resolve e adaptação própria antes de mudanças grandes; não copiar marca ou telas integralmente.

- [Android — permissões em tempo de execução](https://developer.android.com/training/permissions/requesting): pedir acesso no contexto da tarefa e permitir continuidade quando recusado.
- [Android — acessibilidade em Compose](https://developer.android.com/develop/ui/compose/accessibility): semântica, ações e suporte a tecnologias assistivas.
- [Material Design 3](https://m3.material.io/): padrões de componentes e comportamento, adaptados à identidade aprovada.
- [Laws of UX — artigos](https://lawsofux.com/articles/) e [NNGroup — heurísticas de usabilidade](https://www.nngroup.com/articles/ten-usability-heuristics/): hipóteses para avaliar carga cognitiva, clareza, controle e prevenção de erros.
- [Mobbin](https://mobbin.com/), [Flourish](https://flourish.studio/examples/?Industry=Featured), [UI Topic](https://uitopic.com/category/dashboard), [21st.dev](https://21st.dev/), [CodePen](https://codepen.io/), [Shoogle](https://shoogle.dev/search?tab=explore) e demais referências indicadas pela titular: fontes de pesquisa visual, não aprovação nem especificação pronta.
