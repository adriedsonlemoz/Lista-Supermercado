package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 20
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Correção de compatibilidade do leitor de código de barras com a base Android do aplicativo."

    val changes = listOf(
        Change(
            "Build corrigido",
            "Corrigida a incompatibilidade que impedia a compilação Release após a inclusão do leitor de código de barras."
        ),
        Change(
            "CameraX compatível",
            "O leitor agora usa uma versão do CameraX compatível com compileSdk 35 e com o Android Gradle Plugin adotado pelo projeto."
        ),
        Change(
            "Leitor preservado",
            "A leitura local de códigos de barras, o catálogo offline e o preenchimento de produtos continuam funcionando sem consulta externa."
        ),
        Change(
            "Proteção contra regressão",
            "A validação do código-fonte agora verifica também a versão do CameraX para evitar repetir a incompatibilidade no workflow."
        )
    )

    data class Change(val title: String, val description: String)
}
