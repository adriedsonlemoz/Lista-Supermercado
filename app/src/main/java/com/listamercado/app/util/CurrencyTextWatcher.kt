package com.listamercado.app.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

class CurrencyTextWatcher(private val input: EditText) : TextWatcher {
    private var updating = false
    private val formatter = NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(editable: Editable?) {
        if (updating) return
        val digits = editable?.toString()?.filter(Char::isDigit).orEmpty()
        if (digits.isEmpty()) return
        val value = digits.toLongOrNull()?.div(100.0) ?: 0.0
        val formatted = formatter.format(value)
        updating = true
        input.setText(formatted)
        input.setSelection(formatted.length)
        updating = false
    }

    fun setAmount(value: Double) {
        if (value <= 0.0) {
            input.setText("")
            return
        }
        val formatted = formatter.format(value)
        updating = true
        input.setText(formatted)
        input.setSelection(formatted.length)
        updating = false
    }

    fun amount(): Double {
        val digits = input.text?.toString()?.filter(Char::isDigit).orEmpty()
        return digits.toLongOrNull()?.div(100.0) ?: 0.0
    }
}
