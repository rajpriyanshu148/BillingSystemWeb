# ============================================================
# BillingSystem Pro — Local MySQL Quick Start (Run as Admin)
# Right-click PowerShell → "Run as Administrator" → paste this
# ============================================================

Write-Host "Starting MySQL 8.0 service..." -ForegroundColor Cyan
Start-Service MySQL80
Start-Sleep 3

$status = (Get-Service MySQL80).Status
Write-Host "MySQL Status: $status" -ForegroundColor $(if ($status -eq 'Running') { 'Green' } else { 'Red' })

if ($status -eq 'Running') {
    $mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    
    # Prompt for root password
    $pass = Read-Host "Enter your MySQL root password" -AsSecureString
    $plainPass = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pass))
    
    Write-Host "Creating billing_system database..." -ForegroundColor Cyan
    & $mysql -u root -p"$plainPass" -e "
        CREATE DATABASE IF NOT EXISTS billing_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
        SELECT 'Database created!' AS Status;
        SHOW DATABASES LIKE 'billing%';
    " 2>&1
    
    Write-Host ""
    Write-Host "Done! Now update application-prod.properties:" -ForegroundColor Green
    Write-Host "  spring.datasource.password=<your-root-password>" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Run the app with MySQL:" -ForegroundColor Cyan
    Write-Host "  java -jar target\BillingSystemWeb-1.0.0.jar --spring.profiles.active=prod" -ForegroundColor White
}
