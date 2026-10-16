# FitPersonal para iPhone (modo aluno)

App SwiftUI que faz, no iPhone, o que o **modo aluno** faz no Android: receber o treino do professor
(arquivo `.rjetreino`, pacote com vídeos `.rjetreinopack` ou código colado), executar a sessão, registrar
cargas, ver o histórico, enviar o resumo e receber lembretes.

> **Estado: escrito sem Xcode — nunca foi compilado nem executado.** Trate como código-base para
> terminar e validar num Mac. A lista de verificações abaixo diz o que conferir primeiro.

## Como abrir
```bash
brew install xcodegen
cd ios/FitPersonal
xcodegen generate
open FitPersonal.xcodeproj
```
Requer Xcode 15+ (o efeito *Liquid Glass* usa a SDK do Xcode 26; em SDK anterior o app usa materiais
translúcidos comuns, porque o código fica protegido por `#if compiler(>=6.2)` e `#available(iOS 26, *)`).

## O que tem
| Arquivo | Função |
|---|---|
| `Sources/Contrato/ContratoRJE.swift` | Modelos, escrita canônica, SHA-256, leitura/validação e código `RJE1:`/`RJE0:` |
| `Sources/Contrato/PacoteVideos.swift` | Leitor mínimo de zip do `.rjetreinopack` (sem dependências) |
| `Sources/Dados/Repositorio.swift` | Armazenamento local (treinos, sessões, vídeos) e montagem do resumo |
| `Sources/Dados/Lembretes.swift` | Avisos locais: dias de treino e retenção (3 dias sem treinar) |
| `Sources/UI/*` | Lista, execução, histórico, colar código, lembretes, player e tema |
| `Tests/ContratoTests.swift` | Valida contra `docs/contrato/exemplos` (leitura, re-serialização idêntica, código) |

## Verificações a fazer num Mac (nesta ordem)
1. **Compilar** e corrigir o que o compilador apontar (é a primeira vez que o código vê um compilador).
2. **Rodar `FitPersonalTests`**. O teste mais importante é `testLeEReserializaOTreino`: a serialização
   canônica (principalmente `Double`) precisa devolver exatamente o texto do exemplo.
3. **Abrir um `.rjetreino` e um `.rjetreinopack`** vindos do app Android (Arquivos/AirDrop).
4. **Conferir o resumo**: enviar o código gerado no iPhone para o app do professor (Android) e importar.
5. Revisar a caixa de **notificações** (permissão pedida só ao ligar os lembretes).

## Limites conhecidos
- Retenção (“sentimos sua falta”) é agendada **uma vez** a cada abertura/sessão; no Android o worker diário
  recalcula todo dia. Quem não abrir o app recebe só o primeiro aviso.
- O modo professor (cadastro de alunos, avaliações, agenda, financeiro) **não existe** no iPhone: é um app
  grande e continua só no Android.
- Sem sincronização de histórico entre aparelhos do mesmo aluno.
- Ícone do app: falta o conjunto de ícones (use `docs/` e o `logo_app.webp` do Android como origem).

## Publicação
Conta Apple Developer, ícones, capturas de tela, política de privacidade e revisão da App Store são etapas
fora deste repositório.
