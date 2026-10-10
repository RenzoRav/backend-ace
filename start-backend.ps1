param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$runtime = Join-Path $root '.runtime'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
$pg = 'C:\Program Files\PostgreSQL\18\bin'
if (-not (Test-Path -LiteralPath (Join-Path $pg 'pg_ctl.exe'))) { throw 'Configure PostgreSQL ou Docker conforme README.md.' }
$data = Join-Path $runtime 'postgres'
$env:PGPASSWORD = 'florir_dev'
$env:DB_PASSWORD = 'florir_dev'
$env:DB_USER = 'florir'
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5433/florir'
if (-not (Test-Path -LiteralPath (Join-Path $data 'PG_VERSION'))) {
    $pw = Join-Path $runtime 'initdb-password.txt'
    Set-Content -LiteralPath $pw -Value $env:PGPASSWORD -Encoding ascii
    try {
        & (Join-Path $pg 'initdb.exe') -D $data -U florir --auth-local=scram-sha-256 --auth-host=scram-sha-256 --encoding=UTF8 --locale=C --pwfile=$pw
        if ($LASTEXITCODE -ne 0) { throw 'A inicialização do PostgreSQL falhou.' }
    } finally { Remove-Item -LiteralPath $pw -ErrorAction SilentlyContinue }
}
& (Join-Path $pg 'pg_ctl.exe') -D $data status | Out-Null
if ($LASTEXITCODE -ne 0) {
    & (Join-Path $pg 'pg_ctl.exe') -D $data -l (Join-Path $runtime 'postgres.log') -o '-p 5433 -h 127.0.0.1' start
    if ($LASTEXITCODE -ne 0) { throw 'Confira .runtime/postgres.log e a disponibilidade da porta 5433.' }
}
foreach ($db in @('florir','florir_test')) {
    $exists = & (Join-Path $pg 'psql.exe') -h 127.0.0.1 -p 5433 -U florir -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname='$db'"
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao conectar ao PostgreSQL local.' }
    if (([string]$exists).Trim() -ne '1') {
        & (Join-Path $pg 'psql.exe') -h 127.0.0.1 -p 5433 -U florir -d postgres -w -c "CREATE DATABASE $db;"
        if ($LASTEXITCODE -ne 0) { throw "Falha ao criar $db." }
    }
}
$url = "http://localhost:$Port/api/v1/health"
try { $health=Invoke-RestMethod -Uri $url -TimeoutSec 2 } catch { $health=$null }
if ($health -and $health.application -eq 'backend-florir') { Write-Output "Backend Florir já está rodando: http://localhost:$Port"; exit 0 }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "A porta $Port está ocupada. Escolha outra com -Port." }
Push-Location $root
try {
    & (Join-Path $root 'gradlew.bat') bootJar --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
    $jar = Join-Path $root 'build\libs\backend-florir-0.0.1-SNAPSHOT.jar'
    $java = 'C:\Program Files\Java\jdk-21\bin\java.exe'
    if (-not (Test-Path -LiteralPath $java)) { $java = (Get-Command java -ErrorAction Stop).Source }
    $process = Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=dev',"--server.port=$Port") -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime 'backend.out.log') -RedirectStandardError (Join-Path $runtime 'backend.err.log')
    Set-Content -LiteralPath (Join-Path $runtime 'backend.pid') -Value $process.Id
    for ($i=0;$i -lt 90;$i++) {
        if ($process.HasExited) { throw 'A API encerrou. Confira .runtime/backend.err.log e backend.out.log.' }
        try { $health=Invoke-RestMethod -Uri $url -TimeoutSec 2; if ($health.application -eq 'backend-florir') { Write-Output "Backend Florir iniciado: http://localhost:$Port"; exit 0 } } catch { }
        Start-Sleep -Milliseconds 500
    }
    throw 'Confira os logs de inicialização em .runtime.'
} finally { Pop-Location }
