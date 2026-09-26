package com.listamercado.app.util

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

object InsetsHelper {
    fun applyScaffold(activity: Activity, root: View, scrollable: View? = null, fab: View? = null) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val rootPadding = root.paddingState()
        val scrollPadding = scrollable?.paddingState()
        val fabMargins = fab?.marginState()

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, ime.bottom)
            root.updatePadding(
                left = rootPadding.left + systemBars.left,
                top = rootPadding.top + systemBars.top,
                right = rootPadding.right + systemBars.right,
                bottom = rootPadding.bottom
            )

            scrollable?.let { view ->
                val extraFabSpace = if (fab != null) view.resources.dp(96) else 0
                view.updatePadding(
                    left = scrollPadding?.left ?: view.paddingLeft,
                    top = scrollPadding?.top ?: view.paddingTop,
                    right = scrollPadding?.right ?: view.paddingRight,
                    bottom = (scrollPadding?.bottom ?: view.paddingBottom) + bottomInset + extraFabSpace
                )
            }

            fab?.let { view ->
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    leftMargin = fabMargins?.left ?: leftMargin
                    topMargin = fabMargins?.top ?: topMargin
                    rightMargin = (fabMargins?.right ?: rightMargin) + systemBars.right
                    bottomMargin = (fabMargins?.bottom ?: bottomMargin) + systemBars.bottom
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    fun applyBottomSheetInsets(sheet: View) {
        val padding = sheet.paddingState()
        ViewCompat.setOnApplyWindowInsetsListener(sheet) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.updatePadding(
                left = padding.left + bars.left,
                top = padding.top,
                right = padding.right + bars.right,
                bottom = padding.bottom + bars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(sheet)
    }

    private fun View.paddingState(): Insets = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)

    private fun View.marginState(): Insets {
        val lp = layoutParams as? ViewGroup.MarginLayoutParams
        return Insets.of(lp?.leftMargin ?: 0, lp?.topMargin ?: 0, lp?.rightMargin ?: 0, lp?.bottomMargin ?: 0)
    }

    private fun View.resourcesDp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun android.content.res.Resources.dp(value: Int): Int = (value * displayMetrics.density).toInt()
}
