# Changelog

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
