# Library Management System - Local Runner Script
$ProgressPreference = 'SilentlyContinue'
# This script downloads a portable JDK 17 and Apache Maven into this folder,
# configures local environment variables, and launches the application.
# It does not require administrative privileges or global software installs.

$ProjectDir = Get-Location

# 1. Setup local Java 17 JDK
$JavaDir = Join-Path $ProjectDir "jdk-17"
if (-not (Test-Path (Join-Path $JavaDir "bin\java.exe"))) {
    Write-Host "[Retro Library Launcher] JDK 17 not found locally. Downloading Eclipse Temurin JDK 17..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $JavaDir | Out-Null
    $JavaUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip"
    $ZipPath = Join-Path $ProjectDir "jdk17.zip"
    
    # Download using native curl.exe
    curl.exe -L -o $ZipPath $JavaUrl
    
    # Extract
    Write-Host "[Retro Library Launcher] Extracting JDK files..." -ForegroundColor Cyan
    Expand-Archive -Path $ZipPath -DestinationPath $ProjectDir
    
    # Reorganize files to standard jdk-17 directory
    $ExtractedFolder = Get-ChildItem $ProjectDir | Where-Object { $_.PSIsContainer -and $_.Name -like "jdk-17*" -and $_.Name -ne "jdk-17" }
    Move-Item -Path "$($ExtractedFolder.FullName)\*" -Destination $JavaDir
    Remove-Item $ExtractedFolder.FullName -Force -Recurse
    Remove-Item $ZipPath -Force
    Write-Host "[Retro Library Launcher] Java 17 setup complete." -ForegroundColor Green
}

# 2. Setup local Apache Maven
$MavenDir = Join-Path $ProjectDir "maven"
if (-not (Test-Path (Join-Path $MavenDir "bin\mvn.cmd"))) {
    Write-Host "[Retro Library Launcher] Maven not found. Downloading Apache Maven..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $MavenDir | Out-Null
    $MavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
    $ZipPath = Join-Path $ProjectDir "maven.zip"
    
    # Download using native curl.exe
    curl.exe -L -o $ZipPath $MavenUrl
    
    # Extract
    Write-Host "[Retro Library Launcher] Extracting Maven files..." -ForegroundColor Cyan
    Expand-Archive -Path $ZipPath -DestinationPath $ProjectDir
    
    # Reorganize files to standard maven directory
    $ExtractedFolder = Get-ChildItem $ProjectDir | Where-Object { $_.PSIsContainer -and $_.Name -like "apache-maven-*" -and $_.Name -ne "maven" }
    Move-Item -Path "$($ExtractedFolder.FullName)\*" -Destination $MavenDir
    Remove-Item $ExtractedFolder.FullName -Force -Recurse
    Remove-Item $ZipPath -Force
    Write-Host "[Retro Library Launcher] Maven setup complete." -ForegroundColor Green
}

# 3. Configure local environment variables for this terminal session
$Env:JAVA_HOME = $JavaDir
$Env:PATH = "$JavaDir\bin;$MavenDir\bin;$Env:PATH"

Write-Host "=============================================" -ForegroundColor Green
Write-Host "   APEX RETRO LIBRARY SYSTEM - LOCAL RUNNER  " -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host "Runtime Path Info:" -ForegroundColor Yellow
java -version
mvn -v
Write-Host "=============================================" -ForegroundColor Green

# 4. Compile and launch the Spring Boot application
Write-Host "[Retro Library Launcher] Compilation starting..." -ForegroundColor Cyan
mvn clean compile
Write-Host "[Retro Library Launcher] Executing Boot Run..." -ForegroundColor Cyan
mvn spring-boot:run
