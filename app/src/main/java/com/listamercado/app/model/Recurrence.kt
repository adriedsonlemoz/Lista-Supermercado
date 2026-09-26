package com.listamercado.app.model

object Recurrence {
    const val WEEKLY = "weekly"
    const val BIWEEKLY = "biweekly"
    const val MONTHLY = "monthly"

    fun label(value: String?): String? = when (value) {
        WEEKLY -> "Semanal"
        BIWEEKLY -> "Quinzenal"
        MONTHLY -> "Mensal"
        else -> null
    }

    fun values(): Array<String> = arrayOf(WEEKLY, BIWEEKLY, MONTHLY)
    fun labels(): Array<String> = arrayOf("Semanal", "Quinzenal", "Mensal")
}
