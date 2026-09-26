package com.listamercado.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.BuildConfig
import com.listamercado.app.R
import com.listamercado.app.data.SettingsRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.data.TemplateRepository
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var repository: ShoppingRepository
    private val lists = mutableListOf<ShoppingList>()
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var search: EditText
    private lateinit var searchLayout: TextInputLayout
    private lateinit var summary: TextView
    private lateinit var emptyState: View
    private lateinit var dashboardTotal: TextView
    private lateinit var dashboardStats: TextView

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
        registerBackBehavior()
        if (savedInstanceState == null) {
            showWhatsNewIfNeeded()
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun bindViews() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerLists)
        search = findViewById(R.id.inputSearchLists)
        searchLayout = findViewById(R.id.layoutSearchLists)
        summary = findViewById(R.id.textHomeSummary)
        emptyState = findViewById(R.id.textEmptyLists)
        dashboardTotal = findViewById(R.id.textDashboardTotal)
        dashboardStats = findViewById(R.id.textDashboardStats)

        adapter = ShoppingListAdapter(
            onOpen = { openList(it) },
            onMore = { showListActions(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.fabNewList).setOnClickListener { createList() }
        findViewById<ImageButton>(R.id.buttonSearch).setOnClickListener { toggleSearch() }
        findViewById<ImageButton>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCompare).setOnClickListener {
            startActivity(Intent(this, CompareActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonNearbyMarkets).setOnClickListener {
            startActivity(Intent(this, NearbyMarketsActivity::class.java))
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                search.clearFocus()
                true
            } else {
                false
            }
        }
    }

    private fun registerBackBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchLayout.visibility == View.VISIBLE) {
                    closeSearch(clear = true)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun toggleSearch() {
        if (searchLayout.visibility == View.VISIBLE) {
            closeSearch(clear = true)
        } else {
            searchLayout.visibility = View.VISIBLE
            search.requestFocus()
            search.post {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun closeSearch(clear: Boolean) {
        hideKeyboard()
        search.clearFocus()
        if (clear) search.text?.clear()
        searchLayout.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(search.windowToken, 0)
    }

    private fun showWhatsNewIfNeeded() {
        val settings = SettingsRepository(this)
        if (settings.lastSeenWhatsNewVersionCode() < BuildConfig.VERSION_CODE) {
            val intent = Intent(this, WhatsNewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }

    private fun reload() {
        lists.clear()
        lists += repository.loadLists()
        render()
    }

    private fun createList() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Nova lista")
            .setItems(arrayOf("Lista vazia", "Usar modelo")) { _, which ->
                if (which == 0) createEmptyList() else startActivity(Intent(this, TemplatesActivity::class.java))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createEmptyList() {
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
        ListActionsDialog.show(
            activity = this,
            listName = list.name,
            onRename = { renameList(list) },
            onDuplicate = { duplicateList(list) },
            onSaveTemplate = { saveAsTemplate(list) },
            onDelete = { confirmDelete(list) }
        )
    }

    private fun saveAsTemplate(list: ShoppingList) {
        ListDialog.show(
            context = this,
            initialName = "${list.name} - modelo",
            titleOverride = "Salvar como modelo",
            subtitleOverride = "Itens, quantidades e orçamento serão reutilizáveis em novas listas."
        ) { name ->
            TemplateRepository(this).saveFromList(list, name)
            Snackbar.make(findViewById(android.R.id.content), "Modelo salvo", Snackbar.LENGTH_SHORT).show()
        }
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
                item.copy(id = now + index + 1, purchased = false, purchasedQuantity = 0.0)
            }.toMutableList()
            lists.add(0, ShoppingList(id = now, name = name, createdAt = now, updatedAt = now, budget = source.budget, items = copiedItems))
            repository.saveLists(lists)
            render()
        }
    }

    private fun confirmDelete(list: ShoppingList) {
        ConfirmDialog.showDestructive(
            context = this,
            title = "Excluir ${list.name}?",
            message = "A lista e seus preços serão removidos do histórico.",
            confirmLabel = "Excluir"
        ) {
            lists.removeAll { it.id == list.id }
            repository.saveLists(lists)
            render()
        }
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
        val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        summary.text = when {
            lists.isEmpty() -> "Organize suas compras e acompanhe seus preços"
            lists.size == 1 -> "1 lista ativa • $totalItems ${if (totalItems == 1) "item" else "itens"}"
            else -> "${lists.size} listas ativas • $totalItems itens cadastrados"
        }
        dashboardTotal.text = currency.format(totalEstimated)
        dashboardStats.text = "${lists.size} ${if (lists.size == 1) "lista" else "listas"} · $totalItems ${if (totalItems == 1) "item" else "itens"} · $purchasedItems compr."
    }
}
