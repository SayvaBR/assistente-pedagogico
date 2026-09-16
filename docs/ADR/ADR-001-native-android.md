# ADR-001 — Android nativo e fundação enxuta

Status: adotado para M0 em 2026-09-16; sujeito a verificação pelo CI.

Decisão: Kotlin, Jetpack Compose, um único módulo `:app` em M0, sem copiar código do projeto anterior. Expandir modularização somente por razões observáveis. Offline e preservação de dados têm prioridade.

Compilação: `compileSdk=37`, `targetSdk=36`, `minSdk=26`; AGP `9.4.0`, Gradle `9.6.0`, Compose BOM `2026.08.00`, Activity Compose `1.13.0`, Kotlin/Compose Compiler plugin `2.3.21`, JDK 17. Revalidar no CI.

Motivos: Android-only, APIs nativas, interface Compose, target conforme Play a partir de 31/08/2026; Compose 1.12 requer compile SDK 37. O AGP 9 possui suporte Kotlin embutido, mas Compose Compiler exige seu plugin específico. Dependências Room, Hilt, RevenueCat e Rive serão adicionadas quando houver uso real.

Fontes: https://developer.android.com/build/releases/agp-9-4-0-release-notes ; https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html ; https://developer.android.com/jetpack/androidx/releases/activity ; https://support.google.com/googleplay/android-developer/answer/11926878?hl=en .
