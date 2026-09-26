# Release — Meu Supermercado 1.0.23+27

## Destaques

- Quantidade planejada e quantidade comprada agora são independentes.
- Estimativa usa o planejado; carrinho usa o que foi realmente comprado.
- Modo compra permite informar a quantidade real e o total pago.
- Histórico mostra quantidade comprada e total pago.
- Backup atualizado para schema 5 com migração automática de dados antigos.

## Compatibilidade

Listas de versões anteriores continuam funcionando. Para um item antigo já marcado como comprado, a migração considera a quantidade comprada igual à quantidade planejada, reproduzindo o comportamento que existia antes desta versão. Modelos, duplicações e recorrentes nunca carregam uma quantidade comprada antiga.

## Build

O workflow principal continua sendo `.github/workflows/android-release.yml`, gerando APK Release assinado e publicando o `.apk` diretamente na GitHub Release, sem `actions/upload-artifact` e sem `source.zip`.
