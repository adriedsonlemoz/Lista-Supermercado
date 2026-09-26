package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 11
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Mapa, visual renovado e novidades automáticas a cada versão."

    val changes = listOf(
        Change("Supermercados próximos", "Novo mapa com OpenStreetMap, distância e criação de lista direto pelo estabelecimento."),
        Change("Lista mais limpa", "A tela de produtos foi compactada para mostrar mais itens, com ações menos poluídas e melhor hierarquia visual."),
        Change("Novidades por versão", "Esta tela aparece apenas na primeira abertura após cada atualização e será atualizada nas próximas versões."),
        Change("Cicloviagem preservada", "A lista inicial Cicloviagem continua totalmente editável, com preços, quantidades e itens livres para alteração.")
    )

    data class Change(val title: String, val description: String)
}
