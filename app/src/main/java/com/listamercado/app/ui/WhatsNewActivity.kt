package com.listamercado.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.listamercado.app.BuildConfig
import com.listamercado.app.R
import com.listamercado.app.data.SettingsRepository
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController

class WhatsNewActivity : AppCompatActivity() {
    private lateinit var settings: SettingsRepository
    private var acknowledged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_whats_new)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootWhatsNew))
        settings = SettingsRepository(this)

        // If another instance was restored after this version had already been accepted,
        // close it immediately instead of showing duplicate release notes.
        if (settings.lastSeenWhatsNewVersionCode() >= BuildConfig.VERSION_CODE) {
            finish()
            return
        }

        findViewById<TextView>(R.id.textWhatsNewTitle).text = WhatsNewContent.TITLE
        findViewById<TextView>(R.id.textWhatsNewVersion).text =
            "Versão ${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
        findViewById<TextView>(R.id.textWhatsNewSubtitle).text = WhatsNewContent.SUBTITLE

        val container = findViewById<LinearLayout>(R.id.containerWhatsNew)
        val inflater = LayoutInflater.from(this)
        WhatsNewContent.changes.forEach { change ->
            val card = inflater.inflate(R.layout.item_whats_new, container, false)
            card.findViewById<TextView>(R.id.textChangeTitle).text = change.title
            card.findViewById<TextView>(R.id.textChangeDescription).text = change.description
            container.addView(card)
        }

        findViewById<android.view.View>(R.id.buttonContinueWhatsNew).setOnClickListener {
            acknowledgeAndClose()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun acknowledgeAndClose() {
        if (acknowledged) return
        acknowledged = true
        val saved = settings.markWhatsNewSeen(BuildConfig.VERSION_CODE)
        if (saved) {
            finish()
        } else {
            acknowledged = false
            Snackbar.make(
                findViewById(android.R.id.content),
                "Não foi possível salvar a confirmação. Tente novamente.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}
