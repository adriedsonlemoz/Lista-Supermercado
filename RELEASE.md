# Release — Meu Supermercado 1.0.19.1+22

## Destaques

- Tela **Adicionar item** refinada, com campo **Nome do produto** mais legível e sem aparência de texto cortado.
- Fluxo de código de barras com feedback melhor no editor.
- Consulta online opcional para códigos ainda desconhecidos, sem inventar preço.
- Leitor com botão de luz, vibração ao sucesso e seleção mais estável do código central.

## Leitura de código

O catálogo local/offline continua sendo a fonte principal. Quando um código já foi salvo antes, o produto preenche instantaneamente sem internet.

Se o código ainda não estiver no catálogo local e houver conexão, o aplicativo pode tentar consultar o **Open Food Facts** para sugerir nome, categoria e detalhes básicos do produto. Esses dados continuam editáveis pelo usuário antes de salvar. O preço não é buscado online; o app preserva apenas o último preço pago pelo próprio usuário.

## Build

O workflow de produção continua sendo `.github/workflows/android-release.yml`, gera APK Release assinado e publica o `.apk` diretamente na GitHub Release. O APK não faz parte do ZIP do código-fonte.
