$browser = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
if (-not (Test-Path $browser)) {
    $browser = "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
}

$presentationDir = Join-Path (Get-Location).Path "presentation"
$htmlFile = Join-Path $presentationDir "presentation.html"
$pdfFile = Join-Path $presentationDir "clinic_appointment_system_presentation.pdf"

$cmd = "& `"$browser`" --headless=new --disable-gpu --no-pdf-header-footer --print-to-pdf=`"$pdfFile`" `"$htmlFile`""
Write-Host "Executing: $cmd"
Invoke-Expression $cmd

Start-Sleep -Seconds 3

if (Test-Path $pdfFile) {
    $size = (Get-Item $pdfFile).Length
    Write-Host "SUCCESS: PDF generated! Size: $size bytes"
} else {
    Write-Host "Retrying with direct file URL..."
    $fileUri = "file:///" + ($htmlFile -replace '\\', '/')
    $cmd2 = "& `"$browser`" --headless --disable-gpu --print-to-pdf=`"$pdfFile`" `"$fileUri`""
    Invoke-Expression $cmd2
    Start-Sleep -Seconds 3
    if (Test-Path $pdfFile) {
        $size = (Get-Item $pdfFile).Length
        Write-Host "SUCCESS (Attempt 2): PDF generated! Size: $size bytes"
    } else {
        Write-Error "Could not generate PDF directly via CLI."
    }
}
