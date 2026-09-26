package com.listamercado.app.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.listamercado.app.R
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.util.PriceTargetHelper
import com.listamercado.app.util.PriceUnitHelper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class ShoppingItemAdapter(
    private val onChecked: (ShoppingItem, Boolean) -> Unit,
    private val onEdit: (ShoppingItem) -> Unit,
    private val onHistory: (ShoppingItem) -> Unit,
    private val onDelete: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<ShoppingItemAdapter.Holder>() {
    private val items = mutableListOf<ShoppingItem>()
    private var catalogByName: Map<String, CatalogProduct> = emptyMap()
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val quantityFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale("pt", "BR")))

    fun submitList(newItems: List<ShoppingItem>, catalogProducts: List<CatalogProduct> = emptyList()) {
        items.clear()
        items.addAll(newItems)
        catalogByName = catalogProducts.associateBy { it.normalizedName }
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
        private val meta: TextView = view.findViewById(R.id.textMeta)
        private val note: TextView = view.findViewById(R.id.textNote)
        private val priceTargetStatus: TextView = view.findViewById(R.id.textPriceTargetStatus)
        private val unitPrice: TextView = view.findViewById(R.id.textUnitPrice)
        private val totalPrice: TextView = view.findViewById(R.id.textPrice)
        private val more: ImageButton = view.findViewById(R.id.buttonItemMore)

        fun bind(item: ShoppingItem) {
            check.setOnCheckedChangeListener(null)
            check.isChecked = item.purchased
            name.text = item.name
            meta.text = if (item.purchased) {
                "Planejado: ${quantityFormat.format(item.quantity)} ${item.unit} • Comprado: ${quantityFormat.format(item.purchasedQuantity)} ${item.unit} • ${item.category}"
            } else {
                "Planejado: ${quantityFormat.format(item.quantity)} ${item.unit} • ${item.category}"
            }

            if (item.unitPrice > 0.0) {
                totalPrice.text = currency.format(if (item.purchased) item.purchasedSubtotal else item.subtotal)
                unitPrice.text = PriceUnitHelper.formattedUnitPrice(item, currency)
            } else {
                totalPrice.text = "Sem preço"
                unitPrice.text = "Toque para informar"
            }

            val product = catalogByName[com.listamercado.app.data.ProductCatalogRepository.normalizeName(item.name)]
            val targetText = PriceTargetHelper.statusText(item, product, currency)
            priceTargetStatus.text = targetText.orEmpty()
            priceTargetStatus.visibility = if (targetText.isNullOrBlank()) View.GONE else View.VISIBLE

            note.text = item.note
            note.visibility = if (item.note.isBlank()) View.GONE else View.VISIBLE

            name.paintFlags = if (item.purchased) {
                name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            itemView.alpha = if (item.purchased) 0.68f else 1f

            check.setOnCheckedChangeListener { _, checked -> onChecked(item, checked) }
            itemView.setOnClickListener { onEdit(item) }
            more.setOnClickListener { anchor ->
                PopupMenu(anchor.context, anchor).apply {
                    menu.add("Editar")
                    menu.add("Histórico de preço")
                    menu.add("Excluir")
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.title.toString()) {
                            "Editar" -> onEdit(item)
                            "Histórico de preço" -> onHistory(item)
                            "Excluir" -> onDelete(item)
                        }
                        true
                    }
                    show()
                }
            }
        }
    }
}
