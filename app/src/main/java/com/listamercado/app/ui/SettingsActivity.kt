package com.listamercado.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.listamercado.app.BuildConfig
import com.listamercado.app.R
import com.listamercado.app.data.SettingsRepository
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: SettingsRepository
    private lateinit var appearanceValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_settings)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootSettings))
        settings = SettingsRepository(this)
        appearanceValue = findViewById(R.id.textAppearanceValue)
        appearanceValue.text = ThemeController.label(settings.themeMode())

        findViewById<ImageButton>(R.id.buttonBackSettings).setOnClickListener { finish() }
        findViewById<View>(R.id.rowAppearance).setOnClickListener { chooseAppearance() }
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

    private fun showChanges() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Últimas alterações")
            .setMessage(
                "Versão ${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}\n\n" +
                    "• Nova lista inicial Cicloviagem com 18 itens prontos.\n" +
                    "• A sugestão é criada uma única vez e pode ser editada ou excluída normalmente.\n" +
                    "• Adicionadas unidades pacote e lata ao editor de produtos.\n" +
                    "• Mantidas comparação de listas, histórico de preços, cartões renovados e modo escuro."
            )
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
