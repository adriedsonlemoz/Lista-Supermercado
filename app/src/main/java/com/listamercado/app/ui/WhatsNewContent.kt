package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 27
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Agora a lista separa o que você planejou do que realmente comprou."

    val changes = listOf(
        Change(
            "Planejado x comprado",
            "Cada item mantém a quantidade planejada e uma quantidade comprada separada, sem perder compatibilidade com listas antigas."
        ),
        Change(
            "Carrinho com valor real",
            "A estimativa continua usando a quantidade planejada, enquanto o carrinho usa apenas a quantidade realmente comprada dos itens concluídos."
        ),
        Change(
            "Modo compra atualizado",
            "No supermercado você pode informar quanto realmente levou e o total pago antes de marcar o produto como comprado."
        ),
        Change(
            "Histórico mais fiel",
            "O histórico passa a mostrar quantidade comprada e total pago, usando somente compras concluídas nos indicadores de preço."
        ),
        Change(
            "Backup schema 5",
            "Backup e CSV agora preservam quantidades planejadas e compradas, mantendo importação de backups antigos."
        )
    )

    data class Change(val title: String, val description: String)
}
