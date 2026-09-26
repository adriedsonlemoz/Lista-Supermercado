package com.listamercado.app.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import com.listamercado.app.R
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController

class RecurringProductsActivity : AppCompatActivity() {
    private lateinit var shoppingRepository: ShoppingRepository
    private lateinit var catalogRepository: ProductCatalogRepository
    private lateinit var adapter: RecurringProductAdapter
    private var listId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_recurring_products)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootRecurring), findViewById(R.id.recyclerRecurring))
        InsetsHelper.applyBottomSheetInsets(findViewById(R.id.recurringActions))
        listId = intent.getLongExtra(EXTRA_LIST_ID, -1L)
        if (listId <= 0L) {
            finish()
            return
        }
        shoppingRepository = ShoppingRepository(this)
        catalogRepository = ProductCatalogRepository(this)
        catalogRepository.seedFromLists(shoppingRepository.loadLists())
        bindViews()
        render()
    }

    private fun bindViews() {
        adapter = RecurringProductAdapter()
        findViewById<RecyclerView>(R.id.recyclerRecurring).apply {
            layoutManager = LinearLayoutManager(this@RecurringProductsActivity)
            adapter = this@RecurringProductsActivity.adapter
        }
        findViewById<ImageButton>(R.id.buttonBackRecurring).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.buttonAddRecurringSelected).setOnClickListener { addSelected() }
    }

    private fun render() {
        val products = catalogRepository.recurringProducts()
        adapter.submitList(products)
        findViewById<TextView>(R.id.textRecurringEmpty).visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.textRecurringCount).text = when (products.size) {
            0 -> "Nenhum produto recorrente configurado"
            1 -> "1 produto recorrente"
            else -> "${products.size} produtos recorrentes"
        }
    }

    private fun addSelected() {
        val selected = adapter.selectedProducts()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos um produto", Toast.LENGTH_SHORT).show()
            return
        }
        val lists = shoppingRepository.loadLists()
        val list = lists.firstOrNull { it.id == listId }
        if (list == null) {
            finish()
            return
        }
        val existingNames = list.items.map { ProductCatalogRepository.normalizeName(it.name) }.toMutableSet()
        val now = System.currentTimeMillis()
        val addedIds = mutableListOf<Long>()
        var skipped = 0
        selected.forEachIndexed { index, product ->
            val normalized = ProductCatalogRepository.normalizeName(product.name)
            if (normalized in existingNames) {
                skipped += 1
            } else {
                list.items += ShoppingItem(
                    id = now + index + 1,
                    name = product.name,
                    quantity = product.lastQuantity.coerceAtLeast(0.01),
                    unit = product.unit,
                    unitPrice = product.lastUnitPrice.coerceAtLeast(0.0),
                    category = product.category,
                    note = "",
                    purchased = false
                )
                existingNames += normalized
                addedIds += product.id
            }
        }
        if (addedIds.isNotEmpty()) {
            list.updatedAt = now
            shoppingRepository.saveLists(lists)
            catalogRepository.markRecurringAdded(addedIds)
        }
        val message = when {
            addedIds.isEmpty() -> "Os produtos selecionados já estão nesta lista"
            skipped > 0 -> "${addedIds.size} adicionados • $skipped já existiam"
            else -> "${addedIds.size} ${if (addedIds.size == 1) "produto adicionado" else "produtos adicionados"}"
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        if (addedIds.isNotEmpty()) setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_LIST_ID = "list_id"
    }
}
