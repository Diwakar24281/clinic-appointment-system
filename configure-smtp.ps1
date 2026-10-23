<#
.SYNOPSIS
    Configures SMTP environment variables for the Clinic Appointment Booking System.
.DESCRIPTION
    Sets SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD, SPRING_MAIL_HOST, and SPRING_MAIL_PORT
    persistently in the Windows User environment and exports them to the current process.
#>

param(
    [string]$Email,
    [string]$AppPassword,
    [string]$HostServer = "smtp.gmail.com",
    [int]$Port = 587
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " 🏥 Clinic Appointment System - SMTP Configuration Wizard" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

if (-not $Email) {
    $Email = Read-Host "Enter your Sender Email Address (e.g., yourclinic@gmail.com)"
}

if (-not $Email -or $Email.Trim() -eq "") {
    Write-Host "❌ Error: Email address cannot be empty." -ForegroundColor Red
    exit 1
}

$Email = $Email.Trim()

if (-not $AppPassword) {
    Write-Host "`nℹ️ Note for Gmail Users:" -ForegroundColor Yellow
    Write-Host "   Do not enter your normal account password."
    Write-Host "   Enter your 16-character Google App Password (generated from Google Account -> Security -> 2-Step Verification -> App Passwords)."
    $AppPassword = Read-Host "Enter your 16-character SMTP App Password"
}

if (-not $AppPassword -or $AppPassword.Trim() -eq "") {
    Write-Host "❌ Error: SMTP password cannot be empty." -ForegroundColor Red
    exit 1
}

# Remove any spaces that Google displays in the 16-character app password (e.g. "abcd efgh ijkl mnop" -> "abcdefghijklmnop")
$CleanPassword = $AppPassword.Replace(" ", "").Trim()

Write-Host "`n[1/3] Testing TCP connectivity to $HostServer on port $Port..." -ForegroundColor Cyan
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $connect = $tcp.BeginConnect($HostServer, $Port, $null, $null)
    $success = $connect.AsyncWaitHandle.WaitOne(4000, $false)
    if ($success) {
        $tcp.EndConnect($connect)
        $tcp.Close()
        Write-Host "  ✅ Connection to $HostServer:$Port succeeded!" -ForegroundColor Green
    } else {
        $tcp.Close()
        Write-Host "  ⚠️ Warning: Connection to $HostServer timed out. Check firewall or internet connectivity." -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠️ Warning: Could not test TCP port ($($_.Exception.Message)). Continuing with configuration." -ForegroundColor Yellow
}

Write-Host "`n[2/3] Setting persistent environment variables for Windows User..." -ForegroundColor Cyan
[System.Environment]::SetEnvironmentVariable('SPRING_MAIL_HOST', $HostServer, 'User')
[System.Environment]::SetEnvironmentVariable('SPRING_MAIL_PORT', $Port.ToString(), 'User')
[System.Environment]::SetEnvironmentVariable('SPRING_MAIL_USERNAME', $Email, 'User')
[System.Environment]::SetEnvironmentVariable('SPRING_MAIL_PASSWORD', $CleanPassword, 'User')
[System.Environment]::SetEnvironmentVariable('MAIL_USERNAME', $Email, 'User')
[System.Environment]::SetEnvironmentVariable('MAIL_PASSWORD', $CleanPassword, 'User')

Write-Host "`n[3/3] Exporting variables to active process..." -ForegroundColor Cyan
$env:SPRING_MAIL_HOST = $HostServer
$env:SPRING_MAIL_PORT = $Port.ToString()
$env:SPRING_MAIL_USERNAME = $Email
$env:SPRING_MAIL_PASSWORD = $CleanPassword
$env:MAIL_USERNAME = $Email
$env:MAIL_PASSWORD = $CleanPassword

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " ✅ SMTP Environment Configured Successfully!" -ForegroundColor Green
Write-Host "    Host:     $HostServer" -ForegroundColor Gray
Write-Host "    Port:     $Port" -ForegroundColor Gray
Write-Host "    Username: $Email" -ForegroundColor Gray
Write-Host "    Password: [PROTECTED / 16 CHARS CONFIGURED]" -ForegroundColor Gray
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "`nTo restart Spring Boot with the new SMTP credentials, run:" -ForegroundColor White
Write-Host "  powershell -ExecutionPolicy Bypass -File .\restart.ps1`n" -ForegroundColor Yellow
