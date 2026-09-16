# Ferramentas e fluxo

- GitHub e a branch de trabalho são a fonte de verdade; nunca copiar código do antigo `assistente-ped`.
- Android CLI oficial: https://developer.android.com/tools/agents/android-cli . Em ambiente Android, verificar `android help`, inicializar quando apropriado, executar build/run/screen conforme versão instalada.
- CI instala Gradle 9.6.0 por `gradle/actions/setup-gradle`; wrapper oficial deverá ser gerado usando `gradle wrapper --gradle-version 9.6.0` e versionado antes de concluir M0; não copiar JAR de origem desconhecida.
- PR → testes, lint, APK → instalação → screenshot → review → merge. Dados sintéticos somente.
- Materiais consultados: https://developer.android.com/build/releases/agp-9-4-0-release-notes ; https://developer.android.com/develop/ui/compose/bom ; https://support.google.com/googleplay/android-developer/answer/11926878?hl=pt-br .
