package io.github.alexcorrea2001lemon.nummia.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `DialogoNummia` es una hoja inferior que ya desplaza su contenido. Un `verticalScroll` o una lista vertical
 * perezosa dentro le dan a Compose una altura infinita y la app se cierra al abrir el diálogo
 * (así falló «Nueva categoría»). Esta prueba recorre el código y lo impide.
 */
class DialogosSinScrollAnidadoTest {
    private val prohibido = listOf("verticalScroll(", "LazyColumn(", "LazyVerticalGrid(", "LazyVerticalStaggeredGrid(")

    private fun fuentes(): List<File> {
        val raiz = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        return raiz.walkTopDown().filter { it.extension == "kt" }.toList()
    }

    /** Quita las cadenas de texto para que un paréntesis dentro de un texto no descuadre el recuento. */
    private fun sinCadenas(s: String) = s.replace(Regex("\"([^\"\\\\\\n]|\\\\.)*\""), "\"\"")

    @Test fun ningunDialogoLlevaUnScrollVerticalDentro() {
        val fallos = mutableListOf<String>()
        fuentes().forEach { f ->
            val t = sinCadenas(f.readText())
            var desde = 0
            while (true) {
                val i = t.indexOf("DialogoNummia(", desde)
                if (i < 0) break
                desde = i + 1
                if (t.substring(maxOf(0, i - 4), i) == "fun ") continue
                var prof = 0
                var fin = i + "DialogoNummia".length
                while (fin < t.length) {
                    when (t[fin]) { '(' -> prof++; ')' -> { prof--; if (prof == 0) break } }
                    fin++
                }
                val bloque = t.substring(i, minOf(fin, t.length))
                prohibido.filter { bloque.contains(it) }.forEach { p ->
                    fallos += "${f.name}: «$p» dentro de DialogoNummia (línea ${t.substring(0, i).count { it == '\n' } + 1})"
                }
            }
        }
        assertTrue("Scroll vertical anidado en diálogos:\n" + fallos.joinToString("\n"), fallos.isEmpty())
    }
}
