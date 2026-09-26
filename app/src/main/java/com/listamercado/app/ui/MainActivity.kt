package com.listamercado.app.ui

import android.content.Intent
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.listamercado.app.R
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController

class MainActivity : AppCompatActivity() {
    private lateinit var repository: ShoppingRepository
    private val lists = mutableListOf<ShoppingList>()
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var search: EditText
    private lateinit var summary: TextView
    private lateinit var emptyState: View
    private lateinit var dashboardTotal: TextView
    private lateinit var dashboardLists: TextView
    private lateinit var dashboardItems: TextView
    private lateinit var dashboardPurchased: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_main)
        repository = ShoppingRepository(this)
        bindViews()
        InsetsHelper.applyScaffold(
            activity = this,
            root = findViewById(R.id.rootMain),
            scrollable = findViewById(R.id.recyclerLists),
            fab = findViewById(R.id.fabNewList)
        )
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun bindViews() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerLists)
        search = findViewById(R.id.inputSearchLists)
        summary = findViewById(R.id.textHomeSummary)
        emptyState = findViewById(R.id.textEmptyLists)
        dashboardTotal = findViewById(R.id.textDashboardTotal)
        dashboardLists = findViewById(R.id.textDashboardLists)
        dashboardItems = findViewById(R.id.textDashboardItems)
        dashboardPurchased = findViewById(R.id.textDashboardPurchased)

        adapter = ShoppingListAdapter(
            onOpen = { openList(it) },
            onMore = { showListActions(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.fabNewList).setOnClickListener { createList() }
        findViewById<ImageButton>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCompare).setOnClickListener {
            startActivity(Intent(this, CompareActivity::class.java))
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun reload() {
        lists.clear()
        lists += repository.loadLists()
        render()
    }

    private fun createList() {
        ListDialog.show(this) { name ->
            lists.add(0, ShoppingList(name = name))
            repository.saveLists(lists)
            render()
        }
    }

    private fun openList(list: ShoppingList) {
        startActivity(Intent(this, ListDetailActivity::class.java).putExtra(ListDetailActivity.EXTRA_LIST_ID, list.id))
    }

    private fun showListActions(list: ShoppingList) {
        val options = arrayOf("Renomear", "Duplicar para nova compra", "Excluir")
        MaterialAlertDialogBuilder(this)
            .setTitle(list.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> renameList(list)
                    1 -> duplicateList(list)
                    2 -> confirmDelete(list)
                }
            }
            .show()
    }

    private fun renameList(list: ShoppingList) {
        ListDialog.show(this, list.name) { name ->
            list.name = name
            list.updatedAt = System.currentTimeMillis()
            repository.saveLists(lists)
            render()
        }
    }

    private fun duplicateList(source: ShoppingList) {
        ListDialog.show(this, "${source.name} - nova compra") { name ->
            val now = System.currentTimeMillis()
            val copiedItems = source.items.mapIndexed { index, item ->
                item.copy(id = now + index + 1, purchased = false)
            }.toMutableList()
            lists.add(0, ShoppingList(id = now, name = name, createdAt = now, updatedAt = now, items = copiedItems))
            repository.saveLists(lists)
            render()
        }
    }

    private fun confirmDelete(list: ShoppingList) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir ${list.name}?")
            .setMessage("A lista e seus preços serão removidos do histórico.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                lists.removeAll { it.id == list.id }
                repository.saveLists(lists)
                render()
            }
            .show()
    }

    private fun render() {
        val query = search.text?.toString()?.trim()?.lowercase().orEmpty()
        val visible = lists
            .filter { list ->
                query.isBlank() || list.name.lowercase().contains(query) ||
                    list.items.any { it.name.lowercase().contains(query) }
            }
            .sortedByDescending { it.updatedAt }

        adapter.submitList(visible)
        emptyState.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        val totalItems = lists.sumOf { it.items.size }
        val purchasedItems = lists.sumOf { it.purchasedCount }
        val totalEstimated = lists.sumOf { it.estimatedTotal }
        val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))

        summary.text = when {
            lists.isEmpty() -> "Organize suas compras e acompanhe seus preços"
            lists.size == 1 -> "1 lista ativa • $totalItems ${if (totalItems == 1) "item" else "itens"}"
            else -> "${lists.size} listas ativas • $totalItems itens cadastrados"
        }
        dashboardTotal.text = currency.format(totalEstimated)
        dashboardLists.text = lists.size.toString()
        dashboardItems.text = totalItems.toString()
        dashboardPurchased.text = purchasedItems.toString()
    }
}
