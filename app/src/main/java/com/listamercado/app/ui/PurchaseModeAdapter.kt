package com.listamercado.app.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.util.PriceTargetHelper
import com.listamercado.app.util.PriceUnitHelper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class PurchaseModeAdapter(
    private val onPurchased: (ShoppingItem) -> Unit,
    private val onPriceChanged: (ShoppingItem, Double) -> Unit,
    private val onPurchasedQuantityChanged: (ShoppingItem, Double) -> Unit
) : RecyclerView.Adapter<PurchaseModeAdapter.Holder>() {
    private val items = mutableListOf<ShoppingItem>()
    private var catalogByName: Map<String, CatalogProduct> = emptyMap()
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val quantityFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale("pt", "BR")))
    private val priceFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("pt", "BR")))

    fun submitList(newItems: List<ShoppingItem>, catalogProducts: List<CatalogProduct> = emptyList()) {
        items.clear()
        items.addAll(newItems)
        catalogByName = catalogProducts.associateBy { it.normalizedName }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_purchase_mode, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val check: MaterialCheckBox = view.findViewById(R.id.checkPurchaseMode)
        private val name: TextView = view.findViewById(R.id.textPurchaseItemName)
        private val quantity: TextView = view.findViewById(R.id.textPurchaseItemQuantity)
        private val category: TextView = view.findViewById(R.id.textPurchaseItemCategory)
        private val purchasedQuantityLayout: TextInputLayout = view.findViewById(R.id.layoutPurchasedQuantity)
        private val purchasedQuantity: TextInputEditText = view.findViewById(R.id.inputPurchasedQuantity)
        private val priceLayout: TextInputLayout = view.findViewById(R.id.layoutQuickPrice)
        private val price: TextInputEditText = view.findViewById(R.id.inputQuickPrice)
        private val unitPrice: TextView = view.findViewById(R.id.textPurchaseUnitPrice)
        private val targetStatus: TextView = view.findViewById(R.id.textPurchaseTargetStatus)
        private var binding = false
        private var boundItem: ShoppingItem? = null

        private val quantityWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (binding) return
                val item = boundItem ?: return
                val value = parseNumber(s?.toString()).coerceAtLeast(0.0)
                onPurchasedQuantityChanged(item, value)
                if (item.unitPrice > 0.0 && value > 0.0) {
                    binding = true
                    price.setText(priceFormat.format(item.unitPrice * value))
                    price.setSelection(price.text?.length ?: 0)
                    binding = false
                }
                updatePriceSummary(item)
            }
        }

        private val priceWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (binding) return
                val item = boundItem ?: return
                val total = parseNumber(s?.toString()).coerceAtLeast(0.0)
                onPriceChanged(item, total)
                updatePriceSummary(item)
            }
        }

        init {
            purchasedQuantity.addTextChangedListener(quantityWatcher)
            price.addTextChangedListener(priceWatcher)
        }

        fun bind(item: ShoppingItem) {
            boundItem = item
            check.setOnCheckedChangeListener(null)
            check.isChecked = false
            name.text = item.name
            quantity.text = "Planejado: ${quantityFormat.format(item.quantity)} ${item.unit}"
            category.text = item.category
            purchasedQuantityLayout.suffixText = item.unit
            priceLayout.hint = "Total pago"

            val actualQuantity = item.purchasedQuantity.takeIf { it > 0.0 } ?: item.quantity
            binding = true
            purchasedQuantity.setText(quantityFormat.format(actualQuantity))
            purchasedQuantity.setSelection(purchasedQuantity.text?.length ?: 0)
            price.setText(if (item.unitPrice > 0.0) priceFormat.format(item.unitPrice * actualQuantity) else "")
            price.setSelection(price.text?.length ?: 0)
            binding = false
            updatePriceSummary(item, actualQuantity)

            check.setOnCheckedChangeListener { _, checked ->
                if (checked) onPurchased(item)
            }
            itemView.setOnClickListener {
                purchasedQuantity.requestFocus()
                purchasedQuantity.setSelection(purchasedQuantity.text?.length ?: 0)
            }
        }

        private fun updatePriceSummary(item: ShoppingItem, overrideQuantity: Double? = null) {
            val actualQuantity = overrideQuantity
                ?: item.purchasedQuantity.takeIf { it > 0.0 }
                ?: item.quantity
            unitPrice.text = if (item.unitPrice > 0.0) {
                "${PriceUnitHelper.formattedUnitPrice(item, currency)} • total ${currency.format(item.unitPrice * actualQuantity)}"
            } else {
                "Informe quanto comprou e o total pago"
            }
            val product = catalogByName[com.listamercado.app.data.ProductCatalogRepository.normalizeName(item.name)]
            val text = PriceTargetHelper.statusText(item, product, currency)
            targetStatus.text = text.orEmpty()
            targetStatus.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        private fun parseNumber(raw: String?): Double {
            val value = raw?.trim().orEmpty()
            if (value.isBlank()) return 0.0
            val normalized = if (value.contains(',')) {
                value.replace(".", "").replace(',', '.')
            } else {
                value
            }
            return normalized.toDoubleOrNull() ?: 0.0
        }
    }
}
