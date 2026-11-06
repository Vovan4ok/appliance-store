<#
.SYNOPSIS
  Copies your private values from the local .env into Secret Manager (OpenAI key, Brevo SMTP login/key, sender).

.DESCRIPTION
  Run it yourself after the first `terraform apply` (which creates the empty secrets) and whenever a value changes.
  Values go from .env straight to Secret Manager: they are never printed, never passed on the command line and
  never stored in Terraform. A new secret version is only added when the value actually changed.
  The generated secrets (db-password, redis-password, jwt-secret) are managed by Terraform, not by this script.

.EXAMPLE
  .\infra\scripts\set-secrets.ps1
#>
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..\..\.env'),
    [string]$Project = 'appliance-store-513822'
)

$ErrorActionPreference = 'Stop'

# Windows PowerShell 5.1 turns anything a native program writes to stderr into an error record, which 'Stop'
# makes fatal - even gcloud's harmless status lines. Run gcloud with 'Continue' and judge it by its exit code.
function Invoke-Gcloud {
    $ErrorActionPreference = 'Continue'
    $all = & gcloud @args 2>&1
    $code = $LASTEXITCODE
    [pscustomobject]@{
        ExitCode = $code
        Output   = @($all | Where-Object { $_ -isnot [System.Management.Automation.ErrorRecord] } | ForEach-Object { "$_" })
        Errors   = ($all | Where-Object { $_ -is [System.Management.Automation.ErrorRecord] } | ForEach-Object { "$_" }) -join ' '
    }
}

# secret name in Secret Manager => variable name in .env
$mapping = [ordered]@{
    'openai-api-key' = 'OPENAI_API_KEY'
    'mail-username'  = 'MAIL_USERNAME'
    'mail-password'  = 'MAIL_PASSWORD'
    'mail-from'      = 'MAIL_FROM'
}

if (-not (Test-Path $EnvFile)) { throw ".env not found at $EnvFile" }

# Parse KEY=VALUE lines (comments and blank lines ignored, optional surrounding quotes removed)
$values = @{}
foreach ($line in Get-Content $EnvFile) {
    if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
    $key, $value = $line -split '=', 2
    $values[$key.Trim()] = $value.Trim().Trim('"').Trim("'")
}

$tmp = New-TemporaryFile
try {
    foreach ($secret in $mapping.Keys) {
        $var = $mapping[$secret]
        $value = $values[$var]
        if ([string]::IsNullOrWhiteSpace($value)) {
            Write-Warning "$var is empty in .env - '$secret' left unchanged"
            continue
        }

        # Only compare with the current value if the secret has one (a fresh secret has no versions yet)
        $hasVersion = Invoke-Gcloud secrets versions list $secret "--project=$Project" '--filter=state:ENABLED' '--limit=1' '--format=value(name)'
        if ($hasVersion.Output) {
            $current = Invoke-Gcloud secrets versions access latest "--secret=$secret" "--project=$Project"
            if ($current.ExitCode -eq 0 -and ($current.Output -join "`n") -eq $value) {
                Write-Host "$secret : unchanged"
                continue
            }
        }

        # Written without a trailing newline, then handed to gcloud as a file (not visible in the process list)
        [IO.File]::WriteAllText($tmp.FullName, $value)
        $added = Invoke-Gcloud secrets versions add $secret "--project=$Project" "--data-file=$($tmp.FullName)" '--quiet'
        if ($added.ExitCode -ne 0) { throw "Could not add a version to '$secret': $($added.Errors)" }
        Write-Host "$secret : new version added"
    }
}
finally {
    Remove-Item $tmp.FullName -Force -ErrorAction SilentlyContinue
}
