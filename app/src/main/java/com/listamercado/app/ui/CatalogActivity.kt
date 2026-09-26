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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.listamercado.app.R
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.Recurrence
import com.listamercado.app.util.ThemeController

class CatalogActivity : AppCompatActivity() {
    private lateinit var catalogRepository: ProductCatalogRepository
    private lateinit var adapter: CatalogProductAdapter
    private lateinit var search: EditText
    private lateinit var count: TextView
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_catalog)
        InsetsHelper.applyScaffold(
            activity = this,
            root = findViewById(R.id.rootCatalog),
            scrollable = findViewById(R.id.recyclerCatalog)
        )

        catalogRepository = ProductCatalogRepository(this)
        catalogRepository.seedFromLists(ShoppingRepository(this).loadLists())
        bindViews()
        render()
    }

    private fun bindViews() {
        search = findViewById(R.id.inputCatalogSearch)
        count = findViewById(R.id.textCatalogCount)
        empty = findViewById(R.id.textCatalogEmpty)
        adapter = CatalogProductAdapter(
            onFavorite = { product ->
                catalogRepository.setFavorite(product.id, !product.favorite)
                render()
            },
            onRecurring = { product -> chooseRecurrence(product) },
            onPriceTarget = { product -> editPriceTarget(product) }
        )
        findViewById<RecyclerView>(R.id.recyclerCatalog).apply {
            layoutManager = LinearLayoutManager(this@CatalogActivity)
            adapter = this@CatalogActivity.adapter
        }
        findViewById<ImageButton>(R.id.buttonBackCatalog).setOnClickListener { finish() }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun editPriceTarget(product: CatalogProduct) {
        PriceTargetDialog.show(this, product) { value ->
            catalogRepository.setPriceTarget(product.id, value, product.unit)
            render()
        }
    }

    private fun chooseRecurrence(product: CatalogProduct) {
        val labels = arrayOf("Não recorrente") + Recurrence.labels()
        val values = arrayOf<String?>(null) + Recurrence.values()
        val checked = values.indexOf(product.recurringFrequency).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle("Recorrência de ${product.name}")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                catalogRepository.setRecurringFrequency(product.id, values[which])
                dialog.dismiss()
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun render() {
        val all = catalogRepository.suggestions()
        val query = search.text?.toString().orEmpty()
        val visible = if (query.isBlank()) all else catalogRepository.suggestions(query)
        adapter.submitList(visible)
        count.text = if (all.size == 1) "1 produto conhecido" else "${all.size} produtos conhecidos"
        empty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }
}
