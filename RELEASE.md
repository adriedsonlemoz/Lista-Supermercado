# Release — Meu Supermercado 1.0.21+24

## Destaques

- Raio manual de mercados: **5, 10, 20, 30 e 50 km**.
- Expansão automática preservada como fallback.
- Mercados favoritos ordenados primeiro e destacados no mapa.
- Lista de resultados com nome, distância, endereço e origem.
- Associação entre mercado e lista, permitindo **Abrir lista deste mercado**.
- Cache recente para contingência de internet/Overpass.
- Backup JSON atualizado para schema 3 com os novos dados.

## Mercados próximos

A busca continua usando OpenStreetMap/Overpass. O raio escolhido pelo usuário é o ponto de partida; se houver poucas opções, o aplicativo amplia progressivamente até 50 km. Resultados favoritos aparecem primeiro.

A tela combina mapa e lista para não depender somente dos marcadores. Endereço é exibido apenas quando o OpenStreetMap fornece esse dado. A origem também é indicada e resultados vindos do cache são identificados como cache recente.

Ao criar uma lista para um mercado, o identificador do estabelecimento é persistido na lista. Depois disso, o mesmo resultado oferece abertura direta da lista existente.

## Disponibilidade e cache

Falhas do Overpass e ausência de internet são tratadas separadamente de erros de localização. Quando existe cache recente e compatível com a região/raio, ele pode ser mostrado como fallback. Ausência de mercados cadastrados não é considerada automaticamente uma falha da localização do aparelho.

## Build

O workflow principal continua sendo `.github/workflows/android-release.yml`, produz APK Release assinado e publica o `.apk` diretamente na GitHub Release, sem `actions/upload-artifact` e sem `source.zip`.
