[CmdletBinding()]
param(
    [string]$Servidor = 'localhost',
    [int]$Puerto = 5432,
    [string]$BaseDatos = 'mentenova',
    [string]$UsuarioApp = 'mentenova_app',
    [string]$ClaveApp = 'mentenova_app',
    [string]$ClavePostgres,
    [string]$CarpetaCambios,
    [string]$CarpetaDemo,
    [switch]$SinDatosDemo,
    [switch]$Recrear,
    [switch]$Confirmar
)

$ErrorActionPreference = 'Stop'

$TablasApf2 = @(
    'rol', 'usuario', 'paciente', 'psicologo', 'especialidad', 'psicologo_especialidad',
    'disponibilidad', 'excepcion_disponibilidad', 'estado_cita', 'cita', 'historial_cita',
    'atencion', 'pago', 'notificacion', 'auditoria'
)

function Write-Paso([string]$Texto) { Write-Host ''; Write-Host "==> $Texto" -ForegroundColor Cyan }
function Write-Ok([string]$Texto) { Write-Host "    [OK] $Texto" -ForegroundColor Green }
function Write-Info([string]$Texto) { Write-Host "    $Texto" }
function Write-Aviso([string]$Texto) { Write-Host "    [AVISO] $Texto" -ForegroundColor Yellow }

function Find-Psql {
    $enPath = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }
    $instalados = @(Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Directory.Parent.Name -replace '[^0-9]', '') } -Descending)
    if ($instalados.Count -gt 0) { return $instalados[0].FullName }
    throw 'No se encontro psql.exe. Instala PostgreSQL 15 o superior (por ejemplo: winget install --id PostgreSQL.PostgreSQL.18 -e).'
}

function Invoke-Psql {
    param([string]$Usuario, [string]$Clave, [string]$Base, [string[]]$Argumentos)
    $ErrorActionPreference = 'Continue'
    $env:PGPASSWORD = $Clave
    try {
        $salida = & $script:Psql -h $Servidor -p $Puerto -U $Usuario -d $Base -w -X -q -v ON_ERROR_STOP=1 @Argumentos
        if ($LASTEXITCODE -ne 0) {
            throw "psql termino con error (usuario $Usuario, base $Base). Revisa el mensaje anterior."
        }
        return $salida
    }
    finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

function Test-Conexion {
    param([string]$Usuario, [string]$Clave, [string]$Base)
    $ErrorActionPreference = 'Continue'
    $env:PGPASSWORD = $Clave
    try {
        & $script:Psql -h $Servidor -p $Puerto -U $Usuario -d $Base -w -X -At -c 'SELECT 1' 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    }
    finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

function Invoke-Postgres([string]$Base, [string[]]$Argumentos) {
    Invoke-Psql -Usuario 'postgres' -Clave $ClavePostgres -Base $Base -Argumentos $Argumentos
}

function Invoke-App([string[]]$Argumentos) {
    Invoke-Psql -Usuario $UsuarioApp -Clave $ClaveApp -Base $BaseDatos -Argumentos $Argumentos
}

function Get-ValorPostgres([string]$Base, [string]$Sql) {
    @(Invoke-Postgres $Base @('-At', '-c', $Sql)) | Select-Object -First 1
}

function Get-ValorApp([string]$Sql) {
    @(Invoke-App @('-At', '-c', $Sql)) | Select-Object -First 1
}

function Get-Checksum([string]$Ruta) {
    $texto = [System.IO.File]::ReadAllText($Ruta).Replace("`r`n", "`n")
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($texto))
        return -join ($bytes | ForEach-Object { $_.ToString('x2') })
    }
    finally {
        $sha.Dispose()
    }
}

function Invoke-ScriptsPendientes([string]$Carpeta, [string]$Prefijo) {
    $archivos = @()
    if (Test-Path $Carpeta) {
        $archivos = @(Get-ChildItem $Carpeta -Filter '*.sql' -File | Sort-Object Name)
    }
    $aplicados = 0
    foreach ($archivo in $archivos) {
        if ($archivo.Name -notmatch '^[A-Za-z0-9_.-]+$') {
            throw "Nombre de script no valido: $($archivo.Name) (usa letras, numeros, guion y guion bajo)."
        }
        if ((Get-Content $archivo.FullName -Raw) -match '(?im)^\s*(BEGIN|COMMIT|ROLLBACK)\s*;') {
            throw "$($archivo.Name) contiene BEGIN/COMMIT. Quitalos: este script ya ejecuta cada archivo en una transaccion."
        }
        $nombre = "$Prefijo/$($archivo.Name)"
        $checksum = Get-Checksum $archivo.FullName
        $registrado = Get-ValorApp "SELECT checksum FROM control_cambios WHERE nombre = '$nombre'"
        if (-not $registrado) {
            Write-Info "Aplicando $nombre..."
            Invoke-App @('-1', '-c', 'SET client_min_messages = warning', '-f', $archivo.FullName,
                '-c', "INSERT INTO control_cambios (nombre, checksum) VALUES ('$nombre', '$checksum')") | Out-Null
            Write-Ok "$nombre aplicado."
            $aplicados++
        }
        elseif ($registrado -ne $checksum) {
            Write-Aviso "$nombre se modifico despues de aplicarse; no se vuelve a ejecutar. Para corregir algo, crea un script nuevo."
        }
    }
    return [pscustomobject]@{ Total = $archivos.Count; Aplicados = $aplicados }
}

try {
    try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
    $env:PGCLIENTENCODING = 'UTF8'

    $identificador = '^[a-z_][a-z0-9_]*$'
    if ($BaseDatos -notmatch $identificador) { throw "Nombre de base no valido: $BaseDatos (usa minusculas, numeros y _)." }
    if ($UsuarioApp -notmatch $identificador) { throw "Nombre de usuario no valido: $UsuarioApp (usa minusculas, numeros y _)." }
    $claveAppSql = $ClaveApp.Replace("'", "''")

    if (-not $CarpetaCambios) { $CarpetaCambios = Join-Path $PSScriptRoot 'cambios' }
    if (-not $CarpetaDemo) { $CarpetaDemo = Join-Path $PSScriptRoot 'demo' }

    $script:Psql = Find-Psql
    $archivoEsquema = Join-Path $PSScriptRoot 'mentenova_schema.sql'
    $archivoPermisos = Join-Path $PSScriptRoot 'permisos_usuario_app.sql'
    $archivoDemo = Join-Path $PSScriptRoot 'reportes_datos_demo.sql'

    Write-Host 'MenteNova - actualizacion de la base de datos local' -ForegroundColor White
    Write-Info "psql: $script:Psql"
    Write-Info "Servidor: ${Servidor}:$Puerto | Base: $BaseDatos | Usuario de la app: $UsuarioApp"

    if (-not $ClavePostgres) {
        $segura = Read-Host -AsSecureString 'Clave del usuario postgres (la que elegiste al instalar PostgreSQL)'
        $ClavePostgres = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
    }

    Write-Paso '1/6 Conexion con el servidor'
    if (-not (Test-Conexion -Usuario 'postgres' -Clave $ClavePostgres -Base 'postgres')) {
        throw "No se pudo conectar como postgres a ${Servidor}:$Puerto. Revisa que el servicio de PostgreSQL este iniciado y que la clave sea correcta."
    }
    $version = Get-ValorPostgres 'postgres' 'SHOW server_version'
    Write-Ok "PostgreSQL $version"
    if ([int](Get-ValorPostgres 'postgres' 'SHOW server_version_num') -lt 150000) {
        Write-Aviso 'El APF2 pide PostgreSQL 15 o superior.'
    }

    if ($Recrear) {
        Write-Paso "Recrear la base $BaseDatos"
        if (-not $Confirmar) {
            $respuesta = Read-Host "Se BORRARA la base '$BaseDatos' con todos sus datos. Escribe SI para continuar"
            if ($respuesta -cne 'SI') { throw 'Operacion cancelada; no se borro nada.' }
        }
        Invoke-Postgres 'postgres' @('-c', "DROP DATABASE IF EXISTS $BaseDatos WITH (FORCE)") | Out-Null
        Write-Ok "Base $BaseDatos eliminada."
    }

    Write-Paso '2/6 Usuario de la aplicacion y base de datos'
    if ((Get-ValorPostgres 'postgres' "SELECT count(*) FROM pg_roles WHERE rolname = '$UsuarioApp'") -eq '0') {
        Invoke-Postgres 'postgres' @('-c', "CREATE ROLE $UsuarioApp LOGIN PASSWORD '$claveAppSql'") | Out-Null
        Write-Ok "Usuario $UsuarioApp creado."
    }
    else {
        Write-Ok "Usuario $UsuarioApp ya existe."
    }

    if ((Get-ValorPostgres 'postgres' "SELECT count(*) FROM pg_database WHERE datname = '$BaseDatos'") -eq '0') {
        Invoke-Postgres 'postgres' @('-c', "CREATE DATABASE $BaseDatos OWNER $UsuarioApp") | Out-Null
        Write-Ok "Base $BaseDatos creada (dueno: $UsuarioApp)."
    }
    else {
        Write-Ok "Base $BaseDatos ya existe."
    }

    if (-not (Test-Conexion -Usuario $UsuarioApp -Clave $ClaveApp -Base $BaseDatos)) {
        Invoke-Postgres 'postgres' @('-c', "ALTER ROLE $UsuarioApp WITH LOGIN PASSWORD '$claveAppSql'") | Out-Null
        Write-Aviso "Se actualizo la clave de $UsuarioApp para que coincida con application.properties."
        if (-not (Test-Conexion -Usuario $UsuarioApp -Clave $ClaveApp -Base $BaseDatos)) {
            throw "El usuario $UsuarioApp sigue sin poder conectarse a $BaseDatos. Revisa pg_hba.conf."
        }
    }
    Write-Ok "La aplicacion puede conectarse como $UsuarioApp."

    Write-Paso '3/6 Permisos'
    Invoke-Postgres $BaseDatos @('-v', "usuario_app=$UsuarioApp", '-f', $archivoPermisos) | Out-Null
    Write-Ok "$UsuarioApp es dueno de las tablas y funciones del esquema public."

    Write-Paso '4/6 Tablas del APF2 (mentenova_schema.sql)'
    $listaTablas = ($TablasApf2 | ForEach-Object { "'$_'" }) -join ', '
    $consultaTablas = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ($listaTablas)"
    $existentes = @(Invoke-App @('-At', '-c', $consultaTablas) | Where-Object { $_ })
    if ($existentes.Count -eq 0) {
        Invoke-App @('-f', $archivoEsquema) | Out-Null
        Write-Ok 'Tablas, catalogos, indices y triggers creados.'
    }
    elseif ($existentes.Count -eq $TablasApf2.Count) {
        Write-Ok "Las $($TablasApf2.Count) tablas del APF2 ya existen."
    }
    else {
        $faltan = $TablasApf2 | Where-Object { $existentes -notcontains $_ }
        throw "La base tiene $($existentes.Count) de $($TablasApf2.Count) tablas del APF2 (faltan: $($faltan -join ', ')). Si es una base de pruebas, vuelve a ejecutar con -Recrear."
    }

    Write-Paso '5/6 Cambios de la base (database\cambios)'
    Invoke-App @('-c', 'SET client_min_messages = warning',
        '-c', 'CREATE TABLE IF NOT EXISTS control_cambios (nombre VARCHAR(200) PRIMARY KEY, checksum VARCHAR(64) NOT NULL, aplicado_por VARCHAR(100) NOT NULL DEFAULT CURRENT_USER, aplicado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)') | Out-Null

    $cambios = Invoke-ScriptsPendientes $CarpetaCambios 'cambios'
    if ($cambios.Total -eq 0) { Write-Ok 'No hay scripts de cambios todavia.' }
    elseif ($cambios.Aplicados -eq 0) { Write-Ok "Los $($cambios.Total) scripts de cambios ya estaban aplicados." }

    Write-Paso '6/6 Datos de demostracion (reportes_datos_demo.sql y database\demo)'
    if ($SinDatosDemo) {
        Write-Info 'Omitidos (-SinDatosDemo).'
    }
    else {
        if ([int](Get-ValorApp "SELECT count(*) FROM usuario WHERE lower(correo) LIKE '%@demo.mentenova.local'") -gt 0) {
            Write-Ok 'Datos base ya cargados. Para regenerarlos con fechas actuales usa -Recrear.'
        }
        else {
            Invoke-App @('-f', $archivoDemo) | Out-Null
            Write-Ok 'Usuarios, citas y pagos de demostracion cargados.'
        }
        $demo = Invoke-ScriptsPendientes $CarpetaDemo 'demo'
        if ($demo.Total -gt 0 -and $demo.Aplicados -eq 0) {
            Write-Ok "Los $($demo.Total) scripts de datos de prueba ya estaban aplicados."
        }
    }

    Write-Paso 'Resumen'
    Write-Ok (Get-ValorApp "SELECT (SELECT count(*) FROM usuario) || ' usuarios, ' || (SELECT count(*) FROM paciente) || ' pacientes, ' || (SELECT count(*) FROM cita) || ' citas, ' || (SELECT count(*) FROM pago) || ' pagos, ' || (SELECT count(*) FROM control_cambios) || ' scripts registrados'")
    Write-Host ''
    Write-Host 'Listo. Inicia la aplicacion y entra a http://localhost:8080/mentenova/login' -ForegroundColor Green
    if (-not $SinDatosDemo) {
        Write-Host 'Usuarios demo (clave Demo1234!): admin@demo.mentenova.local, recepcion@, psicologo1@ y paciente@demo.mentenova.local'
    }
}
catch {
    Write-Host ''
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
