# Assistente Pedagógico — Design System vivo v0.1

**Status:** fundamentos e tema implementados; toda tela continua sujeita a captura real/aceite visual. Fonte de verdade: `app/src/main/java/com/sayvabr/assistentepedagogico/ui/ApDesignTokens.kt`. `ApTheme.kt` consome a paleta, os cantos e a tipografia; `ApVisualComponents.kt` contém componentes compartilhados. Não confundir este documento com aprovação de todas as telas.

## Objetivo e linguagem
Interface Android-only de professor: profissional, alegre, clara e tátil, nunca uma coleção de formulários Material genéricos. Cinco áreas canônicas Hoje/Início, Turmas, Planejamento, Arquivos, Mais. Rotas e ações devem existir de verdade. Textos em português brasileiro, curtos, úteis e respeitosos; nenhum número inventado, slogan técnico ou botão decorativo.

## Tokens vinculados ao código

| Tipo | Token | Valor | Uso |
| --- | --- | --- | --- |
| Cor | `ApPalette.Sky` | `#DDF4FF` | tela/fundo |
| Cor | `ApPalette.Primary` | `#1CB0F6` | CTA e seleção |
| Cor | `ApPalette.Pressed` | `#1899D6` | borda inferior sólida e ícone |
| Cor | `ApPalette.Navy` | `#102A56` | texto de alta prioridade |
| Cor | `ApPalette.White` | `#FFFFFF` | superfície |
| Cor | `ApPalette.LightSurface` | `#EAF8FF` | superfície alternativa |
| Cor | `ApPalette.Outline` | `#CDE8F8` | divisores e bordas |
| Espaço | `ApSpace.Xs/Sm/Md/Base/Lg/Xl/Xxl` | 4/8/12/16/20/24/32 dp | passos semânticos |
| Raio | `ApShapeToken.Small/Medium/Card/Hero/Pill` | 12/16/20/24/50 dp | superfície e estados |
| Dimensão | `ApSizeToken.ButtonDepth` | 5 dp | profundidade por faixa sólida |
| Dimensão | `ApSizeToken.MinTouchTarget` | 48 dp | alvo mínimo de toque |
| Dimensão | `ApSizeToken.StandardIcon` | 24 dp | escala base de ícones |

Não usar gradientes, glassmorphism, blur ou drop shadows. Um elemento clicável destacado possui uma faixa inferior na mesma cor mais escura, nunca uma sombra cinza difusa. Azul e branco são identidade; navy é azul escuro. Estados destrutivos exigem mensagem e confirmação explícita, não dependem só de cor.

## Tipografia
`ApTheme.kt` centraliza Material typography: headline 28sp Black, title large 23sp ExtraBold, title medium 18sp ExtraBold, body large 16sp Medium, body medium 14sp Medium e CTA label 15sp Bold. Evitar fontes finas; não adicionar arquivo de fonte sem licença e teste. Usar estilos semânticos (`MaterialTheme.typography`) ao migrar telas; respeitar fonte ampliada sem cortar texto.

## Componentes reais
- `ApRaisedButton`: CTA com profundidade inferior 5dp, variante secondary e glifo opcional; uma ação que realmente executa e confirma resultado; futuras alterações devem consumir `ApSizeToken.ButtonDepth`.
- `ApCard`: painel branco com borda azul discreta e raio de 20dp; usar em blocos pedagógicos, itens, erros e vazios.
- `ApEyebrow`: rótulo auxiliar uppercase com espaçamento de letra, nunca substituir título.
- `ApGlyph`: glifos vetoriais autorais temporários; **não chamar de Lucide** até importar/validar ícones verdadeiros.
- `FileCatalogScreen`: tela piloto do sistema; importação/abertura/renomeação/remover vínculo SAF são as ações reais existentes. Pastas, favoritos, lixeira/restore e captura continuam em desenvolvimento.

## Padrões de comportamento
- Home: dados reais, prioridade do dia e próximo passo; estado sem turma oferece cadastro verdadeiro.
- Formulário: agrupar campos por momento pedagógico, data/horário coerentes, CTA fixa ou visível com teclado, erros próximos ao campo e confirmação de rascunho antes de trocar rota/data/aba/Back.
- Catálogo: lista navegável por item, busca sem resultados com ação de limpar, estado vazio com ação de importar, confirmação antes de apagar vínculo; jamais prometer excluir o documento original.
- Feedback: loading durante operações, confirmação de sucesso real e erro recuperável; sem bloqueio infinito, double submit ou operação SQL parcialmente aplicada.
- Segurança: não mostrar dados de aluno de outra turma, evitar seleção por nome duplicado, não exibir observações sensíveis em screenshots públicas.

## Evidência e aceite de UI
A cada PR de UI: captura REAL executando no Android com SHA e cenário; revisar 360/412/480 dp conforme superfície alterada, teclado e barras de sistema no Android 16, toque 48dp, fonte 1.3x+, TalkBack, leitura, foco e contraste, rascunho ao navegar, modo offline e retorno após reinício. Comparar antes/depois e registrar bugs corrigidos. Se não houver screenshot, escrever **validação visual pendente**, mesmo se CI/SQLite passar. Arte conceitual não equivale a tela do app.

## Governança
Qualquer mudança de cor, escala, tipografia ou componente começa pelo token e documento e deve migrar o componente compartilhado em vez de duplicar constantes por tela. Revisão do agente `visual-designer`, verificação independente do `quality-guardian` e aprovação final da titular antes de `main`. Ver `docs/AGENT_WORKFLOW.md`.
