package com.listamercado.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.listamercado.app.R

object ListDialog {
    fun show(context: Context, initialName: String = "", onSave: (String) -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)
        val name = view.findViewById<EditText>(R.id.inputListName)
        name.setText(initialName)
        if (initialName.isNotBlank()) name.setSelection(initialName.length)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (initialName.isBlank()) "Nova lista" else "Renomear lista")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = name.text.toString().trim()
                if (value.isBlank()) {
                    name.error = "Digite um nome para a lista"
                    return@setOnClickListener
                }
                onSave(value)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
