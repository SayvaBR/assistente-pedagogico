# Workflow de agentes no GitHub — implementação gradual

## O que funciona sem serviço de IA
O workflow `.github/workflows/agent-quality.yml` roda automaticamente a cada PR e push em `main` com `GITHUB_TOKEN` somente leitura; executa testes sintéticos do auditor e `tools/agent_quality.py` para bloquear divergência de tokens, perfis inválidos, permissões de escrita e gatilhos privilegiados. Exibe resultado no resumo do Actions e **não escreve código, comenta automaticamente, faz merge ou usa segredos**. CI Android e instrumentados continuam separadamente; este auditor é complementar e não afirma validar interface ou lógica pedagógica.

## Agentes de código com IA (ativação condicionada)
Perfis reais de agentes customizados para GitHub Copilot estão em `.github/agents/`:
- `android-implementer`: implementa uma issue pequena, testes de persistência e cenário de erro; branch e PR DRAFT próprios.
- `quality-guardian`: revisa outro PR procurando falhas e riscos; código somente em branch de correção se explicitamente autorizado.
- `visual-designer`: aplica Design System e produz evidência de telas Android reais, não conceitos ou checks falsos.

`AGENTS.md` e `.github/copilot-instructions.md` dão regras transversais. **Criar perfis não liga um modelo sozinho**: para executar Copilot cloud agent é necessário recurso habilitado/plano compatível na conta/repositório, perfis disponíveis na branch relevante e atribuir uma issue ao Copilot selecionando o agente (ou iniciar pela aba Agents); não há evidência de sessão Copilot iniciada por este PR. Claude, ChatGPT e Copilot devem usar branches distintas.

## Handoff e gestão
1. Orquestrador revisa issues #8, #3, PRs #2/#9/#10 e último HEAD; escolhe uma unidade de entrega que caiba em um PR, com critérios verificáveis, paths exclusivos e cenário de falha. **Nunca atribuir tarefa idêntica a dois agentes**.
2. Implementador lê documentos e faz alterações apenas na sua branch, adiciona testes unitários/instrumentados, registra o SHA e abre PR DRAFT. Estado documentado: não iniciado, implementado, CI, instrumentados, UI real, aprovado pela titular.
3. Quality guardian lê diff, resultados no SHA, migrações/IDs/dados de outra turma/voltar e rascunhos, aponta problema reproduzível. Não aprova apenas porque CI passou.
4. Visual designer revisa componentes/token e apenas capturas reais e pontuais Android; comparar 360/412/480dp, teclado, status bar, texto grande e TalkBack.
5. Orquestrador revisa possíveis conflitos com os outros PRs, resolve preservando mudanças de ambos, executa checks na integração. Só a titular autoriza merge em `main`/release quando gates cumpridos. Nunca force push sobre trabalho do Claude.

## Matriz preventiva para novas features
Antes de editar, escrever na issue as entradas válidas/inválidas, o efeito de toque repetido, mudança de turma, IDs de aluno homônimo, volta com dados não salvos, data/localidade, arquivo SAF revogado, saída/reabertura, migração e rollback SQL. Para operações destrutivas, confirmar efeito exato; para exclusão de referência, nunca remover arquivo original. Colocar pelo menos um teste de falha que demonstra dados preservados e um teste de sucesso com reabertura.

## Uso de APIs, memória e limites
Não acionar LLM via GitHub Actions automaticamente nem inserir token de OpenAI/Anthropic/Copilot no código público. Seria necessária aprovação de custos, permissões, armazenamento e tratamento de dados. As instruções/estado versionados neste repo são contexto dos agentes, não memória autônoma ilimitada. Somente dados sintéticos em issues, logs, PRs e capturas; nenhum nome, diagnóstico, nota ou arquivo real de aluno em serviço externo. Não usar `pull_request_target` com checkout não confiável, tokens `contents: write`, secrets em PRs ou ações automáticas de merge.

## Prioridade na etapa presente
Claude: PR #9, Back/IME/proteção de rascunho e testes de navegação. ChatGPT: PR #10 Arquivos/Registros e PR deste workflow/Design System. Evitar mudança simultânea em `TeacherApp.kt` sem revisão conjunta. Próximo desenvolvimento funcional: pastas/favoritos/lixeira e restauração de vínculos, com migração não destrutiva e testes de dados. Nenhum novo APK é solicitado nesta etapa; o CI apenas compila. RevenueCat/Google Play são gates posteriores, sem paywall fictício.
