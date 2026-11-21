# Novedades de Nummia

Todas las versiones de Nummia, de la más nueva a la más antigua.

## [1.2.2] - 2026-11-21

### Arreglado
- Si la hoja del año se quedó a medio crear (sin conexión o con una API de Google sin activar), se termina de preparar sola en la siguiente sincronización.
- En «Tu Drive» se ve el correo de tu cuenta de Google en lugar de «Google».
- Los errores de configuración de Google Cloud más habituales se explican en español y dicen qué hacer.

## [1.2.1] - 2026-11-21

### Arreglado
- **Conectar con Google**: si la conexión falla, ahora se ve en pantalla un aviso que no desaparece, con el motivo y el código de Google, en lugar de no pasar nada.
- Mientras se conecta se muestra una barra de progreso.

## [1.2.0] - 2026-11-21

### Nuevo
- **Tu Drive** (Ajustes → Tu Drive): conecta tu cuenta de Google y Nummia guarda tus datos en la carpeta **Nummia_Datos** de tu Drive.
  - **Una hoja de cálculo por año** (`Nummia_2026`, `Nummia_2027`…) con las pestañas Resumen, Gastos, Ingresos y Reembolsos, con formato de euros y fechas.
  - **Puedes editarla desde el ordenador**: los cambios llegan al móvil y los del móvil a la hoja. Las filas nuevas que escribas en el ordenador se crean en el móvil con su código.
  - Si cambias lo mismo en los dos sitios, Nummia **no elige por ti**: te enseña las dos versiones y tú decides.
  - Las filas de la hoja que no se puedan leer (sin fecha, importe mal escrito…) se avisan y no se tocan.
  - Solo se sube lo confirmado; lo previsto se queda en el móvil.
- **Copias de seguridad en tu Drive** (carpeta `Copias_de_seguridad`): una al día si has cambiado algo, y siempre antes de recuperar una copia, importar un archivo o instalar una actualización. Se conservan las de los últimos 7 días, 4 semanas y 12 meses. Puedes recuperar cualquiera desde la app.
- **Móvil nuevo**: al conectar Drive en un móvil vacío, Nummia te ofrece recuperar la última copia.
- Icono de estado en Inicio: sincronizando, todo al día o algo que mirar.
- Opción **«Solo con Wi-Fi»** para la sincronización.

### Cuida tu batería y tus datos
- Sincroniza al abrir la app, unos segundos después de cada cambio (los cambios seguidos se agrupan) y cada 12 horas, solo con conexión y batería suficiente.
- Antes de leer la hoja comprueba si ha cambiado: si nada ha cambiado en ningún sitio, no descarga nada.

## [1.1.0] - 2026-11-21

### Nuevo
- **Estadísticas**, con cuatro pestañas:
  - **Mes**: resumen (sueldo, otros ingresos, pagado, reembolsos, gasto real, balance y % ahorrado), gráfico del gasto acumulado frente al mes anterior, reparto por tienda o por categoría y el día de más gasto.
  - **Año**: los 12 indicadores del año (medias, mes de más y de menos gasto…), barras de ingresos y gasto real de cada mes, ahorro acumulado y tabla de los 12 meses. Toca un mes para verlo en detalle.
  - **Comparar**: dos años lado a lado, con frases claras («En 2027 has gastado… más que en 2026») y sus gráficos.
  - **Dónde gasto**: ranking de tiendas de este mes, este año o desde siempre, con veces y media por visita. Toca una tienda para ver todos sus gastos.
- **Calendario**: cada día muestra lo gastado y lo ingresado, con un círculo en los días con algo previsto. Desliza para cambiar de mes, cambia a la vista de semana y toca un día para ver sus movimientos o **añadir un gasto en ese día**.
- Gráficos animados propios, que no aumentan el tamaño de la app.
- Aviso «Actualizado: reembolso recibido el…» cuando un reembolso llega en un mes posterior al del gasto.

## [1.0.6] - 2026-11-21

### Arreglado
- **Iconos centrados**: los tres dibujos quedan simétricos, con el mismo margen a cada lado. La flecha del icono de crecimiento empieza y acaba alineada con las barras.

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
