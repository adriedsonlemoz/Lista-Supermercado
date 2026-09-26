package com.listamercado.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.listamercado.app.BuildConfig
import com.listamercado.app.R
import com.listamercado.app.data.BackupRepository
import com.listamercado.app.data.MarketPreferencesRepository
import com.listamercado.app.data.ProductCatalogRepository
import com.listamercado.app.data.SettingsRepository
import com.listamercado.app.data.ShoppingRepository
import com.listamercado.app.data.TemplateRepository
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: SettingsRepository
    private lateinit var appearanceValue: TextView
    private lateinit var backupRepository: BackupRepository

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { exportJsonBackup(it) } }

    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { exportCsv(it) } }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { readBackupForImport(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_settings)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootSettings))
        settings = SettingsRepository(this)
        backupRepository = BackupRepository(
            ShoppingRepository(this),
            ProductCatalogRepository(this),
            TemplateRepository(this),
            MarketPreferencesRepository(this)
        )
        appearanceValue = findViewById(R.id.textAppearanceValue)
        appearanceValue.text = ThemeController.label(settings.themeMode())

        findViewById<ImageButton>(R.id.buttonBackSettings).setOnClickListener { finish() }
        findViewById<View>(R.id.rowAppearance).setOnClickListener { chooseAppearance() }
        findViewById<View>(R.id.rowCatalog).setOnClickListener {
            startActivity(android.content.Intent(this, CatalogActivity::class.java))
        }
        findViewById<View>(R.id.rowExportBackup).setOnClickListener {
            exportBackupLauncher.launch("MeuSupermercado-backup-${fileDate()}.json")
        }
        findViewById<View>(R.id.rowImportBackup).setOnClickListener {
            importBackupLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
        }
        findViewById<View>(R.id.rowExportCsv).setOnClickListener {
            exportCsvLauncher.launch("MeuSupermercado-dados-${fileDate()}.csv")
        }
        findViewById<View>(R.id.rowChanges).setOnClickListener { showChanges() }
        findViewById<View>(R.id.rowDonation).setOnClickListener { showDonation() }
        findViewById<View>(R.id.rowAbout).setOnClickListener { showAbout() }
    }

    private fun chooseAppearance() {
        val modes = arrayOf("Seguir sistema", "Claro", "Escuro")
        val values = arrayOf(SettingsRepository.THEME_SYSTEM, SettingsRepository.THEME_LIGHT, SettingsRepository.THEME_DARK)
        val checked = values.indexOf(settings.themeMode()).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle("Aparência")
            .setSingleChoiceItems(modes, checked) { dialog, which ->
                val mode = values[which]
                settings.setThemeMode(mode)
                appearanceValue.text = ThemeController.label(mode)
                dialog.dismiss()
                ThemeController.applyMode(mode)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportJsonBackup(uri: Uri) {
        runCatching {
            val json = backupRepository.createJsonBackup()
            contentResolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(json)
            } ?: error("Não foi possível abrir o arquivo escolhido.")
        }.onSuccess {
            message("Backup exportado com sucesso")
        }.onFailure {
            showFileError("Não foi possível exportar o backup", it)
        }
    }

    private fun exportCsv(uri: Uri) {
        runCatching {
            val csv = backupRepository.createCsvExport()
            contentResolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(csv)
            } ?: error("Não foi possível abrir o arquivo escolhido.")
        }.onSuccess {
            message("CSV exportado com sucesso")
        }.onFailure {
            showFileError("Não foi possível exportar o CSV", it)
        }
    }

    private fun readBackupForImport(uri: Uri) {
        runCatching {
            val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Não foi possível abrir o arquivo selecionado.")
            backupRepository.parseAndValidate(raw)
        }.onSuccess { snapshot ->
            showImportSummary(snapshot)
        }.onFailure {
            showFileError("Backup inválido", it)
        }
    }

    private fun showImportSummary(snapshot: BackupRepository.BackupSnapshot) {
        val summary = snapshot.summary
        val message = buildString {
            append("Arquivo validado.\n\n")
            append("Listas: ${summary.listCount}\n")
            append("Itens: ${summary.itemCount}\n")
            append("Produtos no catálogo: ${summary.productCount}\n")
            append("Registros de preço: ${summary.priceRecordCount}\n")
            append("Modelos personalizados: ${summary.templateCount}\n")
            append("Mercados favoritos: ${summary.favoriteMarketCount}\n\n")
            append("Mesclar mantém os dados atuais e acrescenta/atualiza os do backup. Substituir troca listas, catálogo e os dados adicionais disponíveis no schema do arquivo.")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Importar backup")
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Substituir") { _, _ -> confirmReplace(snapshot) }
            .setPositiveButton("Mesclar") { _, _ -> performImport(snapshot, replace = false) }
            .show()
    }

    private fun confirmReplace(snapshot: BackupRepository.BackupSnapshot) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Substituir dados atuais?")
            .setMessage("As listas e o catálogo atuais serão substituídos pelos dados deste backup. Backups mais novos também restauram modelos personalizados e preferências de mercados. Esta ação não pode ser desfeita pelo aplicativo.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Substituir") { _, _ -> performImport(snapshot, replace = true) }
            .show()
    }

    private fun performImport(snapshot: BackupRepository.BackupSnapshot, replace: Boolean) {
        runCatching {
            if (replace) backupRepository.replaceWith(snapshot) else backupRepository.mergeWith(snapshot)
        }.onSuccess {
            message(if (replace) "Backup restaurado" else "Backup mesclado")
        }.onFailure {
            showFileError("Não foi possível importar o backup", it)
        }
    }

    private fun showFileError(title: String, error: Throwable) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(error.message?.takeIf { it.isNotBlank() } ?: "Ocorreu um erro ao processar o arquivo.")
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun message(text: String) {
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).show()
    }

    private fun fileDate(): String = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date())

    private fun showChanges() {
        val details = WhatsNewContent.changes.joinToString("\n\n") { change ->
            "• ${change.title}\n${change.description}"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Últimas alterações")
            .setMessage("Versão ${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}\n\n$details")
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun showDonation() {
        val pix = PIX_KEY
        MaterialAlertDialogBuilder(this)
            .setTitle("Doação via Pix")
            .setMessage("Se quiser apoiar o desenvolvimento:\n\n$pix")
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Copiar Pix") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Chave Pix", pix))
                Snackbar.make(findViewById(android.R.id.content), "Chave Pix copiada", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Lista de Mercado")
            .setMessage(
                "Versão ${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}\n\n" +
                    "Organize compras em listas separadas, registre preços e compare quanto cada produto custou em compras diferentes."
            )
            .setPositiveButton("Fechar", null)
            .show()
    }

    companion object {
        private const val PIX_KEY = "adriedson@outlook.com"
    }
}
