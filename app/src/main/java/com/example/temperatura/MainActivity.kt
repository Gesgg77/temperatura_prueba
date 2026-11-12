package com.example.temperatura

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.temperatura.ui.theme.TemperaturaTheme
import java.text.NumberFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private var sesion by mutableStateOf<String?>(null)
    private var enPrimerPlano by mutableStateOf(false)
    private val relojSesion = Handler(Looper.getMainLooper())
    private val expiracion = Runnable { cerrarSesion() }

    override fun onStart() {
        super.onStart()
        enPrimerPlano = true
    }

    override fun onStop() {
        enPrimerPlano = false
        cerrarSesion()
        super.onStop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (sesion != null) renovarSesion()
    }

    private fun renovarSesion() {
        relojSesion.removeCallbacks(expiracion)
        relojSesion.postDelayed(expiracion, 5 * 60_000L)
    }

    private fun cerrarSesion() {
        relojSesion.removeCallbacks(expiracion)
        sesion = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val cuenta = CuentaLocal(applicationContext)
        val historial = HistorialLocal.obtener(applicationContext)
        setContent {
            BackHandler(enabled = sesion != null, onBack = ::cerrarSesion)
            TemperaturaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { margen ->
                    val contenido = Modifier.padding(margen)
                    val usuario = sesion
                    if (!enPrimerPlano) return@Scaffold
                    if (usuario == null) {
                        PantallaLogin(cuenta, contenido) {
                            if (enPrimerPlano) {
                                sesion = it
                                renovarSesion()
                            }
                        }
                    } else {
                        PantallaMonitoreo(
                            usuario, contenido, historial, dispositivoArduino = null, salir = ::cerrarSesion
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PantallaLogin(cuenta: CuentaLocal, modifier: Modifier, ingresar: (String) -> Unit) {
    var registrada by remember { mutableStateOf<Boolean?>(null) }
    var usuario by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var verificando by remember { mutableStateOf(false) }
    var finBloqueo by remember { mutableStateOf(0L) }
    var segundos by remember { mutableStateOf(0L) }
    val bloqueado = finBloqueo != 0L

    fun bloquear(duracion: Long) {
        if (duracion <= 0) return
        segundos = (duracion + 999) / 1000
        finBloqueo = SystemClock.elapsedRealtime() + duracion
    }

    LaunchedEffect(finBloqueo) {
        if (finBloqueo == 0L) return@LaunchedEffect
        while (true) {
            val restante = finBloqueo - SystemClock.elapsedRealtime()
            if (restante <= 0) {
                finBloqueo = 0L
                break
            }
            segundos = (restante + 999) / 1000
            delay(restante % 1000 + 1)
        }
    }
    val tareas = rememberCoroutineScope()

    LaunchedEffect(cuenta) {
        try {
            val (existe, espera) = withContext(Dispatchers.IO) { cuenta.estadoInicial() }
            registrada = existe
            bloquear(espera)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            mensaje = "No se pudo leer la cuenta local."
        }
    }

    ColumnaPantalla(modifier.imePadding()) {
        CabeceraTemperatura("Temperatura", "Monitoreo ambiental")
        Panel {
            val existe = registrada
            Text(
                when (existe) {
                    true -> "Acceso a tu cuenta"
                    false -> "Configura tu acceso"
                    null -> "Cuenta local"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                when (existe) {
                    true -> "Ingresa para abrir el panel de monitoreo."
                    false -> "Crea una cuenta en este dispositivo. Usa al menos 15 caracteres en la contraseña."
                    null -> if (mensaje == null) "Cargando cuenta…" else "Cuenta local no disponible."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (existe == null && mensaje == null) CircularProgressIndicator()
            if (existe != null) {
                OutlinedTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                        mensaje = null
                    },
                    label = { Text("Usuario") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    enabled = !verificando && !bloqueado,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = {
                        contrasena = it
                        mensaje = null
                    },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    enabled = !verificando && !bloqueado,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !verificando && !bloqueado,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    onClick = {
                        if (!verificando && !bloqueado) {
                            val nombre = usuario.trim()
                            mensaje = when {
                                nombre.isBlank() || contrasena.isBlank() -> "Completa ambos campos."
                                nombre.length > 50 -> "El usuario admite hasta 50 caracteres."
                                contrasena.length > 256 || contrasena.codePointCount(0, contrasena.length) > 128 ->
                                    "Usa hasta 128 caracteres en la contraseña."
                                !existe && contrasena.codePointCount(0, contrasena.length) < CuentaLocal.MINIMO_CLAVE ->
                                    "Usa al menos 15 caracteres en la contraseña."
                                else -> null
                            }
                            if (mensaje == null) {
                                verificando = true
                                tareas.launch {
                                    try {
                                        if (!existe) {
                                            val creada = cuenta.registrar(nombre, contrasena)
                                            registrada = true
                                            mensaje = if (creada) "Cuenta creada. Ahora inicia sesión." else "Ya existe una cuenta local."
                                        } else {
                                            when (val resultado = cuenta.autenticar(nombre, contrasena)) {
                                                ResultadoAcceso.Correcto -> ingresar(nombre)
                                                is ResultadoAcceso.Incorrecto ->
                                                    mensaje = "Usuario o contraseña incorrectos. Intentos restantes: ${resultado.restantes}."
                                                is ResultadoAcceso.Bloqueado -> bloquear(resultado.milisegundos)
                                            }
                                        }
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        mensaje = "No se pudo acceder a la cuenta local."
                                    } finally {
                                        contrasena = ""
                                        verificando = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(if (verificando) "Procesando…" else if (existe) "Iniciar sesión" else "Crear cuenta")
                }
            }
            if (bloqueado) Text("Acceso bloqueado temporalmente. Reintenta en $segundos s.")
            mensaje?.let {
                Text(
                    it,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PantallaMonitoreo(
    usuario: String,
    modifier: Modifier,
    historial: HistorialLocal,
    dispositivoArduino: DispositivoIoT?,
    salir: () -> Unit
) {
    var simulacion by remember { mutableStateOf(false) }
    val dispositivo = remember(simulacion, dispositivoArduino) {
        if (simulacion) DispositivoSimulado() else dispositivoArduino
    }
    val estados = remember(dispositivo) {
        dispositivo?.estados ?: flowOf(EstadoIoT(Conexion.PENDIENTE))
    }
    var estado by remember(dispositivo) { mutableStateOf(EstadoIoT(Conexion.PENDIENTE)) }
    var lecturas by remember { mutableStateOf(emptyList<LecturaGuardada>()) }
    var errorHistorial by remember { mutableStateOf(false) }

    LaunchedEffect(dispositivo) {
        try {
            lecturas = historial.recientes()
            errorHistorial = false
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            errorHistorial = true
        }
        var ultimaMedicion: Medicion? = null
        try {
            estados.collect { recibido ->
                val muestra = recibido.medicion?.takeIf {
                    recibido.conexion == Conexion.CONECTADA && it.esValida
                }
                estado = recibido.copy(medicion = muestra)
                if (muestra != null && muestra != ultimaMedicion) {
                    try {
                        lecturas = historial.guardar(muestra, simulacion)
                        ultimaMedicion = muestra
                        errorHistorial = false
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        errorHistorial = true
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            estado = estado.copy(conexion = Conexion.DESCONECTADA, medicion = null)
        }
    }
    var esperando by remember(dispositivo) { mutableStateOf(false) }
    var aviso by remember(dispositivo) { mutableStateOf<String?>(null) }
    val tareas = rememberCoroutineScope()
    val formato = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 } }
    val medicion = estado.medicion

    val enviar: (Boolean) -> Unit = { activar ->
        if (!esperando && dispositivo != null && estado.conexion == Conexion.CONECTADA) {
            esperando = true
            aviso = null
            tareas.launch {
                try {
                    val confirmado = withTimeoutOrNull(5_000) {
                        dispositivo.solicitarActivacion(activar)
                    }
                    aviso = when {
                        confirmado == null -> "Sin confirmación. Revisa el estado antes de repetir."
                        (confirmado == EstadoActuador.ACTIVADO) != activar ->
                            "El dispositivo informó un estado distinto al solicitado."
                        else -> if (simulacion) "Confirmación recibida del simulador." else "Confirmación recibida del Arduino."
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    aviso = "Error al enviar. No se recibió confirmación."
                } finally {
                    esperando = false
                }
            }
        }
    }

    PanelMonitoreo(
        usuario = usuario,
        modifier = modifier,
        estado = estado,
        temperatura = medicion?.let { formato.format(it.temperatura) },
        humedad = medicion?.let { formato.format(it.humedad) },
        simulacion = simulacion,
        lecturas = lecturas,
        errorHistorial = errorHistorial,
        esperando = esperando,
        aviso = aviso,
        controlesDisponibles = dispositivo != null && estado.conexion == Conexion.CONECTADA && !esperando,
        enviar = enviar,
        alternarModo = { simulacion = !simulacion },
        salir = salir
    )
}


