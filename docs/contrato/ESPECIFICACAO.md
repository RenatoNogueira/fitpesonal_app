# Especificação do contrato de troca (formato 1)

Documentos trocados entre o app do profissional, o app do aluno (Android/iOS) e a página HTML.
**Esta especificação é a fonte de verdade**; a implementação de referência é `contrato/` (Kotlin).
Arquivos de exemplo em [`exemplos/`](exemplos/) devem ser lidos, validados e **re-serializados idênticos** por qualquer implementação.

## 1. Convenções
- Texto UTF-8, JSON (RFC 8259). Extensões: `.rjetreino` (treino) e `.rjeresumo` (resumo).
- Datas: `yyyy-MM-dd`. Identificadores: `[A-Za-z0-9_-]{1,64}` (UUID recomendado).
- Campo `formato` (inteiro): versão **1**. Leitor que receber `formato` maior deve recusar e pedir atualização.
- Campo `tipo`: `"treino"` ou `"resumo"`.
- Campo opcional `integridade`: `"sha256:" + hex` do **documento canônico** (seção 2). Ausente = não verificado; presente e divergente = **recusar**.
- Limites: documento ≤ 512 KB; textos ≤ 200 caracteres (observações ≤ 1000; mensagem do aluno ≤ 2000); ≤ 300 exercícios por divisão; ≤ 400 sessões; ≤ 300 itens por sessão.

## 2. Forma canônica (base do checksum)
O checksum é calculado sobre a string produzida **escrevendo os campos nesta ordem, sem espaços ou quebras de linha**, sem o campo `integridade`:
- Strings: aspas duplas; escapar `"`, `\`, `\n`, `\r`, `\t` e controles `< 0x20` como `\u00xx` minúsculo; demais caracteres (inclusive acentos) **literais**.
- Números inteiros sem sinal `+`; decimais sem zeros à direita e sem notação científica (`22.5`, `20`).
- `null` explícito para campos opcionais ausentes.
- Divisões: apenas as que têm exercícios, na ordem `A`, `B`, `C`, `D`.
- Depois de calculado o checksum, o documento final é a mesma string com `,"integridade":"sha256:…"` antes do `}` final.

Ao **ler**, não depender da ordem das chaves do arquivo: interprete, reconstrua o modelo e re-serialize canonicamente para comparar.

## 3. `.rjetreino`
```json
{"formato":1,"tipo":"treino","treinoId":"…","revisao":3,"emitidoEm":"2026-05-20","validoAte":null,
 "profissional":{"nome":"…","marca":"…","cref":"…","contato":"…"},
 "aluno":{"nome":"…","ref":"…"},
 "treino":{"nome":"…","objetivo":null,"divisoes":{"A":[{"nome":"…","series":"3","repeticoes":"12",
   "carga":"20 kg","descanso":"60 s","obs":null,"catalogoId":null}]}}}
```
Ordem das chaves canônica: `formato, tipo, treinoId, revisao, emitidoEm, validoAte, profissional{nome, marca, cref, contato}, aluno{nome, ref}, treino{nome, objetivo, divisoes}`; exercício: `nome, series, repeticoes, carga, descanso, obs, catalogoId`.

**Campo opcional `video`** (depois de `catalogoId`): link `http://` ou `https://` (até 500 caracteres, sem espaços) de um vídeo de demonstração. **Só é gravado quando existe** — arquivos sem vídeo mantêm exatamente o mesmo texto canônico e checksum. Leitores devem aceitar apenas http/https e descartar qualquer outro valor. Como entra no texto canônico quando presente, leitores anteriores à inclusão do campo (nenhum publicado) recusariam o arquivo por integridade; por isso o campo foi adicionado antes da primeira publicação, sem subir `formato`.

- `treinoId` é estável; `revisao` (≥ 1) sobe a cada novo envio. O app do aluno substitui o treino de mesmo `treinoId` se a revisão for maior e **preserva o histórico**.
- `aluno.ref` é um token aleatório por aluno, devolvido no resumo.
- `validoAte` (opcional) é expiração suave.

## 4. `.rjeresumo`
```json
{"formato":1,"tipo":"resumo","resumoId":"…","alunoRef":"…","periodo":{"de":"…","ate":"…"},
 "sessoes":[{"execucaoId":"…","data":"…","treinoId":"…","divisao":"A","esforco":7,
   "itens":[{"exercicio":"…","cargaKg":22.5,"reps":"12","feito":true}]}],
 "mensagem":null}
```
Ordem canônica: `formato, tipo, resumoId, alunoRef, periodo{de, ate}, sessoes[execucaoId, data, treinoId, divisao, esforco, itens[exercicio, cargaKg, reps, feito]], mensagem`.
- `esforco` 1–10 ou `null`; `cargaKg` 0–2000 ou `null`; `divisao` ∈ A–D.
- Importação é **idempotente** por `execucaoId`.

## 5. Código de texto
Para colar em mensagens quando o arquivo não anexa bem:
- `RJE1:` + base64url **sem padding** de **DEFLATE bruto** (sem cabeçalho zlib) do JSON UTF-8.
- `RJE0:` + base64url sem padding do JSON UTF-8 (sem compressão; usado por navegadores sem `CompressionStream`).
- O leitor deve localizar o código dentro de um texto maior com a expressão `RJE[01]:[A-Za-z0-9_-]+`.
- Ao descomprimir, impor limite de 512 KB (proteção contra "bomba").

Equivalências: Java `Deflater(level, nowrap=true)`/`Inflater(nowrap=true)`; navegador `CompressionStream("deflate-raw")`; Apple `Compression` com `COMPRESSION_ZLIB` (que é DEFLATE bruto).

## 6. Privacidade
O treino **não** contém CPF, avaliações físicas nem observações médicas. O nome do aluno pode ser apenas o primeiro nome. O resumo contém somente o que o aluno decide enviar.

## 7. Compatibilidade
- Campos desconhecidos em versões **iguais** devem ser ignorados na leitura (mas não entram no checksum recalculado → quem os adicionar deve subir `formato`).
- Mudanças incompatíveis sobem `formato`; leitores antigos recusam com mensagem clara.


## Pacote "treino + vídeos" (`.rjetreinopack`)

Extensão opcional ao contrato (não altera o formato 1 do treino). É um **zip** com:

| Entrada | Conteúdo |
|---|---|
| `treino.rjetreino` | o mesmo JSON do contrato, com checksum (obrigatório) |
| `videos.json` | `{"formato":1,"videos":[{"arquivo":"v1.mp4","exercicios":["Supino reto","Supino"]}]}` |
| `videos/v1.mp4`, `videos/v2.mp4`… | vídeos próprios dos exercícios |

Regras de leitura:
- Os vídeos se ligam aos exercícios pelo **nome** (sem diferenciar maiúsculas/minúsculas, com espaços nas pontas removidos), como aparece em `treino.rjetreino`.
- Só são extraídas as entradas `treino.rjetreino`, `videos.json` e `videos/v<N>.mp4`; qualquer outro nome (inclusive com `..`) é ignorado.
- Limites: treino ≤ 512 KB, índice ≤ 64 KB, ≤ 40 vídeos, ≤ 600 MB descompactados.
- O leitor aplica o treino com as mesmas regras de revisão; **os vídeos só são substituídos quando o treino é novo ou mais recente**.
- Um leitor que não conhece o pacote pode abrir `treino.rjetreino` diretamente.
