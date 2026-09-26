package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 18
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Novo Modo compra para usar a lista com rapidez dentro do supermercado."

    val changes = listOf(
        Change(
            "Modo compra",
            "Cada lista ganhou uma tela própria com foco somente nos itens pendentes, controles maiores e uso simplificado com uma mão."
        ),
        Change(
            "Preço rápido no item",
            "Digite diretamente o valor total da quantidade exibida. O app converte internamente para o preço unitário já usado nas comparações e no histórico."
        ),
        Change(
            "Carrinho e orçamento ao vivo",
            "Ao marcar um produto como comprado, ele sai da lista ativa e os itens restantes, o carrinho e o saldo ou excesso do orçamento são atualizados na hora."
        ),
        Change(
            "Desfazer sem perder dados",
            "Uma ação Desfazer permite reverter a última marcação. O modo usa os mesmos itens da lista original, sem criar cópias ou um segundo histórico."
        )
    )

    data class Change(val title: String, val description: String)
}
