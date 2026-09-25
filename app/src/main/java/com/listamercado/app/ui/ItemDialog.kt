package com.listamercado.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.listamercado.app.R
import com.listamercado.app.model.ShoppingItem

object ItemDialog {
    private val categories = listOf(
        "Hortifruti", "Açougue", "Padaria", "Laticínios", "Mercearia",
        "Bebidas", "Congelados", "Limpeza", "Higiene", "Pet", "Outros"
    )
    private val units = listOf("un", "kg", "g", "L", "mL", "pct", "cx")

    fun show(
        context: Context,
        existing: ShoppingItem? = null,
        onSave: (ShoppingItem) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_item, null)
        val name = view.findViewById<EditText>(R.id.inputName)
        val quantity = view.findViewById<EditText>(R.id.inputQuantity)
        val price = view.findViewById<EditText>(R.id.inputPrice)
        val note = view.findViewById<EditText>(R.id.inputNote)
        val category = view.findViewById<Spinner>(R.id.spinnerCategory)
        val unit = view.findViewById<Spinner>(R.id.spinnerUnit)

        category.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
        unit.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, units)

        existing?.let {
            name.setText(it.name)
            quantity.setText(it.quantity.toInput())
            price.setText(if (it.unitPrice == 0.0) "" else it.unitPrice.toInput())
            note.setText(it.note)
            category.setSelection(categories.indexOf(it.category).coerceAtLeast(0))
            unit.setSelection(units.indexOf(it.unit).coerceAtLeast(0))
        } ?: quantity.setText("1")

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) "Adicionar item" else "Editar item")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val itemName = name.text.toString().trim()
                if (itemName.isBlank()) {
                    name.error = "Digite o nome do item"
                    return@setOnClickListener
                }

                val parsedQuantity = quantity.text.toString().replace(',', '.').toDoubleOrNull() ?: 1.0
                val parsedPrice = price.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                val item = (existing?.copy() ?: ShoppingItem(name = itemName)).apply {
                    this.name = itemName
                    this.quantity = parsedQuantity.coerceAtLeast(0.01)
                    this.unitPrice = parsedPrice.coerceAtLeast(0.0)
                    this.category = category.selectedItem.toString()
                    this.unit = unit.selectedItem.toString()
                    this.note = note.text.toString().trim()
                }
                onSave(item)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun Double.toInput(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
}
