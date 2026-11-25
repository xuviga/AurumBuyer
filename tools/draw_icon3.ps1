Add-Type -AssemblyName System.Drawing

$OUT = "D:\MineCraft\plugins\plugins\AurumBuyer"
$S = 1024
$bmp = New-Object System.Drawing.Bitmap $S, $S
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

function C([int]$r, [int]$gg, [int]$b, [int]$a = 255) { [System.Drawing.Color]::FromArgb($a, $r, $gg, $b) }

# ---------------- фон ----------------
$bgRect = New-Object System.Drawing.Rectangle 0, 0, $S, $S
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($bgRect, (C 9 12 19), (C 24 31 45), 115.0)
$g.FillRectangle($bg, $bgRect); $bg.Dispose()

# ---------------- гладкое радиальное свечение (без полос) ----------------
$glowRect = New-Object System.Drawing.Rectangle 42, 32, 940, 940
$glowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$glowPath.AddEllipse($glowRect)
$glow = New-Object System.Drawing.Drawing2D.PathGradientBrush $glowPath
$glow.CenterPoint = New-Object System.Drawing.PointF 512, 500
$glow.CenterColor = (C 226 164 20 96)
$glow.SurroundColors = @((C 226 164 20 0))
$g.FillPath($glow, $glowPath)
$glow.Dispose(); $glowPath.Dispose()

# ---------------- тень монеты ----------------
for ($i = 1; $i -le 12; $i++) {
  $w = 330 + $i * 13
  $h = 70 + $i * 8
  $a = [int](28 - $i * 2); if ($a -lt 1) { $a = 1 }
  $br = New-Object System.Drawing.SolidBrush (C 0 0 0 $a)
  $g.FillEllipse($br, (512 - [int]($w / 2)), (742 - [int]($h / 2)), $w, $h); $br.Dispose()
}

# ---------------- обод ----------------
$rimRect = New-Object System.Drawing.Rectangle 176, 156, 672, 672
$rimBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rimRect, (C 158 102 8), (C 66 39 2), 60.0)
$g.FillEllipse($rimBrush, $rimRect); $rimBrush.Dispose()

$rimPen = New-Object System.Drawing.Pen (C 255 218 130 130), 5
$g.DrawArc($rimPen, 180, 160, 664, 664, 155, 110)
$rimPen.Dispose()

# ---------------- лицо ----------------
$faceRect = New-Object System.Drawing.Rectangle 208, 188, 608, 608
$facePath = New-Object System.Drawing.Drawing2D.GraphicsPath
$facePath.AddEllipse($faceRect)
$pgb = New-Object System.Drawing.Drawing2D.PathGradientBrush $facePath
$pgb.CenterPoint = New-Object System.Drawing.PointF 408, 386
$pgb.CenterColor = (C 255 234 162)
$pgb.SurroundColors = @((C 196 126 15))
$g.FillPath($pgb, $facePath); $pgb.Dispose()

$innerPen = New-Object System.Drawing.Pen (C 122 74 4 150), 9
$g.DrawEllipse($innerPen, 246, 226, 532, 532); $innerPen.Dispose()

$hl = New-Object System.Drawing.Drawing2D.GraphicsPath
$hl.AddEllipse(254, 216, 516, 296)
$hb = New-Object System.Drawing.SolidBrush (C 255 255 255 34)
$g.FillPath($hb, $hl); $hb.Dispose(); $hl.Dispose()

$sh = New-Object System.Drawing.SolidBrush (C 255 255 255 14)
$g.FillPolygon($sh, @(
  (New-Object System.Drawing.Point 288, 700),
  (New-Object System.Drawing.Point 502, 212),
  (New-Object System.Drawing.Point 594, 212),
  (New-Object System.Drawing.Point 380, 700)))
$sh.Dispose()

# ---------------- буква A ----------------
$font = $null
foreach ($name in @("Segoe UI Black", "Arial Black", "Impact")) {
  $f = New-Object System.Drawing.Font $name, 356, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
  if ($f.Name -eq $name) { $font = $f; break }
  $f.Dispose()
}
if (-not $font) { $font = New-Object System.Drawing.Font "Arial", 356, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel) }

$fmt = New-Object System.Drawing.StringFormat
$fmt.Alignment = [System.Drawing.StringAlignment]::Center
$fmt.LineAlignment = [System.Drawing.StringAlignment]::Center

$lightBrush = New-Object System.Drawing.SolidBrush (C 255 244 200 130)
$g.DrawString("A", $font, $lightBrush, (New-Object System.Drawing.RectangleF 205, 191, 624, 612), $fmt)
$lightBrush.Dispose()

$darkBrush = New-Object System.Drawing.SolidBrush (C 104 62 3)
$g.DrawString("A", $font, $darkBrush, (New-Object System.Drawing.RectangleF 200, 186, 624, 612), $fmt)
$darkBrush.Dispose()

# ---------------- частицы ----------------
$rand = New-Object System.Random 11
for ($i = 0; $i -lt 30; $i++) {
  $ang = $rand.NextDouble() * 6.283
  $dist = 360 + $rand.NextDouble() * 110
  $x = 512 + [int]([Math]::Cos($ang) * $dist)
  $y = 492 + [int]([Math]::Sin($ang) * $dist)
  if ($x -lt 10 -or $x -gt ($S - 12) -or $y -lt 10 -or $y -gt ($S - 12)) { continue }
  $sz = 3 + $rand.Next(0, 6)
  $al = 30 + $rand.Next(0, 110)
  $br = New-Object System.Drawing.SolidBrush (C 255 213 96 $al)
  $g.FillEllipse($br, $x, $y, $sz, $sz); $br.Dispose()
}

$facePath.Dispose(); $font.Dispose(); $fmt.Dispose(); $g.Dispose()

function Save-Square($src, [int]$size, [string]$dst) {
  $b = New-Object System.Drawing.Bitmap $size, $size
  $gg = [System.Drawing.Graphics]::FromImage($b)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $gg.DrawImage($src, 0, 0, $size, $size); $gg.Dispose()
  $b.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
}

Save-Square $bmp 512 (Join-Path $OUT "icon.png")
Save-Square $bmp 128 (Join-Path $OUT "icon-128.png")
Save-Square $bmp 64  (Join-Path $OUT "icon-64.png")
Save-Square $bmp 32  (Join-Path $OUT "icon-32.png")
$bmp.Save((Join-Path $OUT "icon-1024.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Get-ChildItem $OUT -Filter "icon*.png" | Select-Object Name, Length
