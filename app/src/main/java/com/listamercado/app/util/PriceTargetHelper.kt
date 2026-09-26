package com.listamercado.app.util

import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import java.text.NumberFormat
import kotlin.math.abs

object PriceTargetHelper {
    enum class Status { BELOW, WITHIN, ABOVE, NO_PRICE, INCOMPATIBLE_UNIT }

    data class Evaluation(
        val status: Status,
        val target: Double,
        val targetUnit: String,
        val current: Double? = null
    )

    fun evaluate(item: ShoppingItem, product: CatalogProduct?): Evaluation? {
        val target = product?.priceTarget?.takeIf { it > 0.0 } ?: return null
        val targetUnit = product.priceTargetUnit?.takeIf { it.isNotBlank() } ?: return null
        val currentUnit = PriceUnitHelper.normalizedUnit(item)
        if (currentUnit != targetUnit) {
            return Evaluation(Status.INCOMPATIBLE_UNIT, target, targetUnit)
        }
        if (item.unitPrice <= 0.0) {
            return Evaluation(Status.NO_PRICE, target, targetUnit)
        }
        val current = PriceUnitHelper.normalizedPrice(item)
        val difference = current - target
        val status = when {
            abs(difference) < 0.005 -> Status.WITHIN
            difference < 0.0 -> Status.BELOW
            else -> Status.ABOVE
        }
        return Evaluation(status, target, targetUnit, current)
    }

    fun statusText(item: ShoppingItem, product: CatalogProduct?, currency: NumberFormat): String? {
        val evaluation = evaluate(item, product) ?: return null
        val targetText = "${currency.format(evaluation.target)}/${evaluation.targetUnit}"
        return when (evaluation.status) {
            Status.BELOW -> "↓ Abaixo do alvo • alvo $targetText"
            Status.WITHIN -> "✓ Dentro do alvo • $targetText"
            Status.ABOVE -> "↑ Acima do alvo • alvo $targetText"
            Status.NO_PRICE -> "◎ Alvo $targetText • preço atual não informado"
            Status.INCOMPATIBLE_UNIT -> "◎ Alvo $targetText • unidade atual diferente"
        }
    }
}
