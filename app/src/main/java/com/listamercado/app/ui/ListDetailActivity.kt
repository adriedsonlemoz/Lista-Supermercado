package com.listamercado.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.listamercado.app.R
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.text.NumberFormat
import java.util.Locale

class ListDetailActivity : AppCompatActivity() {
    private lateinit var repository: ShoppingRepository
    private val lists = mutableListOf<ShoppingList>()
    private lateinit var current: ShoppingList
    private lateinit var adapter: ShoppingItemAdapter
    private lateinit var search: EditText
    private lateinit var summary: TextView
    private lateinit var title: TextView
    private lateinit var empty: TextView
    private lateinit var clearPurchased: MaterialButton
    private var filter = Filter.ALL
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_list_detail)

        repository = ShoppingRepository(this)
        lists += repository.loadLists()
        val id = intent.getLongExtra(EXTRA_LIST_ID, -1L)
        val list = lists.firstOrNull { it.id == id }
        if (list == null) {
            finish()
            return
        }
        current = list
        bindViews()
        InsetsHelper.applyScaffold(
            activity = this,
            root = findViewById(R.id.rootDetail),
            scrollable = findViewById(R.id.recyclerItems),
            fab = findViewById(R.id.fabAddItem)
        )
        render()
    }

    private fun bindViews() {
        title = findViewById(R.id.textListTitle)
        summary = findViewById(R.id.textListSummary)
        search = findViewById(R.id.inputSearchItems)
        empty = findViewById(R.id.textEmptyItems)
        clearPurchased = findViewById(R.id.buttonClearPurchased)
        val recycler = findViewById<RecyclerView>(R.id.recyclerItems)

        adapter = ShoppingItemAdapter(
            onChecked = { item, checked ->
                item.purchased = checked
                persistAndRender()
            },
            onEdit = { editItem(it) },
            onDelete = { confirmDelete(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.buttonRenameList).setOnClickListener { renameList() }
        findViewById<ExtendedFloatingActionButton>(R.id.fabAddItem).setOnClickListener { addItem() }
        findViewById<MaterialButton>(R.id.buttonAll).setOnClickListener { setFilter(Filter.ALL) }
        findViewById<MaterialButton>(R.id.buttonPending).setOnClickListener { setFilter(Filter.PENDING) }
        findViewById<MaterialButton>(R.id.buttonPurchased).setOnClickListener { setFilter(Filter.PURCHASED) }
        clearPurchased.setOnClickListener { clearPurchased() }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun renameList() {
        ListDialog.show(this, current.name) { name ->
            current.name = name
            persistAndRender()
        }
    }

    private fun addItem() = ItemDialog.show(this) { newItem ->
        current.items += newItem
        persistAndRender()
    }

    private fun editItem(item: ShoppingItem) = ItemDialog.show(this, item) { edited ->
        val index = current.items.indexOfFirst { it.id == item.id }
        if (index >= 0) current.items[index] = edited
        persistAndRender()
    }

    private fun confirmDelete(item: ShoppingItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir ${item.name}?")
            .setMessage("O item será removido desta lista.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                current.items.removeAll { it.id == item.id }
                persistAndRender()
            }
            .show()
    }

    private fun clearPurchased() {
        if (current.items.none { it.purchased }) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Limpar itens comprados?")
            .setMessage("Os itens marcados como comprados serão removidos desta lista.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                current.items.removeAll { it.purchased }
                persistAndRender()
            }
            .show()
    }

    private fun setFilter(newFilter: Filter) {
        filter = newFilter
        render()
    }

    private fun persistAndRender() {
        current.updatedAt = System.currentTimeMillis()
        repository.saveLists(lists)
        render()
    }

    private fun render() {
        title.text = current.name
        val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
        val visible = current.items.asSequence()
            .filter {
                when (filter) {
                    Filter.ALL -> true
                    Filter.PENDING -> !it.purchased
                    Filter.PURCHASED -> it.purchased
                }
            }
            .filter { query.isBlank() || it.name.lowercase().contains(query) || it.category.lowercase().contains(query) }
            .sortedWith(compareBy<ShoppingItem> { it.purchased }.thenBy { it.category }.thenBy { it.name.lowercase() })
            .toList()

        adapter.submitList(visible)
        empty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE

        val pending = current.items.count { !it.purchased }
        val purchased = current.items.count { it.purchased }
        summary.text = "$pending pendentes • $purchased comprados\nEstimado: ${currency.format(current.estimatedTotal)} • No carrinho: ${currency.format(current.purchasedTotal)}"
        clearPurchased.visibility = if (purchased > 0) View.VISIBLE else View.INVISIBLE
    }

    private enum class Filter { ALL, PENDING, PURCHASED }

    companion object {
        const val EXTRA_LIST_ID = "list_id"
    }
}
