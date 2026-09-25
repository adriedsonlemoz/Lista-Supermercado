package com.listamercado.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.listamercado.app.BuildConfig
import com.listamercado.app.R
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ShoppingItem
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var repository: ShoppingRepository
    private val items = mutableListOf<ShoppingItem>()
    private lateinit var adapter: ShoppingItemAdapter
    private lateinit var search: EditText
    private lateinit var summary: TextView
    private lateinit var emptyState: TextView
    private var filter: Filter = Filter.ALL
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = ShoppingRepository(this)
        items += repository.load()
        bindViews()
        render()
    }

    private fun bindViews() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerItems)
        search = findViewById(R.id.inputSearch)
        summary = findViewById(R.id.textSummary)
        emptyState = findViewById(R.id.textEmpty)

        adapter = ShoppingItemAdapter(
            onChecked = { item, checked ->
                item.purchased = checked
                persistAndRender()
            },
            onEdit = { item -> editItem(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { addItem() }
        findViewById<MaterialButton>(R.id.buttonAll).setOnClickListener { setFilter(Filter.ALL) }
        findViewById<MaterialButton>(R.id.buttonPending).setOnClickListener { setFilter(Filter.PENDING) }
        findViewById<MaterialButton>(R.id.buttonPurchased).setOnClickListener { setFilter(Filter.PURCHASED) }
        findViewById<MaterialButton>(R.id.buttonClearPurchased).setOnClickListener { clearPurchased() }
        findViewById<MaterialButton>(R.id.buttonAbout).setOnClickListener { showAbout() }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun addItem() = ItemDialog.show(this) { newItem ->
        items += newItem
        persistAndRender()
    }

    private fun editItem(item: ShoppingItem) = ItemDialog.show(this, item) { edited ->
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = edited
        persistAndRender()
    }

    private fun confirmDelete(item: ShoppingItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir ${item.name}?")
            .setMessage("O item será removido da lista.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                items.removeAll { it.id == item.id }
                persistAndRender()
            }
            .show()
    }

    private fun clearPurchased() {
        if (items.none { it.purchased }) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Limpar itens comprados?")
            .setMessage("Todos os itens já marcados como comprados serão removidos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                items.removeAll { it.purchased }
                persistAndRender()
            }
            .show()
    }


    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Lista de Mercado")
            .setMessage(
                "Versão ${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}\n\n" +
                    "Lista de compras offline, simples e rápida para o supermercado."
            )
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun setFilter(newFilter: Filter) {
        filter = newFilter
        render()
    }

    private fun persistAndRender() {
        repository.save(items)
        render()
    }

    private fun render() {
        val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
        val visible = items
            .asSequence()
            .filter { item ->
                when (filter) {
                    Filter.ALL -> true
                    Filter.PENDING -> !item.purchased
                    Filter.PURCHASED -> item.purchased
                }
            }
            .filter { query.isBlank() || it.name.lowercase().contains(query) || it.category.lowercase().contains(query) }
            .sortedWith(compareBy<ShoppingItem> { it.purchased }.thenBy { it.category }.thenBy { it.name.lowercase() })
            .toList()

        adapter.submitList(visible)
        emptyState.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE

        val pending = items.count { !it.purchased }
        val purchased = items.count { it.purchased }
        val estimated = items.sumOf { it.subtotal }
        val purchasedTotal = items.filter { it.purchased }.sumOf { it.subtotal }
        summary.text = "$pending pendentes • $purchased comprados\nEstimado: ${currency.format(estimated)} • No carrinho: ${currency.format(purchasedTotal)}"
    }

    private enum class Filter { ALL, PENDING, PURCHASED }
}
