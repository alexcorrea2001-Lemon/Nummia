package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.devuelto
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.LocalDate

sealed interface EtiquetaMov {
    data object DevueltoTotal : EtiquetaMov
    data class Parcial(val devuelto: Long, val importe: Long) : EtiquetaMov
    data class TeDeben(val persona: String?) : EtiquetaMov
    data object Previsto : EtiquetaMov
    data class AFavor(val cantidad: Long) : EtiquetaMov
}

@Immutable
data class FilaMov(
    val id: String,
    val esGasto: Boolean,
    val titulo: String,
    val subtitulo: String,
    /** Gasto real (positivo) o importe del ingreso. */
    val importe: Long,
    /** Precio pagado si había reembolsos (se muestra tachado). */
    val pagado: Long?,
    val fecha: LocalDate,
    val tienda: String,
    val etiqueta: EtiquetaMov?,
    val creado: Long,
    val categoriaId: String? = null,
    val personaId: String? = null,
    /** Tiene notas escritas (se marca con un icono pequeño). */
    val conNotas: Boolean = false,
)

private val nombresTipo = mapOf(
    TipoIngreso.SUELDO to "Sueldo", TipoIngreso.EXTRA to "Extra", TipoIngreso.VENTA to "Venta",
    TipoIngreso.TRABAJO_EXTRA to "Trabajo extra", TipoIngreso.OTRO to "Otro ingreso",
)

fun nombreTipo(t: TipoIngreso) = nombresTipo.getValue(t)

fun GastoConReembolsos.aFila(personas: Map<String, String>, categorias: Map<String, String>): FilaMov {
    val g = gasto
    val etiqueta = when {
        g.estado == Estado.PREVISTO -> EtiquetaMov.Previsto
        g.meLoDevuelven && falta > 0 -> EtiquetaMov.TeDeben(g.personaId?.let(personas::get))
        real < 0 -> EtiquetaMov.AFavor(-real)
        devuelto > 0 && real == 0L -> EtiquetaMov.DevueltoTotal
        devuelto > 0 -> EtiquetaMov.Parcial(devuelto, g.importe)
        else -> null
    }
    val sub = listOfNotNull(
        g.tienda.takeIf { it.isNotBlank() && !it.equals(g.concepto, ignoreCase = true) },
        g.categoriaId?.let(categorias::get),
    ).joinToString(" · ").ifEmpty { g.codigo }
    return FilaMov(g.id, true, g.concepto, sub, real, g.importe.takeIf { devuelto > 0 }, g.fecha, g.tienda, etiqueta, g.createdAt, g.categoriaId, g.personaId, g.notas.isNotBlank())
}

fun IngresoEntity.aFila(): FilaMov = FilaMov(
    id, false, concepto, "Ingreso · ${nombreTipo(tipo).lowercase()}", importe, null, fecha, "",
    if (estado == Estado.PREVISTO) EtiquetaMov.Previsto else null, createdAt, conNotas = notas.isNotBlank(),
)

@Composable
fun EtiquetaDe(e: EtiquetaMov) {
    val c = LocalColoresNummia.current
    val esquema = MaterialTheme.colorScheme
    when (e) {
        EtiquetaMov.DevueltoTotal -> Etiqueta("Devuelto 100 %", c.ingresoFondo, c.ingreso)
        is EtiquetaMov.Parcial -> Etiqueta("Te devolvieron ${formatearEuros(e.devuelto)}", c.avisoFondo, c.aviso)
        is EtiquetaMov.TeDeben -> Etiqueta(e.persona?.let { "Te deben · $it" } ?: "Te deben", c.avisoFondo, c.aviso)
        EtiquetaMov.Previsto -> Etiqueta("Previsto", esquema.surfaceVariant, c.textoSuave)
        is EtiquetaMov.AFavor -> Etiqueta("+${formatearEuros(e.cantidad)} a tu favor", c.ingresoFondo, c.ingreso)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FilaMovimiento(
    f: FilaMov, discreto: Boolean, alPulsar: () -> Unit, modifier: Modifier = Modifier,
    alMantener: (() -> Unit)? = null, seleccionado: Boolean = false,
) {
    val c = LocalColoresNummia.current
    Row(
        modifier.fillMaxWidth()
            .then(if (seleccionado) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
            .combinedClickable(onClick = alPulsar, onLongClick = alMantener, onLongClickLabel = if (alMantener != null) "Seleccionar varios" else null)
            .padding(horizontal = Espacio.l, vertical = if (io.github.alexcorrea2001lemon.nummia.ui.theme.LocalPreferenciasUi.current.compacto) Espacio.xs + 2.dp else Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        if (seleccionado) Avatar("", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Iconos.Check) else AvatarMov(f)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                Text(f.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (f.conNotas) androidx.compose.material3.Icon(Iconos.Nota, "Tiene notas", Modifier.size(14.dp), tint = c.textoSuave)
            }
            Text(f.subtitulo, style = MaterialTheme.typography.bodySmall, color = c.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
            f.etiqueta?.let { EtiquetaDe(it) }
        }
        Column(horizontalAlignment = Alignment.End) {
            val texto = when {
                !f.esGasto -> formatearEuros(f.importe, conSigno = true, discreto = discreto)
                f.importe < 0 -> formatearEuros(-f.importe, conSigno = true, discreto = discreto)
                else -> formatearEuros(-f.importe, discreto = discreto)
            }
            val color = when {
                f.etiqueta == EtiquetaMov.Previsto -> c.textoSuave
                !f.esGasto || f.importe < 0 -> c.ingreso
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(texto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
            if (f.pagado != null && !discreto) {
                Text(
                    formatearEuros(f.pagado), style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
    }
}


/** Icono del movimiento: el de su categoría con su color; si no tiene, la inicial; los ingresos, «€». */
@Composable
fun AvatarMov(f: FilaMov) {
    val c = LocalColoresNummia.current
    val cat = f.categoriaId?.let { LocalCategorias.current[it] }
    when {
        !f.esGasto -> Avatar("€", c.ingresoFondo, c.ingreso, Iconos.Euro)
        cat != null -> {
            val color = androidx.compose.ui.graphics.Color(cat.color)
            Avatar(cat.nombre, color.copy(alpha = if (c.esOscuro) 0.2f else 0.16f), color, IconosCategoria.de(cat.icono))
        }
        else -> Avatar(f.titulo)
    }
}
