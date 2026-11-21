"""Convierte los errores de compilación y de pruebas en anotaciones de GitHub.

Así los fallos se pueden leer desde la API de check-runs sin descargar registros.
"""
import glob
import re
import xml.etree.ElementTree as ET


def escapar(texto: str) -> str:
    return texto.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def anotar(titulo: str, lineas: list[str]) -> None:
    if lineas:
        cuerpo = "\n".join(lineas)[:60000]
        print(f"::error title={titulo}::{escapar(cuerpo)}")


patron = re.compile(r"^(e: |w: .*error|.*error:|.*FAILED|.*What went wrong|.*Exception|.*Caused by|.*Could not|.*Execution failed|> )")
try:
    with open("build.log", encoding="utf-8", errors="replace") as f:
        log = f.read().splitlines()
except FileNotFoundError:
    log = []

relevantes = [l for l in log if patron.match(l)]
anotar("Compilación", relevantes[:250])
anotar("Final del registro", log[-80:])

fallos = []
for ruta in glob.glob("app/build/test-results/**/*.xml", recursive=True):
    try:
        raiz = ET.parse(ruta).getroot()
    except ET.ParseError:
        continue
    for caso in raiz.iter("testcase"):
        for fallo in list(caso.iter("failure")) + list(caso.iter("error")):
            mensaje = (fallo.get("message") or "").strip()
            traza = (fallo.text or "").strip().splitlines()[:8]
            fallos.append(f"{caso.get('classname')}.{caso.get('name')}: {mensaje}")
            fallos.extend("    " + t for t in traza)
anotar("Pruebas fallidas", fallos[:300])

for ruta in glob.glob("app/build/reports/lint-results-*.xml"):
    try:
        raiz = ET.parse(ruta).getroot()
    except ET.ParseError:
        continue
    errores = [
        f"{i.get('id')}: {i.get('message')} ({(i.find('location').get('file') if i.find('location') is not None else '')}:"
        f"{(i.find('location').get('line') if i.find('location') is not None else '')})"
        for i in raiz.iter("issue")
        if i.get("severity") in ("Error", "Fatal")
    ]
    anotar("Lint", errores[:100])
