<#
.SYNOPSIS
  Gera a chave de assinatura do FitPersonal (keystore) e o arquivo keystore.properties.

.DESCRIPTION
  - Cria "fitpersonal-release.jks" na raiz do projeto, com RSA 2048 e validade de 10.000 dias.
  - Gera uma senha forte ALEATÓRIA (você não digita nem vê senha em tela) e grava em keystore.properties.
  - Os dois arquivos estão no .gitignore: nunca vão para o GitHub.

  ATENÇÃO — leia antes de rodar:
  * Perder o .jks ou a senha impede atualizar o app na loja. Faça cópia dos DOIS arquivos em local seguro
    (gerenciador de senhas, pendrive guardado, nuvem privada) logo depois de gerar.
  * Recomendado: ative o "Play App Signing" no Play Console. Assim esta chave é só a chave de ENVIO, e o Google
    guarda a chave definitiva — se a de envio se perder, dá para pedir uma nova.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\gerar-keystore.ps1
#>
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
$arquivoJks = Join-Path $raiz "fitpersonal-release.jks"
$arquivoProps = Join-Path $raiz "keystore.properties"

if ((Test-Path $arquivoJks) -or (Test-Path $arquivoProps)) {
    Write-Host "Já existe fitpersonal-release.jks ou keystore.properties nesta pasta." -ForegroundColor Yellow
    Write-Host "Não vou sobrescrever (isso destruiria a chave atual). Mova ou apague os arquivos de propósito se tiver certeza." -ForegroundColor Yellow
    exit 1
}

# Localiza o keytool: JAVA_HOME, JDK do Android Studio ou o JDK 21 usado no projeto
$candidatos = @()
if ($env:JAVA_HOME) { $candidatos += (Join-Path $env:JAVA_HOME "bin\keytool.exe") }
$candidatos += "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
$candidatos += (Join-Path $env:USERPROFILE ".jdks\jbr-21.0.11\bin\keytool.exe")
$keytool = $candidatos | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $keytool) {
    $cmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($cmd) { $keytool = $cmd.Source }
}
if (-not $keytool) { throw "keytool não encontrado. Defina JAVA_HOME para um JDK (17 ou 21) e rode de novo." }
Write-Host "Usando $keytool"

Write-Host ""
Write-Host "Dados do certificado (aparecem no certificado do app; podem ficar em branco com Enter):"
$nome = Read-Host "Seu nome ou o da empresa"
$cidade = Read-Host "Cidade"
$estado = Read-Host "Estado (sigla)"
if (-not $nome) { $nome = "FitPersonal" }
function Limpar($t) { return ($t -replace '[,=+<>#;"\\]', ' ').Trim() }
$dname = "CN=" + (Limpar $nome) + ", O=FitPersonal"
if ($cidade) { $dname += ", L=" + (Limpar $cidade) }
if ($estado) { $dname += ", ST=" + (Limpar $estado) }
$dname += ", C=BR"

# Senha forte aleatória (24 caracteres, sem símbolos que atrapalhem arquivos .properties)
$bytes = New-Object byte[] 24
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
$senha = -join ($bytes | ForEach-Object { $alfabeto[$_ % $alfabeto.Length] })

$env:FP_KEYSTORE_SENHA = $senha
try {
    & $keytool -genkeypair -v `
        -keystore $arquivoJks `
        -alias fitpersonal `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -dname $dname `
        -storepass:env FP_KEYSTORE_SENHA `
        -keypass:env FP_KEYSTORE_SENHA
    if ($LASTEXITCODE -ne 0) { throw "O keytool falhou (código $LASTEXITCODE)." }
} finally {
    Remove-Item Env:\FP_KEYSTORE_SENHA -ErrorAction SilentlyContinue
}

$conteudo = @(
    "# Gerado por scripts/gerar-keystore.ps1 em $(Get-Date -Format 'yyyy-MM-dd HH:mm'). NUNCA versione este arquivo.",
    "storeFile=fitpersonal-release.jks",
    "storePassword=$senha",
    "keyAlias=fitpersonal",
    "keyPassword=$senha"
)
Set-Content -Path $arquivoProps -Value $conteudo -Encoding ASCII

Write-Host ""
Write-Host "Pronto." -ForegroundColor Green
Write-Host "  Chave:  $arquivoJks"
Write-Host "  Dados:  $arquivoProps  (contém a senha)"
Write-Host ""
Write-Host "FAÇA AGORA UMA CÓPIA DOS DOIS ARQUIVOS em local seguro e fora deste computador." -ForegroundColor Yellow
Write-Host "Depois gere o pacote da loja:  .\gradlew.bat :app:bundleRelease"
