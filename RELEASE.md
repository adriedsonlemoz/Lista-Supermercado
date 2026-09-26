# Release — Meu Supermercado 1.0.19+21

## Destaques

- Backup completo em JSON usando o seletor de arquivos do Android.
- Importação validada com resumo e opções **Mesclar** ou **Substituir**.
- Exportação CSV de itens e catálogo para uso em planilhas.
- Tema escuro atualizado para uma base grafite/azulada, evitando preto puro.
- Tela **Adicionar item** redesenhada com hierarquia mais clara e menos contornos.

## Backup

O schema inicial é `1`. O JSON contém listas, itens, orçamento, preços, estado de compra, catálogo e uma visão derivada do histórico de preços. O histórico usado pelo aplicativo continua tendo como fonte as ocorrências dos produtos nas próprias listas, portanto a restauração das listas preserva esse histórico sem manter um segundo banco paralelo.

A importação sempre valida o arquivo antes de gravar dados. **Mesclar** conserva os dados locais e incorpora o backup; **Substituir** troca listas e catálogo após uma confirmação adicional.

## Build

O workflow de produção continua sendo `.github/workflows/android-release.yml`, gera APK Release assinado e publica o `.apk` diretamente na GitHub Release. O APK não faz parte do ZIP do código-fonte.
