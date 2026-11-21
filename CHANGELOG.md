# Novedades de Nummia

Todas las versiones de Nummia, de la más nueva a la más antigua.

## [1.0.5] - 2026-11-21

### Cambiado
- **Icono N**: vuelve el diseño de la N con las tres barras que suben encima.
- **Icono de crecimiento**: la flecha ya no toca las barras.

### Arreglado
- **El icono del móvil no cambiaba**: algunas pantallas de inicio guardan el icono antiguo y no lo vuelven a leer. Ahora la app usa entradas nuevas en el lanzador, que el móvil no puede tener guardadas.
- Nuevo botón **«Poner este icono en la pantalla de inicio»** en Apariencia: añade un acceso directo con el icono elegido, que se ve bien en cualquier móvil.

## [1.0.4] - 2026-11-21

### Cambiado
- **Iconos de la app más sencillos**: versión plana de la N que sube, la cartera y el crecimiento, sin degradados ni bordes más claros. Ocupan mucho menos y se ven nítidos en cualquier pantalla.

## [1.0.3] - 2026-11-21

### Arreglado
- El aviso «Nummia vX está disponible» desaparece al instalar esa versión.
- **Icono de la app**: el cambio se aplica al salir de Nummia, para que la pantalla de inicio del móvil lo muestre.
- **Ajustes** más sencillos: iconos sueltos, sin círculos de color, y dibujos más limpios.

## [1.0.2] - 2026-11-21

### Nuevo
- **Iconos nuevos**: N que sube, cartera y crecimiento, en los 12 colores. Se eligen en Ajustes → Apariencia → Icono de la app.
- **Icono de Ajustes** renovado.

### Arreglado
- La tarjeta de actualizaciones ya no se encoge al buscar.
- Se quita de Inicio el icono del móvil: no hacía nada. Volverá en la versión 1.2.0 para mostrar si tus datos están guardados en Drive.

## [1.0.1] - 2026-11-21

### Arreglado
- **Ajustes**: las opciones ya no se abren solas. Antes se abrían todas a la vez, lo que provocaba un ciclo al pulsar «Atrás», abría GitHub sin pedírselo y lanzaba el selector de archivos de la copia de seguridad.
- **Acerca de Nummia**: la privacidad y la licencia se leen dentro de la app; ya no se abre el navegador.
- **Logo** dibujado con trazos continuos y esquinas redondeadas, igual que el icono del móvil.
- **Nuevo gasto**: el «0,00» queda pegado al «€».
- **Barra inferior**: los nombres de las pestañas ya no se cortan; se ajustan al espacio.
- **Buscar y «Ver todos»** desde Inicio: ahora se puede volver a Inicio con la barra inferior o con «Atrás».

## [1.0.0] - 2026-11-20

Primera versión de Nummia.

### Nuevo
- **Gastos**: apunta qué has pagado, dónde, cuándo y cuánto. Categoría opcional.
- **Reembolsos**: varios por gasto, aunque te devuelvan más de lo que pagaste (el sobrante cuenta a tu favor). Cuentan en el mes de la compra.
- **Pago para otra persona**: marca «lo pago yo, pero me lo devuelven», elige quién y cuánto te va a dar. Nummia te recuerda en «Te deben» lo que falta.
- **Ingresos**: sueldo, extras (regalos, cumpleaños…), ventas, trabajos extra y otros.
- **Sueldo previsto**: cada mes aparece a final de mes y se confirma con el día y el importe reales.
- **Inicio**: balance del mes animado, comparación con el mes anterior, pendientes, dónde más has gastado, ahorrado en el año y últimos movimientos.
- **Movimientos**: búsqueda (sin importar tildes), filtros, orden y lista por días. Desliza a la izquierda para borrar (con «Deshacer») y a la derecha para «Repetir hoy».
- **Detalle de gasto**: cuentas, historial de reembolsos, «Devuelto entero», duplicar e historial de cambios.
- **Apariencia**: temas Automático, Claro, Oscuro y Negro AMOLED; 12 colores, color personalizado o colores del fondo de pantalla; tamaño del texto.
- **Icono de la app**: 3 logos y 12 colores, independiente del color de la app.
- **Modo discreto**: oculta los importes con un toque.
- **Copia de seguridad** en un archivo `.nummia` para guardar y restaurar.
- **Actualizaciones** desde la propia app, comprobadas con huella SHA-256 y firma.

### Seguridad
- Base de datos cifrada (SQLCipher) con clave protegida por el chip de seguridad del móvil.
- Sin servidores, sin anuncios, sin rastreo. Los datos no salen del móvil.
