Add-Type -AssemblyName System.Drawing

$OUT = "D:\MineCraft\plugins\plugins\AurumBuyer"
$S = 1024          # рисуем в 2x, потом даунскейлим до 512 — чище края
$bmp = New-Object System.Drawing.Bitmap $S, $S
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

function C([int]$r, [int]$gg, [int]$b, [int]$a = 255) {
  [System.Drawing.Color]::FromArgb($a, $r, $gg, $b)
}

# ---------- фон: тёмный градиент ----------
$bgRect = New-Object System.Drawing.Rectangle 0, 0, $S, $S
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($bgRect, (C 13 17 26), (C 30 38 54), 90.0)
$g.FillRectangle($bg, $bgRect)
$bg.Dispose()

# ---------- мягкое янтарное свечение за монетой ----------
for ($i = 22; $i -ge 1; $i--) {
  $rad = [int](150 + $i * 17)
  $alpha = [int](3 + $i * 0.7)
  $col = C 224 163 0 $alpha
  $br = New-Object System.Drawing.SolidBrush $col
  $g.FillEllipse($br, (512 - $rad), (512 - $rad), ($rad * 2), ($rad * 2))
  $br.Dispose()
}

# ---------- тень под монетой ----------
for ($i = 1; $i -le 10; $i++) {
  $w = 300 + $i * 14
  $h = 60 + $i * 7
  $a = [int](26 - $i * 2)
  if ($a -lt 1) { $a = 1 }
  $br = New-Object System.Drawing.SolidBrush (C 0 0 0 $a)
  $g.FillEllipse($br, (512 - [int]($w / 2)), (700 - [int]($h / 2)), $w, $h)
  $br.Dispose()
}

# ---------- монета ----------
$coin = New-Object System.Drawing.Rectangle 196, 176, 632, 632
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddEllipse($coin)

$pgb = New-Object System.Drawing.Drawing2D.PathGradientBrush $path
$pgb.CenterPoint = New-Object System.Drawing.PointF 430, 400
$pgb.CenterColor = (C 255 236 168)
$pgb.SurroundColors = @((C 186 118 12))
$g.FillPath($pgb, $path)
$pgb.Dispose()

# внешний обод
$ring = New-Object System.Drawing.Pen (C 122 76 6), 22
$g.DrawEllipse($ring, 205, 185, 614, 614)
$ring.Dispose()

# внутренний тонкий обод
$ring2 = New-Object System.Drawing.Pen (C 255 232 158 120), 7
$g.DrawEllipse($ring2, 246, 226, 532, 532)
$ring2.Dispose()

# верхний блик (эллипс-полумесяц)
$hlPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$hlPath.AddEllipse(260, 226, 504, 300)
$hlBrush = New-Object System.Drawing.SolidBrush (C 255 255 255 46)
$g.FillPath($hlBrush, $hlPath)
$hlBrush.Dispose()
$hlPath.Dispose()

# диагональный блик
$shine = New-Object System.Drawing.SolidBrush (C 255 255 255 22)
$shinePts = @(
  (New-Object System.Drawing.Point 250, 700),
  (New-Object System.Drawing.Point 470, 200),
  (New-Object System.Drawing.Point 590, 200),
  (New-Object System.Drawing.Point 370, 700)
)
$g.FillPolygon($shine, $shinePts)
$shine.Dispose()

# ---------- буква A (гравировка) ----------
$font = $null
foreach ($name in @("Segoe UI Black", "Arial Black", "Impact", "Arial")) {
  try {
    $f = New-Object System.Drawing.Font $name, 350, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
    if ($f.Name -eq $name) { $font = $f; break }
    $f.Dispose()
  } catch { }
}
if (-not $font) { $font = New-Object System.Drawing.Font "Arial", 350, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel) }

$fmt = New-Object System.Drawing.StringFormat
$fmt.Alignment = [System.Drawing.StringAlignment]::Center
$fmt.LineAlignment = [System.Drawing.StringAlignment]::Center

# тень гравировки (светлая, снизу-справа)
$light = New-Object System.Drawing.SolidBrush (C 255 245 205 210)
$rectShadow = New-Object System.Drawing.RectangleF 512, 508, 0, 0
$g.DrawString("A", $font, $light, (New-Object System.Drawing.RectangleF 216, 200, 592, 600), $fmt)
$light.Dispose()

# сама буква (тёмное золото)
$dark = New-Object System.Drawing.SolidBrush (C 112 68 4)
$g.DrawString("A", $font, $dark, (New-Object System.Drawing.RectangleF 210, 194, 592, 600), $fmt)
$dark.Dispose()

# ---------- частицы золота ----------
$rand = New-Object System.Random 7
for ($i = 0; $i -lt 46; $i++) {
  $ang = $rand.NextDouble() * 6.283
  $dist = 330 + $rand.NextDouble() * 150
  $x = 512 + [int]([Math]::Cos($ang) * $dist)
  $y = 512 + [int]([Math]::Sin($ang) * $dist)
  if ($x -lt 6 -or $x -gt ($S - 6) -or $y -lt 6 -or $y -gt ($S - 6)) { continue }
  $size = 3 + $rand.Next(0, 7)
  $alpha = 40 + $rand.Next(0, 150)
  $br = New-Object System.Drawing.SolidBrush (C 255 214 102 $alpha)
  $g.FillEllipse($br, $x, $y, $size, $size)
  $br.Dispose()
}

$font.Dispose()
$fmt.Dispose()
$g.Dispose()

# ---------- даунскейл до 512 и 128 ----------
function Save-Square($src, [int]$size, [string]$dst) {
  $b = New-Object System.Drawing.Bitmap $size, $size
  $gg = [System.Drawing.Graphics]::FromImage($b)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $gg.DrawImage($src, 0, 0, $size, $size)
  $gg.Dispose()
  $b.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
  $b.Dispose()
}

Save-Square $bmp 512 (Join-Path $OUT "icon.png")
Save-Square $bmp 128 (Join-Path $OUT "icon-128.png")
$bmp.Save((Join-Path $OUT "icon-1024.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Get-ChildItem $OUT -Filter "icon*.png" | Select-Object Name, Length
