package com.listamercado.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.util.CurrencyTextWatcher
import com.listamercado.app.util.InsetsHelper

object BudgetDialog {
    fun show(context: Context, currentBudget: Double, onSave: (Double) -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_budget, null)
        val root = view.findViewById<View>(R.id.sheetBudgetRoot)
        val layout = view.findViewById<TextInputLayout>(R.id.layoutBudget)
        val input = view.findViewById<TextInputEditText>(R.id.inputBudget)
        val remove = view.findViewById<MaterialButton>(R.id.buttonRemoveBudget)
        val cancel = view.findViewById<MaterialButton>(R.id.buttonCancelBudget)
        val save = view.findViewById<MaterialButton>(R.id.buttonSaveBudget)
        val watcher = CurrencyTextWatcher(input)
        input.addTextChangedListener(watcher)
        if (currentBudget > 0.0) watcher.setAmount(currentBudget)
        remove.visibility = if (currentBudget > 0.0) View.VISIBLE else View.GONE

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        InsetsHelper.applyBottomSheetInsets(root)

        cancel.setOnClickListener { dialog.dismiss() }
        remove.setOnClickListener {
            onSave(0.0)
            dialog.dismiss()
        }
        save.setOnClickListener {
            layout.error = null
            val amount = watcher.amount()
            if (amount <= 0.0) {
                layout.error = "Informe um valor maior que zero"
                input.requestFocus()
                return@setOnClickListener
            }
            onSave(amount)
            dialog.dismiss()
        }
        dialog.show()
    }
}
