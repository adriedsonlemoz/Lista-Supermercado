package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 22
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "A tela de adicionar ficou mais legível e o leitor de código está mais automático."

    val changes = listOf(
        Change(
            "Nome do produto mais legível",
            "O campo principal da tela Adicionar item deixou de usar o rótulo flutuante apertado e passou a ter uma entrada mais alta, com placeholder e ajuda visual do catálogo."
        ),
        Change(
            "Feedback durante a leitura",
            "Ao escanear, o editor agora mostra melhor se o código veio do catálogo local, se é novo ou se encontrou dados online para revisão antes de salvar."
        ),
        Change(
            "Preenchimento mais automático",
            "Quando o código não existe no catálogo local, o app pode consultar opcionalmente a internet para tentar preencher nome, categoria e detalhes básicos sem inventar preço."
        ),
        Change(
            "Leitor com luz e leitura mais estável",
            "A câmera ganhou botão de luz, vibração no sucesso e uma seleção mais estável do código central para reduzir leituras erradas."
        ),
        Change(
            "Catálogo offline preservado",
            "Mesmo com a busca opcional online, o catálogo local continua sendo a fonte principal e o produto salvo passa a abrir instantaneamente nas próximas leituras."
        )
    )

    data class Change(val title: String, val description: String)
}
