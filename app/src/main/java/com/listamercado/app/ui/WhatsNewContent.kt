package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 23
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Favoritos, produtos recorrentes e modelos deixam as próximas compras mais rápidas."

    val changes = listOf(
        Change(
            "Produtos favoritos",
            "O catálogo agora permite marcar produtos como favoritos, mantendo-os em destaque nas sugestões e na organização do catálogo."
        ),
        Change(
            "Produtos recorrentes",
            "Produtos podem ser configurados como semanais, quinzenais ou mensais e adicionados rapidamente a qualquer lista pela nova tela Adicionar recorrentes."
        ),
        Change(
            "Modelos de lista",
            "Nova lista agora pode começar vazia ou a partir de um modelo. Cicloviagem mantém seus itens predefinidos; Compra do mês, Churrasco, Camping e Limpeza começam vazios."
        ),
        Change(
            "Crie seus próprios modelos",
            "Qualquer lista existente pode ser salva como modelo pelo menu de ações e reutilizada em novas compras sem carregar o estado de itens já comprados."
        ),
        Change(
            "Backup ampliado",
            "Favoritos, recorrência e modelos personalizados passam a fazer parte do backup JSON versionado."
        )
    )

    data class Change(val title: String, val description: String)
}
