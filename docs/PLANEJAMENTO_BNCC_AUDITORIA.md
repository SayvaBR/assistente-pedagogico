# Planejamento — auditoria de conteúdo BNCC (18/09/2026)

## Estado: NÃO HOMOLOGADO (2 entradas amostradas, não 1.721)

Este registro separa a checagem técnica do catálogo da **conferência editorial de cada habilidade**. O arquivo offline `app/src/main/assets/bncc/catalog.json` informa versão `dados-2026.07.1`, revisão `daabd7dd63ae0cac0aa520b6189e79f95c24f583`, licença CC BY 4.0 e atribuição `bncc.dev (mantido pela Profy)`. Declara 1.721 entradas, incluindo 141 de Computação. Os testes Android checam contagens, códigos únicos, preenchimento e busca; isso NÃO equivale a 1.721 transcrições conferidas.

## Referências oficiais a preservar por versão

- BNCC Educação Infantil e Ensino Fundamental, PDF no domínio oficial da Base: https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf
- BNCC Educação Básica/Ensino Médio, PDF hospedado pelo MEC: https://www.gov.br/mec/pt-br/cne/bncc_ensino_medio.pdf
- Complemento de Computação, anexo ao Parecer CNE/CEB nº 2/2022, PDF oficial: https://basenacionalcomum.mec.gov.br/images/historico/anexo_parecer_cneceb_n_2_2022_bncc_computacao.pdf
- Resolução CNE/CEB nº 1/2022: https://portal.mec.gov.br/docman/outubro-2022-pdf/241671-rceb001-22/file
- Portal oficial: https://basenacionalcomum.mec.gov.br/ ; fonte **secundária** utilizada no catálogo: https://github.com/bncc-dev/bncc-dados.

## Evidência de AMOSTRA em 18/09/2026 — não generalizar

| Código | Conferência do código e texto | Referência verificável | Observação |
| --- | --- | --- | --- |
| EF01CI02 | Texto do snapshot coincide visualmente com o texto oficial, de `Localizar, nomear e representar graficamente` até `explicar suas funções.` | BNCC EI/EF PDF, páginas PDF **31 e 335** (índices de página 30 e 334); a página 31 apresenta um quadro ilustrativo e a 335 reúne habilidades do 1º ano de Ciências. | **Falso alarme encerrado:** localizador `página PDF 31` do snapshot é válido; códigos vizinhos localizados na página 335 também estão corretos. Não alterar metadado por proximidade numérica. Opcional: registrar ambos os locais. |
| EF01CO01 | Código e texto do snapshot correspondem ao quadro oficial sobre organização de objetos. | Anexo de Computação PDF, página PDF **16** (índice de página 15), quadro Computação — 1º ano. | Complemento, não etapa independente. |

As duas comparações são pontuais. Não foram conferidas todas as páginas, variantes de fonte, faixas etárias e vigência; tampouco foi validada a completude normativa do catálogo. Não utilizar o percentual de 2/1.721 para inferir a qualidade dos demais.

## Salvaguardas de produto

- Buscas e seleção BNCC acontecem no dispositivo, sem transmitir o plano ou dados escolares ao provedor independente.
- UI mostra que o conjunto é independente/não homologado e orienta consulta ao documento oficial. Esse aviso, isoladamente, **não substitui uma auditoria antes de disponibilizar o produto**.
- A versão final não pode completar planos automaticamente com habilidades não verificadas, inventar código ou apresentar uma explicação gerada como transcrição normativa. Separar o texto oficial, a interpretação pedagógica e sugestões de aula com origem identificada.
- Falha de consulta/ausência de fonte deve resultar em estado explícito de indisponibilidade, não em conteúdo inventado. Nenhuma alteração do catálogo é liberada apenas por contar 1.721 entradas.

## Bloqueios antes da homologação editorial

1. Congelar uma cópia integral da versão exata do catálogo e dos documentos oficiais (incluindo erratas/revisões), com SHA-256 e data de coleta.
2. Produzir relatório **por entrada** confrontando código, texto integral, etapa, componente, ano/faixa etária, natureza do complemento, fonte e localizador; marcar conferida/divergente/pendente.
3. Conferir PDF e planilhas oficiais com especial atenção a textos repetidos em páginas ilustrativas: o caso EF01CI02 demonstra que páginas distantes não significam erro por si só.
4. Tratar diferenças na origem com rastreabilidade, preservar alterações humanas, revisar vigência e erratas posteriores ao snapshot de terceiros.
5. Revisão pedagógica e aceite da titular. Até a conclusão, não usar no aplicativo ou divulgação `fonte oficial`, `homologado` ou `todos os textos verificados`.

A validação funcional do seletor (busca, filtros, rotação, persistência de códigos) é um gate SEPARADO da auditoria editorial. Somente dados sintéticos nos testes.
