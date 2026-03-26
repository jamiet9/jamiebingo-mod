$jar = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.gradle\caches\forge_gradle\mcp_repo\de\oceanlabs\mcp\mcp_config\1.21.11-20251223.124241\joined\downloadClient\client.jar'
$src = 'assets/minecraft/textures/gui/advancements/backgrounds/stone.png'
$dest = 'A:\MC bingo server\jamiebingo 1.21.11 development\src\main\resources\assets\jamiebingo\textures\gui\advancement_icon.png'
$dir = Split-Path $dest
New-Item -ItemType Directory -Path $dir -Force | Out-Null

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
$entry = $zip.GetEntry($src)
if ($entry -eq $null) {
    Write-Error "Entry not found: $src"
    $zip.Dispose()
    exit 1
}
Write-Output ("Entry size: " + $entry.Length)
$entryStream = $entry.Open()
$out = [IO.File]::Open($dest, [IO.FileMode]::Create)
$entryStream.CopyTo($out)
$out.Close()
$entryStream.Close()
$zip.Dispose()
