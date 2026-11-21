package io.github.alexcorrea2001lemon.nummia.ui.theme

import androidx.compose.ui.graphics.Color

/** Colores de acento de la app y del icono. El orden es el de Ajustes → Apariencia. */
enum class Acento(val nombre: String, val argb: Long) {
    VERDE_MENTA("Verde menta", 0xFF34D399),
    TURQUESA("Turquesa", 0xFF2DD4BF),
    CIAN("Cian", 0xFF22D3EE),
    AZUL("Azul", 0xFF60A5FA),
    INDIGO("Índigo", 0xFF818CF8),
    VIOLETA("Violeta", 0xFFA78BFA),
    ROSA("Rosa", 0xFFF472B6),
    CORAL("Coral", 0xFFFB7185),
    NARANJA("Naranja", 0xFFFB923C),
    AMBAR("Ámbar", 0xFFFBBF24),
    LIMA("Lima", 0xFFA3E635),
    PIZARRA("Pizarra", 0xFF94A3B8),
    ;

    val color: Color get() = Color(argb)

    companion object {
        val POR_DEFECTO = VIOLETA

        /** El de la paleta más parecido a un color libre (para llevar un color personalizado al icono). */
        fun masParecido(argb: Long): Acento {
            fun c(v: Long, s: Int) = ((v shr s) and 0xFF).toInt()
            return entries.minBy { a ->
                val dr = c(a.argb, 16) - c(argb, 16)
                val dg = c(a.argb, 8) - c(argb, 8)
                val db = c(a.argb, 0) - c(argb, 0)
                dr * dr + dg * dg + db * db
            }
        }
    }
}

enum class Tema(val nombre: String, val descripcion: String) {
    AUTOMATICO("Automático", "Según tu móvil"),
    CLARO("Claro", "Fondo blanco"),
    OSCURO("Oscuro", "Fondo gris oscuro"),
    AMOLED("Negro AMOLED", "Negro puro, ahorra batería"),
}

enum class ModoAcento { PALETA, PERSONALIZADO, FONDO_PANTALLA }

enum class Logo(val nombre: String) {
    A("N que sube"),
    B("Moneda"),
    C("Ahorro que crece"),
}

enum class TamanoTexto(val nombre: String, val escala: Float) {
    PEQUENO("Pequeño", 0.9f),
    NORMAL("Normal", 1f),
    GRANDE("Grande", 1.12f),
    MUY_GRANDE("Muy grande", 1.25f),
}
