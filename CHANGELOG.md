# Changelog

## 1.0.4+5 — 2026-09-25

- Removido completamente o workflow redundante `Android CI`.
- Mantido apenas `Build and Release Android APK`.
- Corrigida a etapa de assinatura que falhava quando `ANDROID_KEYSTORE_BASE64` não existia.
- O workflow agora reconhece secrets `ANDROID_*`, `LISTA_MERCADO_*`, `KEYSTORE_*` e `SIGNING_*`.
- Adicionadas mensagens de erro específicas para identificar exatamente qual secret de assinatura falta.
- Adicionada validação da assinatura do APK antes da publicação.
- Adicionado `SIGNING-SECRETS.txt` com a configuração esperada, sem armazenar a chave privada no código.
- Mantida a regra: APK fora do ZIP e publicação direta do `.apk` na GitHub Release.

## 1.0.3+4 — 2026-09-25

- Corrigida a regra de entrega do GitHub Actions: o workflow agora gera APK **Release assinado** também em `push` para `main` e em execução manual, além de tags.
- O APK é publicado diretamente na GitHub Release como `Meu-Supermercado-v1.0.3.apk`.
- Removida a compilação Debug do workflow de Release.
- `android.yml` ficou restrito à validação de pull requests, evitando build duplicado na `main`.
- Adicionado `scripts/validate_workflow.py` para impedir regressões como ausência de `assembleRelease`, uso de `actions/upload-artifact` ou geração de `source.zip`.
- APK permanece fora do ZIP de código-fonte.

## 1.0.2+3 — 2026-09-25

- Corrigida a compilação Kotlin que falhava com `Unresolved reference 'BuildConfig'` em `MainActivity.kt`.
- Habilitada explicitamente a geração de `BuildConfig` com `android.buildFeatures.buildConfig = true`.
- Reforçado `scripts/validate_source.py` para detectar uso de `BuildConfig` sem a configuração necessária.
- Mantidos os workflows nativos Kotlin/Gradle corrigidos na versão anterior.

## 1.0.1+2 — 2026-09-25

- Corrigido o workflow `android-release.yml`, que tratava incorretamente o projeto Kotlin nativo como Flutter.
- Adicionado `scripts/validate_source.py`, anteriormente chamado pelo CI mas ausente.
- Separado CI de Debug (`android.yml`) do fluxo de Release por tag (`android-release.yml`).
- Release passou a obter a versão dinamicamente de `VERSION`, sem nome de APK fixo em versão antiga.
- Mantida compatibilidade com secrets genéricos `ANDROID_*` e com os secrets legados `LISTA_MERCADO_*`.
- Adicionadas validações para impedir comandos Flutter e APKs dentro do ZIP do código-fonte.

## 1.0.0+1 — 2026-09-25

- Primeira versão funcional do Lista de Mercado.
- Cadastro, edição e exclusão de itens.
- Quantidade, unidade, categoria, preço e observações.
- Busca e filtros por estado da compra.
- Totais estimado e comprado.
- Persistência local offline.
- Estrutura Android nativa Kotlin/XML modular.
- Manifestos de identidade e integração com GitHub Manager.
- Workflow de validação e release preparado para APK assinado.
