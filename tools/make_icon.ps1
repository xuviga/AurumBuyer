param(
  [string]$Source = "D:\MineCraft\plugins\plugins\AurumBuyer\icon.png",
  [string]$OutDir = "D:\MineCraft\plugins\plugins\AurumBuyer"
)
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path $Source)) {
  $cand = Get-ChildItem $OutDir -Filter "icon.*" | Where-Object { $_.Extension -ne ".ps1" } | Select-Object -First 1
  if (-not $cand) { throw "icon file not found" }
  $Source = $cand.FullName
}

function Resize-Square([string]$src, [int]$size, [string]$dst) {
  $img = [System.Drawing.Image]::FromFile($src)
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

  # cover-crop (центрирование по меньшей стороне, без искажений)
  $side = [Math]::Min($img.Width, $img.Height)
  $sx = [int](($img.Width - $side) / 2)
  $sy = [int](($img.Height - $side) / 2)
  $srcRect = New-Object System.Drawing.Rectangle $sx, $sy, $side, $side
  $dstRect = New-Object System.Drawing.Rectangle 0, 0, $size, $size
  $g.DrawImage($img, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
  $g.Dispose()
  $bmp.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  $img.Dispose()
  "ok: $dst ($size x $size)"
}

# Spigot требует ровно 512x512
Resize-Square $Source 512 (Join-Path $OutDir "icon.png")
# превью для README/темы на форуме
Resize-Square $Source 128 (Join-Path $OutDir "icon-128.png")
Get-ChildItem $OutDir -Filter "icon*.png" | Select-Object Name, Length
