package com.example.temperatura

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class RegistroProtegido(private val ruta: File) {
    private val archivo = AtomicFile(ruta)

    fun leer(): JSONObject? {
        // AtomicFile puede recuperar el respaldo del formato antiguo tras una interrupción.
        if (!ruta.exists() && !File(ruta.path + ".bak").exists()) return null
        val contenedor = JSONObject(archivo.readFully().toString(Charsets.UTF_8))
        if (!contenedor.has("version")) {
            // Migra las instalaciones anteriores sin pedir que se vuelva a crear la cuenta.
            contenedor.getString("verificador")
            guardar(contenedor)
            return contenedor
        }
        check(contenedor.getInt("version") == 1)
        val nonce = decodificar(contenedor.getString("nonce"))
        check(nonce.size == 12)
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.DECRYPT_MODE, clave(crear = false), GCMParameterSpec(128, nonce))
        val contenido = cifrador.doFinal(decodificar(contenedor.getString("datos")))
        return JSONObject(contenido.toString(Charsets.UTF_8))
    }

    fun guardar(registro: JSONObject) {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, clave(crear = true))
        val contenido = cifrador.doFinal(registro.toString().toByteArray(Charsets.UTF_8))
        val contenedor = JSONObject()
            .put("version", 1)
            .put("nonce", codificar(cifrador.iv))
            .put("datos", codificar(contenido))
        val salida = archivo.startWrite()
        try {
            salida.write(contenedor.toString().toByteArray(Charsets.UTF_8))
            archivo.finishWrite(salida)
            // AtomicFile registra algunos errores de escritura sin propagarlos.
            check(archivo.readFully().toString(Charsets.UTF_8) == contenedor.toString())
        } catch (error: Exception) {
            archivo.failWrite(salida)
            throw error
        }
    }

    private fun clave(crear: Boolean): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(crear)
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private companion object {
        const val ALIAS = "cuenta_local_aes_v1"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
    }
}

internal fun codificar(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
internal fun decodificar(texto: String): ByteArray = Base64.decode(texto, Base64.NO_WRAP)
