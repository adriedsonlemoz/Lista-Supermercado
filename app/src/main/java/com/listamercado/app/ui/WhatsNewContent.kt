package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 17
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Lista mais compacta, ações mais claras e mercados encontrados mais longe."

    val changes = listOf(
        Change(
            "Detalhes da lista remodelados",
            "A busca agora fica na lupa do cabeçalho. O resumo mostra Estimado, Carrinho e quanto falta do orçamento com mais destaque."
        ),
        Change(
            "Ações mais visíveis",
            "Os três pontos agora acompanham a cor do tema. Renomear, Duplicar e Excluir ficam lado a lado em uma caixa mais compacta, com exclusão destacada em vermelho."
        ),
        Change(
            "Confirmações mais claras",
            "Ações destrutivas como excluir lista, excluir item e limpar comprados usam confirmação padronizada com o botão perigoso em vermelho."
        ),
        Change(
            "Mercados em até 30 km",
            "A busca começa perto e amplia automaticamente para 15 km e 30 km quando encontra poucas opções. Mercados de conveniência também entram na consulta."
        )
    )

    data class Change(val title: String, val description: String)
}
