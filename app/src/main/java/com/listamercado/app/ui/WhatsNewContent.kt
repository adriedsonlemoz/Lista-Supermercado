package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 26
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Preço-alvo por produto para saber rapidamente se vale a pena comprar."

    val changes = listOf(
        Change(
            "Preço-alvo por produto",
            "Cada produto do catálogo pode ter um preço-alvo opcional, editável tanto no catálogo quanto ao adicionar ou editar um item."
        ),
        Change(
            "Status direto na lista",
            "Produtos com alvo mostram se o preço atual está abaixo, dentro ou acima do valor definido usando texto e símbolo, sem depender apenas de cor."
        ),
        Change(
            "Modo compra mais útil",
            "Ao informar o preço no supermercado, o status do preço-alvo é atualizado imediatamente no próprio card do produto."
        ),
        Change(
            "Histórico comparativo",
            "O histórico passa a destacar preço atual, preço-alvo, último preço anterior, média, menor e maior valor registrados."
        ),
        Change(
            "Backup atualizado",
            "O backup usa schema 4 e preserva preço-alvo e unidade do alvo, mantendo compatibilidade com backups anteriores."
        )
    )

    data class Change(val title: String, val description: String)
}
