param(
    [Parameter(Mandatory = $true)]
    [string]$SourcePng,

    [string]$ResDir = "app/src/main/res",

    [string]$BackgroundColor = "#1976D2"
)

$ErrorActionPreference = "Stop"

function Assert-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "缺少命令：$Name"
    }
}

function Ensure-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Write-Utf8File {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Content
    )

    $parent = Split-Path -Parent $Path
    Ensure-Directory -Path $parent
    Set-Content -LiteralPath $Path -Value $Content -Encoding utf8
}

function Resolve-AbsolutePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location).Path $Path))
}

function Invoke-Magick {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    & magick @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ImageMagick 执行失败：magick $($Arguments -join ' ')"
    }
}

$sourcePath = Resolve-AbsolutePath -Path $SourcePng
$resolvedResDir = Resolve-AbsolutePath -Path $ResDir

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "找不到源 PNG：$sourcePath"
}

if ([System.IO.Path]::GetExtension($sourcePath).ToLowerInvariant() -ne ".png") {
    throw "只支持 PNG 输入：$sourcePath"
}

Assert-Command -Name "magick"

$legacySizes = @(
    @{ Density = "mdpi"; Size = 48 },
    @{ Density = "hdpi"; Size = 72 },
    @{ Density = "xhdpi"; Size = 96 },
    @{ Density = "xxhdpi"; Size = 144 },
    @{ Density = "xxxhdpi"; Size = 192 }
)

foreach ($entry in $legacySizes) {
    $targetDir = Join-Path $resolvedResDir "mipmap-$($entry.Density)"
    Ensure-Directory -Path $targetDir

    $size = $entry.Size
    $launcherTarget = Join-Path $targetDir "ic_launcher.png"
    $roundTarget = Join-Path $targetDir "ic_launcher_round.png"

    Invoke-Magick $sourcePath "-resize" "${size}x${size}" "-background" "none" "-gravity" "center" "-extent" "${size}x${size}" $launcherTarget
    Copy-Item -LiteralPath $launcherTarget -Destination $roundTarget -Force
}

$drawableDir = Join-Path $resolvedResDir "drawable"
$drawableNoDpiDir = Join-Path $resolvedResDir "drawable-nodpi"
$anyDpiDir = Join-Path $resolvedResDir "mipmap-anydpi-v26"
$valuesDir = Join-Path $resolvedResDir "values"

Ensure-Directory -Path $drawableDir
Ensure-Directory -Path $drawableNoDpiDir
Ensure-Directory -Path $anyDpiDir
Ensure-Directory -Path $valuesDir

$adaptiveForeground = Join-Path $drawableNoDpiDir "ic_launcher_foreground_art.png"
Invoke-Magick $sourcePath "-resize" "288x288" "-background" "none" "-gravity" "center" "-extent" "432x432" $adaptiveForeground

$backgroundXml = @'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/ic_launcher_background_color" />
</shape>
'@

$foregroundXml = @'
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:gravity="center"
    android:src="@drawable/ic_launcher_foreground_art" />
'@

$adaptiveIconXml = @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'@

$colorsXml = @"
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background_color">$BackgroundColor</color>
</resources>
"@

Write-Utf8File -Path (Join-Path $drawableDir "ic_launcher_background.xml") -Content $backgroundXml
Write-Utf8File -Path (Join-Path $drawableDir "ic_launcher_foreground.xml") -Content $foregroundXml
Write-Utf8File -Path (Join-Path $anyDpiDir "ic_launcher.xml") -Content $adaptiveIconXml
Write-Utf8File -Path (Join-Path $anyDpiDir "ic_launcher_round.xml") -Content $adaptiveIconXml
Write-Utf8File -Path (Join-Path $valuesDir "ic_launcher_colors.xml") -Content $colorsXml

Write-Host "已生成启动图标资源：$resolvedResDir"
