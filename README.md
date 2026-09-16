# Assistente Pedagógico

Projeto **Android nativo, clean-room** para o trabalho cotidiano de professores. Repositório novo: preservar requisitos pedagógicos, mas não copiar implementação, arquitetura ou dependências do aplicativo anterior.

> Estado: M0 em desenvolvimento. Código inicial não é aplicativo completo nem release aprovado. Confira [STATUS](docs/STATUS.md).

## Stack inicial
Kotlin + Jetpack Compose; `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`; AGP 9.4.0 e Gradle 9.6.0 (JDK 17). A primeira entrega usa somente o módulo `:app` e dependências indispensáveis; banco, billing e Rive entram quando houver uso real.

## Validar
Em máquina com JDK 17 e Android SDK API 37, executar `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` ou utilizar a ação de CI. **Ainda não há Gradle Wrapper no bootstrap**: será gerado exclusivamente com o comando oficial `gradle wrapper --gradle-version 9.6.0`, versionado antes da aprovação do M0 e então o CI passará a invocar `./gradlew`.

A automação deve publicar o APK debug como artefato. Antes de aprovar: instalar e abrir o APK em Android, anexar screenshot real e registrar resultados no PR. O sistema não tem conta, internet obrigatória, telemetria ou dados de alunos nesta fase.

## Organização
- [Produto](docs/PRODUCT.md) e [decisão arquitetural](docs/ADR/ADR-001-native-android.md)
- [Status verificável](docs/STATUS.md) e [ferramentas](docs/TOOLS.md)
- `AGENTS.md` e `.agent/skills/`: contrato para agentes de desenvolvimento.
- Issue #1 e PR correspondente: primeira entrega e critérios de aceite.

Não publicar, assinar release, armazenar dados reais de alunos ou iniciar monetização antes de concluir os gates correspondentes.
