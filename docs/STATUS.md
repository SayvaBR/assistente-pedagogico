# Estado verificável — 2026-09-16

- [x] Novo repositório privado `SayvaBR/assistente-pedagogico` confirmado.
- [x] Issue #1 e branch `feat/1-android-bootstrap` criadas.
- [x] Fontes oficiais de SDK 37, target 36, AGP 9.4 / Gradle 9.6 e Compose BOM verificadas.
- [x] Estrutura inicial e testes unitários escritos, sem alegação de execução.
- [ ] CI verde com build/lint/testes.
- [ ] APK debug gerado e instalado no Android.
- [ ] Screenshot real revisada.
- [ ] Gradle Wrapper oficial gerado e versionado.
- [ ] Blueprint longo original versionado integralmente (o arquivo `BLUEPRINT.md` atual é síntese identificada).
- [ ] M0 aprovado; M1 só depois do gate.

O ambiente de geração do código não possui SDK Android/Gradle. Não declarar APK compilado, teste instrumentado ou release concluído sem artefatos verificáveis. Evidência de CI e APK aparecerá no PR #1 quando executada.
