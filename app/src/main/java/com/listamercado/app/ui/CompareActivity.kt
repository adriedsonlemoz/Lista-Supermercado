package com.listamercado.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.listamercado.app.R
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.model.ComparisonRow
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.model.ShoppingList
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class CompareActivity : AppCompatActivity() {
    private val lists = mutableListOf<ShoppingList>()
    private lateinit var adapter: ComparisonAdapter
    private lateinit var listSection: View
    private lateinit var productSection: View
    private lateinit var listA: Spinner
    private lateinit var listB: Spinner
    private lateinit var product: Spinner
    private lateinit var summary: TextView
    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val date = DateFormat.getDateInstance(DateFormat.SHORT, Locale("pt", "BR"))
    private var mode = Mode.LISTS
    private var productNames = emptyList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_compare)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootCompare), findViewById(R.id.recyclerComparison))
        lists += ShoppingRepository(this).loadLists().sortedByDescending { it.createdAt }
        bindViews()
        populateSelectors()
        renderMode()
    }

    private fun bindViews() {
        findViewById<ImageButton>(R.id.buttonBackCompare).setOnClickListener { finish() }
        listSection = findViewById(R.id.sectionCompareLists)
        productSection = findViewById(R.id.sectionProductHistory)
        listA = findViewById(R.id.spinnerListA)
        listB = findViewById(R.id.spinnerListB)
        product = findViewById(R.id.spinnerProduct)
        summary = findViewById(R.id.textCompareSummary)

        adapter = ComparisonAdapter()
        findViewById<RecyclerView>(R.id.recyclerComparison).apply {
            layoutManager = LinearLayoutManager(this@CompareActivity)
            adapter = this@CompareActivity.adapter
        }

        findViewById<MaterialButton>(R.id.buttonModeLists).setOnClickListener {
            mode = Mode.LISTS
            renderMode()
        }
        findViewById<MaterialButton>(R.id.buttonModeProducts).setOnClickListener {
            mode = Mode.PRODUCTS
            renderMode()
        }
        findViewById<MaterialButton>(R.id.buttonRunComparison).setOnClickListener { compareLists() }
        findViewById<MaterialButton>(R.id.buttonShowHistory).setOnClickListener { showProductHistory() }
    }

    private fun populateSelectors() {
        val listNames = lists.map { it.name }
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listNames)
        listA.adapter = listAdapter
        listB.adapter = listAdapter
        if (lists.size > 1) listB.setSelection(1)

        productNames = lists.flatMap { it.items }
            .groupBy { normalize(it.name) }
            .values
            .mapNotNull { entries -> entries.firstOrNull()?.name }
            .sortedBy { it.lowercase() }
        product.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, productNames)
    }

    private fun renderMode() {
        listSection.visibility = if (mode == Mode.LISTS) View.VISIBLE else View.GONE
        productSection.visibility = if (mode == Mode.PRODUCTS) View.VISIBLE else View.GONE
        summary.text = when {
            lists.isEmpty() -> "Crie listas e registre preços para começar a comparar."
            mode == Mode.LISTS && lists.size < 2 -> "Você precisa de pelo menos duas listas para comparar."
            mode == Mode.PRODUCTS && productNames.isEmpty() -> "Nenhum produto cadastrado ainda."
            mode == Mode.LISTS -> "Escolha duas listas."
            else -> "Escolha um produto para ver o histórico de preços."
        }
        adapter.submitList(emptyList())
    }

    private fun compareLists() {
        if (lists.size < 2) return
        val first = lists.getOrNull(listA.selectedItemPosition) ?: return
        val second = lists.getOrNull(listB.selectedItemPosition) ?: return
        if (first.id == second.id) {
            summary.text = "Escolha duas listas diferentes."
            adapter.submitList(emptyList())
            return
        }

        val firstMap = first.items.groupBy { normalize(it.name) }.mapValues { it.value.first() }
        val secondMap = second.items.groupBy { normalize(it.name) }.mapValues { it.value.first() }
        val keys = (firstMap.keys + secondMap.keys).distinct().sorted()
        val rows = keys.map { key -> compareProduct(firstMap[key], secondMap[key], first.name, second.name) }
        val difference = second.estimatedTotal - first.estimatedTotal
        val diffText = when {
            abs(difference) < 0.005 -> "totais iguais"
            difference > 0 -> "${currency.format(difference)} a mais na segunda lista"
            else -> "${currency.format(abs(difference))} a menos na segunda lista"
        }
        summary.text = "${first.name}: ${currency.format(first.estimatedTotal)}\n${second.name}: ${currency.format(second.estimatedTotal)}\n$diffText"
        adapter.submitList(rows)
    }

    private fun compareProduct(first: ShoppingItem?, second: ShoppingItem?, firstName: String, secondName: String): ComparisonRow {
        val title = first?.name ?: second?.name ?: "Produto"
        val firstPrice = first?.takeIf { it.unitPrice > 0 }?.unitPrice
        val secondPrice = second?.takeIf { it.unitPrice > 0 }?.unitPrice
        val firstLabel = firstPrice?.let { "${currency.format(it)}/${first?.unit}" } ?: "sem preço"
        val secondLabel = secondPrice?.let { "${currency.format(it)}/${second?.unit}" } ?: "sem preço"
        val subtitle = "$firstName: $firstLabel • $secondName: $secondLabel"
        val detail = when {
            first == null -> "Só aparece em $secondName"
            second == null -> "Só aparece em $firstName"
            firstPrice == null || secondPrice == null -> "Preço ainda não informado em uma das listas"
            first.unit != second.unit -> "Unidades diferentes (${first.unit} e ${second.unit}); o aplicativo não calcula diferença direta."
            abs(firstPrice - secondPrice) < 0.005 -> "Mesmo preço unitário"
            secondPrice > firstPrice -> "${currency.format(secondPrice - firstPrice)} mais caro na segunda lista"
            else -> "${currency.format(firstPrice - secondPrice)} mais barato na segunda lista"
        }
        return ComparisonRow(title, subtitle, detail)
    }

    private fun showProductHistory() {
        if (productNames.isEmpty()) return
        val selected = product.selectedItem?.toString() ?: return
        val key = normalize(selected)
        val rows = lists.mapNotNull { list ->
            val item = list.items.firstOrNull { normalize(it.name) == key } ?: return@mapNotNull null
            ComparisonRow(
                title = list.name,
                subtitle = "${date.format(Date(list.createdAt))} • ${priceLabel(item.unitPrice.takeIf { it > 0 })} por ${item.unit}",
                detail = "${formatQuantity(item.quantity)} ${item.unit} • subtotal ${currency.format(item.subtotal)}"
            )
        }
        val prices = rows.size
        summary.text = "$selected • encontrado em $prices ${if (prices == 1) "lista" else "listas"}"
        adapter.submitList(rows)
    }

    private fun priceLabel(price: Double?): String = price?.let(currency::format) ?: "sem preço"

    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("pt", "BR"), "%.2f", value).trimEnd('0').trimEnd(',')

    private fun normalize(value: String): String = value.trim().lowercase(Locale("pt", "BR"))

    private enum class Mode { LISTS, PRODUCTS }
}
