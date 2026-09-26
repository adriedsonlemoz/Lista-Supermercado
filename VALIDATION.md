# Validação — 1.0.22+26

- [x] versionName `1.0.22`
- [x] versionCode `26`
- [x] VERSION `1.0.22+26`
- [x] app_identity.json e github-manager.json sincronizados
- [x] WhatsNewContent sincronizado com versionCode 26
- [x] Preço-alvo opcional persistido no catálogo com unidade normalizada
- [x] Preço-alvo pode ser definido, editado e removido
- [x] Lista normal mostra abaixo/dentro/acima do alvo com texto + símbolo
- [x] Modo compra atualiza o status do alvo após alteração do preço
- [x] Histórico compara atual, alvo, último anterior e média histórica
- [x] Backup JSON atualizado para schema 4 e compatível com schemas anteriores
- [x] Exportação CSV inclui preço-alvo e unidade
- [x] Funcionalidades anteriores de scanner, backup, recorrentes, modelos e mercados preservadas
- [x] compileSdk 35, targetSdk 35, minSdk 26, AGP 8.7.3, Kotlin 2.0.21 e Java/JVM 17 preservados
- [x] Workflow principal sem actions/upload-artifact e sem source.zip
- [ ] Build Gradle completo depende do GitHub Actions porque este ambiente não possui Android SDK/Gradle configurado.
- [x] 50 XMLs analisados e bem formados
- [x] Smoke test Kotlin do PriceTargetHelper passou para abaixo/dentro/acima e normalização g → kg
