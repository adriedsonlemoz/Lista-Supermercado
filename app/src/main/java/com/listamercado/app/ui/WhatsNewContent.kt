package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 21
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Backup completo e uma interface escura mais azulada e confortável."

    val changes = listOf(
        Change(
            "Backup e restauração",
            "Configurações agora permite exportar todas as listas, itens, orçamento, preços e catálogo em JSON e restaurar pelo seletor de arquivos do Android."
        ),
        Change(
            "Importação segura",
            "O backup é validado antes da importação e mostra um resumo com listas, itens, produtos e registros de preço antes de escolher Mesclar ou Substituir."
        ),
        Change(
            "Exportação CSV",
            "Itens das listas e produtos do catálogo também podem ser exportados em CSV para consulta em planilhas."
        ),
        Change(
            "Tema escuro renovado",
            "O preto fechado foi substituído por superfícies grafite azuladas mais claras, mantendo o violeta como destaque."
        ),
        Change(
            "Adicionar item redesenhado",
            "O editor foi reorganizado em Produto, Compra e Detalhes, com campos preenchidos, menos contornos e leitura de código mais integrada."
        )
    )

    data class Change(val title: String, val description: String)
}
