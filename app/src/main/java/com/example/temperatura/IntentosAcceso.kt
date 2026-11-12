package com.example.temperatura

internal data class IntentosAcceso(val fallos: Int = 0, val nivel: Int = 0) {
    val restantes: Int get() = MAX_FALLOS - fallos

    fun registrarFallo(): IntentosAcceso =
        if (restantes == 1) IntentosAcceso(nivel = (nivel + 1).coerceAtMost(MAX_NIVEL))
        else copy(fallos = fallos + 1)

    val esperaNueva: Long
        get() = if (fallos != 0 || nivel == 0) 0L
        else (60_000L shl (nivel - 1)).coerceAtMost(3_600_000L)

    companion object {
        private const val MAX_FALLOS = 5
        private const val MAX_NIVEL = 7

        fun restante(
            duracion: Long, inicio: Long, ahora: Long, arranqueIgual: Boolean
        ): Long = if (!arranqueIgual || inicio > ahora) duracion
        else (duracion - (ahora - inicio)).coerceAtLeast(0)
    }
}
