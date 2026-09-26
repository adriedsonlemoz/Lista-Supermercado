package com.listamercado.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.listamercado.app.R
import com.listamercado.app.model.ListTemplate

class TemplateAdapter(
    private val onUse: (ListTemplate) -> Unit,
    private val onDelete: (ListTemplate) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.Holder>() {
    private val items = mutableListOf<ListTemplate>()

    fun submitList(templates: List<ListTemplate>) {
        items.clear()
        items.addAll(templates)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_template, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.textTemplateName)
        private val meta: TextView = view.findViewById(R.id.textTemplateMeta)
        private val use: MaterialButton = view.findViewById(R.id.buttonUseTemplate)
        private val delete: MaterialButton = view.findViewById(R.id.buttonDeleteTemplate)

        fun bind(template: ListTemplate) {
            name.text = template.name
            meta.text = when {
                template.items.isNotEmpty() -> "${template.items.size} ${if (template.items.size == 1) "item" else "itens"} • ${if (template.builtIn) "modelo inicial" else "modelo criado por você"}"
                template.builtIn -> "Sem itens predefinidos • adicione depois de criar a lista"
                else -> "Modelo vazio criado por você"
            }
            use.setOnClickListener { onUse(template) }
            delete.visibility = if (template.builtIn) View.GONE else View.VISIBLE
            delete.setOnClickListener { onDelete(template) }
            itemView.setOnClickListener { onUse(template) }
        }
    }
}
