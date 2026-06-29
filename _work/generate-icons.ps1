$ErrorActionPreference = "Stop"
$iconDir = Join-Path (Split-Path -Parent $PSScriptRoot) "plugin\bundles\ru.cursor.edt.copypath.ui\icons"
New-Item -ItemType Directory -Force -Path $iconDir | Out-Null
Add-Type -AssemblyName System.Drawing

function Add-RoundedRectangle([System.Drawing.Drawing2D.GraphicsPath]$path, [System.Drawing.Rectangle]$rect, [int]$radius) {
    $d = $radius * 2
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

function New-CopyPathIcon([string]$path, [int]$canvasSize) {
    $bmp = New-Object System.Drawing.Bitmap $canvasSize, $canvasSize
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $g.Clear([System.Drawing.Color]::Transparent)

    # Горизонтальный прямоугольник (не квадрат) внутри квадратного холста 16x16 / 32x32.
    # Рамка ~1.5× к предыдущему варианту, с отступом 1 px от краёв холста.
    $rectWidth = [Math]::Min($canvasSize - 2, [Math]::Max(8, [int]($canvasSize * 0.72 * 1.5)))
    $rectHeight = [Math]::Min($canvasSize - 2, [Math]::Max(5, [int]($canvasSize * 0.40 * 1.5)))
    $rectX = [int](($canvasSize - $rectWidth) / 2)
    $rectY = [int](($canvasSize - $rectHeight) / 2)
    $rect = New-Object System.Drawing.Rectangle $rectX, $rectY, $rectWidth, $rectHeight
    $radius = [Math]::Max(2, [int]($canvasSize * 0.10))

    $fill = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 245, 245, 245))
    $border = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 110, 110, 110)), 1
    $shape = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-RoundedRectangle $shape $rect $radius
    $g.FillPath($fill, $shape)
    $g.DrawPath($border, $shape)

    $fontSize = [Math]::Max(5, [single]($canvasSize * 0.24))
    $font = New-Object System.Drawing.Font("Segoe UI", $fontSize, [System.Drawing.FontStyle]::Bold)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $textRect = New-Object System.Drawing.RectangleF 0, 0, $canvasSize, $canvasSize
    $g.DrawString("\\..", $font, [System.Drawing.Brushes]::DimGray, $textRect, $sf)

    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose(); $font.Dispose(); $fill.Dispose(); $border.Dispose(); $shape.Dispose()
}

New-CopyPathIcon (Join-Path $iconDir "copy-as-path.png") 16
New-CopyPathIcon (Join-Path $iconDir "copy-as-path@2x.png") 32
Write-Output "Icons generated in $iconDir (16x16 / 32x32 canvas, horizontal frame)"
