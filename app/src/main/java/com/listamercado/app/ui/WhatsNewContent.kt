package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 19
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Catálogo local de produtos e leitura de código de barras sem depender de consulta externa."

    val changes = listOf(
        Change(
            "Catálogo offline",
            "Os produtos já cadastrados nas listas passam a formar um catálogo local com nome, categoria, unidade padrão, último preço e código de barras opcional."
        ),
        Change(
            "Sugestões ao adicionar",
            "Ao digitar o nome de um item, o app sugere produtos conhecidos e pode preencher categoria, unidade e último preço já registrado. Nomes equivalentes são consolidados para evitar duplicações no catálogo."
        ),
        Change(
            "Leitor de código de barras",
            "O editor de item ganhou leitura pela câmera. Se o código já estiver no catálogo, os dados são preenchidos; se for novo, ele é associado somente ao produto informado pelo usuário."
        ),
        Change(
            "Sem dados inventados",
            "A leitura funciona localmente com o modelo embarcado no APK e não consulta nomes ou preços na internet. O catálogo pode ser consultado nas Configurações."
        )
    )

    data class Change(val title: String, val description: String)
}
