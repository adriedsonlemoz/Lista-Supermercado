# Changelog

## 1.0.21+24 — 2026-09-26

- Adicionada seleção manual do raio de mercados em **5 km, 10 km, 20 km, 30 km e 50 km**.
- Mantida a expansão automática para raios maiores quando a opção escolhida encontra poucos resultados.
- Supermercados agora podem ser **favoritados** e são ordenados antes dos demais resultados.
- A tela Mercados próximos ganhou lista de resultados com nome, distância, endereço quando disponível e origem do dado.
- Marcadores favoritos recebem destaque no mapa e o popup permite favoritar/desfavoritar.
- Listas criadas a partir de um mercado ficam associadas a ele; buscas futuras mostram **Abrir lista deste mercado**.
- Adicionado cache simples de resultados recentes para uso quando internet/Overpass estiverem indisponíveis.
- Mensagens de erro diferenciam falha do Overpass, falta de internet e ausência de resultados, sem atribuir automaticamente o problema à localização.
- Backup atualizado para **schema 3**, preservando associação lista/mercado, mercados favoritos e raio preferido.
- Tela de novidades atualizada exclusivamente com as mudanças desta versão.

## 1.0.20+23 — 2026-09-26

- Adicionado suporte a **produtos favoritos** no catálogo, com favoritos priorizados nas sugestões.
- Adicionada configuração de **produto recorrente** com frequência semanal, quinzenal ou mensal.
- Nova tela **Adicionar recorrentes** dentro das listas para selecionar rapidamente produtos recorrentes sem duplicar itens já presentes.
- Adicionados modelos iniciais **Cicloviagem**, **Compra do mês**, **Churrasco**, **Camping** e **Limpeza**.
- Somente o modelo **Cicloviagem** possui itens predefinidos; os demais começam vazios.
- O botão **Nova lista** agora permite escolher entre lista vazia e criação a partir de modelo.
- Qualquer lista do usuário pode ser transformada em modelo pelo menu de ações.
- Modelos personalizados podem ser excluídos sem afetar listas já criadas.
- Backup JSON atualizado para schema 2, incluindo favoritos, recorrência e modelos personalizados.
- Tela de novidades atualizada exclusivamente com as mudanças desta versão.

## 1.0.19.1+22 — 2026-09-26

- Refinada a tela **Adicionar item**, com foco especial no campo principal **Nome do produto**, que deixou de usar a apresentação comprimida e passou a ter entrada mais alta, placeholder e ajuda visual.
- O editor agora exibe melhor o estado do código de barras: sem código, reconhecido no catálogo local, código novo e identificado online para revisão.
- Quando um código não existe no catálogo local, o app tenta opcionalmente consultar a internet para preencher nome, categoria e detalhes básicos sem inventar preço.
- A leitura continua priorizando o catálogo offline; após salvar, o produto passa a abrir diretamente do banco local nas próximas leituras.
- O leitor de código ganhou botão de luz, vibração ao reconhecer e uma seleção mais estável do código central para reduzir leituras erradas.
- Tela de novidades atualizada para versionCode 22.

## 1.0.19+21 — 2026-09-26

- Implementado backup completo em JSON com `schemaVersion = 1`.
- Exportação inclui listas, itens, quantidades, unidades, preços, orçamento, estado comprado, observações, catálogo e histórico de preços derivado das listas.
- Adicionada importação pelo seletor de arquivos do Android com validação de formato e schema.
- Antes de importar, o app mostra quantidade de listas, itens, produtos do catálogo e registros de preço.
- Adicionadas opções **Mesclar**, **Substituir** e **Cancelar**, com confirmação adicional antes da substituição.
- Adicionada exportação CSV tabular de listas/itens e catálogo.
- Tema escuro atualizado de preto fechado para grafite azulado mais visível.
- Tela **Adicionar item** redesenhada com seções Produto, Compra e Detalhes, superfícies preenchidas e menos contornos.
- Fluxos existentes de catálogo e leitura offline de código de barras preservados.
- Tela de novidades atualizada para versionCode 21.

## 1.0.18.1+20 — 2026-09-26

- Corrigida a falha do workflow em `:app:checkReleaseAarMetadata`.
- A causa era o CameraX `1.6.2`, que exige compileSdk 36 e Android Gradle Plugin 8.9.1 ou superior.
- CameraX (`camera-camera2`, `camera-lifecycle` e `camera-view`) foi fixado em `1.5.3`, compatível com a base compileSdk 35 do projeto.
- Mantidos AGP 8.7.3, Kotlin 2.0.21, Java 17, targetSdk 35 e minSdk 26.
- Leitor de código de barras, catálogo local e ML Kit embarcado foram preservados.
- Adicionada validação preventiva para impedir CameraX 1.6.x sem atualização coordenada da toolchain Android.
- Tela de novidades atualizada para o novo versionCode.

## 1.0.18+19 — 2026-09-26

- Criado catálogo local/offline com produtos já cadastrados nas listas.
- Catálogo guarda nome, categoria, unidade padrão, último preço e código de barras opcional.
- Adicionada deduplicação por nome normalizado, ignorando acentos, diferenças entre maiúsculas/minúsculas e espaços extras.
- Editor de item passou a sugerir produtos conhecidos e preencher automaticamente categoria, unidade e último preço ao selecionar uma sugestão.
- Adicionado leitor de código de barras pela câmera com CameraX e modelo ML Kit embarcado no APK.
- Códigos conhecidos carregam o produto; códigos desconhecidos são associados somente aos dados digitados pelo usuário, sem consulta externa.
- Adicionada tela **Catálogo de produtos** em Configurações para consultar os dados locais.
- Alterações de preço feitas no Modo compra atualizam o último preço do catálogo.
- Tela de novidades atualizada para esta versão.

## 1.0.17+18 — 2026-09-26

- Adicionado botão **Modo compra** dentro de cada lista.
- Criada tela dedicada ao uso no supermercado, exibindo somente itens pendentes.
- Cards do modo de compra usam checkbox ampliado, nome do produto e quantidade/unidade em destaque.
- Adicionado campo de preço rápido diretamente no item; o valor digitado representa o total da quantidade e é convertido para o preço unitário interno já usado pelo app.
- Ao marcar como comprado, o item é ocultado da lista ativa e itens restantes, carrinho e orçamento são atualizados imediatamente.
- Adicionada ação **Desfazer** após a marcação de um item.
- O modo de compra reutiliza os mesmos registros da lista original, sem duplicar itens ou criar armazenamento paralelo.
- Histórico e comparação de preços permanecem compatíveis com o modelo existente.
- Tela de novidades atualizada para esta versão.

## 1.0.16+17 — 2026-09-26

- Três pontos dos cards de listas e produtos agora usam `colorOnSurface`, ficando claros e legíveis no modo escuro.
- Menu de ações da lista redesenhado em bottom sheet compacto, com Renomear, Duplicar e Excluir lado a lado.
- Exclusão de listas/itens e limpeza de comprados passam a usar confirmação padronizada, com ação destrutiva em vermelho.
- Busca da tela de detalhes movida para uma lupa no cabeçalho e recolhida por padrão.
- Resumo da lista remodelado com Estimado, Carrinho e Falta/Excedeu em destaque; valor faltante usa vermelho nos temas claro e escuro.
- Valor do carrinho recebeu o mesmo peso visual do total estimado.
- Busca de mercados agora amplia automaticamente o raio de 5 km para 15 km e 30 km quando encontra poucas opções.
- Consulta de mercados passou a considerar `shop=supermarket` e `shop=convenience` e ganhou endpoint Overpass de contingência.
- HTML do mapa foi limpo para remover script duplicado.
- Tela de novidades atualizada para esta versão.

## 1.0.15+16 — 2026-09-25

- Busca da tela inicial movida para um ícone de lupa ao lado da engrenagem.
- Campo de busca agora abre somente quando solicitado e pode ser fechado novamente pelo mesmo botão ou pelo Voltar.
- Resumo de compras compactado: listas, itens e comprados ficam na linha superior do card.
- Removida a fileira grande de estatísticas na parte inferior do card de resumo.
- Mais espaço vertical disponível para os cards das listas.
- Tela de novidades atualizada para esta versão.

## 1.0.14+15 — 2026-09-25

- Corrigido o erro de `lintVitalRelease` que impedia a geração do APK.
- Adicionadas declarações padrão em `values/colors.xml` para todas as 12 cores definidas em `values-night/colors.xml`.
- Mantido o novo fundo grafite azulado no modo escuro.
- Adicionada validação preventiva para detectar recursos de cor noturnos sem equivalente padrão antes do build.
- Tela de novidades atualizada para esta versão.

## 1.0.13+14 — 2026-09-25

- Preenchidos os preços de referência já definidos para a lista inicial Cicloviagem.
- Migração segura: itens já alterados pelo usuário não têm seus preços sobrescritos.
- Valores de itens em g/mL passam a ser exibidos de forma legível por kg/L, mantendo os subtotais corretos.
- Modo escuro deixou de usar preto puro e passou para grafite azulado, com superfícies em camadas.
- Paleta violeta ajustada para melhor contraste sobre o novo fundo escuro.
- Tela de novidades atualizada para a versão atual.

## 1.0.12+13 — 2026-09-25

- Corrigida a tela de novidades que podia aparecer mais de uma vez na mesma versão.
- A tela principal só dispara novidades em uma criação nova, evitando duplicação por recriação de Activity.
- `WhatsNewActivity` agora usa `singleTop` e flags de navegação para impedir instâncias duplicadas.
- A versão é marcada como vista somente ao tocar em **Continuar**.
- A gravação da confirmação usa `commit()` para garantir persistência antes de fechar a tela.
- Uma instância restaurada fecha automaticamente caso a versão já tenha sido confirmada.

## 1.0.11+12 — 2026-09-25

- Adicionado orçamento por lista, com valor restante ou excedido e progresso visual.
- Adicionado histórico inteligente de preços por produto: último, menor, maior, média e tendência.
- Adicionado acesso ao histórico pelo menu de cada produto.
- Corrigida a combinação visual verde + texto azul no tema escuro.
- Nova paleta violeta/lilás aplicada ao aplicativo, mapa e ícone.
- Botões preenchidos agora usam explicitamente cor principal + `colorOnPrimary`.
- Tela de novidades atualizada para esta versão.

## 1.0.10+11 — 2026-09-25

- Adicionado mapa de supermercados próximos com OpenStreetMap, localização do aparelho e consulta Overpass sem chave de API.
- Adicionada distância até cada supermercado e ação para criar uma nova lista diretamente pelo marcador.
- Reformulada a tela de detalhes da lista para ficar mais compacta e visualmente limpa.
- Cards de produtos compactados; edição por toque e ações agrupadas em menu de três pontos.
- Adicionada tela de novidades pós-atualização, exibida apenas uma vez por `versionCode`.
- `Últimas alterações` nas Configurações passa a usar a mesma fonte da tela de novidades.
- Adicionadas permissões de Internet e localização necessárias ao mapa.

## 1.0.9+10 — 2026-09-25

- Adicionada a lista inicial pronta **Cicloviagem** com 18 produtos e quantidades predefinidas.
- A sugestão é criada uma única vez e continua totalmente editável como qualquer outra lista.
- Usuários existentes recebem a sugestão sem perder ou alterar listas já cadastradas.
- Se a lista Cicloviagem for excluída, o aplicativo não a recria.
- Adicionada unidade `lata` às opções do editor de produtos.

## 1.0.8+9 — 2026-09-25

- Tela inicial redesenhada com resumo visual das compras.
- Adicionado total estimado geral, número de listas, itens e produtos comprados.
- Cards de listas modernizados com ícone, data de atualização e progresso visual.
- Cards de produtos modernizados com preço unitário, subtotal, chips e ações reorganizadas.
- Busca e estado vazio da tela inicial refinados.
- Mantido o comportamento de listas, comparação, histórico, configurações e modo escuro.

## 1.0.7+8 — 2026-09-25

- Reformulada a experiência visual das caixas de adicionar e renomear em bottom sheets modernas.
- Melhorado o formulário de item com campos mais arredondados, dropdown de categoria/unidade e ações mais claras.
- Corrigido o layout para respeitar barra de status, teclado e barra de navegação.
- Melhoradas as caixas de busca principais com visual Material 3 mais consistente.
- Mantida a estrutura por listas, comparação e histórico de preços.

## 1.0.6+7 — 2026-09-25

- Nova tela inicial baseada em listas de compras, em vez de itens soltos.
- Migração automática dos dados antigos para uma lista preservada.
- Suporte a criar, renomear, duplicar e excluir listas.
- Nova comparação entre listas e histórico de preço por produto.
- Nova área Configurações com modo Sistema/Claro/Escuro, Últimas alterações, Doação Pix e Sobre.
- Preço unitário passa a usar formatação automática em reais durante a digitação.
- Filtros da lista reorganizados para caberem em telas menores.
- `Limpar comprados` movido para ação separada dos filtros.
- Interface de criação/edição de itens modernizada com campos Material 3.

## 1.0.5+6 — 2026-09-25

- Corrigido o build que falhava ao detectar `.github/workflows/android.yml` ainda presente no repositório.
- O workflow principal agora remove automaticamente o workflow legado da branch `main`.
- A limpeza é registrada em commit com `[skip ci]`, evitando uma execução em cascata.
- Mantido somente `android-release.yml` no ZIP entregue.
- Mantida geração de APK Release assinado e publicação direta na GitHub Release.

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
