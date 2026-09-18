# Planejamento — auditoria de conteúdo BNCC (18/09/2026)

## Estado: NÃO HOMOLOGADO

Este é um registro de proveniência e riscos, **não uma declaração de conferência oficial**. O catálogo offline `app/src/main/assets/bncc/catalog.json` informa versão `dados-2026.07.1`, revisão `daabd7dd63ae0cac0aa520b6189e79f95c24f583`, licença CC BY 4.0 e atribuição `bncc.dev (mantido pela Profy)`. Inclui 1.721 entradas no total, das quais 141 marcadas como complemento de Computação. A aplicação valida contagem, unicidade dos códigos, preenchimento de texto e origem; isso não prova que cada transcrição esteja correta.

## Referências primárias para conferência

- MEC/CNE — documentos normativos e anexos oficiais por etapa e Computação: https://portal.mec.gov.br/pde-escola/323-secretarias-112877938/orgaos-vinculados-82187207/12992-diretrizes-para-a-educacao-basica
- MEC — histórico da BNCC, incluindo o complemento de Computação (Parecer CNE/CEB 2/2022 e Resolução CNE/CEB 1/2022): https://basenacionalcomum.mec.gov.br/historico/
- Fonte secundária utilizada no snapshot (não é MEC): https://bncc.dev/ — dataset `dados-2026.07.1` e repositório https://github.com/bncc-dev/bncc-dados.

## Conferências concluídas

- A versão, a atribuição e a contagem declaradas pelo arquivo foram localizadas no snapshot do repositório; testes Android validam 1.721 códigos únicos e 141 registros com `complemento=true`.
- A classificação de Computação como **complemento**, em vez de uma quarta etapa da BNCC, é consistente com a lista normativa do MEC/CNE acima.
- Busca e seleção são locais: sem envio de pesquisas, nomes ou planos dos professores para o provedor secundário.
- A UI agora indica expressamente fonte independente, ausência de homologação pelo MEC e necessidade de conferência no documento oficial.

## Bloqueios antes de chamar de conteúdo auditado

1. Exportar a versão exata do snapshot e obter cópias oficiais íntegras dos documentos do MEC/CNE, inclusive os anexos de Computação; armazenar checksums e data de coleta.
2. Confrontar **cada** código, texto, etapa, componente, ano ou faixa etária, e indicação de complemento com a fonte oficial; produzir relatório por entrada com resultado e divergências.
3. Conferir os localizadores de PDF: a presença do campo `localizador_pdf` não prova correspondência. **Amostra a inspecionar:** `EF01CI02` consta com `página PDF 31` no snapshot enquanto códigos vizinhos de Ciências apontam `página PDF 335`. É uma inconsistência aparente de metadados, não prova conclusiva de erro; verificar o documento antes de corrigir.
4. Reavaliar as regras de vigência e eventuais revisões oficiais ocorridas depois da versão `dados-2026.07.1`, sem inferir que o conteúdo de terceiros está atualizado apenas pela data da versão.
5. Resolver divergências com rastreabilidade e testes; obter aceite pedagógico da titular. Até lá, não usar os rótulos “fonte oficial”, “homologado” ou “todos os textos verificados” em comunicação do aplicativo.

O escopo funcional do seletor (salvar códigos, pesquisar e preservar a seleção na rotação) pode ser validado separadamente desta auditoria editorial de 1.721 entradas. Nenhum dado real de estudante deve entrar nos testes.
