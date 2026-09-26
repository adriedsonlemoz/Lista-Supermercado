package com.listamercado.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.listamercado.app.R
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.data.TemplateRepository
import com.listamercado.app.model.ListTemplate
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController

class TemplatesActivity : AppCompatActivity() {
    private lateinit var templateRepository: TemplateRepository
    private lateinit var shoppingRepository: ShoppingRepository
    private lateinit var adapter: TemplateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_templates)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootTemplates), findViewById(R.id.recyclerTemplates))
        templateRepository = TemplateRepository(this)
        shoppingRepository = ShoppingRepository(this)
        bindViews()
        render()
    }

    private fun bindViews() {
        adapter = TemplateAdapter(
            onUse = { useTemplate(it) },
            onDelete = { confirmDelete(it) }
        )
        findViewById<RecyclerView>(R.id.recyclerTemplates).apply {
            layoutManager = LinearLayoutManager(this@TemplatesActivity)
            adapter = this@TemplatesActivity.adapter
        }
        findViewById<ImageButton>(R.id.buttonBackTemplates).setOnClickListener { finish() }
    }

    private fun render() {
        val templates = templateRepository.loadTemplates()
        adapter.submitList(templates)
        val userCount = templates.count { !it.builtIn }
        findViewById<TextView>(R.id.textTemplatesCount).text =
            "5 modelos iniciais${if (userCount > 0) " • $userCount ${if (userCount == 1) "modelo seu" else "modelos seus"}" else ""}"
    }

    private fun useTemplate(template: ListTemplate) {
        ListDialog.show(
            context = this,
            initialName = template.name,
            titleOverride = "Criar lista pelo modelo",
            subtitleOverride = "Revise o nome antes de criar a nova lista."
        ) { listName ->
            val newList = templateRepository.createListFromTemplate(template, listName)
            val lists = shoppingRepository.loadLists()
            lists.add(0, newList)
            shoppingRepository.saveLists(lists)
            ProductCatalogRepository(this).seedFromLists(lists)
            startActivity(Intent(this, ListDetailActivity::class.java).putExtra(ListDetailActivity.EXTRA_LIST_ID, newList.id))
            finish()
        }
    }

    private fun confirmDelete(template: ListTemplate) {
        if (template.builtIn) return
        ConfirmDialog.showDestructive(
            context = this,
            title = "Excluir modelo ${template.name}?",
            message = "As listas já criadas com este modelo não serão alteradas.",
            confirmLabel = "Excluir"
        ) {
            templateRepository.deleteUserTemplate(template.id)
            render()
        }
    }
}
