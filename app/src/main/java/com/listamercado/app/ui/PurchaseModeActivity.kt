package com.listamercado.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.listamercado.app.R
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class PurchaseModeActivity : AppCompatActivity() {
    private lateinit var repository: ShoppingRepository
    private lateinit var catalogRepository: ProductCatalogRepository
    private val lists = mutableListOf<ShoppingList>()
    private lateinit var current: ShoppingList
    private lateinit var adapter: PurchaseModeAdapter
    private lateinit var root: View
    private lateinit var remainingCount: TextView
    private lateinit var cartTotal: TextView
    private lateinit var budgetLabel: TextView
    private lateinit var budgetValue: TextView
    private lateinit var empty: TextView
    private lateinit var title: TextView
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_purchase_mode)

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
            root = root,
            scrollable = findViewById(R.id.recyclerPurchaseMode)
        )
        registerBackBehavior()
        render()
    }

    private fun bindViews() {
        root = findViewById(R.id.rootPurchaseMode)
        title = findViewById(R.id.textPurchaseModeTitle)
        remainingCount = findViewById(R.id.textRemainingCount)
        cartTotal = findViewById(R.id.textPurchaseCartTotal)
        budgetLabel = findViewById(R.id.textPurchaseBudgetLabel)
        budgetValue = findViewById(R.id.textPurchaseBudgetValue)
        empty = findViewById(R.id.textPurchaseModeEmpty)

        val recycler = findViewById<RecyclerView>(R.id.recyclerPurchaseMode)
        adapter = PurchaseModeAdapter(
            onPurchased = { markPurchased(it) },
            onPriceChanged = { item, total -> updateQuickPrice(item, total) },
            onPurchasedQuantityChanged = { item, quantity -> updatePurchasedQuantity(item, quantity) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<ImageButton>(R.id.buttonBackPurchaseMode).setOnClickListener { closeMode() }
        findViewById<MaterialButton>(R.id.buttonFinishPurchaseMode).setOnClickListener { closeMode() }
    }

    private fun registerBackBehavior() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeMode()
        })
    }

    private fun updatePurchasedQuantity(item: ShoppingItem, purchasedQuantity: Double) {
        item.purchasedQuantity = purchasedQuantity.coerceAtLeast(0.0)
        persist(renderItems = false)
    }

    private fun updateQuickPrice(item: ShoppingItem, totalPrice: Double) {
        val actualQuantity = item.purchasedQuantity.takeIf { it > 0.0 } ?: item.quantity
        item.unitPrice = if (actualQuantity > 0.0) totalPrice / actualQuantity else 0.0
        catalogRepository.recordItem(item)
        persist(renderItems = false)
    }

    private fun markPurchased(item: ShoppingItem) {
        item.ensurePurchasedQuantity()
        item.purchased = true
        catalogRepository.recordItem(item)
        persist(renderItems = true)
        Snackbar.make(
            root,
            "${item.name}: ${formatQuantity(item.purchasedQuantity)} ${item.unit} comprado",
            Snackbar.LENGTH_LONG
        )
            .setAction("Desfazer") {
                item.purchased = false
                persist(renderItems = true)
            }
            .show()
    }

    private fun persist(renderItems: Boolean) {
        current.updatedAt = System.currentTimeMillis()
        repository.saveLists(lists)
        if (renderItems) render() else renderSummary()
    }

    private fun render() {
        title.text = current.name
        val pending = current.items
            .asSequence()
            .filter { !it.purchased }
            .sortedWith(compareBy<ShoppingItem> { it.category }.thenBy { it.name.lowercase() })
            .toList()
        adapter.submitList(pending, catalogRepository.loadProducts())
        empty.visibility = if (pending.isEmpty()) View.VISIBLE else View.GONE
        renderSummary()
    }

    private fun renderSummary() {
        val pending = current.items.count { !it.purchased }
        remainingCount.text = pending.toString()
        cartTotal.text = currency.format(current.purchasedTotal)

        val normalColor = MaterialColors.getColor(budgetValue, com.google.android.material.R.attr.colorOnPrimaryContainer)
        val errorColor = MaterialColors.getColor(budgetValue, com.google.android.material.R.attr.colorError)
        if (current.budget <= 0.0) {
            budgetLabel.text = "Orçamento"
            budgetValue.text = "—"
            budgetLabel.setTextColor(normalColor)
            budgetValue.setTextColor(normalColor)
            return
        }

        val remainingBudget = current.budget - current.purchasedTotal
        budgetLabel.text = if (remainingBudget >= 0.0) "Falta" else "Excedeu"
        budgetValue.text = currency.format(abs(remainingBudget))
        val color = if (remainingBudget >= 0.0) normalColor else errorColor
        budgetLabel.setTextColor(color)
        budgetValue.setTextColor(color)
    }

    private fun formatQuantity(value: Double): String =
        java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols(Locale("pt", "BR"))).format(value)

    private fun closeMode() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_LIST_ID = "list_id"
    }
}
