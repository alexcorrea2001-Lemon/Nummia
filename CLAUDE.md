# Nummia — Instrucciones del proyecto

App Android de finanzas personales. Diseño completo en `docs/superpowers/specs/2026-11-20-nummia-design.md`.

## Pilares (obligatorios en CADA cambio)

1. **Animaciones y fluidez:**
   - Transiciones, números animados, gestos con respuesta háptica y movimiento con efecto muelle de Material 3.
   - Nada pesado en el hilo principal. Arranque por debajo de 1 s.
   - Respetar «Quitar animaciones».
2. **Batería:** sin servicios permanentes. Solo WorkManager con condiciones, agrupando trabajo. Sin ubicación, anuncios ni analítica.
3. **Datos móviles:** sincronización incremental, comprobar metadatos antes de descargar, actualizaciones solo con Wi-Fi por defecto y APK de menos de 10 MB.
4. **Diseño profesional:**
   - Usar siempre los tokens del sistema de diseño.
   - Cifras tabulares y formato `1.520,75 €`.
   - Estados vacío, de carga y de error cuidados.
   - Accesibilidad (AA, escalado de texto, TalkBack).

Además: **segura, exacta y sin fallos.**

## Reglas

- **Dinero:** siempre `Long` en céntimos. Nunca `Double` ni `Float`.
- **Repositorio PÚBLICO:**
  - Ningún secreto, keystore, ID de cliente ni dato real.
  - Las pruebas usan solo datos inventados.
- **Pruebas:** toda regla de cálculo y de sincronización tiene pruebas antes de implementarse (TDD).
- **Textos:** la interfaz y el `CHANGELOG.md` en español.
- **Versionado semántico:** `vX.Y.Z`.
- **Ramas:** se trabaja directamente en `main`. Una versión solo se publica cuando sube el número de versión en `app/build.gradle.kts`.
- **Ideas descartadas** (no implementar): autocompletado, calendario coloreado por intensidad, «lo más repetido», comparar meses, comparar el mismo periodo, «dinero adelantado en el año», recordatorio nocturno.
