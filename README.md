# Lista de Mercado

Aplicativo Android nativo, leve e offline para organizar compras em listas separadas e acompanhar preços ao longo do tempo.

## Versão

`1.0.19+21`

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
- modo de compra rápida por lista, mostrando apenas pendentes, checkbox grande, preço total direto no item, carrinho/orçamento em tempo real e ação para desfazer;
- catálogo local/offline com sugestões ao adicionar itens, último preço, unidade/categoria padrão e código de barras opcional;
- leitor de código de barras pela câmera, com modelo embarcado e sem consulta externa de nomes ou preços;
- busca por lista, supermercado, produto ou categoria;
- filtros Todos/Pendentes/Comprados adaptados à largura da tela;
- preço com formatação automática em real;
- comparação de duas listas e de preços unitários;
- histórico de um produto em diferentes listas/datas;
- Configurações por engrenagem no topo;
- aparência Sistema/Claro/Escuro;
- Últimas alterações, Sobre e Doação via Pix;
- armazenamento local offline;
- backup completo em JSON com schema versionado, importação validada e opções de mesclar/substituir;
- exportação CSV de itens e catálogo usando o seletor de arquivos do Android.

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


## Mapa e tela de novidades — 1.0.10+11

- Novo **Mercados próximos** com OpenStreetMap e consulta Overpass sem chave de API.
- Usa a localização do Android, mostra a distância e amplia automaticamente a busca de 5 km para 15 km e até 30 km quando necessário.
- Cada marcador permite criar uma lista já nomeada com o supermercado e a data.
- Tela de detalhes da lista foi compactada para mostrar mais produtos, com resumo, busca e filtros mais limpos.
- Cards dos produtos foram redesenhados: tocar no card edita e o menu de três pontos concentra editar/excluir.
- Nova tela **O que mudou nesta atualização**, exibida somente na primeira abertura após cada versão.
- `WhatsNewContent.kt` centraliza as novidades e deve ser atualizado em todas as próximas versões.


## Versão 1.0.11+12

- Orçamento configurável por lista, com restante, excesso e barra de uso.
- Histórico inteligente de preços com último, menor, maior, média e tendência.
- Atalho “Histórico de preço” diretamente no menu de cada produto.
- Paleta principal alterada de verde para violeta/lilás, incluindo mapa, ícone e botões principais.
- Contraste de `colorOnPrimary` definido explicitamente para impedir texto azul sobre botões preenchidos.


## Correção da tela de atualização — 1.0.12+13

A tela de novidades agora é protegida contra abertura duplicada durante recriações de Activity/tema. A confirmação é persistida de forma síncrona ao tocar em **Continuar**, e instâncias restauradas fecham automaticamente se a versão já tiver sido vista.


## v1.0.13+14 — Cicloviagem e tema escuro

A lista inicial Cicloviagem recebe os preços de referência que já tinham sido definidos nas conversas de planejamento. Em instalações existentes, apenas itens ainda sem preço são preenchidos. O tema escuro usa agora um grafite azulado (`#101820`/`#121B24`) em vez de preto puro, mantendo o violeta como destaque.


## Correção de build 1.0.14+15

O Android Lint exige que recursos definidos em `values-night` tenham uma declaração padrão em `values`. A versão anterior introduziu 12 cores exclusivas do tema escuro sem esses defaults. Esta versão adiciona os equivalentes padrão e uma validação automática para impedir regressão.


## Interface da tela inicial — 1.0.15+16

A busca agora é aberta pela lupa no cabeçalho, ao lado das Configurações. O resumo de compras foi compactado e mostra listas, itens e comprados junto ao título, deixando mais espaço para as listas cadastradas.


## Interface e mercados — 1.0.16+17

- Busca dentro da lista movida para uma lupa no cabeçalho, abrindo somente quando necessária.
- Resumo da lista reorganizado para dar o mesmo destaque ao total estimado e ao valor do carrinho, além de mostrar quanto falta ou excedeu do orçamento.
- Menus de três pontos ficam legíveis no tema escuro e as ações de lista aparecem lado a lado em um painel compacto.
- Confirmações destrutivas usam vermelho para destacar exclusão/limpeza.
- A busca de mercados é adaptativa: começa em 5 km, amplia para 15 km e pode chegar a 30 km se houver poucas opções.
- A consulta inclui supermercados e mercados de conveniência cadastrados no OpenStreetMap.

## Modo compra — 1.0.17+18

Cada lista possui agora um **Modo compra** dedicado ao uso dentro do supermercado. A tela trabalha sobre os mesmos itens persistidos da lista normal, mostra apenas produtos pendentes, oferece checkbox grande, quantidade em destaque e entrada rápida do preço total da quantidade comprada. Ao marcar um item, ele sai da lista ativa, o carrinho e o orçamento são recalculados imediatamente e uma ação permite desfazer. Nenhum conjunto paralelo de itens ou histórico é criado.



## Catálogo e código de barras — 1.0.18+19

O aplicativo mantém agora um catálogo local/offline formado pelos produtos já usados nas listas. Nomes equivalentes são consolidados por forma normalizada para evitar duplicações no catálogo. Ao adicionar um item, o campo de nome sugere produtos conhecidos e pode preencher categoria, unidade e último preço registrado.

O editor ganhou leitura de código de barras pela câmera. A leitura usa o modelo embarcado do ML Kit, disponível sem conexão após a instalação: códigos conhecidos preenchem o produto; códigos desconhecidos apenas ficam associados aos dados que o usuário informar, sem consultar serviços externos nem inventar nome ou preço. O catálogo pode ser consultado em **Configurações > Catálogo de produtos**.

## Correção de build — 1.0.18.1+20

- Corrigida a falha `checkReleaseAarMetadata` introduzida pelo CameraX 1.6.2.
- CameraX fixado em `1.5.3`, mantendo compatibilidade com `compileSdk 35`, AGP `8.7.3`, Kotlin `2.0.21` e Java 17.
- O leitor de código de barras e o catálogo offline permanecem inalterados funcionalmente.
- `scripts/validate_source.py` passa a bloquear CameraX 1.6.x enquanto a base técnica permanecer em compileSdk 35 / AGP 8.7.3.


## Backup, restauração e refinamento visual — 1.0.19+21

- Configurações ganhou **Exportar backup**, **Importar backup** e **Exportar CSV**.
- O backup JSON possui schema versionado e inclui listas, itens, preços, orçamento, estado de compra, observações, catálogo e uma visão derivada do histórico de preços.
- A importação valida o arquivo e mostra um resumo antes de permitir **Mesclar** ou **Substituir**.
- Arquivos são escolhidos pelo Storage Access Framework do Android; não existe pasta fixa obrigatória.
- O tema escuro recebeu superfícies mais claramente grafite/azuladas, sem preto puro, mantendo violeta/lilás como destaque.
- A tela **Adicionar item** foi reorganizada em blocos de Produto, Compra e Detalhes, com campos preenchidos, menos bordas e ação de leitura de código integrada.
