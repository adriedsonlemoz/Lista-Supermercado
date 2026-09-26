# Release — Meu Supermercado 1.0.22+26

## Destaques

- Preço-alvo opcional por produto.
- Status abaixo/dentro/acima do alvo na lista normal e no Modo compra.
- Edição e remoção do alvo pelo catálogo ou pelo editor do item.
- Histórico compara preço atual, alvo, último registro anterior e média histórica.
- Backup atualizado para schema 4 com preço-alvo e unidade do alvo.

## Comportamento

O preço-alvo pertence ao produto do catálogo, não a uma lista específica. Assim, o mesmo produto usa o mesmo objetivo de preço em listas diferentes sem duplicar dados. O preço atual de cada lista e o histórico continuam independentes.

Quando o preço atual é igual ao alvo na precisão de centavos, o status é **Dentro do alvo**; abaixo ou acima disso, o aplicativo informa explicitamente a direção.

## Build

O workflow principal continua sendo `.github/workflows/android-release.yml`, com APK Release assinado publicado diretamente na GitHub Release, sem `actions/upload-artifact` e sem `source.zip`.
