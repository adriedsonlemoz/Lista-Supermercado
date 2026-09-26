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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.text.NumberFormat
import java.util.Locale

class ListDetailActivity : AppCompatActivity() {
    private lateinit var repository: ShoppingRepository
    private lateinit var catalogRepository: ProductCatalogRepository
    private val lists = mutableListOf<ShoppingList>()
    private lateinit var current: ShoppingList
    private lateinit var adapter: ShoppingItemAdapter
    private lateinit var search: EditText
    private lateinit var searchLayout: TextInputLayout
    private lateinit var title: TextView
    private lateinit var empty: TextView
    private lateinit var clearPurchased: MaterialButton
    private lateinit var estimatedTotal: TextView
    private lateinit var cartTotal: TextView
    private lateinit var purchaseStatus: TextView
    private lateinit var budgetStatus: TextView
    private lateinit var budgetRemainingLabel: TextView
    private lateinit var budgetRemaining: TextView
    private lateinit var budgetButton: MaterialButton
    private lateinit var budgetProgress: LinearProgressIndicator
    private var filter = Filter.ALL
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var pendingBarcodeConsumer: ((String) -> Unit)? = null
    private val purchaseModeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        reloadCurrentList()
    }
    private val barcodeScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val barcode = result.data?.getStringExtra(BarcodeScannerActivity.EXTRA_BARCODE)
        val consumer = pendingBarcodeConsumer
        pendingBarcodeConsumer = null
        if (result.resultCode == RESULT_OK && !barcode.isNullOrBlank()) {
            consumer?.invoke(barcode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_list_detail)

        repository = ShoppingRepository(this)
        catalogRepository = ProductCatalogRepository(this)
        lists += repository.loadLists()
        catalogRepository.seedFromLists(lists)
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
        registerBackBehavior()
        render()
    }

    private fun bindViews() {
        title = findViewById(R.id.textListTitle)
        search = findViewById(R.id.inputSearchItems)
        searchLayout = findViewById(R.id.layoutSearchItems)
        empty = findViewById(R.id.textEmptyItems)
        clearPurchased = findViewById(R.id.buttonClearPurchased)
        estimatedTotal = findViewById(R.id.textEstimatedTotal)
        cartTotal = findViewById(R.id.textCartTotal)
        purchaseStatus = findViewById(R.id.textPurchaseStatus)
        budgetStatus = findViewById(R.id.textBudgetStatus)
        budgetRemainingLabel = findViewById(R.id.textBudgetRemainingLabel)
        budgetRemaining = findViewById(R.id.textBudgetRemaining)
        budgetButton = findViewById(R.id.buttonBudget)
        budgetProgress = findViewById(R.id.progressBudget)
        val recycler = findViewById<RecyclerView>(R.id.recyclerItems)

        adapter = ShoppingItemAdapter(
            onChecked = { item, checked ->
                item.purchased = checked
                persistAndRender()
            },
            onEdit = { editItem(it) },
            onHistory = { openPriceHistory(it) },
            onDelete = { confirmDelete(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.buttonSearchItems).setOnClickListener { toggleSearch() }
        findViewById<ImageButton>(R.id.buttonRenameList).setOnClickListener { renameList() }
        findViewById<FloatingActionButton>(R.id.fabAddItem).setOnClickListener { addItem() }
        findViewById<MaterialButton>(R.id.buttonAll).setOnClickListener { setFilter(Filter.ALL) }
        findViewById<MaterialButton>(R.id.buttonPending).setOnClickListener { setFilter(Filter.PENDING) }
        findViewById<MaterialButton>(R.id.buttonPurchased).setOnClickListener { setFilter(Filter.PURCHASED) }
        findViewById<MaterialButton>(R.id.buttonPurchaseMode).setOnClickListener { openPurchaseMode() }
        clearPurchased.setOnClickListener { clearPurchased() }
        budgetButton.setOnClickListener { editBudget() }

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
            } else false
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


    private fun openPurchaseMode() {
        purchaseModeLauncher.launch(
            Intent(this, PurchaseModeActivity::class.java)
                .putExtra(PurchaseModeActivity.EXTRA_LIST_ID, current.id)
        )
    }

    private fun reloadCurrentList() {
        if (!::current.isInitialized) return
        val currentId = current.id
        val refreshed = repository.loadLists()
        val updated = refreshed.firstOrNull { it.id == currentId }
        if (updated == null) {
            finish()
            return
        }
        lists.clear()
        lists += refreshed
        current = updated
        render()
    }

    private fun editBudget() {
        BudgetDialog.show(this, current.budget) { value ->
            current.budget = value
            persistAndRender()
        }
    }

    private fun openPriceHistory(item: ShoppingItem) {
        startActivity(Intent(this, CompareActivity::class.java).putExtra(CompareActivity.EXTRA_PRODUCT_NAME, item.name))
    }

    private fun renameList() {
        ListDialog.show(this, current.name) { name ->
            current.name = name
            persistAndRender()
        }
    }

    private fun addItem() = ItemDialog.show(
        context = this,
        catalogRepository = catalogRepository,
        onRequestBarcode = { consumer -> requestBarcode(consumer) }
    ) { newItem, barcode ->
        current.items += newItem
        catalogRepository.recordItem(newItem, barcode)
        persistAndRender()
    }

    private fun editItem(item: ShoppingItem) = ItemDialog.show(
        context = this,
        catalogRepository = catalogRepository,
        existing = item,
        onRequestBarcode = { consumer -> requestBarcode(consumer) }
    ) { edited, barcode ->
        val index = current.items.indexOfFirst { it.id == item.id }
        if (index >= 0) current.items[index] = edited
        catalogRepository.recordItem(edited, barcode)
        persistAndRender()
    }

    private fun requestBarcode(consumer: (String) -> Unit) {
        pendingBarcodeConsumer = consumer
        barcodeScannerLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
    }

    private fun confirmDelete(item: ShoppingItem) {
        ConfirmDialog.showDestructive(
            context = this,
            title = "Excluir ${item.name}?",
            message = "O item será removido desta lista.",
            confirmLabel = "Excluir"
        ) {
            current.items.removeAll { it.id == item.id }
            persistAndRender()
        }
    }

    private fun clearPurchased() {
        if (current.items.none { it.purchased }) return
        ConfirmDialog.showDestructive(
            context = this,
            title = "Limpar itens comprados?",
            message = "Os itens marcados como comprados serão removidos desta lista.",
            confirmLabel = "Limpar"
        ) {
            current.items.removeAll { it.purchased }
            persistAndRender()
        }
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
        estimatedTotal.text = currency.format(current.estimatedTotal)
        cartTotal.text = currency.format(current.purchasedTotal)
        purchaseStatus.text = "$pending pendentes • $purchased comprados"
        renderBudget()
        clearPurchased.visibility = if (purchased > 0) View.VISIBLE else View.GONE
    }

    private fun renderBudget() {
        val normalColor = MaterialColors.getColor(budgetRemaining, com.google.android.material.R.attr.colorOnPrimaryContainer)
        val errorColor = MaterialColors.getColor(budgetRemaining, com.google.android.material.R.attr.colorError)

        if (current.budget <= 0.0) {
            budgetStatus.text = "Sem orçamento definido"
            budgetButton.text = "Definir"
            budgetRemainingLabel.text = "Orçamento"
            budgetRemaining.text = "—"
            budgetRemainingLabel.setTextColor(normalColor)
            budgetRemaining.setTextColor(normalColor)
            budgetProgress.visibility = View.GONE
            return
        }

        val remaining = current.budgetRemaining
        budgetStatus.text = "Orçamento ${currency.format(current.budget)}"
        budgetButton.text = "Alterar"
        budgetRemainingLabel.text = if (remaining >= 0.0) "Falta" else "Excedeu"
        budgetRemaining.text = currency.format(kotlin.math.abs(remaining))
        budgetRemainingLabel.setTextColor(errorColor)
        budgetRemaining.setTextColor(errorColor)

        val percent = ((current.estimatedTotal / current.budget) * 100.0).toInt().coerceIn(0, 100)
        budgetProgress.setProgressCompat(percent, true)
        budgetProgress.visibility = View.VISIBLE
    }

    private enum class Filter { ALL, PENDING, PURCHASED }

    companion object {
        const val EXTRA_LIST_ID = "list_id"
    }
}
