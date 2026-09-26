package com.listamercado.app.ui

import android.view.LayoutInflater
import android.widget.ImageButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.util.InsetsHelper
import androidx.appcompat.app.AppCompatActivity

object ListDialog {
    fun show(
        context: AppCompatActivity,
        initialName: String = "",
        titleOverride: String? = null,
        subtitleOverride: String? = null,
        onSave: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)
        val sheet = view.findViewById<android.view.View>(R.id.sheetListRoot)
        val title = view.findViewById<android.widget.TextView>(R.id.textListSheetTitle)
        val subtitle = view.findViewById<android.widget.TextView>(R.id.textListSheetSubtitle)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.layoutListName)
        val name = view.findViewById<TextInputEditText>(R.id.inputListName)
        val buttonSave = view.findViewById<MaterialButton>(R.id.buttonSaveList)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.buttonCancelList)
        val buttonClose = view.findViewById<ImageButton>(R.id.buttonCloseListSheet)

        title.text = titleOverride ?: if (initialName.isBlank()) "Nova lista" else "Renomear lista"
        subtitle.text = subtitleOverride ?: if (initialName.isBlank()) {
            "Crie uma lista para cada mercado, data de compra ou ocasião."
        } else {
            "Dê um nome mais claro para identificar seu mercado ou compra."
        }

        name.setText(initialName)
        if (initialName.isNotBlank()) name.setSelection(initialName.length)

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        InsetsHelper.applyBottomSheetInsets(sheet)

        buttonClose.setOnClickListener { dialog.dismiss() }
        buttonCancel.setOnClickListener { dialog.dismiss() }
        buttonSave.setOnClickListener {
            inputLayout.error = null
            val value = name.text?.toString()?.trim().orEmpty()
            if (value.isBlank()) {
                inputLayout.error = "Digite um nome para a lista"
                name.requestFocus()
                return@setOnClickListener
            }
            onSave(value)
            dialog.dismiss()
        }
        dialog.show()
    }
}
