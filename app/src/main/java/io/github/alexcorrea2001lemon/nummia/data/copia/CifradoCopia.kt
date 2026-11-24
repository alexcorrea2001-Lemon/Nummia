package io.github.alexcorrea2001lemon.nummia.data.copia

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** La copia está protegida con contraseña y no se ha dado. */
class CopiaCifrada : Exception("Esta copia está protegida con contraseña.")

/**
 * Copias `.nummia` protegidas con contraseña: AES-256-GCM con una clave sacada de la contraseña
 * (PBKDF2-SHA256, 200 000 vueltas, sal aleatoria). GCM además detecta cualquier cambio en el archivo:
 * una copia dañada o manipulada no se restaura.
 *
 * Formato: «NUMMIA-CIFRADA-1\n» + sal (16) + iv (12) + datos cifrados con su etiqueta.
 */
object CifradoCopia {
    private val MAGIA = "NUMMIA-CIFRADA-1\n".toByteArray(Charsets.US_ASCII)
    private const val VUELTAS = 200_000
    private val azar = SecureRandom()

    fun estaCifrada(bytes: ByteArray): Boolean = bytes.size > MAGIA.size && bytes.copyOfRange(0, MAGIA.size).contentEquals(MAGIA)

    fun cifrar(datos: ByteArray, contrasena: String): ByteArray {
        val sal = ByteArray(16).also(azar::nextBytes)
        val iv = ByteArray(12).also(azar::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, clave(contrasena, sal), GCMParameterSpec(128, iv)) }
        c.updateAAD(MAGIA)
        return MAGIA + sal + iv + c.doFinal(datos)
    }

    fun descifrar(bytes: ByteArray, contrasena: String): ByteArray {
        if (!estaCifrada(bytes) || bytes.size < MAGIA.size + 16 + 12 + 16) throw CopiaInvalida("La copia está dañada.")
        var i = MAGIA.size
        val sal = bytes.copyOfRange(i, i + 16); i += 16
        val iv = bytes.copyOfRange(i, i + 12); i += 12
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, clave(contrasena, sal), GCMParameterSpec(128, iv))
                updateAAD(MAGIA)
                doFinal(bytes, i, bytes.size - i)
            }
        } catch (e: AEADBadTagException) {
            throw CopiaInvalida("Contraseña incorrecta (o la copia está dañada).")
        } catch (e: Exception) {
            throw CopiaInvalida("La copia está dañada.")
        }
    }

    private fun clave(contrasena: String, sal: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(contrasena.toCharArray(), sal, VUELTAS, 256)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
