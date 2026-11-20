param(
    [string]$Jdk = 'C:\Program Files\Java\jdk-23',
    [switch]$Completo
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$java = Join-Path $Jdk 'bin/java.exe'
$javac = Join-Path $Jdk 'bin/javac.exe'
New-Item -ItemType Directory -Force diagnostico/clases,diagnostico/resultados | Out-Null
$fuentes = @((Get-ChildItem src/main/java/com/example/practica6/logica/*.java).FullName)
$fuentes += @((Get-ChildItem diagnostico/*.java).FullName)
& $javac -d diagnostico/clases @fuentes
if($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion' }
function Ejecutar {
    & $java @args
    if($LASTEXITCODE -ne 0) { throw 'Fallo una prueba' }
}
# Las JVM se ejecutan en serie para evitar competir entre pruebas.
Ejecutar @('-cp','diagnostico/clases','Investigacion','funcional')
Ejecutar @('-cp','diagnostico/clases','Investigacion','integracion')
Ejecutar @('-cp','diagnostico/clases','PruebaCSV')
Ejecutar @('-cp','diagnostico/clases','Investigacion','gui3','diagnostico/resultados/gui-protocolo3-repetido.csv')
Ejecutar @('-cp','diagnostico/clases','Investigacion','gui','diagnostico/resultados/gui-corregido-repetido.csv')
if($Completo) {
    Ejecutar @('-cp','diagnostico/clases','Investigacion','repetir','danceability','5000','100','-1','diagnostico/resultados/repetidas-5000.csv','jfr')
    Ejecutar @('-cp','diagnostico/clases','Investigacion','repetir','danceability','32503','20','-1','diagnostico/resultados/repetidas-32503.csv','jfr')
    foreach($fork in 1..3) {
        foreach($metodo in 2..5) {
            Ejecutar @('-cp','diagnostico/clases','Investigacion','repetir','danceability','5000','100',"$metodo","diagnostico/resultados/aislado-$fork-$metodo.csv")
        }
    }
    foreach($metodo in 2..5) {
        Ejecutar @('-Xint','-cp','diagnostico/clases','Investigacion','repetir','danceability','5000','30',"$metodo","diagnostico/resultados/interpretado-$metodo.csv")
    }
    Ejecutar @('-cp','diagnostico/clases','Investigacion','conteos','diagnostico/resultados/comparaciones.csv')
    Ejecutar @('-cp','diagnostico/clases','CompararVersiones','diagnostico/resultados/versiones-quick.csv')
    New-Item -ItemType Directory -Force diagnostico/resultados/matriz | Out-Null
    Ejecutar @('-cp','diagnostico/clases','Investigacion','matriz','diagnostico/resultados/matriz')
}
