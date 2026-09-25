package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.listamercado.app.R
import com.listamercado.app.model.ComparisonRow

class ComparisonAdapter : RecyclerView.Adapter<ComparisonAdapter.Holder>() {
    private val rows = mutableListOf<ComparisonRow>()

    fun submitList(values: List<ComparisonRow>) {
        rows.clear()
        rows.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comparison, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(rows[position])
    override fun getItemCount(): Int = rows.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.textComparisonTitle)
        private val subtitle: TextView = view.findViewById(R.id.textComparisonSubtitle)
        private val detail: TextView = view.findViewById(R.id.textComparisonDetail)

        fun bind(row: ComparisonRow) {
            title.text = row.title
            subtitle.text = row.subtitle
            detail.text = row.detail
            detail.visibility = if (row.detail.isBlank()) View.GONE else View.VISIBLE
        }
    }
}
