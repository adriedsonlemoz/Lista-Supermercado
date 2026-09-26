package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 12
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Orçamento, histórico inteligente de preços e um tema escuro mais consistente."

    val changes = listOf(
        Change("Orçamento por lista", "Defina um limite de gastos e acompanhe quanto resta ou quanto a compra passou do orçamento."),
        Change("Histórico inteligente", "Agora cada produto mostra último preço, menor, maior, média e tendência em relação ao registro anterior."),
        Change("Atalho pelo produto", "No menu de cada item, Histórico de preço abre diretamente a análise daquele produto."),
        Change("Nova cor do aplicativo", "O verde foi substituído por violeta/lilás, com texto correto nos botões e contraste melhor no modo escuro.")
    )

    data class Change(val title: String, val description: String)
}
