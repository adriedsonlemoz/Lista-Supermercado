package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 15
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Correção do tema escuro e do build Release."

    val changes = listOf(
        Change(
            "Build Release corrigido",
            "Corrigimos as cores do tema que existiam apenas no modo noturno e faziam o Android Lint bloquear a geração do APK Release."
        ),
        Change(
            "Tema escuro preservado",
            "O grafite azulado inspirado no Vigia IA continua no modo escuro; a correção adiciona apenas valores padrão seguros para outras configurações do Android."
        ),
        Change(
            "Validação mais forte",
            "O projeto agora verifica automaticamente se recursos declarados em values-night também possuem um valor padrão em values, evitando que o mesmo erro volte em versões futuras."
        )
    )

    data class Change(val title: String, val description: String)
}
