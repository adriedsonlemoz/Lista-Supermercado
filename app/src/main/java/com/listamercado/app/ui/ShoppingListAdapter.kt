package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.listamercado.app.R
import com.listamercado.app.model.ShoppingList
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class ShoppingListAdapter(
    private val onOpen: (ShoppingList) -> Unit,
    private val onMore: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.Holder>() {
    private val lists = mutableListOf<ShoppingList>()
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val date = DateFormat.getDateInstance(DateFormat.SHORT, Locale("pt", "BR"))

    fun submitList(newLists: List<ShoppingList>) {
        lists.clear()
        lists.addAll(newLists)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(lists[position])
    override fun getItemCount(): Int = lists.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textListName)
        private val meta: TextView = view.findViewById(R.id.textListMeta)
        private val progressLabel: TextView = view.findViewById(R.id.textListProgress)
        private val total: TextView = view.findViewById(R.id.textListTotal)
        private val progress: LinearProgressIndicator = view.findViewById(R.id.progressList)
        private val more: ImageButton = view.findViewById(R.id.buttonListMore)

        fun bind(list: ShoppingList) {
            name.text = list.name
            val count = list.items.size
            meta.text = "Atualizada ${date.format(Date(list.updatedAt))} • $count ${if (count == 1) "item" else "itens"}"

            val purchaseLabel = when {
                count == 0 -> "Lista vazia"
                list.purchasedCount == 0 -> "Nenhum item comprado"
                list.purchasedCount == count -> "Compra concluída"
                else -> "${list.purchasedCount} de $count comprados"
            }
            progressLabel.text = if (list.budget > 0.0) {
                "$purchaseLabel • orçamento ${currency.format(list.budget)}"
            } else {
                purchaseLabel
            }

            val percent = if (count == 0) 0 else ((list.purchasedCount * 100.0) / count).toInt()
            progress.setProgressCompat(percent, false)
            total.text = if (list.estimatedTotal > 0.0) currency.format(list.estimatedTotal) else "Sem preços"

            itemView.setOnClickListener { onOpen(list) }
            more.setOnClickListener { onMore(list) }
        }
    }
}
