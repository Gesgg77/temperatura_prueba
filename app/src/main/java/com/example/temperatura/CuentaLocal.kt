package com.example.temperatura

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal sealed interface ResultadoAcceso {
    data object Correcto : ResultadoAcceso
    data class Incorrecto(val restantes: Int) : ResultadoAcceso
    data class Bloqueado(val milisegundos: Long) : ResultadoAcceso
}

internal class CuentaLocal(private val context: Context) {
    private val archivo = RegistroProtegido(File(context.noBackupFilesDir, "cuenta.json"))

    fun estadoInicial(): Pair<Boolean, Long> = synchronized(bloqueo) {
        val registro = archivo.leer() ?: return@synchronized false to 0L
        true to tiempoBloqueado(registro)
    }

    suspend fun registrar(usuario: String, contrasena: String): Boolean =
        withContext(Dispatchers.IO) {
            require(usuario.isNotBlank() && usuario.length <= 50)
            require(contrasena.codePointCount(0, contrasena.length) in MINIMO_CLAVE..128)
            synchronized(bloqueo) {
                if (archivo.leer() != null) return@synchronized false
                val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val sha256 = Build.VERSION.SDK_INT >= 26
                val registro = JSONObject()
                    .put("usuario", usuario)
                    .put("sal", codificar(sal))
                    .put("sha256", sha256)
                    .put("verificador", codificar(derivar(contrasena, sal, sha256)))
                archivo.guardar(registro)
                true
            }
        }

    suspend fun autenticar(usuario: String, contrasena: String): ResultadoAcceso =
        withContext(Dispatchers.IO) {
            synchronized(bloqueo) {
                val registro = checkNotNull(archivo.leer())
                val espera = tiempoBloqueado(registro)
                if (espera > 0) return@synchronized ResultadoAcceso.Bloqueado(espera)

                val intentos = IntentosAcceso(registro.optInt("fallos"), registro.optInt("nivel"))
                    .registrarFallo()
                // Reserva el intento antes de verificar para que cerrar el proceso no borre el fallo.
                registro.put("fallos", intentos.fallos).put("nivel", intentos.nivel)
                val duracion = intentos.esperaNueva
                marcarBloqueo(registro, duracion)
                archivo.guardar(registro)

                val sal = decodificar(registro.getString("sal"))
                val esperado = decodificar(registro.getString("verificador"))
                val calculado = derivar(contrasena, sal, registro.getBoolean("sha256"))
                if (MessageDigest.isEqual(esperado, calculado) && usuario == registro.getString("usuario")) {
                    registro.put("fallos", 0).put("nivel", 0)
                    marcarBloqueo(registro, 0)
                    archivo.guardar(registro)
                    ResultadoAcceso.Correcto
                } else if (duracion > 0) {
                    ResultadoAcceso.Bloqueado(tiempoBloqueado(registro))
                } else {
                    ResultadoAcceso.Incorrecto(intentos.restantes)
                }
            }
        }

    private fun tiempoBloqueado(registro: JSONObject): Long {
        val duracion = registro.optLong("duracionBloqueo")
        if (duracion == 0L) return 0
        val ahora = SystemClock.elapsedRealtime()
        val inicio = registro.getLong("inicioBloqueo")
        val mismoArranque = registro.getInt("arranque") == arranque() && inicio <= ahora
        val restante = IntentosAcceso.restante(duracion, inicio, ahora, mismoArranque)
        if (!mismoArranque) {
            // Un reinicio durante la espera vuelve a iniciar el plazo completo.
            marcarBloqueo(registro, duracion)
            archivo.guardar(registro)
            return restante
        }
        if (restante == 0L) {
            marcarBloqueo(registro, 0)
            archivo.guardar(registro)
        }
        return restante
    }

    private fun marcarBloqueo(registro: JSONObject, duracion: Long) {
        registro.put("duracionBloqueo", duracion)
        if (duracion > 0) {
            registro.put("inicioBloqueo", SystemClock.elapsedRealtime())
            registro.put("arranque", arranque())
        }
    }

    private fun arranque(): Int = Settings.Global.getInt(
        context.contentResolver, Settings.Global.BOOT_COUNT, -1
    )

    private fun derivar(contrasena: String, sal: ByteArray, sha256: Boolean): ByteArray {
        val algoritmo = if (sha256) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val iteraciones = if (sha256) 600_000 else 1_300_000
        val caracteres = contrasena.toCharArray()
        val especificacion = PBEKeySpec(caracteres, sal, iteraciones, 256)
        caracteres.fill('\u0000')
        return try {
            SecretKeyFactory.getInstance(algoritmo).generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }

    companion object {
        const val MINIMO_CLAVE = 15
        private val bloqueo = Any()
    }
}


