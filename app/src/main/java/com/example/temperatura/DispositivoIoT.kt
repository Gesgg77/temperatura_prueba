package com.example.temperatura

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

internal data class Medicion(
    val temperatura: Double,
    val humedad: Double,
    val instante: Long = System.currentTimeMillis()
) {
    val esValida: Boolean
        get() = temperatura.isFinite() && temperatura >= -273.15 && humedad in 0.0..100.0
}

internal enum class Conexion { PENDIENTE, CONECTANDO, CONECTADA, DESCONECTADA }
internal enum class EstadoActuador { ACTIVADO, DESACTIVADO }

internal data class EstadoIoT(
    val conexion: Conexion,
    val medicion: Medicion? = null,
    val actuador: EstadoActuador? = null
)

internal interface DispositivoIoT {
    val estados: Flow<EstadoIoT>

    // Devuelve el estado comunicado por el dispositivo en respuesta a esta solicitud.
    // Devuelve null si no hay confirmación válida. Escribir el comando no basta.
    suspend fun solicitarActivacion(activar: Boolean): EstadoActuador?
}

internal class DispositivoSimulado : DispositivoIoT {
    private val actuador = MutableStateFlow<EstadoActuador?>(null)

    override val estados: Flow<EstadoIoT> = flow {
        emit(EstadoIoT(Conexion.CONECTANDO))
        delay(450)
        val muestras = listOf(22.4 to 53.0, 22.7 to 52.0, 23.1 to 51.0)
        var indice = 0
        while (true) {
            val (temperatura, humedad) = muestras[indice]
            emit(EstadoIoT(Conexion.CONECTADA, Medicion(temperatura, humedad)))
            indice = (indice + 1) % muestras.size
            delay(2_000)
        }
    }.combine(actuador) { estado, confirmado -> estado.copy(actuador = confirmado) }

    override suspend fun solicitarActivacion(activar: Boolean): EstadoActuador {
        delay(650)
        val confirmado = if (activar) EstadoActuador.ACTIVADO else EstadoActuador.DESACTIVADO
        actuador.value = confirmado
        return confirmado
    }
}


