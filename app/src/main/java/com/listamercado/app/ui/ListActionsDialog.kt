package com.listamercado.app.ui

import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.listamercado.app.R
import com.listamercado.app.util.InsetsHelper

object ListActionsDialog {
    fun show(
        activity: AppCompatActivity,
        listName: String,
        onRename: () -> Unit,
        onDuplicate: () -> Unit,
        onSaveTemplate: () -> Unit,
        onDelete: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_list_actions, null)
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        InsetsHelper.applyBottomSheetInsets(view.findViewById(R.id.sheetListActionsRoot))

        view.findViewById<TextView>(R.id.textListActionsTitle).text = listName
        view.findViewById<MaterialButton>(R.id.buttonActionRename).setOnClickListener {
            dialog.dismiss()
            onRename()
        }
        view.findViewById<MaterialButton>(R.id.buttonActionDuplicate).setOnClickListener {
            dialog.dismiss()
            onDuplicate()
        }
        view.findViewById<MaterialButton>(R.id.buttonActionTemplate).setOnClickListener {
            dialog.dismiss()
            onSaveTemplate()
        }
        view.findViewById<MaterialButton>(R.id.buttonActionDelete).setOnClickListener {
            dialog.dismiss()
            onDelete()
        }
        dialog.show()
    }
}
