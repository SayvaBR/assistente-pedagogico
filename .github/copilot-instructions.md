# Assistente Pedagógico — instruções de trabalho para agentes no GitHub

**Somente `SayvaBR/assistente-pedagogico`.** O antigo `assistente-ped` é outro projeto: nunca consultá-lo ou copiar sua base. Ler `AGENTS.md`, `docs/BLUEPRINT.md`, `docs/STATUS.md`, `docs/DESIGN_SYSTEM.md` e a issue antes de editar. A documentação antiga pode estar desatualizada; confrontar com HEAD e resultados dos workflows.

## Branches e colaboração
- `main` permanece protegida pela decisão da titular; PR #2 é integração DRAFT, Claude trabalha PR #9 (Back, perda de rascunhos, IME) e PR #10 evolui Arquivos, histórico de observações e isolamento. Verificar SHAs e status atuais antes de agir; não assumir que estes números ainda são os mais recentes.
- Criar branch por issue a partir da base explicitamente combinada, informar quais arquivos vai alterar, evitar sobrescrever outro agente e propor integração após revisão dos dois diffs. Nunca executar merge, force push, publicar release, requisitar segredos ou alterar Play/RevenueCat sem aprovação da titular.
- Tarefa precisa ter comportamento testável, cenários de falha e confirmação de persistência; não confundir UI visível com funcionalidade pronta. Botões têm destino real ou não são exibidos.

## Engenharia
Kotlin/Compose, local-first SQLite; dados de cada turma devem ser validados pelo ID do aluno/turma. Testar homônimos, registros arquivados, duplicação, transações/rollback, restart, migrações não destrutivas, documentos SAF sem permissão e Back/aba/data com rascunho. Não introduzir bibliotecas sem necessidade/licença verificadas. Testes destrutivos apenas em emulador descartável, dados sempre sintéticos; sem segredos ou dados pedagógicos nos commits e artefatos.

Executar Gradle equivalente a `gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`; instrumentados via CI Android dedicado. Anotar SHA e links dos checks. Se não rodou, registrar pendência; CI verde não equivale a teste visual.

## UI
Design System: `docs/DESIGN_SYSTEM.md`, tokens em `ApDesignTokens.kt`, tema em `ApTheme.kt`, componentes em `ApVisualComponents.kt`. Azul/branco, profundidade sólida, tipografia forte, sem gradientes, emoji, placeholders ou métricas fictícias. Checar captura real Android, 360/412/480dp, teclado, navegação por gestos, contraste e TalkBack conforme área alterada.

## Entrega
PR em DRAFT com resumo de código real, testes no HEAD, screenshots reais quando houver UI, riscos/limitações, arquivos tocados e conflitos previstos. Não gerar APK só porque o CI compila; a titular suspendeu novos APKs nesta etapa. Agentes são colaboradores supervisionados, não autorizam merge ou acesso a dados por conta própria.
