# Lista de Mercado

Aplicativo Android nativo, leve e offline para organizar compras em listas separadas e acompanhar preços ao longo do tempo.

## Versão

`1.0.6+7`

## Identidade técnica

- Nome público: Lista de Mercado
- Nome técnico: listamercado
- Application ID: `com.listamercado.app`
- Android mínimo: 8.0 (API 26)
- compileSdk/targetSdk: 35
- Kotlin: 2.0.21
- Java/JVM: 17
- Android Gradle Plugin: 8.7.3
- Interface: XML tradicional, sem Jetpack Compose

## Funcionalidades atuais

- tela inicial com listas independentes, por exemplo `Supermercado do João`, `Compra do mês` ou `Viagem`;
- criar, renomear, duplicar e excluir listas;
- migração automática dos itens das versões antigas para `Minha lista`;
- itens com quantidade, unidade, categoria, preço e observação;
- marcação de comprado e totais estimado/no carrinho;
- busca por lista, supermercado, produto ou categoria;
- filtros Todos/Pendentes/Comprados adaptados à largura da tela;
- preço com formatação automática em real;
- comparação de duas listas e de preços unitários;
- histórico de um produto em diferentes listas/datas;
- Configurações por engrenagem no topo;
- aparência Sistema/Claro/Escuro;
- Últimas alterações, Sobre e Doação via Pix;
- armazenamento local offline.

## Persistência e histórico

Cada lista preserva a data, os itens e os preços registrados. Duplicar uma lista cria uma nova compra com os mesmos produtos e preços de referência, mas desmarca os itens como comprados. Isso permite atualizar os valores e depois comparar compras diferentes.

## Build e Release

O projeto mantém somente `.github/workflows/android-release.yml` como workflow principal. Ele valida versão e código-fonte, compila `:app:assembleRelease`, usa a chave permanente configurada nos Secrets, verifica a assinatura e publica o APK diretamente na GitHub Release.

O APK de produção fica fora do ZIP do código-fonte e o workflow não usa `actions/upload-artifact` nem gera `source.zip`.

Os nomes de Secrets aceitos estão documentados em `SIGNING-SECRETS.txt`.
