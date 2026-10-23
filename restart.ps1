<#
.SYNOPSIS
    Stops any running Spring Boot server on port 8080 and restarts with the latest environment.
#>

Write-Host "Stopping any running instance on port 8080..." -ForegroundColor Cyan
$conn = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conn) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "Process stopped." -ForegroundColor Green
} else {
    Write-Host "No process currently locking port 8080." -ForegroundColor Gray
}

# 1. Load from local .env file if present
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $idx = $line.IndexOf("=")
            $name = $line.Substring(0, $idx).Trim()
            $val = $line.Substring($idx + 1).Trim()
            if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            [System.Environment]::SetEnvironmentVariable($name, $val, 'Process')
            Set-Item -Path "env:$name" -Value $val
        }
    }
}

# 2. Pull persistent User/Machine environment variables if not already set in process
$envVars = @(
    'SPRING_DATASOURCE_PASSWORD', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_URL',
    'MYSQL_HOST', 'MYSQL_PORT', 'MYSQL_DATABASE', 'MYSQL_USER', 'MYSQL_PASSWORD', 'MYSQL_USE_SSL', 'MYSQL_SSL_MODE',
    'EMAIL_PROVIDER', 'BREVO_API_KEY', 'BREVO_SENDER_EMAIL', 'BREVO_SENDER_NAME',
    'MAIL_USERNAME', 'MAIL_PASSWORD', 'SPRING_MAIL_USERNAME', 'SPRING_MAIL_PASSWORD', 'SPRING_MAIL_HOST', 'SPRING_MAIL_PORT',
    'SMS_PROVIDER', 'TWILIO_ACCOUNT_SID', 'TWILIO_AUTH_TOKEN', 'TWILIO_PHONE_NUMBER', 'FAST2SMS_API_KEY', 'FAST2SMS_ROUTE'
)
foreach ($v in $envVars) {
    if (-not (Get-Item "env:$v" -ErrorAction SilentlyContinue)) {
        $val = [System.Environment]::GetEnvironmentVariable($v, 'User')
        if (-not $val) {
            $val = [System.Environment]::GetEnvironmentVariable($v, 'Machine')
        }
        if ($val) {
            Set-Item -Path "env:$v" -Value $val
        }
    }
}

Write-Host "Configuring application runtime environment..." -ForegroundColor Cyan

# Check Email OTP status (Brevo HTTPS vs Gmail SMTP)
if ($env:BREVO_API_KEY) {
    $brevoSender = if ($env:BREVO_SENDER_EMAIL) { $env:BREVO_SENDER_EMAIL } else { $env:MAIL_USERNAME }
    Write-Host "Email OTP:  Brevo HTTPS REST API Active (Sender: $brevoSender | API Key: [Configured])" -ForegroundColor Green
} else {
    $mailUser = if ($env:MAIL_USERNAME) { $env:MAIL_USERNAME } else { $env:SPRING_MAIL_USERNAME }
    $mailPass = if ($env:MAIL_PASSWORD) { $env:MAIL_PASSWORD } else { $env:SPRING_MAIL_PASSWORD }

    if ($mailUser -and $mailPass) {
        Write-Host "Email OTP:  Gmail SMTP Configured (Sender: $mailUser | Password: [16-char App Password Active])" -ForegroundColor Green
    } else {
        Write-Host "Email OTP:  Neither Brevo nor Gmail SMTP configured. Set BREVO_API_KEY or MAIL_USERNAME/MAIL_PASSWORD in .env." -ForegroundColor Yellow
    }
}

mvn spring-boot:run
