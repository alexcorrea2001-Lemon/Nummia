# v3.0.0 — Escanear tickets y selector de letra

Pedido por el usuario el 27/11/2026. Los avisos del banco y de Google Wallet quedan **descartados** (no implementar).

## Por qué
Apuntar un gasto a mano lleva tiempo: importe, tienda y fecha están ya impresos en el ticket.

## Qué hace
- «Nuevo gasto» tiene un botón «Escanear ticket» (no aparece al editar un gasto).
- Abre el escáner de documentos de Google Play Services (recorta y endereza; admite una foto de la galería; no hace falta permiso de cámara).
- El texto se reconoce en el móvil (ML Kit, versión de Play Services: no pesa en el APK). Las líneas que el reconocimiento separa en columnas se vuelven a unir por filas (`juntarFilas`).
- `leerTicket` saca el importe (línea «TOTAL», con preferencia por «a pagar»; si no, «IMPORTE»; si no, el mayor importe que no sea IVA ni cambio), la tienda (primera línea que parece un nombre, sin forma legal, con el nombre que el usuario ya usa si coincide) y la fecha (dd/MM/aaaa, dd-MM-aa o aaaa-MM-dd; nunca futura ni de hace más de 2 años; ignora «válido hasta»).
- Rellena el formulario (el concepto queda como «Compra» si estaba vacío). **Nunca guarda solo**: el usuario revisa y guarda.
- Estados: leyendo (botón con progreso), ticket vacío o ilegible, importe no encontrado, escáner no disponible.
- La foto se borra tras leerla y no se guarda en ningún sitio.

## No objetivos
Leer línea a línea los productos; guardar la foto del ticket; guardar sin revisar; avisos del banco o de Wallet.

## Enfoques
- *Escáner de documentos + OCR de Play Services (elegido)*: poco código, sin permisos, APK casi igual.
- *CameraX + OCR incluido en el APK*: más control, pero suma varios MB y pide permiso de cámara.
- *Tesseract*: sin Google, pero pesa más y lee peor los tickets.

## Selector de letra y tamaño
Tarjetas iguales (mismo alto, una sola línea de texto): «Sistema / Serifa / Mono» con su «Aa», y los cuatro tamaños con «Aa» a su escala real.

## Límites conocidos
Un ticket arrugado o con poca luz se lee mal; por eso todo es editable. No se ha podido probar en un móvil.
