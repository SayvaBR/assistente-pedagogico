# Luna Alto — contrato visual

**Correção expressa da titular: o aplicativo não tem mascote.** O livro é um símbolo gráfico. A menina e o menino das referências são personagens ilustrados; não inventar personalidade, nome ou papel de mascote para o livro.

## Antes de editar

1. Ler `AGENTS.md`, `docs/BLUEPRINT.md`, `docs/STATUS.md` e `docs/DESIGN_SYSTEM.md` desta branch.
2. Abrir visualmente as âncoras AP-01 a AP-05 e a prancha específica da tarefa, identificadas em `REFERENCES.md`. Ler uma descrição não substitui ver a imagem.
3. Consultar HEAD e PRs atuais; informar tela, base, arquivos e componentes compartilhados afetados.
4. Para splash, ler `SPLASH.md`. Localizar assets existentes antes de reconstruir qualquer elemento.

## Durante a implementação

- A referência aprovada governa aparência; BLUEPRINT governa funcionalidades. Implementar somente o escopo solicitado.
- Preservar azul/ciano, fundos muito claros, ilustrações com volume, títulos arredondados e superfícies suaves.
- O livro não é mascote. Preservar o desenho aprovado, inclusive detalhes gráficos existentes, sem atribuir personalidade ou criar gestos/animação de personagem.
- A menina e o menino podem aparecer nas ilustrações previstas. Não acrescentar outros personagens nem colocá-los em toda tela operacional.
- Gradientes locais e sombras discretas são permitidos. A antiga proibição geral e a faixa inferior obrigatória de 5dp foram substituídas.
- Não importar presets genéricos de skills, Material ou outra biblioteca como nova direção visual.
- Não acrescentar slogans, telas extras, métricas fictícias, autenticação ou novas abas copiadas dos mockups.
- Ajustar tokens e componentes compartilhados; preservar acessibilidade, rotas existentes, dados e funcionamento offline.
- Não gerar novas identidades por tentativa. Reconstruções de assets precisam conservar a referência e ser identificadas como pendentes de aceite.
- Trabalhar em branch própria; não sobrescrever trabalho concorrente. Observar as restrições de merge e distribuição de APK em AGENTS.md.

## Entrega verificável

Informar referência usada, arquivos alterados, cenário e limitações. Para UI implementada, comparar captura Android real com a referência em escala equivalente, identificando SHA, dispositivo/emulador e dimensões. Revisar hierarquia, tipografia, proporções, arte, cor, estados e insets.

Não basta escrever “segui o design system”. Sem captura real, registrar **validação visual pendente**. Build verde, imagem gerada e checklist não equivalem a aceite visual da titular. Não chamar protótipo estático de aplicativo implementado.

Se o acervo não estiver acessível, solicitar os arquivos necessários em vez de inventar uma substituição. As decisões técnicas normalizadas do sistema servem para implementação; não são aprovação individual de fontes, medidas ou novos assets.
