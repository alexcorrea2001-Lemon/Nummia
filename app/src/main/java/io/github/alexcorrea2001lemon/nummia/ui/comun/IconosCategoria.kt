package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.ui.graphics.vector.ImageVector

/** Círculo como trazo SVG. */
private fun c(x: Float, y: Float, r: Float) = "M${x - r} ${y}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

/**
 * Iconos de las categorías (24×24, del mismo estilo que el resto). La clave se guarda en la categoría.
 * Se dibujan en código: no ocupan nada en el APK.
 */
object IconosCategoria {
    private val caminos: Map<String, List<String>> = linkedMapOf(
        "restaurant" to listOf("M7 3v8M5 3v5a2 2 0 0 0 4 0V3M7 11v10", "M17 3c-2 0-3 2-3 6v4h3M17 3v18"),
        "shopping" to listOf("M5 8h14l-1 12H6zM9 8V6a3 3 0 0 1 6 0v2"),
        "bus" to listOf("M6 3h12a2 2 0 0 1 2 2v13H4V5a2 2 0 0 1 2-2zM4 11h16M7 18v3M17 18v3", c(8f, 14.5f, 0.6f), c(16f, 14.5f, 0.6f)),
        "car" to listOf("M4 16v-4l2.2-5h11.6L20 12v4zM4 12h16M6.5 16v2.5M17.5 16v2.5", c(8f, 14f, 0.6f), c(16f, 14f, 0.6f)),
        "fuel" to listOf("M4 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M3 21h12M6 9h6M14 11h2a2 2 0 0 1 2 2v4a1.5 1.5 0 0 0 3 0V8l-3-3"),
        "flight" to listOf("M3 12.5 21 5l-4.5 15-4-6.5zM12.5 13.5 21 5"),
        "home" to listOf("M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"),
        "bolt" to listOf("M13 2 4 14h7l-1 8 9-12h-7z"),
        "water" to listOf("M12 3c3 4 6 7.5 6 11a6 6 0 0 1-12 0c0-3.5 3-7 6-11z"),
        "phone" to listOf("M8 2h8a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zM11 18h2"),
        "wifi" to listOf("M2 9a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0", c(12f, 19.5f, 0.6f)),
        "autorenew" to listOf("M4 12a8 8 0 1 0 3-6.2M4 4v4h4"),
        "favorite" to listOf("M12 20s-7-4.4-9-9a4.5 4.5 0 0 1 9-3 4.5 4.5 0 0 1 9 3c-2 4.6-9 9-9 9z"),
        "health" to listOf("M9.5 3h5v6.5H21v5h-6.5V21h-5v-6.5H3v-5h6.5z"),
        "gym" to listOf("M6 7v10M3 9.5v5M18 7v10M21 9.5v5M6 12h12"),
        "beauty" to listOf("M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM18.5 16l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z"),
        "clothes" to listOf("M8.5 4 3 7l2 4 2-1v10h10V10l2 1 2-4-5.5-3c-.7 1.8-2 2.8-3.5 2.8S9.2 5.8 8.5 4z"),
        "coffee" to listOf("M4 9h12v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5zM16 10h1.5a2.5 2.5 0 0 1 0 5H16M8 3.5v2.5M12 3.5v2.5"),
        "sports_esports" to listOf("M7 8h10a5 5 0 0 1 5 5v.5a3.5 3.5 0 0 1-6.3 2.1L14.5 14h-5l-1.2 1.6A3.5 3.5 0 0 1 2 13.5V13a5 5 0 0 1 5-5zM7.5 10.5v3M6 12h3", c(15.5f, 11f, 0.6f), c(17.5f, 13f, 0.6f)),
        "movie" to listOf("M3 9h18v11H3zM3 9l1.5-4.5 15.5-1 1 4.5M8.5 4.2 9.8 8.6M14 3.8l1.3 4.5"),
        "music" to listOf("M9 18V5l11-2v13", c(6f, 18f, 3f), c(17f, 16f, 3f)),
        "book" to listOf("M5 19.5V4.5A1.5 1.5 0 0 1 6.5 3H19v15H6.5A1.5 1.5 0 0 0 5 19.5 1.5 1.5 0 0 0 6.5 21H19v-3M9 7h6"),
        "school" to listOf("M2 9l10-5 10 5-10 5zM6 11v5c3 2 9 2 12 0v-5M22 9v6"),
        "pets" to listOf(c(6f, 10f, 1.6f), c(9.5f, 6.2f, 1.6f), c(14.5f, 6.2f, 1.6f), c(18f, 10f, 1.6f), "M8 17.5c0-3 1.8-5.5 4-5.5s4 2.5 4 5.5a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2z"),
        "gift" to listOf("M4 11h16v9H4zM3 7.5h18V11H3zM12 7.5V20M12 7.5c-1.5-3.5-5.5-3-4.5-.5.4 1 4.5.5 4.5.5zM12 7.5c1.5-3.5 5.5-3 4.5-.5-.4 1-4.5.5-4.5.5z"),
        "kids" to listOf(c(12f, 6f, 2.5f), "M7 21v-6l-2-3 4-2.5h6L19 12l-2 3v6M10 21v-4h4v4"),
        "plant" to listOf("M12 21V11M12 11c0-4 3-7 7-7 0 4-3 7-7 7zM12 14c0-3-2.5-5-6-5 0 3 2.5 5 6 5z"),
        "tools" to listOf("M14.5 5.5a4 4 0 0 0 4.8 5.2l-9 9a2.1 2.1 0 0 1-3-3l9-9a4 4 0 0 1-1.8-2.2zM16 4l4 4"),
        "ticket" to listOf("M3 7h18v3a2 2 0 0 0 0 4v3H3v-3a2 2 0 0 0 0-4zM14 7v10"),
        "card" to listOf("M3 6h18v12H3zM3 10h18M7 15h4"),
        "bank" to listOf("M3 10 12 4l9 6M5 10v8M9.5 10v8M14.5 10v8M19 10v8M3 20.5h18"),
        "description" to listOf("M14 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8zM14 3v5h5M9 13h6M9 17h6"),
        "more" to listOf(c(6f, 12f, 1f), c(12f, 12f, 1f), c(18f, 12f, 1f)),
    )

    val claves: List<String> = caminos.keys.toList()

    private val cache = HashMap<String, ImageVector>()

    fun de(clave: String?): ImageVector {
        val k = clave?.takeIf { it in caminos } ?: "more"
        return cache.getOrPut(k) { Iconos.trazo("cat_$k", *caminos.getValue(k).toTypedArray()) }
    }
}

/** Categorías por id, para pintar icono y color en cualquier lista sin pasarlas a mano. */
val LocalCategorias = androidx.compose.runtime.staticCompositionLocalOf<Map<String, io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity>> { emptyMap() }
