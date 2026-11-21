# Nummia

**Tus cuentas personales, sin hojas de cálculo.** Nummia es una app Android para apuntar gastos, ingresos, sueldos y reembolsos, y saber de verdad cuánto gastas y cuánto ahorras.

- Gastos con **varios reembolsos**, aunque te devuelvan más de lo que pagaste.
- **Pago para otra persona**: Nummia te recuerda lo que te deben.
- **Sueldo previsto** cada mes, que confirmas cuando cobras.
- Balance del mes, comparación con el mes anterior y lo ahorrado en el año.
- Temas claro, oscuro y negro AMOLED; 12 colores; 3 iconos.
- Base de datos **cifrada**. Sin servidores, sin anuncios, sin rastreo.
- Se actualiza sola desde GitHub.

> Próximamente (v1.3.0): importar Excel, pagos mensuales, presupuestos, informes PDF/Excel y más.

## Instalar

1. Abre la página de [la última versión](https://github.com/alexcorrea2001-Lemon/Nummia/releases/latest) desde tu móvil Android (8.0 o posterior).
2. Descarga el archivo `nummia-vX.Y.Z.apk`.
3. Ábrelo y permite instalar apps de esa fuente si Android te lo pide.

Las siguientes versiones se instalan desde la propia app: **Ajustes → Acerca de → Buscar actualizaciones**.

## Privacidad

Lee la [política de privacidad](docs/privacidad.md). Resumen: tus datos solo están en tu móvil y, si lo conectas, en tu propio Google Drive.

## Compilar tu propia copia

Necesitas Android Studio o JDK 17 y el SDK de Android.

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`. Para la versión firmada, define las variables de entorno `NUMMIA_KEYSTORE_PATH`, `NUMMIA_KEYSTORE_PASSWORD`, `NUMMIA_KEY_ALIAS` y `NUMMIA_KEY_PASSWORD` y ejecuta `./gradlew assembleRelease`. **Nunca subas tu keystore ni sus contraseñas al repositorio.**

Si compilas tu propia copia y quieres usar Google Drive (v1.2.0), necesitarás tu propio proyecto de Google Cloud con un ID de cliente OAuth para tu nombre de paquete y tu firma.

## Documentación del proyecto

- [Diseño](docs/superpowers/specs/2026-11-20-nummia-design.md)
- [Novedades](CHANGELOG.md)

## Licencia

[MIT](LICENSE)
