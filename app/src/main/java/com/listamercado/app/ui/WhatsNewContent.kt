package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 14
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Preços da Cicloviagem preenchidos e modo escuro mais confortável."

    val changes = listOf(
        Change(
            "Cicloviagem com preços",
            "Os valores que já tínhamos definido para a lista Cicloviagem agora vêm preenchidos automaticamente. Preços que você já alterou manualmente são preservados."
        ),
        Change(
            "Preços por kg e litro",
            "Itens em gramas e mililitros mostram o valor de referência por kg ou litro, deixando a leitura mais natural sem alterar o total da compra."
        ),
        Change(
            "Novo modo escuro",
            "O preto puro foi trocado por um grafite azulado escuro, com superfícies em camadas e melhor contraste, inspirado no visual noturno do Vigia IA."
        )
    )

    data class Change(val title: String, val description: String)
}
