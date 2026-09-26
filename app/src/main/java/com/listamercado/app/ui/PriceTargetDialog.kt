package com.listamercado.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.util.CurrencyTextWatcher
import com.listamercado.app.util.PriceUnitHelper

object PriceTargetDialog {
    fun show(
        context: Context,
        product: CatalogProduct,
        onSave: (Double?) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_price_target, null)
        val productText = view.findViewById<TextView>(R.id.textPriceTargetProduct)
        val layout = view.findViewById<TextInputLayout>(R.id.layoutCatalogPriceTarget)
        val input = view.findViewById<TextInputEditText>(R.id.inputCatalogPriceTarget)
        val watcher = CurrencyTextWatcher(input)
        input.addTextChangedListener(watcher)

        val unit = product.priceTargetUnit ?: PriceUnitHelper.priceUnit(product.unit)
        productText.text = "${product.name} • preço por $unit"
        layout.hint = "Preço-alvo por $unit"
        watcher.setAmount(product.priceTarget ?: 0.0)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Preço-alvo")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Remover", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onSave(watcher.amount().takeIf { it > 0.0 })
                dialog.dismiss()
            }
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                onSave(null)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
