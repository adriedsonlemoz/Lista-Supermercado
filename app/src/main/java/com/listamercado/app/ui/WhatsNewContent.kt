package com.listamercado.app.ui

object WhatsNewContent {
    const val CONTENT_VERSION_CODE = 24
    const val TITLE = "O que mudou nesta atualização"
    const val SUBTITLE = "Mercados próximos ficaram mais fáceis de filtrar, favoritar e reutilizar."

    val changes = listOf(
        Change(
            "Raio de busca manual",
            "Agora é possível escolher 5, 10, 20, 30 ou 50 km. Se houver poucas opções, o app continua ampliando o raio automaticamente como fallback."
        ),
        Change(
            "Mercados favoritos",
            "Supermercados podem ser favoritados e passam a aparecer primeiro nos resultados, com destaque também nos marcadores do mapa."
        ),
        Change(
            "Resultados mais completos",
            "A tela mostra nome, distância, endereço quando disponível e a origem do dado, além do mapa e de uma lista de resultados."
        ),
        Change(
            "Lista vinculada ao mercado",
            "Ao criar uma lista a partir de um mercado, a associação é salva. Nas próximas buscas aparece Abrir lista deste mercado em vez de criar outra."
        ),
        Change(
            "Cache e mensagens melhores",
            "Resultados recentes podem ser usados quando a internet ou o Overpass estiverem indisponíveis, sem tratar ausência de mercados como erro de localização."
        )
    )

    data class Change(val title: String, val description: String)
}
