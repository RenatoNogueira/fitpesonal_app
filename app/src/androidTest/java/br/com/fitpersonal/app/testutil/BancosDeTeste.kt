package br.com.fitpersonal.app.testutil

import android.database.sqlite.SQLiteDatabase
import br.com.fitpersonal.app.data.local.BancoCriptografado
import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase as SqlCipher

/** Cria um SQLite comum (texto puro) com uma tabela simples e `user_version` definido. */
fun criarBancoTextoPuro(arquivo: File, nomes: List<String>, versao: Int = 2) {
    arquivo.parentFile?.mkdirs()
    arquivo.delete()
    val db = SQLiteDatabase.openOrCreateDatabase(arquivo.path, null)
    try {
        db.execSQL("CREATE TABLE pessoas (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL)")
        nomes.forEach { db.execSQL("INSERT INTO pessoas(nome) VALUES (?)", arrayOf<Any>(it)) }
        db.version = versao
    } finally {
        db.close()
    }
}

/** Cria um banco SQLCipher com a mesma tabela, convertendo um texto puro temporário. */
fun criarBancoCifrado(arquivo: File, senha: ByteArray, nomes: List<String>, versao: Int = 2) {
    val temp = File(arquivo.path + ".plain")
    criarBancoTextoPuro(temp, nomes, versao)
    BancoCriptografado.converterParaCriptografado(temp, arquivo, senha)
    temp.delete()
}

private fun abrirCifrado(arquivo: File, senha: ByteArray): SqlCipher {
    BancoCriptografado.carregarBiblioteca()
    return SqlCipher.openDatabase(
        arquivo.path, String(senha, Charsets.US_ASCII), null, SqlCipher.OPEN_READONLY, null,
    )
}

/** Lê os nomes de um banco cifrado com [senha]; lança se a chave estiver errada. */
fun lerNomesCifrado(arquivo: File, senha: ByteArray): List<String> {
    val db = abrirCifrado(arquivo, senha)
    return try {
        db.rawQuery("SELECT nome FROM pessoas ORDER BY id", arrayOf<String>()).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
    } finally {
        db.close()
    }
}

/** Lê os nomes de um SQLite comum (falha se o arquivo estiver cifrado). */
fun lerNomesTextoPuro(arquivo: File): List<String> {
    val db = SQLiteDatabase.openDatabase(arquivo.path, null, SQLiteDatabase.OPEN_READONLY)
    return try {
        db.rawQuery("SELECT nome FROM pessoas ORDER BY id", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
    } finally {
        db.close()
    }
}

fun versaoTextoPuro(arquivo: File): Int {
    val db = SQLiteDatabase.openDatabase(arquivo.path, null, SQLiteDatabase.OPEN_READONLY)
    return try { db.version } finally { db.close() }
}

fun versaoCifrado(arquivo: File, senha: ByteArray): Int {
    val db = abrirCifrado(arquivo, senha)
    return try { db.version } finally { db.close() }
}

/** Senha no mesmo formato da usada pelo app (hex ASCII de 32 bytes aleatórios). */
fun senhaAleatoria(): ByteArray {
    val b = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
    return b.joinToString("") { "%02x".format(it) }.toByteArray(Charsets.US_ASCII)
}
