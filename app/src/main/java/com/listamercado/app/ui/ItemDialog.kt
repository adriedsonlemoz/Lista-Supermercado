package com.listamercado.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.listamercado.app.R
import com.listamercado.app.data.BarcodeLookupRepository
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.model.BarcodeLookupResult
import com.listamercado.app.model.CatalogProduct
import com.listamercado.app.model.ShoppingItem
import com.listamercado.app.util.CurrencyTextWatcher
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.PriceUnitHelper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ItemDialog {
    private val categories = listOf(
        "Hortifruti", "Açougue", "Padaria", "Laticínios", "Mercearia",
        "Bebidas", "Congelados", "Limpeza", "Higiene", "Pet", "Outros"
    )
    private val units = listOf("un", "kg", "g", "L", "mL", "pct", "pacote", "lata", "cx")

    fun show(
        context: Context,
        catalogRepository: ProductCatalogRepository,
        existing: ShoppingItem? = null,
        initialBarcode: String? = null,
        onRequestBarcode: (((String) -> Unit) -> Unit)? = null,
        onSave: (ShoppingItem, String?) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_item, null)
        val lookupRepository = BarcodeLookupRepository(context)
        val sheet = view.findViewById<View>(R.id.sheetItemRoot)
        val title = view.findViewById<TextView>(R.id.textSheetTitle)
        val subtitle = view.findViewById<TextView>(R.id.textSheetSubtitle)
        val nameHelper = view.findViewById<TextView>(R.id.textNameHelper)
        val barcodeStatus = view.findViewById<TextView>(R.id.textBarcodeStatus)
        val lookupStatus = view.findViewById<TextView>(R.id.textLookupStatus)
        val lookupProgress = view.findViewById<LinearProgressIndicator>(R.id.progressLookup)
        val nameLayout = view.findViewById<TextInputLayout>(R.id.layoutName)
        val quantityLayout = view.findViewById<TextInputLayout>(R.id.layoutQuantity)
        val priceLayout = view.findViewById<TextInputLayout>(R.id.layoutPrice)
        val name = view.findViewById<MaterialAutoCompleteTextView>(R.id.inputName)
        val quantity = view.findViewById<TextInputEditText>(R.id.inputQuantity)
        val price = view.findViewById<TextInputEditText>(R.id.inputPrice)
        val note = view.findViewById<TextInputEditText>(R.id.inputNote)
        val category = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerCategory)
        val unit = view.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerUnit)
        val buttonScan = view.findViewById<MaterialButton>(R.id.buttonScanBarcode)
        val buttonCancel = view.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonSave = view.findViewById<MaterialButton>(R.id.buttonSave)
        val buttonClose = view.findViewById<android.widget.ImageButton>(R.id.buttonCloseSheet)
        val priceWatcher = CurrencyTextWatcher(price)
        price.addTextChangedListener(priceWatcher)

        val knownProducts = catalogRepository.suggestions()
        val productByDisplayedName = knownProducts.associateBy { it.name }
        name.setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_dropdown_item_1line,
                knownProducts.map { it.name }
            )
        )

        title.text = if (existing == null) "Adicionar item" else "Editar item"
        subtitle.text = if (existing == null) {
            "Escolha do catálogo, escaneie o código ou cadastre um produto novo."
        } else {
            "Altere os dados sem perder o histórico da lista."
        }

        category.setSimpleItems(categories.toTypedArray())
        unit.setSimpleItems(units.toTypedArray())
        category.setText(categories.first(), false)
        unit.setText(units.first(), false)

        var selectedBarcode: String? = existing
            ?.let { catalogRepository.findByName(it.name)?.barcode }
        var scannedUnknownBarcode: String? = null
        var dialogOpen = true
        var pendingLookupBarcode: String? = null
        var suppressNameWatcher = false
        var statusMessage: String? = null

        fun setLookupStatus(message: String?, visibleWhenEmpty: Boolean = false) {
            statusMessage = message
            lookupStatus.text = message.orEmpty()
            lookupStatus.visibility = if (message.isNullOrBlank() && !visibleWhenEmpty) View.GONE else View.VISIBLE
        }

        fun setLookupLoading(isLoading: Boolean) {
            lookupProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                setLookupStatus("Consultando nome do produto...", visibleWhenEmpty = true)
            }
        }

        fun renderBarcodeStatus(isNew: Boolean = false, fromCatalog: Boolean = false) {
            val code = selectedBarcode.orEmpty()
            barcodeStatus.text = when {
                code.isBlank() -> "Sem código associado"
                fromCatalog -> "Código $code • catálogo local"
                isNew -> "Código $code • novo produto"
                else -> "Código associado • $code"
            }
        }

        fun applyCatalogProduct(product: CatalogProduct) {
            pendingLookupBarcode = null
            setLookupLoading(false)
            suppressNameWatcher = true
            name.setText(product.name, false)
            suppressNameWatcher = false
            category.setText(product.category.ifBlank { categories.first() }, false)
            unit.setText(product.unit.ifBlank { units.first() }, false)
            priceLayout.hint = PriceUnitHelper.inputHint(product.unit)
            if (product.lastUnitPrice > 0.0) {
                priceWatcher.setAmount(PriceUnitHelper.editorAmount(product.lastUnitPrice, product.unit))
            }
            selectedBarcode = scannedUnknownBarcode ?: product.barcode
            renderBarcodeStatus(isNew = scannedUnknownBarcode != null, fromCatalog = scannedUnknownBarcode == null)
            nameHelper.text = "Produto conhecido. Toque numa sugestão para preencher automaticamente."
            setLookupStatus("Preenchido pelo catálogo local.")
        }

        fun applyOnlineLookup(result: BarcodeLookupResult) {
            val currentName = name.text?.toString()?.trim().orEmpty()
            if (currentName.isBlank()) {
                suppressNameWatcher = true
                name.setText(result.name, false)
                suppressNameWatcher = false
            }
            val currentCategory = category.text?.toString()?.trim().orEmpty()
            if ((currentCategory.isBlank() || currentCategory == categories.first() || currentCategory == "Outros")
                && !result.category.isNullOrBlank()
            ) {
                category.setText(result.category, false)
            }
            nameHelper.text = "Dados externos são opcionais: revise o nome antes de salvar."
            val extras = buildList {
                result.brand?.takeIf { it.isNotBlank() }?.let { add(it) }
                result.quantityDescription?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" • ")
            val suffix = if (extras.isNotBlank()) ": $extras" else ""
            setLookupStatus("Encontrado online em ${result.source}. Revise antes de salvar$suffix")
        }

        fun lookupBarcodeOnline(barcode: String) {
            pendingLookupBarcode = barcode
            setLookupLoading(true)
            lookupRepository.lookup(barcode) { result ->
                if (!dialogOpen || pendingLookupBarcode != barcode) return@lookup
                setLookupLoading(false)
                result
                    .onSuccess { lookup ->
                        if (lookup == null) {
                            setLookupStatus("Código $barcode lido. Nome não encontrado automaticamente; preencha o produto e salve para reconhecê-lo nas próximas vezes.")
                        } else {
                            applyOnlineLookup(lookup)
                        }
                    }
                    .onFailure {
                        setLookupStatus("Código $barcode lido. A consulta online falhou, mas você pode preencher e salvar normalmente.")
                    }
            }
        }

        existing?.let {
            suppressNameWatcher = true
            name.setText(it.name, false)
            suppressNameWatcher = false
            quantity.setText(it.quantity.toInput())
            priceWatcher.setAmount(PriceUnitHelper.editorAmount(it.unitPrice, it.unit))
            note.setText(it.note)
            category.setText(it.category, false)
            unit.setText(it.unit, false)
            priceLayout.hint = PriceUnitHelper.inputHint(it.unit)
        } ?: run {
            quantity.setText("1")
            quantity.setSelection(quantity.text?.length ?: 0)
            priceLayout.hint = PriceUnitHelper.inputHint(units.first())
        }
        renderBarcodeStatus()

        name.doAfterTextChanged {
            if (!suppressNameWatcher) {
                nameLayout.error = null
                if (pendingLookupBarcode != null && statusMessage?.contains("Consultando") == true) {
                    setLookupStatus("Lendo código... você pode ajustar o nome manualmente se quiser.", visibleWhenEmpty = true)
                }
            }
        }

        name.setOnItemClickListener { _, _, position, _ ->
            val selectedName = name.adapter.getItem(position)?.toString().orEmpty()
            val product = productByDisplayedName[selectedName] ?: catalogRepository.findByName(selectedName)
            if (product != null) applyCatalogProduct(product)
        }

        name.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                catalogRepository.findByName(name.text?.toString().orEmpty())?.let { product ->
                    if (existing == null) applyCatalogProduct(product)
                }
            }
        }

        unit.setOnItemClickListener { _, _, _, _ ->
            priceLayout.hint = PriceUnitHelper.inputHint(unit.text?.toString().orEmpty())
        }

        fun handleBarcode(rawBarcode: String) {
            val barcode = ProductCatalogRepository.sanitizeBarcode(rawBarcode)
            if (barcode == null) {
                setLookupStatus("A leitura não retornou um código válido. Tente novamente.", visibleWhenEmpty = true)
                return
            }
            val product = catalogRepository.findByBarcode(barcode)
            selectedBarcode = barcode
            if (product != null) {
                scannedUnknownBarcode = null
                pendingLookupBarcode = null
                applyCatalogProduct(product)
            } else {
                scannedUnknownBarcode = barcode
                renderBarcodeStatus(isNew = true)
                setLookupStatus("Código $barcode reconhecido. Tentando identificar o produto online...", visibleWhenEmpty = true)
                lookupBarcodeOnline(barcode)
            }
        }

        buttonScan.isEnabled = onRequestBarcode != null
        buttonScan.alpha = if (buttonScan.isEnabled) 1f else 0.5f
        buttonScan.setOnClickListener {
            val request = onRequestBarcode ?: return@setOnClickListener
            request { rawBarcode -> handleBarcode(rawBarcode) }
        }

        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = true
        InsetsHelper.applyBottomSheetInsets(sheet)
        dialog.setOnDismissListener { dialogOpen = false }

        buttonClose.setOnClickListener { dialog.dismiss() }
        buttonCancel.setOnClickListener { dialog.dismiss() }
        buttonSave.setOnClickListener {
            nameLayout.error = null
            quantityLayout.error = null
            priceLayout.error = null

            val itemName = name.text?.toString()?.trim().orEmpty()
            if (itemName.isBlank()) {
                nameLayout.error = "Digite o nome do item"
                name.requestFocus()
                return@setOnClickListener
            }

            val parsedQuantity = quantity.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
            if (parsedQuantity == null || parsedQuantity <= 0.0) {
                quantityLayout.error = "Informe uma quantidade válida"
                quantity.requestFocus()
                return@setOnClickListener
            }

            val unitValue = unit.text?.toString()?.ifBlank { units.first() } ?: units.first()
            val categoryValue = category.text?.toString()?.ifBlank { categories.first() } ?: categories.first()

            val item = (existing?.copy() ?: ShoppingItem(name = itemName)).apply {
                this.name = itemName
                this.quantity = parsedQuantity
                this.unitPrice = PriceUnitHelper.internalAmount(
                    priceWatcher.amount().coerceAtLeast(0.0),
                    unitValue
                )
                this.category = categoryValue
                this.unit = unitValue
                this.note = note.text?.toString()?.trim().orEmpty()
            }
            onSave(item, selectedBarcode)
            dialog.dismiss()
        }
        dialog.show()
        initialBarcode?.let { barcode ->
            view.post { handleBarcode(barcode) }
        }
    }

    private fun Double.toInput(): String {
        val symbols = DecimalFormatSymbols(Locale("pt", "BR"))
        return DecimalFormat("0.##", symbols).format(this)
    }
}
