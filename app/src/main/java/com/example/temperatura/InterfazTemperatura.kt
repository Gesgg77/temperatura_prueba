package com.example.temperatura

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

private val formaPanel = RoundedCornerShape(24.dp)

@Composable
internal fun ColumnaPantalla(modifier: Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = contenido
        )
    }
}

@Composable
internal fun CabeceraTemperatura(titulo: String, detalle: String) {
    val fondo = Brush.linearGradient(listOf(Color(0xFF082F2B), Color(0xFF146B5A)))
    Box(Modifier.fillMaxWidth().clip(formaPanel).background(fondo)) {
        Canvas(Modifier.matchParentSize()) {
            val centro = Offset(size.width * 0.95f, size.height * 0.18f)
            for (radio in listOf(42.dp, 65.dp, 88.dp)) {
                drawCircle(Color.White.copy(alpha = 0.13f), radio.toPx(), centro, style = Stroke(1.dp.toPx()))
            }
        }
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconoMedicion(humedad = false, tinta = Color(0xFFBFF7E7), fondo = Color(0xFF225E53))
            Text(
                titulo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC9F3E8),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun Panel(contenido: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = formaPanel) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido
        )
    }
}

@Composable
internal fun PanelMonitoreo(
    usuario: String,
    modifier: Modifier,
    estado: EstadoIoT,
    temperatura: String?,
    humedad: String?,
    simulacion: Boolean,
    lecturas: List<LecturaGuardada>,
    errorHistorial: Boolean,
    esperando: Boolean,
    aviso: String?,
    controlesDisponibles: Boolean,
    enviar: (Boolean) -> Unit,
    alternarModo: () -> Unit,
    salir: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    ColumnaPantalla(modifier) {
        CabeceraTemperatura("Monitoreo", "Sesión de $usuario")
        Panel {
            Text(
                if (simulacion) "Modo simulación · datos de prueba" else "Modo Arduino",
                style = MaterialTheme.typography.titleMedium
            )
            val conexion = when (estado.conexion) {
                Conexion.PENDIENTE -> "Conexión pendiente de configurar"
                Conexion.CONECTANDO -> "Conectando…"
                Conexion.CONECTADA -> if (simulacion) "Simulador conectado" else "Arduino conectado"
                Conexion.DESCONECTADA -> "Desconectado"
            }
            EtiquetaEstado(conexion, estado.conexion == Conexion.CONECTADA)
            if (estado.conexion == Conexion.PENDIENTE) {
                Text(
                    "Prueba el panel con datos simulados mientras preparas la conexión.",
                    color = colores.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(onClick = alternarModo, enabled = !esperando) {
                Text(if (simulacion) "Volver al modo Arduino" else "Probar simulación")
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.25f) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaMedicion(false, temperatura, Modifier.fillMaxWidth())
                    TarjetaMedicion(true, humedad, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaMedicion(false, temperatura, Modifier.weight(1f))
                    TarjetaMedicion(true, humedad, Modifier.weight(1f))
                }
            }
        }
        Panel {
            Text("Actuador", style = MaterialTheme.typography.titleLarge)
            EtiquetaEstado(
                texto = when (estado.actuador) {
                    EstadoActuador.ACTIVADO -> "Último estado confirmado · Activado"
                    EstadoActuador.DESACTIVADO -> "Último estado confirmado · Desactivado"
                    null -> "Estado sin confirmar"
                },
                positivo = estado.actuador == EstadoActuador.ACTIVADO
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { enviar(true) },
                    enabled = controlesDisponibles,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Activar") }
                OutlinedButton(
                    onClick = { enviar(false) },
                    enabled = controlesDisponibles,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Desactivar") }
            }
            if (esperando) LinearProgressIndicator(Modifier.fillMaxWidth())
            val mensaje = if (esperando) "Esperando confirmación…" else aviso
            mensaje?.let {
                Text(
                    it,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = colores.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        PanelHistorial(lecturas, errorHistorial)
        TextButton(onClick = salir, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun PanelHistorial(lecturas: List<LecturaGuardada>, error: Boolean) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val fechas = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
    val numeros = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 } }
    Panel {
        Text("Historial", style = MaterialTheme.typography.titleLarge)
        val resumen = when {
            error -> "No se pudo actualizar el historial."
            lecturas.isEmpty() -> "Aún no hay lecturas guardadas."
            else -> "Lecturas recientes guardadas en este teléfono."
        }
        Text(resumen, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (lecturas.isNotEmpty()) {
            OutlinedButton(onClick = { abierto = !abierto }) {
                Text(if (abierto) "Ocultar lecturas" else "Ver lecturas")
            }
            if (abierto) {
                lecturas.forEach { lectura ->
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "${numeros.format(lectura.temperatura)} °C · ${numeros.format(lectura.humedad)} %",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val origen = if (lectura.simulada) "Simulación" else "Arduino"
                        Text(
                            "${fechas.format(Date(lectura.instante))} · $origen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EtiquetaEstado(texto: String, positivo: Boolean) {
    val colores = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (positivo) colores.primaryContainer else colores.surfaceVariant,
        contentColor = if (positivo) colores.onPrimaryContainer else colores.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(7.dp).background(
                if (positivo) colores.primary else colores.outline, CircleShape
            ))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TarjetaMedicion(humedad: Boolean, valor: String?, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    val fondo = if (humedad) colores.tertiaryContainer else colores.secondaryContainer
    val tinta = if (humedad) colores.onTertiaryContainer else colores.onSecondaryContainer
    val unidad = if (humedad) "%" else "°C"
    Surface(modifier, shape = formaPanel, color = fondo, contentColor = tinta) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconoMedicion(humedad, tinta, tinta.copy(alpha = 0.10f))
            Text(if (humedad) "Humedad" else "Temperatura", style = MaterialTheme.typography.labelLarge)
            Text(
                if (valor == null) "— $unidad" else "$valor $unidad",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (valor == null) Text("Sin lectura", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun IconoMedicion(humedad: Boolean, tinta: Color, fondo: Color) {
    Canvas(Modifier.size(42.dp).background(fondo, RoundedCornerShape(13.dp)).padding(10.dp)) {
        val ancho = size.width
        val alto = size.height
        if (humedad) {
            val gota = Path().apply {
                moveTo(ancho * 0.5f, alto * 0.08f)
                quadraticTo(ancho * 0.84f, alto * 0.49f, ancho * 0.84f, alto * 0.65f)
                quadraticTo(ancho * 0.84f, alto * 0.93f, ancho * 0.5f, alto * 0.93f)
                quadraticTo(ancho * 0.16f, alto * 0.93f, ancho * 0.16f, alto * 0.65f)
                quadraticTo(ancho * 0.16f, alto * 0.49f, ancho * 0.5f, alto * 0.08f)
                close()
            }
            drawPath(gota, tinta, style = Stroke(2.dp.toPx()))
        } else {
            drawLine(tinta, Offset(ancho * 0.5f, alto * 0.13f), Offset(ancho * 0.5f, alto * 0.75f),
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(tinta, ancho * 0.18f, Offset(ancho * 0.5f, alto * 0.75f))
            drawLine(tinta, Offset(ancho * 0.66f, alto * 0.25f), Offset(ancho * 0.86f, alto * 0.25f),
                strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(tinta, Offset(ancho * 0.66f, alto * 0.43f), Offset(ancho * 0.79f, alto * 0.43f),
                strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

