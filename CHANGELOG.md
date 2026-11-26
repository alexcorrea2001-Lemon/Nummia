# Novedades de Nummia

Todas las versiones de Nummia, de la más nueva a la más antigua.

## [2.8.3] - 2026-11-26

### Arreglado
- Modo discreto: al tocar el ojo de Inicio para ver los importes, ya no se apaga el ajuste para siempre. Se ven mientras la app sigue abierta y, al salir de ella, vuelven a ocultarse.

## [2.8.2] - 2026-11-24

### Cambiado
- Inicio: nueva tarjeta de balance. La cabecera va en color con el borde inferior en onda, y debajo están lo que entró, lo gastado y el gasto medio por día, cada cifra con su icono. Sustituye a la curva.

## [2.8.1] - 2026-11-24

### Arreglado
- Inicio: la curva y la barra de colores ya no se vuelven a dibujar cada vez que bajas y subes; solo se animan la primera vez que las ves.
- Inicio: el contenido ya no se mete por debajo de la barra de estado al desplazarte.
- El botón + se queda siempre a la vista en Inicio y en Movimientos (antes se escondía al desplazarte y a veces no volvía).
- Ocultar y mostrar importes ya no da un salto al final: el aviso «Mantén pulsado…» sustituye al título del balance en vez de añadir una línea.

## [2.8.0] - 2026-11-24

### Novedades
- **Inicio renovado:** el balance lleva un degradado y una curva con lo que llevas gastado este mes frente al anterior (discontinua), y la variación va en una píldora de color.
- **Anillo de gasto:** las cifras del mes van junto a un anillo con la parte de lo que entró que ya has gastado («De cada 100 € que entraron…»).
- **En qué se va:** barra de colores con el reparto por categorías y las tres tiendas con su puesto.
- **Calendario:** «Añadir en este día» deja elegir entre gasto e ingreso.

### Arreglado
- En Movimientos, el botón + vuelve a aparecer al llegar al final de la lista.

## [2.7.0] - 2026-11-24

### Nuevo
- **Informes de una persona**: en «A tu medida» elige de quién (por ejemplo, Luis). El informe trae solo lo que has pagado por esa persona, lo que te ha devuelto y lo que te debe, y se llama «Informe de Luis».
- **Tarjetas de Inicio a tu gusto** (Ajustes → Apariencia → Tarjetas de Inicio): muestra u oculta cada tarjeta y cambia su orden con las flechas. El balance del mes siempre va arriba.
- **Historial de sincronización** en Tu Drive: las últimas sincronizaciones con lo que se subió, lo que llegó del ordenador, los conflictos y los errores.

## [2.6.0] - 2026-11-24

### Arreglado
- **Calendario**: el nombre del mes se cortaba («Septiemb…», «Octubre 2…») y no se veía el año. Ahora siempre se ve entero, o abreviado si no cabe («Sept 2026»).

### Nuevo
- **Ficha de cada tienda**:
  - Toca un mes de la gráfica para ver cuánto gastaste.
  - Nueva gráfica **«Lo que cuesta cada compra»** mes a mes, con aviso de si ha subido o bajado en el último año.
- **Presupuestos semanales y anuales**, además de los mensuales. Se agrupan en «Esta semana», «Este mes» y «Este año». Los avisos del 80 % y el 100 % funcionan con cada uno.
- **Te deben**: debajo de cada persona, cuánto suele tardar en devolverte («Suele devolverte en unos 5 días»).

## [2.5.1] - 2026-11-24

### Arreglado
- **Nuevo pago**: la opción «Cada año» no cabía y salía vacía, y la ventana daba saltos al elegir la frecuencia. Ahora es un selector de tres partes (Mensual · Trimestral · Anual) y los meses salen en una cuadrícula, todo a la vista.

### Mejorado (diseño)
- **Todas las ventanas** (nuevo pago o cobro, contraseña de la copia, restaurar, borrar, ir a un mes…) salen ahora **desde abajo**, como en las apps de Google: más cómodas con el pulgar, con título grande, icono, campos con icono y botones más grandes. Ya no dan saltos al crecer.
- **Pantallas de Ajustes** con **título grande** que se encoge al bajar.
- **Sueldo** rediseñado: el importe en grande, cuándo será el próximo previsto y cómo funciona en tres pasos.

## [2.5.0] - 2026-11-24

### Nuevo
- **Calendario con lo que viene**: en los meses siguientes se marcan los días en que tocan tus pagos y cobros fijos (también los trimestrales y anuales). Al elegir uno de esos días ves qué toca y cuánto, y arriba del mes, el total previsto de pagos y de cobros.
- **Gráficas que se tocan**: en «Gasto acumulado» y «Gasto de cada mes», toca o desliza el dedo sobre la gráfica para ver el importe exacto de ese día o de ese mes en cada línea.

## [2.4.0] - 2026-11-24

### Nuevo
- **Copias con contraseña**: al guardar una copia en archivo, Nummia te ofrece protegerla con una contraseña. Va cifrada (AES-256) y, si alguien la modifica o se daña, no se restaura. Para restaurarla se pide la contraseña. Las copias sin contraseña de antes se siguen pudiendo restaurar.
- **Suscripciones**: los pagos fijos pueden ser **cada mes, cada 3 meses o cada año**. Cada uno muestra la **próxima fecha de cobro** y lo que te cuesta **al año**, y arriba ves el total al año de todos tus pagos. Los trimestrales y anuales **avisan 3 días antes** (si tienes los avisos de pagos activados).
- **«A fin de mes te quedarán unos…»** en Inicio (mes en curso): lo que tienes, más el sueldo y los cobros previstos, menos los pagos pendientes y lo que gastarás si sigues a este ritmo. Tócalo para ver el desglose.

## [2.3.0] - 2026-11-24

### Nuevo
- **«El mes en cifras»** en Estadísticas → Mes (sustituye a «Día de más gasto»):
  - Si sigues a este ritmo, cuánto acabarás gastando el mes (solo en el mes en curso).
  - Media al día y gasto medio de cada compra.
  - Gastos apuntados.
  - Días sin gastar.
- **Importes en el lateral** de las gráficas «Gasto acumulado» y «Gasto de cada mes» (0 €, 250 €, 500 €…), con números redondos. En modo discreto se ocultan.

### Mejorado
- **Menú del botón +**: el fondo se oscurece bastante más y las opciones (Gasto, Ingreso, Devolución…) tienen más contraste. Ahora también puedes tocar el texto, no solo el icono.

## [2.2.3] - 2026-11-24

### Arreglado
- **Botón «Confirmar» de las notificaciones**: si el pago o el sueldo ya lo habías confirmado en la app (quizá con otro importe) o habías dicho «Este mes no», el botón de una notificación antigua lo volvía a cambiar. Ahora solo confirma lo que sigue pendiente, y si ya no existe no hace nada (antes podía cerrar la app).
- **Widget al cambiar de mes**: si no abrías la app, seguía mostrando el mes anterior. Ahora se refresca dos veces al día (solo lee tus datos; no usa internet).
- Errores raros al actualizar el widget o al apuntar un cambio para Drive ya no pueden cerrar la app.

### Optimizado
- Fuera una biblioteca de iconos que no se usaba.

## [2.2.2] - 2026-11-24

### Arreglado
- **Cobros fijos editados desde el ordenador**: al cambiar en la hoja de Drive un ingreso que se cobra cada mes, perdía el enlace con su cobro fijo y ese mes podía aparecer otro previsto repetido. Ya no.
- **Modo discreto con huella**: si la app se cerraba de golpe con los importes a la vista, al volver a abrirla siguen ocultos.

### Mejorado
- **Menos batería y datos**:
  - Al abrir la app solo se sincroniza con Drive si hay algo que subir, la última vez falló o han pasado 15 minutos. Antes se sincronizaba cada vez que la abrías.
  - Varios cambios seguidos (borrar varios, deshacer, importar…) actualizan el widget, los avisos y la sincronización una sola vez en lugar de una por cambio.
- **Movimientos**: mientras cargan, se ve la forma de la lista en lugar de un hueco vacío.

## [2.2.1] - 2026-11-22

### Arreglado
- **Calendario**: el botón «Semana» se apretaba junto a «Hoy» y salía en vertical. Ahora está arriba, junto a Ajustes, y la fila del mes queda despejada.
- Los días cuyo saldo redondea a 0 € muestran «0» en lugar de «+0».

## [2.2.0] - 2026-11-22

### Nuevo
- **Buscar por importe**: en Movimientos escribe `>50` (más de 50 €), `<10` (menos de 10 €) o `20-30` (entre 20 y 30 €).
- **Botón «Hoy»** en Inicio, Movimientos y Calendario: aparece cuando no estás en el mes actual y te devuelve a él con un toque.
- **«Repetir hoy»** en el detalle de un gasto: apunta hoy otro igual (mismo concepto, tienda, categoría e importe) y lo abre por si quieres cambiar algo. Sustituye a «Duplicar».
- **Movimientos con notas** llevan un icono pequeño junto al nombre.
- **La fecha de cada día se queda fija arriba** en Movimientos mientras bajas por sus movimientos.

### Cambiado
- **«Devolución» en toda la app** en lugar de «reembolso» (en informes también). La hoja de tu Drive no cambia.
- La etiqueta **«Parcial 7,00 €» ahora dice «Te devolvieron 7,00 €»**.
- **Sin «Total del día» repetido**: si un día tiene un solo movimiento, ya no se muestra (era el mismo importe).
- **Calendario más claro**: cada día muestra un solo importe (lo que entró menos lo que gastaste) en verde o rojo, y hoy se marca con un punto.
- Estadísticas: la pestaña «Dónde gasto» se llama **«Tiendas»**, así caben las cuatro en pantallas estrechas.

## [2.1.3] - 2026-11-22

### Cambiado
- Fuera la flecha morada junto al mes del Calendario y a los años de Comparar: se sigue tocando igual para elegir.

## [2.1.2] - 2026-11-22

### Mejorado
- **Calendario: pasar de mes es mucho más cómodo.** Los meses están uno al lado del otro y siguen al dedo, como el calendario del móvil. Puedes deslizar despacio o rápido, cambiar de idea a mitad y los días del mes de al lado ya se ven mientras deslizas. Todos los meses tienen ahora el mismo alto, así que lo de abajo ya no salta. Las flechas también deslizan.
- **Movimientos: cada día dice de qué año es** cuando buscas en todos los meses o cuando no es de este año («Viernes 12 dic 2025»).
- **Estadísticas → Comparar**: la barra del otro año se ve bien (antes era un gris muy apagado).

## [2.1.1] - 2026-11-22

### Arreglado
- **Buscador de Movimientos**: solo dejaba escribir una letra. Ahora puedes escribir con normalidad.

### Mejorado
- **Calendario más fluido**: al cambiar de mes, la cuadrícula se desliza hacia el lado al que vas con efecto muelle, el nombre del mes cambia con una animación y, al deslizar con el dedo, el calendario te sigue.
- **Estadísticas → Año, barras más claras**: cada barra es lo que entró ese mes y la parte roja es lo que gastaste de ello («el 22 % de lo que entró»). Antes se comparaba con el mes que más gastaste, y por eso ese mes salía con la barra entera en rojo.

## [2.1.0] - 2026-11-21

### Nuevo
- **Exportar todo a CSV** (Ajustes → Datos): un .zip con tus gastos, ingresos, devoluciones y pagos y cobros fijos, listo para abrir en Excel, Google Sheets o Numbers.
- **Qué llegó del ordenador** (Ajustes → Tu Drive): debajo de la última sincronización verás, por ejemplo, «Desde el ordenador: 2 nuevos y 1 cambiado».
- **Recordatorio de copia**: si no tienes Drive conectado, una vez al mes Nummia te recuerda guardar una copia en un archivo (solo si la última tiene más de un mes). Al tocarlo se abre Ajustes → Datos.
- **Inicio en dos columnas** en tablets y plegables abiertos.

## [2.0.0] - 2026-11-21

### Nuevo
- **Cobros fijos**: activa «Se cobra cada mes» al apuntar un ingreso (o añádelo en Ajustes → Pagos y cobros fijos). Cada mes aparece en Inicio con «Cobrado», «Otro importe» o «Este mes no».
- **Filtrar Movimientos por categoría, tienda o persona** (botón de filtros). Los filtros activos se ven arriba y se quitan con un toque.
- **Botón «Nuevo gasto» en los Ajustes rápidos** (el panel que se baja desde arriba): añádelo editando los botones del panel.
- **✓ al guardar**: un gasto, un ingreso o una devolución guardados se confirman con una marca animada.
- **Pasar de un gasto a otro en el detalle**: desliza a los lados o usa las flechas de arriba («3 de 12 del mes»).
- **Ordenar las categorías arrastrando** (Ajustes → Categorías). El orden es el de los chips al apuntar un gasto.
- **Modo discreto con huella**: mantén pulsado el balance de Inicio y, con tu huella, se ven los importes hasta que salgas de la app.
- **A tu gusto** (Ajustes → Apariencia): forma de las tarjetas (redondas, normales o rectas), listas compactas y vibración sí/no.

### Cambiado
- «Pagos mensuales» pasa a llamarse **«Pagos y cobros fijos»**.

## [1.9.0] - 2026-11-21

### Nuevo
- **Te deben, por persona** (desde «Te deben» en Inicio o Ajustes → Personas): cuánto te debe cada uno, qué cosas y desde cuándo, y el botón **«Me lo ha pagado todo»** (con «Deshacer»).
- **Elegir varios en Movimientos**: mantén pulsado un movimiento y toca otros; arriba puedes borrarlos, cambiarles la categoría o la tienda de una vez.
- **Ficha de cada tienda** (toca una tienda en «Dónde gasto» o en el detalle de un gasto): cuánto llevas, veces, media, los últimos 12 meses en un gráfico, sus gastos y cambiar la categoría de todos a la vez.
- **Bienvenida en dos pasos**: después de presentarte Nummia te pregunta cómo empezar: conectar tu Drive, importar tu Excel o empezar de cero.
- **Buscador en Ajustes**: escribe «huella», «drive» o «presupuesto» y lo encuentras.
- **Pantallas grandes** (tablet, plegable o en horizontal): barra lateral y contenido centrado.

### Cambiado
- **4 pestañas abajo**: Informes pasa a Estadísticas (botón «Informes» arriba), y las demás tienen más sitio.
- **Ajustes ordenados por secciones**: Tus datos, Dinero, Aspecto, Seguridad y avisos, y Nummia.
- **Detalle del gasto**: arriba el icono de su categoría, la tienda se puede tocar y el historial es una línea de tiempo.
- **Estados vacíos con dibujo** en vez de un simple icono.

## [1.7.1] - 2026-11-21

### Cambiado
- **Estadísticas → Año, más fácil de entender**: arriba el balance del año en grande con una frase («Has ahorrado el … de lo que has ingresado») y solo tres cifras: ingresos, gasto real y gasto al mes. Después el gráfico mes a mes, «Lo destacado» en frases (el mes que más y el que menos gastaste, lo que te han devuelto) y la lista de meses con una barra.
- **Estadísticas → Comparar, más visual**: el año que comparas destacado, y para gasto, ingresos y ahorro dos barras (una por año) con la diferencia en verde si es buena y en rojo si es mala.
- **Informes → En un toque**: se quita «Lo que me deben» (sigue en «A tu medida»).

### Arreglado
- **Informes**: al pulsar un atajo o «Crear informe» la pantalla sube sola hasta el informe, se ve «Creando el informe…» y avisa con «Informe listo».

## [1.7.0] - 2026-11-21

### Nuevo
- **Ir a cualquier mes al momento**: toca el nombre del mes (en el Calendario, Inicio o Movimientos) y elige año y mes. Sin deslizar mes a mes.
- **Teclado propio para el importe**, grande y con sumas y restas: escribe «12+3,50» y se apunta 15,50 €. Se ve el total mientras escribes.
- **Accesos directos**: mantén pulsado el icono de Nummia y elige «Nuevo gasto» o «Nuevo ingreso».
- **Widget** para la pantalla de inicio: el balance del mes y un botón «+ Gasto». Se actualiza solo cuando cambias algo (no gasta batería) y respeta el modo discreto.
- **Botones en las notificaciones**: confirma un pago mensual o el sueldo con «Confirmar» sin abrir la app, o pulsa «Otro importe».
- **Deshacer al editar**: después de cambiar un gasto aparece «Gasto actualizado · Deshacer».

### Cambiado
- **Informes más bonitos**: formato elegido con dos tarjetas (PDF para leer o imprimir, Excel para hacer cuentas), atajos con icono y lo que incluyen, «A tu medida» y una lista de tus informes con su tipo y un botón para compartir.
- **Categorías con su icono y color** al elegirlas en el formulario de gasto.

## [1.6.0] - 2026-11-21

### Nuevo
- **Iconos en las categorías**: 33 iconos para elegir (Ajustes → Categorías). Cada movimiento muestra el icono y el color de su categoría.
- **Inicio**:
  - Desliza a los lados para cambiar de mes.
  - Todo se puede tocar: el balance y las cifras llevan a Estadísticas del mes, «Dónde más has gastado» a «Dónde gasto», «Ahorrado» al año y «Te deben» a los pendientes.
  - Si el sueldo aún no está confirmado, el balance dice cómo quedará: «Con el sueldo previsto: +…».

### Cambiado
- **Inicio más limpio**: las cuatro cifras en una sola tarjeta y sin la lupa arriba (buscar está en Movimientos), para que el mes respire.
- **Movimientos**: los de un mismo día forman un solo bloque.
- **El botón + ya no tapa nada**: se esconde al bajar por una lista y vuelve al subir.
- **Tema oscuro con más contraste**: las tarjetas se distinguen mejor del fondo y los textos grises se leen mejor.

### Más rápido
- Las tareas de mantenimiento (papelera, tiendas, categorías, pagos previstos) se hacen una vez al día y no en cada apertura.

## [1.5.2] - 2026-11-21

### Arreglado
- **Nombre del mes**: ya nunca se corta el año. Si no cabe entero, se abrevia («Nov 2026»).
- **Tiendas iguales**: «BonoBus» y «Bonobús» (o con espacios distintos) son la misma tienda. Se unen solas con la forma que más usas, también al apuntar un gasto nuevo y al importar el Excel.
- **Guardar informe en Drive**: el botón muestra «Guardando…» y después «En tu Drive».

### Mejorado
- **Excel con más diseño**: título y periodo, cabecera en el color de Nummia que se queda fija al bajar, filtros, filas alternas, totales destacados y listo para imprimir en horizontal.
- **Vista previa del Excel** dentro de la app, con una pestaña por hoja, igual que la del PDF.

## [1.5.1] - 2026-11-21

### Arreglado
- **Inicio**: los iconos de arriba ya no cambian de tamaño según lo largo que sea el nombre del mes.
- **Se paga cada mes**: ahora también se puede activar (o quitar) al editar un gasto que ya existía, y el pago aparece en Ajustes → Pagos mensuales.

### Cambiado
- **Movimientos, más clara**: arriba el mes que estás viendo, debajo el buscador y unas pestañas que siempre se ven enteras. Cada día indica su «Total del día». Se quita la barra flotante con el número de movimientos.
- **Categoría automática**: si un gasto se guarda sin categoría y esa tienda ya tiene una en otros gastos (por ejemplo, Cine → Ocio), se pone sola. Al poner categoría a un gasto, los de la misma tienda que no tenían ninguna la toman. También se aplica a lo importado del Excel y a lo que ya tenías.

## [1.5.0] - 2026-11-21

### Nuevo
- **Informes** (pestaña Informes), en **PDF** o en **Excel**:
  - Rápidos: este mes, el año y **Lo que me deben** (por persona, con lo esperado, lo devuelto y lo que falta).
  - Personalizado: elige el periodo (este mes, el anterior, este año, el anterior o las fechas que quieras) y qué incluir: resumen, mes a mes, movimientos, por tienda, por categoría y lo que me deben.
  - Vista previa de la primera página del PDF antes de compartir.
  - Compartir, guardar en el móvil o guardar en tu Drive (`Nummia_Datos/Informes`).
  - Lista de los últimos 20 informes creados.
  - El Excel lleva euros y fechas con su formato y se abre en Excel, Google Sheets y LibreOffice.

### Mejorado
- **La app pesa casi la mitad**: de 9 MB a unos 5 MB. Menos datos al descargar cada actualización.
- **Más fluida**: las cuentas de Inicio, Movimientos, Calendario, Estadísticas y el detalle se hacen fuera del hilo de la pantalla, y Android prepara el código de Nummia al instalarla para que arranque más rápido.
- Guardar y abrir copias ya no puede congelar la pantalla un instante.

## [1.4.0] - 2026-11-21

### Nuevo
- **Presupuestos** (Ajustes → Presupuestos): un límite al mes en total, por categoría o por tienda.
  - En Inicio verás una barra por presupuesto: se pone ámbar al 80 % y roja al pasarte.
  - Cuenta el gasto real: los reembolsos restan y lo previsto no cuenta.
- **Tiendas** (Ajustes → Tiendas): une las que son la misma tienda escrita de varias formas, o cámbiales el nombre. Nummia te sugiere las que parecen repetidas. El cambio llega también a tu hoja de Drive.
- **Categorías** (Ajustes → Categorías): crea las tuyas, renombra cualquiera y cambia su color.
- **Notificaciones** (Ajustes → Notificaciones), desactivadas hasta que las enciendas:
  - Sueldo sin confirmar, desde el día 27.
  - El día que toca un pago mensual.
  - Presupuesto al 80 % y al pasarte, una sola vez al mes.
  - Sin internet y sin gastar batería: revisa una vez al día y poco después de cada cambio.
- **Bloqueo** (Ajustes → Seguridad): pide tu huella, tu cara o el PIN del móvil al abrir Nummia y al volver tras más de 1 minuto fuera.

## [1.3.1] - 2026-11-21

### Arreglado
- **Importar Excel**: el archivo salía en gris y no se podía elegir. Ahora se puede elegir cualquier archivo y la app comprueba que es un Excel.
- Si eliges una hoja de cálculo de Google Drive, se descarga como Excel automáticamente.
- Si el archivo es un Excel antiguo (.xls) o de LibreOffice (.ods), se explica cómo guardarlo como .xlsx.

## [1.3.0] - 2026-11-21

### Nuevo
- **Importar tu Excel de cuentas** (Ajustes → Datos → Importar Excel):
  - Lee los gastos de cada mes con su fecha, su precio y su reembolso, el sueldo de cada mes y las ganancias extraordinarias.
  - Une los nombres escritos de varias formas (mayúsculas, tildes o una errata) y se queda con la más usada.
  - **Antes de guardar** te enseña un resumen y comprueba mes a mes que el gasto real **cuadra con tu Excel**, con avisos de lo que no ha podido leer.
  - Puedes marcar los gastos que pagaste por otra persona y elegir quién te los devuelve.
  - Antes de importar se guarda una copia de seguridad.
- **Pagos mensuales** (Ajustes → Pagos mensuales, o el interruptor «Se paga cada mes» al apuntar un gasto):
  - Cada mes aparecen en Inicio como pendientes con **Confirmar**, **Otro importe** o **Este mes no**.
  - Si el precio cambia, el nuevo precio se queda para los meses siguientes.
  - Total al mes, pausar, editar y lo que has pagado cada mes.
  - Lo previsto no cuenta en tus cifras hasta que lo confirmas.
- **Papelera** (Ajustes → Datos → Papelera): lo borrado se guarda 30 días para recuperarlo; después desaparece solo.
- **Buscar por fecha**: en Movimientos puedes escribir «15/03», «marzo», «marzo de 2026» o «2026».

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
