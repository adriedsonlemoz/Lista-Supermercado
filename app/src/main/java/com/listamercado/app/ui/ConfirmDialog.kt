package com.listamercado.app.ui

import android.content.Context
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ConfirmDialog {
    fun showDestructive(
        context: Context,
        title: String,
        message: String,
        confirmLabel: String,
        onConfirm: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(confirmLabel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorError))
                setOnClickListener {
                    dialog.dismiss()
                    onConfirm()
                }
            }
        }
        dialog.show()
    }
}
