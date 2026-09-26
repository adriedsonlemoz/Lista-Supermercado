package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 16
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Tela inicial mais compacta e com busca sob demanda."

    val changes = listOf(
        Change(
            "Busca no topo",
            "A pesquisa saiu da tela principal e virou um botão de lupa ao lado das Configurações. Toque nele para abrir ou fechar o campo de busca."
        ),
        Change(
            "Resumo mais compacto",
            "Listas, itens e comprados agora aparecem junto de Resumo das compras, liberando espaço e mantendo o valor estimado em destaque."
        ),
        Change(
            "Mais espaço para suas listas",
            "Com a busca recolhida e o resumo menor, mais listas ficam visíveis sem precisar rolar tanto a tela."
        )
    )

    data class Change(val title: String, val description: String)
}
