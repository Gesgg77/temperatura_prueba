package com.example.temperatura

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class LecturaGuardada(
    val id: Long,
    val instante: Long,
    val temperatura: Double,
    val humedad: Double,
    val simulada: Boolean
)

internal class HistorialLocal(context: Context, nombre: String = "mediciones.db") :
    SQLiteOpenHelper(context.applicationContext, nombre, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE lecturas (
                id INTEGER PRIMARY KEY,
                instante INTEGER NOT NULL,
                temperatura REAL NOT NULL,
                humedad REAL NOT NULL CHECK (humedad BETWEEN 0 AND 100),
                simulada INTEGER NOT NULL CHECK (simulada IN (0, 1))
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Falta una migración de la base de mediciones")
    }

    suspend fun recientes(): List<LecturaGuardada> = withContext(Dispatchers.IO) {
        consultar(readableDatabase)
    }

    suspend fun guardar(medicion: Medicion, simulada: Boolean): List<LecturaGuardada> =
        withContext(Dispatchers.IO) {
            require(medicion.esValida)
            val db = writableDatabase
            db.beginTransaction()
            try {
                val valores = ContentValues().apply {
                    put("instante", medicion.instante)
                    put("temperatura", medicion.temperatura)
                    put("humedad", medicion.humedad)
                    put("simulada", if (simulada) 1 else 0)
                }
                db.insertOrThrow("lecturas", null, valores)
                db.execSQL(
                    "DELETE FROM lecturas WHERE id < (SELECT MIN(id) FROM " +
                        "(SELECT id FROM lecturas ORDER BY id DESC LIMIT 200))"
                )
                val resultado = consultar(db)
                db.setTransactionSuccessful()
                resultado
            } finally {
                db.endTransaction()
            }
        }

    private fun consultar(db: SQLiteDatabase): List<LecturaGuardada> =
        db.rawQuery(
            "SELECT id, instante, temperatura, humedad, simulada FROM lecturas ORDER BY id DESC LIMIT 20",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(LecturaGuardada(
                        cursor.getLong(0), cursor.getLong(1), cursor.getDouble(2),
                        cursor.getDouble(3), cursor.getInt(4) == 1
                    ))
                }
            }
        }

    companion object {
        private var instancia: HistorialLocal? = null

        @Synchronized
        fun obtener(context: Context): HistorialLocal =
            instancia ?: HistorialLocal(context).also { instancia = it }
    }
}

