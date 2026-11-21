package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.alexcorrea2001lemon.nummia.R
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo

/** Icono de la app tal y como se ve en el móvil: fondo + dibujo, mostrando la zona visible de 72 dp. */
@Composable
fun ImagenLogo(logo: Logo, color: Acento, modifier: Modifier = Modifier, descripcion: String? = null) {
    val (fondo, arte) = recursoIcono(logo, color)
    Box(modifier.clip(RoundedCornerShape(26))) {
        val escala = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
        Image(painterResource(fondo), null, contentScale = ContentScale.Crop, modifier = escala)
        Image(painterResource(arte), descripcion, contentScale = ContentScale.Crop, modifier = escala)
    }
}

/** Generado: logo × color → (fondo, dibujo) del icono. */
fun recursoIcono(logo: Logo, color: Acento): Pair<Int, Int> = when (logo to color) {
    Logo.A to Acento.VERDE_MENTA -> R.drawable.ic_app_a_verde_menta_fondo to R.drawable.ic_app_a_verde_menta
    Logo.A to Acento.TURQUESA -> R.drawable.ic_app_a_turquesa_fondo to R.drawable.ic_app_a_turquesa
    Logo.A to Acento.CIAN -> R.drawable.ic_app_a_cian_fondo to R.drawable.ic_app_a_cian
    Logo.A to Acento.AZUL -> R.drawable.ic_app_a_azul_fondo to R.drawable.ic_app_a_azul
    Logo.A to Acento.INDIGO -> R.drawable.ic_app_a_indigo_fondo to R.drawable.ic_app_a_indigo
    Logo.A to Acento.VIOLETA -> R.drawable.ic_app_a_violeta_fondo to R.drawable.ic_app_a_violeta
    Logo.A to Acento.ROSA -> R.drawable.ic_app_a_rosa_fondo to R.drawable.ic_app_a_rosa
    Logo.A to Acento.CORAL -> R.drawable.ic_app_a_coral_fondo to R.drawable.ic_app_a_coral
    Logo.A to Acento.NARANJA -> R.drawable.ic_app_a_naranja_fondo to R.drawable.ic_app_a_naranja
    Logo.A to Acento.AMBAR -> R.drawable.ic_app_a_ambar_fondo to R.drawable.ic_app_a_ambar
    Logo.A to Acento.LIMA -> R.drawable.ic_app_a_lima_fondo to R.drawable.ic_app_a_lima
    Logo.A to Acento.PIZARRA -> R.drawable.ic_app_a_pizarra_fondo to R.drawable.ic_app_a_pizarra
    Logo.B to Acento.VERDE_MENTA -> R.drawable.ic_app_b_verde_menta_fondo to R.drawable.ic_app_b_verde_menta
    Logo.B to Acento.TURQUESA -> R.drawable.ic_app_b_turquesa_fondo to R.drawable.ic_app_b_turquesa
    Logo.B to Acento.CIAN -> R.drawable.ic_app_b_cian_fondo to R.drawable.ic_app_b_cian
    Logo.B to Acento.AZUL -> R.drawable.ic_app_b_azul_fondo to R.drawable.ic_app_b_azul
    Logo.B to Acento.INDIGO -> R.drawable.ic_app_b_indigo_fondo to R.drawable.ic_app_b_indigo
    Logo.B to Acento.VIOLETA -> R.drawable.ic_app_b_violeta_fondo to R.drawable.ic_app_b_violeta
    Logo.B to Acento.ROSA -> R.drawable.ic_app_b_rosa_fondo to R.drawable.ic_app_b_rosa
    Logo.B to Acento.CORAL -> R.drawable.ic_app_b_coral_fondo to R.drawable.ic_app_b_coral
    Logo.B to Acento.NARANJA -> R.drawable.ic_app_b_naranja_fondo to R.drawable.ic_app_b_naranja
    Logo.B to Acento.AMBAR -> R.drawable.ic_app_b_ambar_fondo to R.drawable.ic_app_b_ambar
    Logo.B to Acento.LIMA -> R.drawable.ic_app_b_lima_fondo to R.drawable.ic_app_b_lima
    Logo.B to Acento.PIZARRA -> R.drawable.ic_app_b_pizarra_fondo to R.drawable.ic_app_b_pizarra
    Logo.C to Acento.VERDE_MENTA -> R.drawable.ic_app_c_verde_menta_fondo to R.drawable.ic_app_c_verde_menta
    Logo.C to Acento.TURQUESA -> R.drawable.ic_app_c_turquesa_fondo to R.drawable.ic_app_c_turquesa
    Logo.C to Acento.CIAN -> R.drawable.ic_app_c_cian_fondo to R.drawable.ic_app_c_cian
    Logo.C to Acento.AZUL -> R.drawable.ic_app_c_azul_fondo to R.drawable.ic_app_c_azul
    Logo.C to Acento.INDIGO -> R.drawable.ic_app_c_indigo_fondo to R.drawable.ic_app_c_indigo
    Logo.C to Acento.VIOLETA -> R.drawable.ic_app_c_violeta_fondo to R.drawable.ic_app_c_violeta
    Logo.C to Acento.ROSA -> R.drawable.ic_app_c_rosa_fondo to R.drawable.ic_app_c_rosa
    Logo.C to Acento.CORAL -> R.drawable.ic_app_c_coral_fondo to R.drawable.ic_app_c_coral
    Logo.C to Acento.NARANJA -> R.drawable.ic_app_c_naranja_fondo to R.drawable.ic_app_c_naranja
    Logo.C to Acento.AMBAR -> R.drawable.ic_app_c_ambar_fondo to R.drawable.ic_app_c_ambar
    Logo.C to Acento.LIMA -> R.drawable.ic_app_c_lima_fondo to R.drawable.ic_app_c_lima
    Logo.C to Acento.PIZARRA -> R.drawable.ic_app_c_pizarra_fondo to R.drawable.ic_app_c_pizarra
    else -> R.drawable.ic_app_a_violeta_fondo to R.drawable.ic_app_a_violeta
}
