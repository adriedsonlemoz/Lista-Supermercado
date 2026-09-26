package com.listamercado.app.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.listamercado.app.R
import com.listamercado.app.model.ShoppingItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class ShoppingItemAdapter(
    private val onChecked: (ShoppingItem, Boolean) -> Unit,
    private val onEdit: (ShoppingItem) -> Unit,
    private val onDelete: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<ShoppingItemAdapter.Holder>() {
    private val items = mutableListOf<ShoppingItem>()
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val quantityFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale("pt", "BR")))

    fun submitList(newItems: List<ShoppingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val check: MaterialCheckBox = view.findViewById(R.id.checkPurchased)
        private val name: TextView = view.findViewById(R.id.textName)
        private val unitPrice: TextView = view.findViewById(R.id.textUnitPrice)
        private val totalPrice: TextView = view.findViewById(R.id.textPrice)
        private val quantity: Chip = view.findViewById(R.id.chipQuantity)
        private val category: Chip = view.findViewById(R.id.chipCategory)
        private val note: TextView = view.findViewById(R.id.textNote)
        private val edit: MaterialButton = view.findViewById(R.id.buttonEdit)
        private val delete: ImageButton = view.findViewById(R.id.buttonDelete)

        fun bind(item: ShoppingItem) {
            check.setOnCheckedChangeListener(null)
            check.isChecked = item.purchased
            name.text = item.name
            quantity.text = "${quantityFormat.format(item.quantity)} ${item.unit}"
            category.text = item.category

            if (item.unitPrice > 0.0) {
                totalPrice.text = currency.format(item.subtotal)
                unitPrice.text = "${currency.format(item.unitPrice)} por ${item.unit}"
                unitPrice.visibility = View.VISIBLE
            } else {
                totalPrice.text = "Sem preço"
                unitPrice.visibility = View.GONE
            }

            note.text = item.note
            note.visibility = if (item.note.isBlank()) View.GONE else View.VISIBLE

            name.paintFlags = if (item.purchased) {
                name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            itemView.alpha = if (item.purchased) 0.72f else 1f

            check.setOnCheckedChangeListener { _, checked -> onChecked(item, checked) }
            itemView.setOnClickListener { onEdit(item) }
            edit.setOnClickListener { onEdit(item) }
            delete.setOnClickListener { onDelete(item) }
        }
    }
}
