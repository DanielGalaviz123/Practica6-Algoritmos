"""Resume datos crudos sin sustituir ni filtrar las mediciones originales."""
import csv
import json
import statistics as st
import sys
from pathlib import Path
from datetime import datetime, timedelta

ROOT = Path(__file__).parent / "resultados"

def filas(path):
    with path.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))

def resumen(path):
    grupos = {}
    for r in filas(path):
        grupos.setdefault(r["metodo"], []).append(r)
    print("\n" + path.name)
    print("metodo | primera | rondas 3..11 | ultimas 20 mediana [p10,p90] | mitad final/anterior | gc | jit_ms")
    for nombre, rows in grupos.items():
        t = [float(r["ms"]) for r in rows]
        cola = sorted(t[-20:])
        ratio = st.median(t[-20:]) / st.median(t[-40:-20]) if len(t)>=40 else 0
        print(f"{nombre}: {t[0]:.4f} | {st.median(t[3:12]):.4f} | "
              f"{st.median(cola):.4f} [{cola[int((len(cola)-1)*.1)]:.4f},"
              f"{cola[int((len(cola)-1)*.9)]:.4f}] | {ratio:.3f} | "
              f"{sum(int(r.get('gc_delta',0)) for r in rows)} | "
              f"{sum(int(r.get('jit_ms_delta',0)) for r in rows)}")

def dur(s):
    return float(s.removeprefix("PT").removesuffix("S"))

def matriz(path):
    grupos={}
    for p in sorted(path.glob("*.csv")):
        for r in filas(p):
            grupos.setdefault((r["columna"],int(r["n"]),r["metodo"]),[]).append(r)
    with (path.parent/"resumen-matriz.csv").open("w",newline="",encoding="utf-8") as f:
        w=csv.writer(f)
        w.writerow(["columna","n","metodo","mediana_rondas_6_a_14_ms","min_ms","max_ms"])
        for k,rs in grupos.items():
            if len(rs)!=15:
                raise ValueError(f"Muestras incompletas: {k}")
            t=[float(r["ms"]) for r in rs[6:]]
            w.writerow([*k,st.median(t),min(t),max(t)])
    print(len(grupos),"grupos completos;",sum(map(len,grupos.values())),"mediciones")

def jfr(path):
    events=json.loads(path.read_text(encoding="utf-8-sig"))["recording"]["events"]
    muestras=[e["values"] for e in events if e["type"]=="practica.Muestra"]
    comps=[e["values"] for e in events if e["type"]=="jdk.Compilation"]
    pauses=[e["values"] for e in events if e["type"]=="jdk.GCPhasePause"]
    def interval(e):
        ini=datetime.fromisoformat(e["startTime"])
        return ini,ini+timedelta(seconds=dur(e["duration"]))
    def solapan(a,b):
        ai,af=interval(a); bi,bf=interval(b)
        return ai<bf and bi<af
    print("\nJFR",path.name,"compilaciones",len(comps),"pausas",len(pauses))
    out=[]
    for m in muestras:
        cs=[c for c in comps if solapan(m,c)]
        ps=[p for p in pauses if solapan(m,p)]
        relevantes=[c for c in cs if any(s in c["method"]["type"]["name"] for s in
                     ("TimSort","Ordenamientos","DatosColumna","ArraysParallelSort"))]
        names=sorted(set(c["method"]["type"]["name"]+"."+c["method"]["name"]+
                         ":L"+str(c["compileLevel"]) for c in relevantes))
        out.append(dict(metodo=m["metodo"],ronda=m["ronda"],ms=m["ms"],
                        compilaciones=len(cs),pausas=len(ps),metodos=";".join(names)))
        if ps or (m["ronda"]<12 and relevantes):
            print(m["metodo"],m["ronda"],m["ms"],"GC",len(ps),"JIT",names)
    if out:
        with path.with_suffix(".solapamientos.csv").open("w",newline="",encoding="utf-8") as f:
            w=csv.DictWriter(f,fieldnames=list(out[0])); w.writeheader(); w.writerows(out)

if __name__=="__main__":
    for arg in sys.argv[1:]:
        p=Path(arg)
        if p.is_dir():
            matriz(p)
        elif p.suffix==".json":
            jfr(p)
        else:
            resumen(p)
