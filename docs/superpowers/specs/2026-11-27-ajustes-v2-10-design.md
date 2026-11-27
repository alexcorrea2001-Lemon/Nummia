# v2.10.0 — Tiendas, Personas, Pagos, portada de Ajustes y cursor en el importe

Aprobado por el usuario el 27/11/2026 sobre las maquetas del lienzo
«Nummia · Rediseño de Ajustes (v2.10)». Datos de las maquetas inventados.

## Por qué
Las pantallas funcionaban pero escondían las acciones (casillas para renombrar,
tocar el nombre para renombrar, papelera sin confirmar, desplegar para editar) y
obligaban a dar pasos de más. Objetivo: más intuitivo, bonito y fluido. En el
importe, cambiar una cifra del medio obligaba a borrar todo lo que había detrás.

## Qué cambia
- **Portada de Ajustes**: las tres pastillas pasan a ser tres botones en rejilla.
  «Importes» activa/desactiva el modo discreto al momento; «Bloqueo» abre Seguridad
  y «Avisos» abre Notificaciones. Las filas de Tiendas, Personas y Pagos muestran
  un resumen útil (nº de tiendas, cuánto te deben, coste al mes).
- **Tiendas**: buscador, orden (más veces / más gasto / A–Z), total arriba, aviso
  de repetidas compacto con «Unir». Sin casillas: tocar una abre un panel con
  total, veces, media, «Ver sus gastos» (ruta TIENDA existente), «Cambiar nombre» y
  «Unir con otra tienda…». Mantener pulsado entra en modo selección con barra
  inferior «Unir N tiendas». Cada fila: inicial en círculo, veces y fecha del último gasto.
- **Personas**: tarjeta «Te deben en total» que abre «Te deben»; cada persona con
  iniciales, cuánto debe o «Al día»; «Añadir persona» como última fila. Tocar una
  abre un panel con lo pendiente, «Me lo ha pagado todo», «Cambiar nombre» y
  «Quitar» (con confirmación y «Deshacer»). La hoja «Nueva persona» abre el teclado
  sola, Intro guarda y el botón es grande. Sin sugerencias de nombres
  (el autocompletado está descartado).
- **Pagos y cobros fijos**: selector Pagos | Cobros con su número; resumen con el
  próximo pago; filas tocables (interruptor en la fila) que abren un panel con
  próximo, coste al año, total pagado, historial, Editar, Pausar/Reanudar y Borrar
  (con «Deshacer»). «Añadir» como última fila. Textos largos fuera.
- **Cursor en el importe**: al tocar una cifra del importe se coloca ahí un cursor;
  las teclas escriben en ese punto y ⌫ borra la cifra de antes. Sin tocar, el cursor
  va al final y todo funciona como antes. Respeta coma única por número, 2 decimales
  y las operaciones +/−. El cursor parpadea salvo con «Quitar animaciones».

## Enfoques considerados
- *Rediseñar con el estilo actual (elegido)*: mejora el uso ya; el estilo final
  (Ticket/Franjas/Hoja) se aplicará después sin tocar la lógica.
- *Esperar a elegir dirección visual*: el usuario lo descartó para esta entrega.
- Cursor: *tocar para colocar cursor (elegido)* frente a flechas ◀ ▶ en el teclado
  (ocupa sitio y es más lento) o abrir el teclado del sistema (pierde + y −).

## Restricciones
Reglas de CLAUDE.md: dinero en Long, TDD para la lógica de edición del importe,
tokens del diseño, AA/TalkBack (acciones personalizadas para mover el cursor),
«Quitar animaciones», sin verticalScroll ni Lazy dentro de DialogoNummia.

## Decisiones del constructor
Colores de avatar a partir del nombre con la paleta existente; textos exactos;
tamaño del cursor.
