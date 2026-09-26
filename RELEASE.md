# Release — Meu Supermercado 1.0.21.1+25

## Destaques

- Scanner de código de barras mais estável, com confirmação em três frames.
- Validação de checksum para EAN/UPC quando aplicável.
- Feedback háptico protegido para não interromper a leitura.
- Código reconhecido fica visível no editor mesmo quando não há nome automático.
- Mensagens explícitas quando o scanner volta sem resultado válido.
- Recuperação do resultado quando a tela da lista é recriada durante o uso da câmera.
- Consulta opcional de produto com mais campos de fallback.

## Compatibilidade

A base técnica permanece em compileSdk 35, targetSdk 35, minSdk 26, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Java/JVM 17, CameraX 1.5.3 e ML Kit embarcado.

## Build

O workflow principal continua sendo `.github/workflows/android-release.yml`, gerando APK Release assinado e publicando o `.apk` diretamente na GitHub Release. O APK permanece fora do ZIP do código-fonte.
