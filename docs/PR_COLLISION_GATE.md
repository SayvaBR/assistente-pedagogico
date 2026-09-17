# Detecção preventiva de colisões entre agentes e PRs

## O que roda de verdade
`.github/workflows/agent-quality.yml` testa `tools/pr_overlap.py` com fixtures sintéticas e, em eventos de PR, consulta a API do GitHub em modo **somente leitura**. Compara arquivos alterados por PRs abertos que apontam para a mesma branch base; ignora deliberadamente PRs empilhados cuja base é a branch de outro PR.

- Sem arquivos iguais: check verde; isso **não** prova compatibilidade semântica entre APIs ou qualidade visual.
- Somente documentação igual: aviso no resumo do job para conciliação editorial.
- Mesmo arquivo em `app/`, `tools/`, `.github/workflows/` ou arquivos centrais de build/AGENTS: check vermelho para exigir revisão do diff conjunto. O bloqueio é sobre a **integração**, não sobre a capacidade de cada agente trabalhar isolado.
- API indisponível, resposta incompleta ou páginas acima do limite: falha fechada; não afirmar ausência de conflitos sem consultar a lista completa.

O verificador usa `GITHUB_TOKEN` efêmero com `contents: read` e `pull-requests: read`. Não tem `contents: write`, não recebe chaves de serviços externos, não comenta, não altera branches e não faz merge. O conteúdo dos PRs não é executado por este verificador: ele só inspeciona metadados de arquivos, embora outros jobs tenham suas próprias regras de execução.

## Exemplo do risco conhecido no projeto
Os PRs #9 e #10 partem de `feat/1-android-bootstrap` e editam `TeacherApp.kt` para finalidades diferentes. Antes de combinar, fazer revisão lado a lado, preservar ambos os conjuntos de mudanças, testar Back/frequência e histórico de observações no mesmo commit de integração e confirmar CI+emulador nesse SHA. Não resolver sobrescrevendo um dos arquivos, forçando push ou marcando o conflito como inexistente. O PR #11 é empilhado sobre #10: não é um PR irmão de #9 nem de #10 na detecção automática; essa limitação é intencional e exige revisão humana do diff final.

## Política para novas entregas
A issue define proprietário, paths, pré-condições, entradas inválidas e cenário que preserva os dados. Agente implementador abre branch e PR DRAFT; agente de qualidade analisa o código e os testes no HEAD; agente visual verifica evidência Android real quando necessária. Sem aprovação do titular, não integrar `main`, gerar APK para distribuição, publicar ou ativar monetização. Arquivos pedagógicos reais e credenciais nunca entram no repositório público.

O checker cobre colisões por **caminho**, não conflitos de linhas, migrações silenciosas, equivalência do comportamento ou exploração completa de riscos. Para esses casos continuam obrigatórios revisão técnica e testes.
