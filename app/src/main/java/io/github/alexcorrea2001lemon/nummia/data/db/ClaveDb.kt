package io.github.alexcorrea2001lemon.nummia.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Clave de la base de datos: 32 bytes aleatorios, cifrados con una clave de Android Keystore
 * (que nunca sale del chip de seguridad) y guardados en una carpeta sin copia de seguridad.
 */
object ClaveDb {
    private const val ALIAS = "nummia_db"
    private const val ARCHIVO = "db.key"
    private const val GCM_BITS = 128

    fun obtener(context: Context): ByteArray {
        val archivo = File(context.noBackupFilesDir, ARCHIVO)
        val maestra = claveMaestra()
        if (archivo.exists()) {
            val datos = archivo.readBytes()
            val iv = datos.copyOfRange(0, 12)
            val cifrado = datos.copyOfRange(12, datos.size)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, maestra, GCMParameterSpec(GCM_BITS, iv))
            return c.doFinal(cifrado)
        }
        val clave = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, maestra)
        val salida = c.iv + c.doFinal(clave)
        val tmp = File(context.noBackupFilesDir, "$ARCHIVO.tmp")
        tmp.writeBytes(salida)
        tmp.renameTo(archivo)
        return clave
    }

    private fun claveMaestra(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }
}
