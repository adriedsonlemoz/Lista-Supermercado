# Release 1.0.18.1+20

Correção de build da etapa de catálogo e leitura de código de barras.

- Corrigida a falha `checkReleaseAarMetadata` observada no Build and Release Android APK #13.
- CameraX 1.6.2 foi removido porque exige compileSdk 36 e AGP 8.9.1+, acima do padrão técnico atual.
- CameraX fixado em 1.5.3, mantendo compileSdk/targetSdk 35, AGP 8.7.3, Kotlin 2.0.21 e Java 17.
- Funcionalidades da v1.0.18+19 foram preservadas: catálogo local, sugestões, associação de código e leitura offline pela câmera.
- Validação preventiva adicionada para detectar regressões de compatibilidade da dependência CameraX.
- Workflow principal permanece único, gera APK Release assinado e publica o `.apk` diretamente na GitHub Release.
- APK permanece fora do ZIP do código-fonte e não há geração de `source.zip`.
