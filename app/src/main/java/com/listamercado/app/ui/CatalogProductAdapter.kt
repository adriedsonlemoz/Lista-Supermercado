package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.listamercado.app.R
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.Recurrence
import com.listamercado.app.util.PriceUnitHelper
import java.text.NumberFormat
import java.util.Locale

class CatalogProductAdapter(
    private val onFavorite: (CatalogProduct) -> Unit,
    private val onRecurring: (CatalogProduct) -> Unit,
    private val onPriceTarget: (CatalogProduct) -> Unit
) : RecyclerView.Adapter<CatalogProductAdapter.Holder>() {
    private val items = mutableListOf<CatalogProduct>()
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    fun submitList(products: List<CatalogProduct>) {
        items.clear()
        items.addAll(products)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_catalog_product, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textCatalogProductName)
        private val meta: TextView = view.findViewById(R.id.textCatalogProductMeta)
        private val price: TextView = view.findViewById(R.id.textCatalogProductPrice)
        private val barcode: TextView = view.findViewById(R.id.textCatalogProductBarcode)
        private val target: TextView = view.findViewById(R.id.textCatalogProductTarget)
        private val favorite: MaterialButton = view.findViewById(R.id.buttonCatalogFavorite)
        private val recurring: MaterialButton = view.findViewById(R.id.buttonCatalogRecurring)
        private val priceTarget: MaterialButton = view.findViewById(R.id.buttonCatalogPriceTarget)

        fun bind(product: CatalogProduct) {
            name.text = product.name
            meta.text = "${product.category} • padrão: ${formatQuantity(product.lastQuantity)} ${product.unit}"
            price.text = if (product.lastUnitPrice > 0.0) {
                val display = PriceUnitHelper.displayUnitPrice(product.lastUnitPrice, product.unit)
                "Último preço: ${currency.format(display.first)} / ${display.second}"
            } else {
                "Último preço: não informado"
            }
            barcode.text = product.barcode?.let { "Código: $it" } ?: "Sem código de barras"
            target.text = product.priceTarget?.let { value ->
                "Preço-alvo: ${currency.format(value)} / ${product.priceTargetUnit ?: PriceUnitHelper.priceUnit(product.unit)}"
            } ?: "Preço-alvo: não definido"
            favorite.text = if (product.favorite) "★ Favorito" else "☆ Favorito"
            recurring.text = Recurrence.label(product.recurringFrequency)?.let { "Recorrente: $it" } ?: "Definir recorrência"
            favorite.setOnClickListener { onFavorite(product) }
            recurring.setOnClickListener { onRecurring(product) }
            priceTarget.text = if (product.priceTarget != null) "Editar/remover preço-alvo" else "Definir preço-alvo"
            priceTarget.setOnClickListener { onPriceTarget(product) }
        }
    }

    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("pt", "BR"), "%.2f", value).trimEnd('0').trimEnd(',')
}
