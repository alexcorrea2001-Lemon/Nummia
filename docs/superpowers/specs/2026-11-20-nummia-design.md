# Nummia — Documento de diseño

- **Fecha:** 20/11/2026
- **Estado:** completo; pendiente de la aprobación final
- **Repositorio:** https://github.com/alexcorrea2001-Lemon/Nummia (público, licencia MIT)

Las decisiones marcadas con **[decisión técnica]** las ha tomado el desarrollador y el propietario puede cambiarlas. Todo lo demás lo ha decidido el propietario durante la conversación de diseño.

---

## 1. Intención

Nummia sustituye una hoja de Excel anual que el propietario rellena a mano cada mes. Esa hoja tiene 12 bloques mensuales con las columnas Gasto · Precio · Reembolso, el sueldo y un total.

Lo que se busca con la app:

- **Apuntar cualquier gasto en segundos desde el móvil**, sin fórmulas ni copiar y pegar entre meses.
- **Saber de verdad cuánto se gasta**, teniendo en cuenta los reembolsos. El propietario paga a menudo cosas por otros (familiares o amigos) y luego le dan el dinero en efectivo, a veces redondeado hacia arriba.
- **Ver en qué sitios se va el dinero:** la tienda o el lugar es la dimensión principal y la categoría es opcional.
- **Tener los datos en su Google Drive** como hoja de cálculo, para consultarlos **y editarlos** desde el ordenador si el móvil no está disponible.
- **App pública:** cualquiera puede descargar el APK desde GitHub y conectar **su propio** Drive.

## 2. Pilares (no negociables, se aplican a cada cambio)

1. **Animaciones y fluidez:** transiciones con elemento compartido (lista → detalle), números animados, gráficos que se dibujan, gestos con respuesta háptica, gesto de volver predictivo, movimiento con efecto muelle de Material 3, sin trabajo en el hilo principal y arranque en frío por debajo de 1 s en un móvil actual. Se respeta la opción de Android «Quitar animaciones».
2. **Batería:** ningún servicio permanente. Todo el trabajo en segundo plano va por WorkManager, con condiciones de red y de batería y agrupando cambios. No se usa ubicación, publicidad, analítica ni rastreadores.
3. **Datos móviles:**
   - Sincronización incremental, solo de las filas que cambian.
   - Antes de leer la hoja se hace una consulta mínima de metadatos para saber si ha cambiado.
   - Las actualizaciones se descargan solo con Wi-Fi por defecto.
   - No se descargan imágenes.
   - El APK debe pesar menos de 10 MB.
4. **Diseño profesional:**
   - Un sistema de diseño único (tokens de color, tipografía, espaciado en múltiplos de 4 dp, formas, iconos).
   - Cifras tabulares en formato español `1.520,75 €`.
   - Pantallas vacías, de carga y de error cuidadas.
   - Pantalla de borde a borde.
   - Accesibilidad: contraste mínimo AA, escalado de texto y TalkBack.
   - Significado fijo de los colores (ingreso verde, gasto rojo).

Además: **segura, exacta y con los menos fallos posibles.** Las fórmulas económicas tienen pruebas automáticas y el pipeline no publica nada si falla una prueba.

## 3. Objetivos, no objetivos y antiobjetivos

**Objetivos**
- Registrar gastos, ingresos y reembolsos (varios por gasto) sin conexión.
- Calcular automáticamente los resúmenes mensuales y anuales.
- Sincronizar en los dos sentidos con una hoja de Google por año.
- Hacer copias de seguridad en Drive.
- Actualizarse desde la propia app.

**No objetivos**
- Conectarse a bancos o conocer el saldo bancario real.
- Varias monedas o idiomas: solo EUR y español.
- Tablets o Wear OS.
- Publicar en Google Play (podrá hacerse más adelante).

**Antiobjetivos** (fracaso aunque «funcione»)
- Que algún dato del usuario acabe en el repositorio o en un servidor de terceros.
- Que un cálculo dé un céntimo de diferencia respecto a la realidad.
- Que la sincronización duplique o pierda movimientos.
- Que la app gaste batería o datos en segundo plano sin motivo.
- Que se note lenta, brusca o con aspecto de plantilla.

**Ideas descartadas por el propietario** (no implementar): autocompletado del formulario, calendario coloreado por intensidad de gasto, «lo más repetido», comparar dos meses, comparar el mismo periodo, «dinero adelantado en el año» y recordatorio nocturno.

## 4. Restricciones

- **Repositorio público:**
  - Ningún secreto en el código. El keystore, sus contraseñas y el ID de cliente OAuth van como secretos de GitHub Actions o en `local.properties` (ignorado por git).
  - Nunca se suben datos reales: las pruebas usan datos inventados y `*.xlsx` está ignorado en la raíz.
- **El móvil del propietario usa Android 17.** `targetSdk` = el último estable disponible en el momento de compilar. `minSdk = 26` (Android 8). **[decisión técnica]**
- **Instalación por APK desde GitHub Releases**, sin Play Store.
- **Nombre del paquete:** `io.github.alexcorrea2001lemon.nummia` **[decisión técnica]**. No se puede cambiar una vez registrado en Google Cloud.
- **Un único proyecto de Google Cloud**, del propietario, en estado «En producción». Los usuarios no necesitan el suyo; solo quien compile su propia copia del código.

## 5. Enfoques considerados

### 5.1 Plataforma
| Opción | Ventajas e inconvenientes | Elección |
|---|---|---|
| **Android nativo (Kotlin + Jetpack Compose)** | Mejor fluidez, consumo y tamaño; acceso directo a WorkManager, PackageInstaller, biometría y Keystore | ✅ |
| Flutter | Multiplataforma (no se necesita); APK más pesado y peor integración con el sistema | ✗ |
| Web / PWA | No puede autoactualizarse como APK ni trabajar en segundo plano de forma fiable | ✗ |

### 5.2 Almacenamiento en Drive (elegido por el propietario)
| Opción | Ventajas e inconvenientes | Elección |
|---|---|---|
| **Hoja de Google, una por año** + exportar .xlsx | Actualización fila a fila, editable desde el ordenador, historial de versiones de Google | ✅ |
| Archivo .xlsx en Drive | Hay que reescribirlo entero en cada cambio y no se puede editar a la vez | ✗ |

### 5.3 Sincronización (elegida por el propietario: en los dos sentidos)
| Opción | Ventajas e inconvenientes | Elección |
|---|---|---|
| Solo la app escribe y la hoja es de consulta | Simple, pero no sirve si el móvil se rompe | ✗ |
| **En los dos sentidos, con instantánea por fila** | Permite editar desde el ordenador; exige detectar cambios, conflictos y borrados | ✅ |

### 5.4 Acceso a la API de Google **[decisión técnica]**
Se usa el REST de Sheets v4 y Drive v3 con OkHttp y kotlinx.serialization, **no** las bibliotecas cliente de Google. Esas bibliotecas añaden varios MB y van en contra del pilar 3.

## 6. Modelo de datos (Room, base de datos cifrada)

Reglas generales:

- **Importes:** `Long` en céntimos. Nunca `Double` ni `Float`.
- **Fechas:** `LocalDate`.
- **Identificadores:**
  - Cada registro sincronizable tiene un `id` (UUID, oculto en la hoja) y un `codigo` legible: `G-0001` (gasto), `I-0001` (ingreso), `R-0001` (reembolso).
  - El código es secuencial por tipo y global en la cuenta: se calcula como el máximo conocido (local y de la hoja) + 1.
  - Las filas creadas desde el ordenador sin código reciben uno en la siguiente sincronización.
- **Borrado:** lógico (`deletedAt`), con papelera de 30 días.
- **Campos comunes:** `createdAt`, `updatedAt`, `deletedAt`.

| Entidad | Campos |
|---|---|
| **Gasto** | id, codigo, fecha, concepto, tiendaId, categoriaId?, importe, meLoDevuelven (bool), personaId?, importeEsperado?, notas, pagoMensualId?, estado (`CONFIRMADO` \| `PREVISTO`) |
| **Reembolso** | id, codigo, gastoId, fecha? (puede faltar en lo importado), importe, personaId?, nota |
| **Ingreso** | id, codigo, fecha, concepto, tipo (`SUELDO` \| `EXTRA` \| `VENTA` \| `TRABAJO_EXTRA` \| `OTRO`), importe, notas, estado (`CONFIRMADO` \| `PREVISTO`) |
| **Persona** | id, nombre |
| **Tienda** | id, nombre, alias (lista de nombres unificados en esta tienda) |
| **Categoria** | id, nombre, icono, color, predefinida (bool) |
| **PagoMensual** | id, concepto, tiendaId, categoriaId?, importeActual, dia, activo, historial de precios (desde, importe) |
| **Presupuesto** | id, ámbito (`TOTAL` \| `CATEGORIA` \| `TIENDA`), refId?, limiteMensual |
| **HistorialCambio** | id, entidad, entidadId, campo, valorAntes, valorDespues, origen (`MOVIL` \| `ORDENADOR` \| `IMPORTACION` \| `RESTAURACION`), fecha |
| **EstadoSync** | entidad, entidadId, hojaAnio, fila, hashUltimaSincronizacion |
| **ColaSync** | operaciones pendientes de subir |

Los ajustes (tema, acento, sueldo habitual, bloqueo, modo discreto, preferencias de sincronización y actualización) van en DataStore.

## 7. Reglas de cálculo

Para un gasto concreto:
- **Gasto real del gasto** = importe − Σ reembolsos. **Puede ser negativo**, por ejemplo 473,20 € pagados y 475,00 € recibidos dan +1,80 € a favor del usuario.
- **Falta por recuperar** = max(0, importeEsperado − Σ reembolsos). Solo aplica si `meLoDevuelven`.

Para un periodo (mes o año). Solo cuenta lo `CONFIRMADO` y no borrado:
- **Ingresos** = Σ ingresos con fecha en el periodo.
- **Pagado** = Σ importes de gastos con fecha en el periodo.
- **Reembolsos** = Σ reembolsos de los gastos cuya **fecha de gasto** cae en el periodo, sea cual sea la fecha en que se recibió el dinero (decidido por el propietario, ver 13.1). Por eso las cifras de un mes pasado pueden cambiar cuando llega un reembolso más tarde; la app lo indica con un aviso discreto en ese mes («Actualizado: reembolso recibido el 03/02»).
- **Gasto real** = Pagado − Reembolsos.
- **Balance** = Ingresos − Gasto real.
- **% ahorro** = Balance / Ingresos × 100. Si no hay ingresos, se muestra «—».
- **Media mensual** = total del año / número de meses transcurridos con datos.
- **Ahorrado en el año** = Σ de los balances mensuales del año (el equivalente a la celda «Total 2026»).
- **Te deben** = Σ «falta por recuperar», agrupado por persona.

Redondeo: no hace falta, todo son enteros. Para mostrar los porcentajes se redondea a 1 decimal (HALF_UP).

## 8. Pantallas

Navegación: barra inferior con **Inicio · Movimientos · Calendario · Estadísticas · Informes**. Arriba a la derecha, 🔍 buscador y ⚙️ Ajustes. Botón ➕ flotante que se despliega con: Gasto, Ingreso, Reembolso y Pago para otra persona.

### Inicio
- Selector de mes `‹ Noviembre 2026 ›` e icono de estado de sincronización (✓ / ⏳ / ⚠️).
- **Tarjeta de balance**, con el número animado y la comparación con el mes anterior: «Has gastado un 12 % menos que en octubre».
- Cuatro tarjetas: Ingresos, Pagado, Reembolsos y Gasto real. Al tocarlas se va a Estadísticas → Mes.
- **Pendientes:**
  - Sueldo previsto: aparece desde el día 27 y se confirma con el día y el importe reales.
  - Pagos mensuales previstos: Confirmar / Cambiar importe / Este mes no.
  - **Te deben**, por persona.
- Barras de presupuesto, solo si hay presupuestos.
- Las **3 tiendas** con más gasto del mes.
- **Ahorrado en el año.**
- Últimos 5 movimientos.

### Movimientos
- Pestañas: Todos · Gastos · Ingresos · Pendientes de devolver.
- Lista agrupada por día, con el total diario. Las filas llevan una etiqueta de estado: devuelto 100 %, parcial, te deben, previsto o a tu favor.
- Filtros por fechas, tipo, tienda, categoría, persona, con o sin reembolso e importe mínimo y máximo.
- Orden por fecha, importe o tienda.
- Gestos:
  - Deslizar a la izquierda: **Borrar**, con «Deshacer» y papelera.
  - Deslizar a la derecha: **Repetir hoy**.
  - Mantener pulsado: selección múltiple.
- Barra inferior con el número de movimientos y el total filtrado.

**Detalle de gasto:**
- Datos y código.
- Cuentas: pagado, devuelto, falta por recuperar y gasto real.
- Quién lo devuelve.
- Historial de reembolsos, con alta, edición y borrado.
- Botón «Me lo han devuelto entero».
- **Historial de cambios.**
- Editar / Duplicar / Borrar.

**Formulario de gasto**, en este orden:
1. Importe, con teclado numérico y coma decimal.
2. Concepto.
3. Tienda.
4. Fecha: Hoy / Ayer / calendario.
5. Categoría (opcional, se sugiere según la tienda).
6. Interruptor «Lo pago yo, pero me lo devuelven»: Quién, Cuánto te van a dar (por defecto, el precio) y ¿Ya te lo han dado?
7. Interruptor «Se paga cada mes».
8. Notas.
9. Guardar / Guardar y añadir otro.

**Pago para otra persona** (opción del ➕): abre el mismo formulario de gasto, con el interruptor «me lo devuelven» ya activado. No es una entidad distinta.

**Sueldo previsto:** cada mes se crea un `Ingreso` `PREVISTO` con el importe habitual, fechado el último día del mes. Aparece en Pendientes desde el día 27 y al confirmarlo pasa a `CONFIRMADO` con el día y el importe reales. Si no hay sueldo habitual configurado, no se crea.

**Importe esperado:** si `meLoDevuelven` está activo y no se indica cuánto, se toma el importe del gasto.

**Formulario de ingreso:** tipo, importe, fecha, concepto y notas.

**Formulario de reembolso:** se elige el gasto (primero los pendientes), y luego importe, fecha, persona y nota.

### Calendario
- Cuadrícula del mes con el gasto y el ingreso diarios en pequeño, y lo previsto con un círculo hueco.
- Al tocar un día se abre un panel con sus movimientos y el botón «Añadir en este día».
- Se puede cambiar entre vista de mes y de semana.

### Estadísticas
Cuatro pestañas:
- **Mes:** tabla resumen, línea acumulada frente al mes anterior, gráfico circular por tienda o categoría y el día de más gasto.
- **Año:** los 12 indicadores, barras de ingresos frente a gasto real en los 12 meses, línea de ahorro acumulado y tabla de los 12 meses (al tocar un mes se va a Mes).
- **Comparar:** dos años lado a lado, con la diferencia en € y en %, y frases claras.
- **Dónde gasto:** ranking de tiendas (total, veces y media por visita), por mes, año o desde siempre. Al tocar una tienda se ve su historial. Incluye el apartado «Pagos mensuales».

### Informes
- Atajos: este mes, año actual y **Lo que me deben**.
- Informe personalizado: formato (PDF o .xlsx), periodo, contenido, filtros (tienda, categoría, persona) y vista previa.
- Salida: compartir, guardar en el móvil o guardar en Drive (`Nummia_Datos/Informes`).
- Historial de informes generados.

### Ajustes
- **Tu Drive:** conectar, desconectar o cambiar de cuenta; sincronizar ahora; última sincronización y pendientes; abrir en Drive; solo con Wi-Fi; conflictos.
- **Copias de seguridad:** lista, crear ahora y restaurar.
- **Sueldo:** importe habitual.
- **Pagos mensuales.**
- **Personas.**
- **Tiendas:** unir duplicados.
- **Categorías.**
- **Presupuestos.**
- **Apariencia:**
  - Tema: Automático (según el móvil), Claro, Oscuro o Negro AMOLED.
  - **Color de la app:** 12 colores predefinidos (**violeta por defecto**, verde menta, turquesa, cian, azul, índigo, violeta, rosa, coral, naranja, ámbar, lima y pizarra), **Personalizado** (rueda de color) o **Del fondo de pantalla** (Material You).
  - El color elegido se aplica a toda la app al instante, con una animación suave, y se puede ver en la vista previa.
  - Tamaño del texto.
  - **Icono de la app:** por defecto, el **logo A (N que sube) en violeta**. Se puede elegir entre los 3 logos (A, B o C) y cualquiera de los 12 colores predefinidos. **El color del icono es independiente del color de la app.** Se implementa con `activity-alias` (36 combinaciones, iconos vectoriales de poco peso). Un color personalizado no se puede llevar al icono: se usa el predefinido más parecido. Todos los iconos llevan capa monocroma para los iconos temáticos de Android.
- **Seguridad:** bloqueo biométrico o PIN (opcional, desactivado por defecto) y **modo discreto**.
- **Notificaciones:** sueldo, pagos del día y presupuesto al 80 % y 100 %.
- **Datos:** importar Excel, copia local, papelera, historial y borrar todo.
- **Acerca de:** versión, **buscar actualizaciones**, código, privacidad y licencia.

### Buscador
Busca en todos los años por concepto, tienda, importe o fecha, y muestra los totales del resultado.

### Primera vez
Bienvenida (3 pantallas) → tema → sueldo habitual (se puede saltar) → conectar Drive o dejarlo para más tarde → importar Excel o empezar de cero.

## 9. Hoja de cálculo en Drive

### 9.1 Estructura
```
Nummia_Datos/
├── Nummia_2026          (Hoja de cálculo de Google)
├── Nummia_2027
├── Copias_de_seguridad/
└── Informes/
```

Pestañas de cada hoja anual:

| Pestaña | Columnas |
|---|---|
| Resumen | 12 meses × (Ingresos, Pagado, Reembolsos, Gasto real, Balance) con fórmulas `SUMIFS` sobre las otras pestañas, más el total del año. Se recalcula en el ordenador sin la app. La app la regenera y nunca lee de ella |
| Gastos | Código, Fecha, Tienda, Concepto, Importe, Categoría, ¿Me lo devuelven?, Quién, Esperado, Notas, `id` (columna oculta) |
| Ingresos | Código, Fecha, Concepto, Tipo, Importe, Notas, `id` |
| Reembolsos | Código, Código del gasto, Fecha, Importe, Quién, Nota, `id` |

Detalles:
- Validación de datos en las columnas Tipo, ¿Me lo devuelven? y Quién (listas desplegables).
- Fila de cabecera congelada y protegida (solo aviso, no bloqueo).
- Los datos que la app usa en toda la cuenta (personas, tiendas, categorías, pagos mensuales, presupuestos) no van en la hoja. Viven en la app y en las copias de seguridad.

### 9.2 Algoritmo de sincronización
1. **Subir:** se vacía `ColaSync` agrupando operaciones (`spreadsheets.values.batchUpdate` y `append`).
2. **Comprobar:** `files.get?fields=modifiedTime,version` de cada hoja del año actual y del anterior. Si no ha cambiado desde la última sincronización, se termina sin descargar nada.
3. **Bajar:** si ha cambiado, `values.batchGet` de Gastos, Ingresos y Reembolsos. Para cada fila se calcula su hash y se compara con `EstadoSync`:
   - **Fila sin `id`** → alta desde el ordenador. Se crea en local y se le escribe el `id` y el código en la hoja.
   - **Hash distinto de la instantánea y sin cambio local pendiente** → se aplica en local (origen `ORDENADOR`).
   - **Hash distinto y con cambio local pendiente** → **conflicto**. No se aplica; se guarda y se avisa en Ajustes → Conflictos, donde el usuario elige qué versión se queda.
   - **`id` conocido que ya no aparece en la hoja** → borrado lógico en local, que va a la papelera.
   - **Fila inválida** (importe o fecha que no se pueden leer, o un reembolso cuyo código de gasto no existe) → se ignora, se marca en la hoja con una nota y se avisa en la app.
   - La hoja de un año se crea al sincronizar el primer movimiento de ese año. Los cambios en años antiguos se suben a su hoja correspondiente.
4. Se actualizan las instantáneas y la fecha de la última sincronización.

Cuándo se lanza:
- Al abrir la app.
- Tras un cambio local (con un retardo de unos 30 s para agrupar varios cambios).
- Con el botón «Sincronizar ahora».
- Periódicamente cada 12 h como máximo.

Siempre con `NetworkType.CONNECTED` (o `UNMETERED` si está activado «solo con Wi-Fi») y `requiresBatteryNotLow`.

### 9.3 Copias de seguridad
- **Formato:** archivo `.nummia` (JSON comprimido con gzip) con **todas** las entidades y ajustes, y la versión del esquema. Sin contraseña.
- **Ubicación:** `Nummia_Datos/Copias_de_seguridad/Nummia_copia_AAAA-MM-DD_HH-mm.nummia`.
- **Cuándo se hace:**
  - Automática a diario, solo si hubo cambios.
  - Manual.
  - Siempre antes de importar, resolver conflictos, restaurar, borrar todo o instalar una actualización.
- **Retención:** 7 diarias + 4 semanales + 12 mensuales. Las demás se borran.
- **Restaurar:** primero se hace una copia del estado actual y después se reemplaza todo.
- **Móvil nuevo:** después de conectar Drive, se detectan las copias y se ofrece restaurar la más reciente.

### 9.4 Autenticación
- Credential Manager (inicio de sesión con Google) + `AuthorizationClient` de Identity Services con el permiso **`drive.file`** únicamente. Ese permiso no es sensible y basta para crear y editar las hojas que crea la app.
- El ID de cliente se inyecta en la compilación desde un secreto de GitHub.
- Sin Drive configurado, la app funciona completa solo en local.

## 10. Importación del Excel 2026

- Se elige el archivo con el selector de Android. **Nunca se sube al repositorio.**
- Estructura reconocida:
  - Bloques mensuales en las filas 9–23 (enero a junio) y 38–52 (julio a diciembre).
  - Columnas C/G/K/O/S/W (gasto), +1 (precio) y +2 (reembolso).
  - Sueldo en las filas 3 y 32.
  - «Ganancias Extraordinarias» desde la fila 56, columnas C:D.
- **Concepto con fecha:** `Nombre(dd/mm)`. El año se toma de la elección del usuario (por defecto, el que aparece en el nombre del archivo).
- **Normalización de nombres:** sin distinguir mayúsculas ni tildes al comparar, y unificando variantes del mismo nombre (mayúsculas, tildes o erratas). Se conserva la grafía más frecuente.
- **Reembolso** en la misma fila → un `Reembolso` sin fecha (cuenta en el mes del gasto) ni persona.
- **Gastos que el usuario marque como pagados por otros** en la vista previa → `meLoDevuelven = true`, con la persona que elija.
- **Sueldo de cada mes** → `Ingreso` de tipo `SUELDO`, fechado el último día del mes y con la nota «fecha aproximada (importado)» **[decisión técnica]**.
- **Ganancias extraordinarias** → `Ingreso` de tipo `EXTRA`, con la fecha que indique el concepto o el día 1 del mes.
- **Se ignoran** las celdas H56:I60 (confirmado por el propietario) y las celdas de fórmulas.
- **Vista previa** antes de confirmar: número de gastos, ingresos y reembolsos, totales por mes comparados con los del Excel, y avisos.
- Antes de importar se hace una copia de seguridad.

## 11. Actualizador (desde la v1.0.0)

- **Consulta** `GET https://api.github.com/repos/alexcorrea2001-Lemon/Nummia/releases/latest`:
  - Una vez al día con WorkManager (con red y batería no baja), o manualmente.
  - Se compara `tag_name` (`vX.Y.Z`) con la versión instalada.
- **Aviso** con las notas de la versión en español. La descarga va a `cacheDir/updates/`, por defecto solo con Wi-Fi, con progreso.
- **Verificación:**
  - El SHA-256 del APK debe coincidir con el `.sha256` publicado en la misma versión.
  - El certificado de firma del APK descargado debe coincidir con el de la app instalada.
- **Instalación:**
  - Se hace una copia de seguridad.
  - Se instala con `PackageInstaller`, pidiendo `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` cuando el sistema lo permita.
  - Si es la primera vez, se guía al usuario para activar «Instalar apps desconocidas».
- **Limpieza:** se borra el APK cuando la instalación termina bien, falla o se cancela, y también al arrancar si queda alguno.

## 12. Ingeniería

### 12.1 Stack **[decisión técnica]**
- **Lenguaje e interfaz:** Kotlin, Jetpack Compose y Material 3 (movimiento *expressive*).
- **Navegación:** Navigation Compose con rutas tipadas.
- **Inyección de dependencias:** Hilt.
- **Datos:**
  - Room sobre **SQLCipher**. La clave se genera al instalar y se guarda envuelta con Android Keystore.
  - DataStore para los ajustes.
- **Segundo plano:** WorkManager.
- **Red:** OkHttp + kotlinx.serialization.
- **Gráficos:** Vico.
- **PDF:** `PdfDocument` de Android.
- **.xlsx:** escritor propio y mínimo (ZIP + SpreadsheetML).
- **Biometría:** `androidx.biometric`.
- **Rendimiento:** R8 con reducción de recursos y Baseline Profiles.
- **APK:** solo `arm64-v8a`, para mantener el tamaño con SQLCipher.

### 12.2 Arquitectura
- **Capas:** `ui` (pantallas + ViewModel con `StateFlow` de estado inmutable) → `domain` (casos de uso y cálculos puros) → `data` (repositorios, Room, Drive).
- Los **cálculos** están en funciones puras de `domain` sin dependencias de Android, con pruebas exhaustivas.
- Las **agregaciones** se hacen con consultas SQL (`SUM` y `GROUP BY`), no en memoria.

### 12.3 Pruebas
- **Unitarias:**
  - Todas las reglas de la sección 7, incluidos reembolsos múltiples, reembolsos mayores que el precio, `PREVISTO` excluido y meses sin ingresos.
  - Analizador del Excel con un archivo de prueba **inventado**.
  - Cálculo de diferencias de la sincronización.
  - Retención de copias.
  - Comparación de versiones.
- **Room:** pruebas en memoria de las consultas de agregación.
- **Sincronización:** Drive y Sheets simulados (MockWebServer), con escenarios sin red, conflicto, alta desde el ordenador, borrado desde el ordenador y fila inválida.
- **Interfaz:** pruebas de Compose de los flujos principales (añadir gasto, reembolso, repetir hoy y deshacer borrado).
- **Rendimiento:** Macrobenchmark de arranque y desplazamiento, que también genera el Baseline Profile.

### 12.4 Versiones y publicación
- **Versionado semántico:** `vMAYOR.MENOR.CORRECCIÓN`, con `versionCode = MAYOR×10000 + MENOR×100 + CORRECCIÓN`. Se muestra en Ajustes → Acerca de.
- **Ramas:** se trabaja directamente en `main`. Cuando llega a `main` un número de versión aún no publicado, el flujo de publicación crea la etiqueta `vX.Y.Z` y el Release automáticamente.
- **GitHub Actions:**
  - En cada push o PR: lint + pruebas unitarias + compilación debug.
  - En cada push a `main` con una versión aún no publicada: pruebas → APK firmado (release) → `.sha256` → GitHub Release con las notas de `CHANGELOG.md`. **Si cualquier paso falla, no se publica.**
- **Secretos de GitHub:** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` y, desde la v1.2.0, `GOOGLE_WEB_CLIENT_ID`.
- **`CHANGELOG.md`** en español, con un apartado por versión.
- **Política de privacidad y página de la app** en GitHub Pages (necesarias para el consentimiento de Google).

### 12.5 Hoja de ruta
| Versión | Contenido |
|---|---|
| **v1.0.0** | Gastos, ingresos, reembolsos, pago para otra persona, personas, sueldo previsto; Inicio; Movimientos (filtros, Repetir hoy, deshacer, historial de cambios); detalle y formularios; temas y acento; modo discreto; copia local manual; **actualizador**; pipeline de publicación |
| v1.1.0 | Estadísticas (Mes, Año, Comparar años, Dónde gasto), calendario y gráficos animados |
| v1.2.0 | Google Drive (configuración de Cloud junto al propietario), sincronización en los dos sentidos, conflictos y copias de seguridad en Drive |
| v1.3.0 | Importar Excel, pagos mensuales, papelera de 30 días y buscador también por fecha |
| v1.4.0 | Presupuestos, tiendas (unir duplicados), categorías, notificaciones y bloqueo con huella o PIN |
| v1.5.0 | Informes PDF y .xlsx con «Lo que me deben» |

La v1.3.0 prevista al principio se repartió en tres versiones para poder probar cada parte antes (decidido el 21/11/2026).

## 13. Preguntas abiertas

### 13.1 ¿En qué mes cuenta un reembolso?
Por ejemplo: compra el 28/01 y te devuelven el dinero el 03/02.
- **(a) En el mes del gasto.** Así funciona el Excel actual y el gasto real de enero queda «limpio».
- **(b) En el mes en que recibes el dinero.** Refleja mejor el dinero que tienes cada mes.

**Decidido: (a), en el mes del gasto**, porque se está devolviendo un producto comprado ese mes. La fecha del reembolso se guarda para el historial, pero no cambia el mes en que cuenta.

### 13.2 Logo
Propuestas (en la maqueta):
- **A.** Una N cuyo último trazo es una flecha hacia arriba.
- **B.** Una moneda con una «n».
- **C.** Tres barras que crecen.

**Versión plana y vectorial de los tres iconos que diseñó el propietario** (N que sube, cartera y crecimiento), en los 12 colores, con capa monocroma. **Por defecto: N que sube, en violeta.** Los otros dos se ofrecen como iconos alternativos en Ajustes. Será un icono adaptable de Android, con capa monocroma para los iconos temáticos.

## 14. Queda a criterio del desarrollador

- Iconografía concreta y paleta exacta de cada tema, partiendo del violeta `#A78BFA` como acento por defecto (validada en la maqueta).
- Textos de la interfaz.
- Categorías predefinidas.
- Duraciones concretas de las animaciones.
- Formato interno de `.nummia`.
- Nombres de clases y paquetes.
- Rotación de la clave de la base de datos.
- Distribución exacta de las celdas del informe PDF.
