package io.github.alexcorrea2001lemon.nummia.ui.nav

object Rutas {
    const val INICIO = "inicio"
    const val MOVIMIENTOS = "movimientos"
    const val CALENDARIO = "calendario"
    const val ESTADISTICAS = "estadisticas"
    const val INFORMES = "informes"
    const val DETALLE = "detalle/{id}"
    const val GASTO = "gasto?id={id}&paraOtro={paraOtro}&fecha={fecha}"
    const val INGRESO = "ingreso?id={id}"
    const val REEMBOLSO = "reembolso?gastoId={gastoId}"
    const val AJUSTES = "ajustes"
    const val APARIENCIA = "ajustes/apariencia"
    const val PERSONAS = "ajustes/personas"
    const val SUELDO = "ajustes/sueldo"
    const val DATOS = "ajustes/datos"
    const val ACERCA = "ajustes/acerca"
    const val DRIVE = "ajustes/drive"
    const val IMPORTAR = "ajustes/importar"
    const val PAPELERA = "ajustes/papelera"
    const val PAGOS = "ajustes/pagos"
    const val PRESUPUESTOS = "ajustes/presupuestos"
    const val TIENDAS = "ajustes/tiendas"
    const val CATEGORIAS = "ajustes/categorias"
    const val NOTIFICACIONES = "ajustes/notificaciones"
    const val SEGURIDAD = "ajustes/seguridad"

    fun detalle(id: String) = "detalle/$id"
    fun gasto(id: String? = null, paraOtro: Boolean = false, fecha: java.time.LocalDate? = null) =
        "gasto?id=${id.orEmpty()}&paraOtro=$paraOtro&fecha=${fecha?.toString().orEmpty()}"
    fun ingreso(id: String? = null) = "ingreso?id=${id.orEmpty()}"
    fun reembolso(gastoId: String? = null) = "reembolso?gastoId=${gastoId.orEmpty()}"

    val PRINCIPALES = listOf(INICIO, MOVIMIENTOS, CALENDARIO, ESTADISTICAS)
}
