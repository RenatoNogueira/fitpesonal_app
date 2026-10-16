> **Atualização:** já existe um projeto SwiftUI (não compilado) em `ios/FitPersonal`, que usa e amplia esta referência.

# App iOS do aluno — plano

Estado: **somente especificação e referência**. Nenhum código aqui foi compilado (sem Xcode no ambiente atual).

## O que existe
- `../ESPECIFICACAO.md`: contrato (formato 1) — fonte de verdade.
- `../exemplos/`: `treino.rjetreino`, `resumo.rjeresumo` e os códigos de texto equivalentes. A implementação iOS deve:
  1. ler os dois arquivos e validar a integridade;
  2. re-serializá-los e obter **exatamente** o mesmo texto;
  3. decodificar `treino.codigo.txt` / `resumo.codigo.txt` e obter o mesmo JSON.
- `ContratoRJE.swift`: tradução de referência (modelos, escrita canônica, checksum, código de texto).

## Equivalências importantes
| Tema | Kotlin/Android | Swift/iOS | Web |
|---|---|---|---|
| SHA-256 | `MessageDigest` | `CryptoKit.SHA256` | — |
| DEFLATE bruto | `Deflater(nowrap=true)` | `NSData.compressed(using: .zlib)` | `CompressionStream("deflate-raw")` |
| base64url sem `=` | `Base64.getUrlEncoder().withoutPadding()` | trocar `+/` e remover `=` | idem manual |

## Telas previstas (espelham o app Android do aluno)
1. **Meus treinos** (lista, receber por arquivo/código).
2. **Executar** (abas A–D, checklist, carga, cronômetro de descanso, esforço).
3. **Histórico** e **enviar resumo** (código via folha de compartilhamento).

Receber arquivos: declarar o tipo `br.com.fitpersonal.treino` (UTI exportada) com extensão `rjetreino` e `CFBundleDocumentTypes`; tratar `onOpenURL` e a folha "Abrir com".

## Visual
Fase prevista com o estilo "Liquid Glass" (iOS 26+), com fallback de materiais padrão em versões anteriores.

## Riscos
- Divergência de formatação numérica (`Double`) na escrita canônica: cobrir com os exemplos e casos como `0.1`, `22.5`, `20`, `1e-7`.
- Normalização Unicode: o contrato exige texto **literal** (NFC não é aplicado); não normalizar ao ler/escrever.
- Armazenamento do navegador (página HTML) não é compartilhado com o app: o aluno que usar os dois terá históricos separados até enviar resumos.
