package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.listamercado.app.R
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.Recurrence

class RecurringProductAdapter : RecyclerView.Adapter<RecurringProductAdapter.Holder>() {
    private val items = mutableListOf<CatalogProduct>()
    private val selectedIds = linkedSetOf<Long>()

    fun submitList(products: List<CatalogProduct>) {
        items.clear()
        items.addAll(products)
        selectedIds.retainAll(products.map { it.id }.toSet())
        notifyDataSetChanged()
    }

    fun selectedProducts(): List<CatalogProduct> = items.filter { it.id in selectedIds }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recurring_product, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val check: CheckBox = view.findViewById(R.id.checkRecurringProduct)
        private val name: TextView = view.findViewById(R.id.textRecurringName)
        private val meta: TextView = view.findViewById(R.id.textRecurringMeta)

        fun bind(product: CatalogProduct) {
            check.setOnCheckedChangeListener(null)
            check.isChecked = product.id in selectedIds
            name.text = if (product.favorite) "★ ${product.name}" else product.name
            val frequency = Recurrence.label(product.recurringFrequency) ?: "Sem frequência"
            meta.text = "$frequency • ${formatQuantity(product.lastQuantity)} ${product.unit}"
            check.setOnCheckedChangeListener { _, checked ->
                if (checked) selectedIds += product.id else selectedIds -= product.id
            }
            itemView.setOnClickListener { check.isChecked = !check.isChecked }
        }
    }

    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale("pt", "BR"), "%.2f", value).trimEnd('0').trimEnd(',')
}
