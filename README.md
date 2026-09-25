# Lista de Mercado

Aplicativo Android nativo, leve e offline para organizar compras de supermercado.

## Versão

`1.0.4+5`

## Identidade

- Nome público: Lista de Mercado
- Nome técnico: listamercado
- Application ID: `com.listamercado.app`
- Android mínimo: 8.0 (API 26)
- compileSdk/targetSdk: 35
- Kotlin: 2.0.21
- Java/JVM: 17
- Android Gradle Plugin: 8.7.3
- Interface: XML tradicional; sem Jetpack Compose

## Funcionalidades da primeira versão

- criar, editar e excluir itens;
- quantidade, unidade, categoria, preço e observação;
- categorias de supermercado prontas;
- marcar itens como comprados;
- filtros Todos/Pendentes/Comprados;
- busca por nome ou categoria;
- total estimado da lista e total já colocado no carrinho;
- armazenamento local offline;
- tema claro/escuro pelo sistema;
- confirmação antes de apagar ou limpar compras.

## Build

```bash
gradle :app:assembleDebug
```

Para release assinado, configure as variáveis:

- `LISTA_MERCADO_KEYSTORE_PATH`
- `LISTA_MERCADO_KEYSTORE_PASSWORD`
- `LISTA_MERCADO_KEY_ALIAS`
- `LISTA_MERCADO_KEY_PASSWORD`

O APK de produção deve permanecer fora do ZIP do código-fonte.


## CI / GitHub Actions

O projeto possui dois fluxos nativos Android:

- `android.yml`: valida e compila Debug em `main`, pull requests e execução manual.
- `android-release.yml`: valida o projeto e publica APK Release assinado quando uma tag `v*` é enviada.

Nenhum workflow usa Flutter. A validação `scripts/validate_source.py` interrompe o CI caso comandos Flutter sejam reintroduzidos ou um APK seja incluído dentro do código-fonte.

### BuildConfig

O módulo `app` habilita explicitamente `buildFeatures.buildConfig = true`, usado pela tela Sobre para exibir a versão instalada.


## Regra de build — 1.0.3+4

Ao enviar código para `main`, executar manualmente o workflow ou enviar uma tag `v*`, o workflow
`.github/workflows/android-release.yml`:

1. valida versão, fonte e configuração do próprio workflow;
2. compila `:app:assembleRelease`;
3. exige a chave permanente de assinatura configurada nos Secrets;
4. verifica o APK gerado;
5. renomeia para `Meu-Supermercado-v1.0.3.apk`;
6. publica **diretamente o arquivo `.apk`** na GitHub Release `v1.0.3`.

Não é usado `actions/upload-artifact` e nenhum `source.zip` é gerado pelo workflow.
O ZIP de código-fonte continua separado do APK.


## Workflow único — 1.0.4+5

A partir desta versão existe somente `.github/workflows/android-release.yml`.
O antigo `Android CI` foi removido.

O workflow principal faz validação, compila o APK Release assinado, verifica a assinatura,
renomeia para `Meu-Supermercado-v1.0.4.apk` e publica o arquivo diretamente na GitHub Release.

A etapa de assinatura reconhece quatro convenções de nomes de Secrets; consulte `SIGNING-SECRETS.txt`.
