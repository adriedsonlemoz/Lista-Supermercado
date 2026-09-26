package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.listamercado.app.R
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.util.PriceUnitHelper
import java.text.NumberFormat
import java.util.Locale

class CatalogProductAdapter : RecyclerView.Adapter<CatalogProductAdapter.Holder>() {
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

        fun bind(product: CatalogProduct) {
            name.text = product.name
            meta.text = "${product.category} • unidade padrão: ${product.unit}"
            price.text = if (product.lastUnitPrice > 0.0) {
                val display = PriceUnitHelper.displayUnitPrice(product.lastUnitPrice, product.unit)
                "Último preço: ${currency.format(display.first)} / ${display.second}"
            } else {
                "Último preço: não informado"
            }
            barcode.text = product.barcode?.let { "Código de barras: $it" } ?: "Sem código de barras"
        }
    }
}
