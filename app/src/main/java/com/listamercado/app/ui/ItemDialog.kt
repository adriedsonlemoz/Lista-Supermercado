package com.listamercado.app.ui

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.util.CurrencyTextWatcher
import com.listamercado.app.util.InsetsHelper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ItemDialog {
    private val categories = listOf(
        "Hortifruti", "Açougue", "Padaria", "Laticínios", "Mercearia",
        "Bebidas", "Congelados", "Limpeza", "Higiene", "Pet", "Outros"
    )
    private val units = listOf("un", "kg", "g", "L", "mL", "pct", "cx")

    fun show(context: Context, existing: ShoppingItem? = null, onSave: (ShoppingItem) -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_item, null)
        val sheet = view.findViewById<android.view.View>(R.id.sheetItemRoot)
        val title = view.findViewById<android.widget.TextView>(R.id.textSheetTitle)
        val subtitle = view.findViewById<android.widget.TextView>(R.id.textSheetSubtitle)
        val nameLayout = view.findViewById<TextInputLayout>(R.id.layoutName)
        val quantityLayout = view.findViewById<TextInputLayout>(R.id.layoutQuantity)
        val priceLayout = view.findViewById<TextInputLayout>(R.id.layoutPrice)
        val name = view.findViewById<TextInputEditText>(R.id.inputName)
        val quantity = view.findViewById<TextInputEditText>(R.id.inputQuantity)
        val price = view.findViewById<TextInputEditText>(R.id.inputPrice)
        val note = view.findViewById<TextInputEditText>(R.id.inputNote)
        val category = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerCategory)
        val unit = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerUnit)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonSave = view.findViewById<MaterialButton>(R.id.buttonSave)
        val buttonClose = view.findViewById<android.widget.ImageButton>(R.id.buttonCloseSheet)
        val priceWatcher = CurrencyTextWatcher(price)
        price.addTextChangedListener(priceWatcher)

        title.text = if (existing == null) "Adicionar item" else "Editar item"
        subtitle.text = if (existing == null) {
            "Preencha nome, quantidade, preço e categoria de forma mais rápida."
        } else {
            "Atualize o item desta lista sem perder o histórico de preço."
        }

        category.setSimpleItems(categories.toTypedArray())
        unit.setSimpleItems(units.toTypedArray())
        category.setText(categories.first(), false)
        unit.setText(units.first(), false)

        existing?.let {
            name.setText(it.name)
            quantity.setText(it.quantity.toInput())
            priceWatcher.setAmount(it.unitPrice)
            note.setText(it.note)
            category.setText(it.category, false)
            unit.setText(it.unit, false)
        } ?: run {
            quantity.setText("1")
            quantity.setSelection(quantity.text?.length ?: 0)
        }

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = true
        InsetsHelper.applyBottomSheetInsets(sheet)


        buttonClose.setOnClickListener { dialog.dismiss() }
        buttonCancel.setOnClickListener { dialog.dismiss() }
        buttonSave.setOnClickListener {
            nameLayout.error = null
            quantityLayout.error = null
            priceLayout.error = null

            val itemName = name.text?.toString()?.trim().orEmpty()
            if (itemName.isBlank()) {
                nameLayout.error = "Digite o nome do item"
                name.requestFocus()
                return@setOnClickListener
            }

            val parsedQuantity = quantity.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
            if (parsedQuantity == null || parsedQuantity <= 0.0) {
                quantityLayout.error = "Informe uma quantidade válida"
                quantity.requestFocus()
                return@setOnClickListener
            }

            val unitValue = unit.text?.toString()?.ifBlank { units.first() } ?: units.first()
            val categoryValue = category.text?.toString()?.ifBlank { categories.first() } ?: categories.first()

            val item = (existing?.copy() ?: ShoppingItem(name = itemName)).apply {
                this.name = itemName
                this.quantity = parsedQuantity
                this.unitPrice = priceWatcher.amount().coerceAtLeast(0.0)
                this.category = categoryValue
                this.unit = unitValue
                this.note = note.text?.toString()?.trim().orEmpty()
            }
            onSave(item)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun Double.toInput(): String {
        val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
        return DecimalFormat("0.##", symbols).format(this)
    }
}
