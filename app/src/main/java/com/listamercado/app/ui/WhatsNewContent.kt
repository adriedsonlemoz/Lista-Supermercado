package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 25
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Leitura de código de barras mais estável, segura e clara."

    val changes = listOf(
        Change(
            "Leitura mais confiável",
            "O scanner agora confirma o mesmo código em três leituras consecutivas antes de voltar ao produto, reduzindo reconhecimentos acidentais."
        ),
        Change(
            "Validação de códigos",
            "EAN e UPC passam por validação do dígito verificador antes de serem aceitos quando o formato permite essa conferência."
        ),
        Change(
            "Resultado não se perde",
            "O código lido continua sendo aplicado mesmo se a tela da lista for recriada enquanto a câmera está aberta; quando não há produto automático, o número aparece claramente no editor."
        ),
        Change(
            "Feedback da câmera protegido",
            "A vibração de confirmação agora é tratada como feedback opcional e não interfere na leitura caso o aparelho não consiga executá-la."
        ),
        Change(
            "Busca de produto ampliada",
            "Para códigos novos, a consulta opcional também considera nomes genéricos, marca e quantidade quando o nome principal não estiver disponível."
        )
    )

    data class Change(val title: String, val description: String)
}
