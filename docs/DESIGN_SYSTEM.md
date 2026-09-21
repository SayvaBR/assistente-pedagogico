# Assistente Pedagógico — Design System v2

**Direção visual fechada pelas referências da titular. Especificação técnica pronta para implementação; aplicativo ainda não migrado.** Substitui as regras visuais do v0.1.

## Fonte de verdade

As cinco imagens finais são as âncoras: **AP-01 Home; AP-02 conjunto de telas; AP-03 aula e frequência; AP-04 Configurações/Mais; AP-05 apresentação e identidade**. Ver [catálogo](design-system/REFERENCES.md) e [manifesto](design-system/reference-manifest.json). Os arquivos estão em `IMAGENS PARA REF/APROVADAS PRINCIPAIS`, no workspace.

Precedência: instrução atual da titular → âncora da tela → demais referências aprovadas → parâmetros deste documento → código existente. Presets de skills e bibliotecas não substituem esta identidade.

**Não temos mascote.** O livro é símbolo gráfico da marca, mesmo quando a arte de referência contém rosto. Não atribuir personalidade, nome ou animação de personagem ao livro. Menina e menino são os personagens ilustrados do repertório. Não inventar outros personagens ou uma narrativa de mascotes.

BLUEPRINT continua governando funcionalidades, dados e monetização. Login, preços, nomes, métricas e slogans das imagens são exemplos; não autorização de implementação.

## Assinatura visual obrigatória

- Fundo azul muito claro, cartões branco-gelo, navy nos títulos, azul/ciano luminoso em ações e ilustrações.
- Títulos arredondados, espessos e compactos. Corpo de leitura simples, sem transformar cada texto em display.
- Ícones de domínio ilustrados com volume e contorno azul, dentro de pequenos suportes arredondados azul-claros. Setas, busca e navegação usam traço simples e coerente.
- Cartões generosamente arredondados, bordas discretas e sombras frias leves. Gradientes locais são permitidos, como nas referências.
- Menina/menino em avatar e composições ilustradas apropriadas; preservar traço, cabelo, proporções, roupa e luz da família aprovada.
- Home com saudação/avatar, turma, aula em foco, ações úteis e agenda. Configurações com linhas claras; Mais com grade de ferramentas. Conteúdo real e recursos existentes.

Não trocar por dashboard cinza genérico, minimalismo sem ilustração, novo logo abstrato, emoji, vidro ou sombras pretas pesadas. Não copiar molduras de telefone nem status bars desenhadas. Não acrescentar slogans. O visual aprovado não obriga reproduzir a publicidade dos mockups.

## Tokens de cor

Parâmetros normalizados para implementar a direção; não são amostragem exata de pixels. [JSON](design-system/tokens.json).

| Papel | Valor | Uso |
| --- | --- | --- |
| Fundo de Home | #DDF4FF | Azul-claro predominante, AP-01 |
| Fundo interno | #EAF8FF | Listas e formulários, AP-03/04 |
| Superfície | #FFFFFF | Cartões e painéis |
| Texto principal | #102A56 | Títulos e conteúdo |
| Texto secundário | #456285 | Metadados e ajuda |
| Ciano da identidade | #1CB0F6 | Arte e realces |
| Ação / link | #0063D9 | Controles e seleção |
| CTA com texto branco | #0874D1 → #0063D9 | Gradiente vertical acessível, ajuste técnico |
| Borda decorativa | #CDE8F8 | Separação delicada |
| Borda funcional | #6A93B5 | Limites de controles quando necessários |
| Sucesso / atenção / erro | #087B57 / #865500 / #BA3045 | Ícone e texto, sem depender só da cor |

O branco sobre #1CB0F6 tem contraste aproximado de 2,44:1. Por isso, ciano continua na identidade, mas a área sob labels pequenos brancos usa azul mais escuro. Não escurecer toda a arte. Validar contraste dos estados e gradiente reais. Cores auxiliares de categorias permanecem locais. Tema escuro completo não está definido por estas referências.

## Tipografia

Escolha técnica inicial: **Baloo 2** para títulos e **Nunito Sans** para corpo/controles. Não alegar que são as fontes exatas das imagens. Fontes dos projetos com licença OFL: [Baloo 2](https://github.com/EkType/Baloo2) e [Nunito Sans](https://github.com/googlefonts/NunitoSans).

| Papel | Família/peso | Tamanho/entrelinha sp |
| --- | --- | --- |
| Marca de entrada | Baloo 2 / 800 | 32/36 |
| Título de tela | Baloo 2 / 800 | 26/32 |
| Seção | Baloo 2 / 700 | 20/26 |
| Título de cartão | Nunito Sans / 800 | 18/24 |
| Corpo | Nunito Sans / 400 | 16/24 |
| Metadado | Nunito Sans / 400 | 14/20 |
| Botão | Nunito Sans / 800 | 16/20 |
| Label pequeno | Nunito Sans / 700 | 12/16 |

Não reduzir texto para caber; permitir quebras e crescimento com fonte ampliada. Não usar 12sp em parágrafos. Incorporar fontes localmente, com origem, versão e licença; esta entrega não adiciona arquivos de fonte. O título grande da arte AP-05 pertence à apresentação, não a todas as telas operacionais.

## Geometria e efeitos

Espaços: 4/8/12/16/20/24/32/48dp. Margem lateral 20dp em telas compactas, 24dp quando houver espaço. Cartões com padding 16–20dp; seções separadas por 24dp. Raio: campo 12, botão 16, cartão 20, hero 24dp; chips cápsula. Alvo mínimo 48dp, ícone funcional 24dp, suporte de ícone ilustrado 48–56dp.

Sombras frias discretas: aparência alvo y=3, blur=12, navy a 6% em cartões elevados; CTA y=3, blur=8, azul a 12%. São valores de desenho, não equivalência literal de elevation Compose. Não sombrear toda linha/campo. Borda inferior tonal pode aparecer em CTA conforme AP-01, sem obrigar uma faixa de 5dp em todo componente.

Gradientes são locais: CTA, hero e arte. Respeitar os grandes campos claros. Sem blur de fundo, vidro ou decoração gratuita. Usar insets Android reais; não escalar a prancha como se pixels fossem dp.

## Assets e movimento

Manter uma família única de menina/menino e objetos pedagógicos. Procurar originais antes de reconstruir. Prancha raster não é asset pronto: não apresentar recorte com fundo/ruído como logo final. Registrar origem, tamanho, transparência e aprovação de cada reconstrução. Texto funcional é texto nativo, nunca imagem.

Motion proposto: toque 100ms, transição 180ms, entrada 280ms, curva (0.2,0,0,1). As imagens são estáticas; não afirmar animação aprovada. Não antropomorfizar o livro. Respeitar movimento reduzido; sem loop ornamental, som inesperado ou atraso artificial da abertura.

## Implementação e aceite

Ler [componentes](design-system/COMPONENTS.md), [splash](design-system/SPLASH.md) e [contrato Luna](design-system/LUNA_HANDOFF.md).

Base auditada b0597bb: `ApDesignTokens.kt`, `ApTheme.kt` e `ApVisualComponents.kt` implementam a direção anterior parcialmente. A proibição geral de gradientes/sombras e a faixa inferior obrigatória estão revogadas. Estes arquivos Kotlin não foram alterados nesta entrega.

Migrar componentes centrais e uma tela por tarefa. Preservar dados, offline e rotas. Não aproveitar a tarefa para criar login, alterar preço ou mudar navegação. Os cinco destinos canônicos seguem o BLUEPRINT; diferenças de ordem/rotulagem nas imagens não autorizam migração de rotas.

Entregar comparação com a âncora e captura Android real com SHA, cenário e dimensões. Rever 360/412/480dp conforme a área, fonte 1,3x+, TalkBack, foco, teclado e insets. Dados sintéticos. Sem captura: **validação visual pendente**. A aprovação da referência não aprova automaticamente a implementação.
