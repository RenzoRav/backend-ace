$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot '.runtime\backend.pid'
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Output 'Nenhum processo iniciado pelo script está registrado.'; exit 0 }
$backendPid = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
$process = Get-CimInstance Win32_Process -Filter "ProcessId=$backendPid"
$jar = Join-Path $PSScriptRoot 'build\libs\backend-florir-0.0.1-SNAPSHOT.jar'
if ($process -and $process.Name -eq 'java.exe' -and $process.CommandLine.Contains($jar)) {
    Stop-Process -Id $backendPid
    Remove-Item -LiteralPath $pidFile
    Write-Output 'Backend Florir encerrado; PostgreSQL e dados foram preservados.'
} elseif (-not $process) {
    Remove-Item -LiteralPath $pidFile
    Write-Output 'O processo já estava encerrado.'
} else { throw 'O PID pertence a outro processo; nenhum processo foi encerrado.' }
