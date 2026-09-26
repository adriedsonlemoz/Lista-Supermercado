# Lista de Mercado

Aplicativo Android nativo, leve e offline para organizar compras em listas separadas e acompanhar preços ao longo do tempo.

## Versão

`1.0.9+10`

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


## Melhorias visuais da v1.0.7+8

- Corrigido o problema de conteúdo encostando na barra de notificações e na navegação com tratamento de insets.
- Tela de adicionar/editar item refeita como bottom sheet moderna, com campos mais agradáveis, dropdowns melhores e botões mais claros.
- Caixa de renomear/criar lista refeita no mesmo padrão visual moderno.
- Campos de busca principais migrados para caixas Material 3 mais consistentes.


## Refinamento visual — 1.0.8+9

- Tela inicial redesenhada com card de resumo geral, valor estimado, quantidade de listas, itens e comprados.
- Cards das listas ganharam ícone, data de atualização, total e barra de progresso da compra.
- Cards dos produtos foram reorganizados com preço total, preço unitário, chips de quantidade/categoria e ações mais limpas.
- Estado vazio e busca da tela inicial foram refinados para seguir o mesmo padrão Material 3.


## Lista inicial Cicloviagem — 1.0.9+10

O aplicativo cria uma única vez a lista padrão **Cicloviagem**, com 18 itens iniciais de alimentação para facilitar o preparo de uma viagem de bicicleta. A lista é totalmente editável e pode ser excluída; se for removida pelo usuário, não é recriada automaticamente. Em instalações já existentes, ela é adicionada uma única vez sem alterar as listas atuais.
