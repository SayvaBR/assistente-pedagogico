# Planejamento — fonte e integridade do catálogo BNCC (18/09/2026)

## Decisão de produto: referência pedagógica, sem auditoria editorial exaustiva como bloqueio

A titular definiu em 18/09/2026 que as habilidades BNCC servem como **norteador do planejamento** e que podemos confiar na fonte de dados adotada para essa finalidade. Portanto, conferir individualmente as 1.719 habilidades restantes **não é critério de conclusão do Planejamento nem bloqueio de lançamento**. A auditoria editorial pode ser feita futuramente, sem comprometer os testes funcionais.

O aplicativo usa atualmente um **snapshot offline de fonte independente**, e não uma API consultada em tempo real. O arquivo `app/src/main/assets/bncc/catalog.json` informa versão `dados-2026.07.1`, revisão `daabd7dd63ae0cac0aa520b6189e79f95c24f583`, licença CC BY 4.0 e atribuição `bncc.dev (mantido pela Profy)`. Declara 1.721 entradas, incluindo 141 de Computação. Contagens, formato, unicidade de códigos e busca são testados; essas verificações técnicas não demonstram conferência editorial de cada texto.

## Requisitos funcionais que permanecem

- Encontrar habilidades por código ou texto e filtrar por etapa e ano de forma coerente.
- Selecionar códigos existentes na fonte, preservar a seleção durante edição/rotação e salvar corretamente no plano e no backup.
- Apresentar ao professor os códigos e descrições fornecidos pelo catálogo, sem inventar transcrições ou declarar homologação/certificação MEC.
- Em caso de catálogo indisponível ou código ausente, apresentar erro claro; não inventar habilidade nem apagar dados anteriores silenciosamente.
- Identificar a fonte e a licença. A consulta aos documentos oficiais é uma opção quando o professor necessita de conferência normativa, não um passo obrigatório de cada planejamento.

## Amostra conferida em 18/09/2026 — não generalizar

| Código | Conferência pontual | Documento |
| --- | --- | --- |
| EF01CI02 | Código e texto do snapshot coincidem visualmente com o documento oficial; o localizador PDF p. 31 é válido e a habilidade também está no quadro da p. 335. Não modificar metadados por suposta proximidade numérica. | [BNCC Educação Infantil e Ensino Fundamental](https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf) |
| EF01CO01 | Código e texto coincidem com o quadro de Computação para o primeiro ano, p. 16. | [Complemento de Computação](https://basenacionalcomum.mec.gov.br/images/historico/anexo_parecer_cneceb_n_2_2022_bncc_computacao.pdf) |

Também existem documentos para [Ensino Médio](https://www.gov.br/mec/pt-br/cne/bncc_ensino_medio.pdf), a [Resolução CNE/CEB nº 1/2022](https://portal.mec.gov.br/docman/outubro-2022-pdf/241671-rceb001-22/file) e o [portal oficial BNCC](https://basenacionalcomum.mec.gov.br/). A fonte secundária do snapshot está em [bncc-dev/bncc-dados](https://github.com/bncc-dev/bncc-dados). Essas duas amostras não autorizam dizer que as 1.721 entradas foram verificadas ou que o catálogo é oficial.

## Trabalho editorial opcional, fora dos gates desta entrega

Para uma eventual homologação editorial do nosso próprio conjunto de dados, seria necessário congelar versão e documentos com hash e data; confrontar código, texto integral, componente, ano, complemento e referência por entrada; registrar divergências e erratas; e obter revisão pedagógica. Esse processo **não integra o escopo nem impede o aceite funcional atual**. Não empregar os termos “homologado pelo MEC”, “fonte oficial” ou “todos os textos verificados” sem evidência específica.

Os testes do seletor, da persistência e do backup seguem obrigatórios e independem da auditoria editorial opcional. Utilizar apenas dados sintéticos nos testes.
