#!/usr/bin/env pwsh

# Maven wrapper script for Windows (PowerShell)
$MAVEN_VERSION = "3.9.6"
$MAVEN_HOME = "$PSScriptRoot\.mvn\apache-maven-$MAVEN_VERSION"
$MAVEN_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

# Set JAVA_HOME only if not already set and candidate directory exists
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\Java\jdk-21.0.12.1") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
    }
}

# Download Maven if not present
if (-not (Test-Path "$MAVEN_HOME\bin\mvn.cmd")) {
    Write-Host "Downloading Maven $MAVEN_VERSION..."

    # Create .mvn/cache directory
    $cacheDir = "$PSScriptRoot\.mvn\cache"
    if (-not (Test-Path $cacheDir)) {
        New-Item -ItemType Directory -Path $cacheDir -Force | Out-Null
    }

    # Download Maven
    $zipFile = "$cacheDir\maven-$MAVEN_VERSION.zip"
    if (-not (Test-Path $zipFile)) {
        Invoke-WebRequest -Uri $MAVEN_URL -OutFile $zipFile
    }

    # Extract Maven
    $extractDir = "$PSScriptRoot\.mvn"
    if (-not (Test-Path $extractDir)) {
        New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
    }

    Expand-Archive -Path $zipFile -DestinationPath $extractDir -Force
}

# Run Maven
& "$MAVEN_HOME\bin\mvn.cmd" @args
