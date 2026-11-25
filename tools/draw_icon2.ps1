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
$bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush($bgRect, (C 10 13 20), (C 26 33 47), 115.0)
$g.FillRectangle($bg, $bgRect); $bg.Dispose()

# тонкая радиальная подсветка (сдержанная)
for ($i = 14; $i -ge 1; $i--) {
  $rad = [int](230 + $i * 26)
  $alpha = [int](2 + $i * 0.55)
  $br = New-Object System.Drawing.SolidBrush (C 214 152 12 $alpha)
  $g.FillEllipse($br, (512 - $rad), (500 - $rad), ($rad * 2), ($rad * 2)); $br.Dispose()
}

# ---------------- тень монеты ----------------
for ($i = 1; $i -le 12; $i++) {
  $w = 330 + $i * 13
  $h = 70 + $i * 8
  $a = [int](30 - $i * 2); if ($a -lt 1) { $a = 1 }
  $br = New-Object System.Drawing.SolidBrush (C 0 0 0 $a)
  $g.FillEllipse($br, (512 - [int]($w / 2)), (742 - [int]($h / 2)), $w, $h); $br.Dispose()
}

# ---------------- обод (тёмное золото) ----------------
$rimRect = New-Object System.Drawing.Rectangle 176, 156, 672, 672
$rimBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rimRect, (C 150 96 6), (C 74 44 2), 60.0)
$g.FillEllipse($rimBrush, $rimRect); $rimBrush.Dispose()

# тонкая светлая кромка обода сверху-слева
$rimPen = New-Object System.Drawing.Pen (C 255 214 120 150), 6
$g.DrawArc($rimPen, 179, 159, 666, 666, 150, 120)
$rimPen.Dispose()

# ---------------- лицо монеты ----------------
$faceRect = New-Object System.Drawing.Rectangle 208, 188, 608, 608
$facePath = New-Object System.Drawing.Drawing2D.GraphicsPath
$facePath.AddEllipse($faceRect)
$pgb = New-Object System.Drawing.Drawing2D.PathGradientBrush $facePath
$pgb.CenterPoint = New-Object System.Drawing.PointF 410, 390
$pgb.CenterColor = (C 255 233 158)
$pgb.SurroundColors = @((C 198 128 16))
$g.FillPath($pgb, $facePath)
$pgb.Dispose()

# внутренний обод лица
$innerPen = New-Object System.Drawing.Pen (C 120 72 4 160), 10
$g.DrawEllipse($innerPen, 244, 224, 536, 536)
$innerPen.Dispose()

# блик сверху
$hl = New-Object System.Drawing.Drawing2D.GraphicsPath
$hl.AddEllipse(252, 214, 520, 300)
$hb = New-Object System.Drawing.SolidBrush (C 255 255 255 40)
$g.FillPath($hb, $hl); $hb.Dispose(); $hl.Dispose()

# диагональный блик
$sh = New-Object System.Drawing.SolidBrush (C 255 255 255 18)
$g.FillPolygon($sh, @(
  (New-Object System.Drawing.Point 286, 700),
  (New-Object System.Drawing.Point 500, 210),
  (New-Object System.Drawing.Point 596, 210),
  (New-Object System.Drawing.Point 382, 700)))
$sh.Dispose()

# ---------------- буква A (гравировка) ----------------
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
$box = New-Object System.Drawing.RectangleF 200, 186, 624, 612

# highlight (смещение вниз-вправо) — эффект вдавленной буквы
$lightBrush = New-Object System.Drawing.SolidBrush (C 255 246 208 220)
$g.DrawString("A", $font, $lightBrush, (New-Object System.Drawing.RectangleF 207, 193, 624, 612), $fmt)
$lightBrush.Dispose()

# тёмная буква
$darkBrush = New-Object System.Drawing.SolidBrush (C 108 65 3)
$g.DrawString("A", $font, $darkBrush, $box, $fmt)
$darkBrush.Dispose()

# ---------------- частицы ----------------
$rand = New-Object System.Random 11
for ($i = 0; $i -lt 34; $i++) {
  $ang = $rand.NextDouble() * 6.283
  $dist = 355 + $rand.NextDouble() * 120
  $x = 512 + [int]([Math]::Cos($ang) * $dist)
  $y = 492 + [int]([Math]::Sin($ang) * $dist)
  if ($x -lt 8 -or $x -gt ($S - 10) -or $y -lt 8 -or $y -gt ($S - 10)) { continue }
  $sz = 3 + $rand.Next(0, 6)
  $al = 35 + $rand.Next(0, 120)
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
Save-Square $bmp 32  (Join-Path $OUT "icon-32.png")
$bmp.Save((Join-Path $OUT "icon-1024.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Get-ChildItem $OUT -Filter "icon*.png" | Select-Object Name, Length
