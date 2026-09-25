# Release 1.0.4+5

Correção do pipeline do Meu Supermercado.

- Um único workflow: `Build and Release Android APK`.
- `Android CI` removido.
- Geração obrigatória de APK Release assinado.
- Compatibilidade com quatro padrões de nomes de Secrets.
- Verificação de integridade e assinatura antes da publicação.
- Publicação direta de `Meu-Supermercado-v1.0.4.apk`.
- Sem `actions/upload-artifact`.
- Sem `source.zip` gerado pelo workflow.
