# Cria a árvore sugerida em docs/lojapp/02-pilotos-e-xmls.md (fora do Git).
# Uso (na raiz do repo ou de qualquer sítio):
#   .\scripts\setup-pilotos-xml-folders.ps1
#   .\scripts\setup-pilotos-xml-folders.ps1 -Root "D:\dados\LojApp-pilotos-xmls"
param(
    [string] $Root = (Join-Path ([Environment]::GetFolderPath("Desktop")) "LojApp-pilotos-xmls")
)

$ErrorActionPreference = "Stop"

$sub = @(
    "piloto-1-alto-volume\grandes-itens",
    "piloto-1-alto-volume\poucos-itens",
    "piloto-1-alto-volume\misto-marcas",
    "piloto-2-mix-marcas\grandes-itens",
    "piloto-2-mix-marcas\poucos-itens",
    "piloto-2-mix-marcas\misto-marcas",
    "piloto-3-operacao-menor\grandes-itens",
    "piloto-3-operacao-menor\poucos-itens",
    "piloto-3-operacao-menor\misto-marcas",
    "anonimizados"
)

foreach ($rel in $sub) {
    $full = Join-Path $Root $rel
    New-Item -ItemType Directory -Force -Path $full | Out-Null
}

$readme = @"
LojApp — pasta de XMLs de piloto (NÃO commitar no Git público)

- Coloque aqui apenas cópias de trabalho ou versões anonimizadas.
- Nome sugerido por ficheiro: fornecedor-resumo-AAAA-MM-DD.xml
- Registe validações na tabela de docs/lojapp/02-pilotos-e-xmls.md (sem colar XML).

Criado/atualizado por: scripts/setup-pilotos-xml-folders.ps1
Root: $Root
"@
$readmePath = Join-Path $Root "LEIAME.txt"
Set-Content -Path $readmePath -Value $readme -Encoding UTF8

Write-Host "Pastas criadas em: $Root"
