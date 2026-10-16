> **Atualização:** o app do aluno deixou de ser um aplicativo separado. Hoje existe **um único app** FitPersonal: na primeira abertura a pessoa escolhe **professor** ou **aluno** (e pode trocar depois). O código do aluno está em `app/.../aluno`; o módulo `:aluno` foi aposentado e a decisão "app separado" abaixo ficou superada.

# Desenho: modo aluno (receber o treino direto)

Status: **proposta, nada implementado.** Pressupostos: app individual, sem servidor, dados no aparelho.

## 1. Objetivo e limites
- O aluno **recebe o treino** do profissional e **executa** (marca feito, anota carga e esforço).
- O aluno **devolve um resumo** que o profissional importa (cargas, sessões, observações).
- Sem contas, sem nuvem própria, sem tempo real. Tudo trafega por **arquivo, texto ou QR** (WhatsApp, e-mail, Drive).
- Fora do escopo agora: chat, pagamento pelo aluno, ver avaliações físicas (dado sensível fica só com o profissional).

## 2. Como o treino viaja

### Formato `.rjetreino` (JSON, texto UTF-8, neutro de plataforma)
```json
{
  "formato": 1,
  "treinoId": "b1d2…",          // UUID estável do treino
  "revisao": 3,                  // sobe a cada reenvio
  "emitidoEm": "2026-05-20",
  "validoAte": null,             // opcional
  "profissional": { "nome": "Renato N.", "marca": "RJE", "cref": "000000-G/SP", "contato": "(11) 9…" },
  "aluno": { "nome": "Maria", "ref": "9f3a…" },   // ref = token aleatório por aluno (devolvido no resumo)
  "treino": {
    "nome": "Hipertrofia A/B", "objetivo": "Ganhar massa",
    "divisoes": {
      "A": [ { "nome": "Supino reto", "series": "3", "repeticoes": "12", "carga": "20 kg",
               "descanso": "60 s", "obs": "controlar a descida", "catalogoId": "bench-press" } ]
    }
  },
  "integridade": "sha256:…"      // checksum do conteúdo (detecta arquivo corrompido, não é identidade)
}
```
Mínimo necessário: **sem CPF, sem avaliações, sem observações médicas**; nome do aluno pode ser só o primeiro.

### Três formas de entrega (todas com o mesmo conteúdo)
| Forma | Quando | Observação |
|---|---|---|
| **Arquivo** `.rjetreino` | uso normal | abre pelo app (intent-filter); WhatsApp/e-mail às vezes trocam o tipo MIME → ter plano B |
| **Código de texto** (`RJE1:` + base64url comprimido) | quando o arquivo falha | aluno copia a mensagem e toca em "Colar código" |
| **QR** | presencial | ~1–2 KB comprimido cabe; treinos muito grandes usam arquivo |

Os formatos de hoje (página HTML offline e texto do WhatsApp) **continuam** como plano B e para quem usa iPhone.

## 3. O lado do aluno

### Onde roda
| | Mesmo app com "modo aluno" | App separado "FitPersonal Treino" |
|---|---|---|
| Esforço | menor (tela inicial pergunta "sou profissional / aluno") | maior (módulo/flavor + publicação) |
| Privacidade | código de gestão presente no aparelho do aluno | superfície mínima, sem banco de saúde |
| Loja | confunde o público | descrição própria para alunos |
| Evolução | rápido para validar | melhor para escalar e para a versão iOS |

**Recomendação:** começar com **modo aluno no mesmo app** (validação rápida), mas estruturar o contrato (`.rjetreino`, regras de resumo) em pacote isolado para poder virar `:aluno` depois sem retrabalho.

### Telas (MVP)
1. **Meus treinos**: lista de treinos recebidos (nome, profissional, revisão, data).
2. **Executar treino**: abas A/B/C/D, checklist por exercício, **cronômetro de descanso** (usa o campo `descanso`), campo de carga usada e esforço (1–10) opcional.
3. **Histórico**: sessões feitas, cargas por exercício (gráfico simples), sequência de dias.
4. **Enviar resumo**: monta o resumo do período e abre o seletor (WhatsApp, e-mail).

### Armazenamento do aluno
- Banco local simples (Room **sem** SQLCipher é aceitável; opcionalmente ligar bloqueio por biometria do aparelho).
- Tabelas: `treino_recebido(treinoId, revisao, json, recebidoEm)`, `execucao(id UUID, treinoId, data, divisao, duracao, esforco)`, `serie(execucaoId, exercicio, cargaKg, reps, feito)`.
- Reenvio com mesmo `treinoId` e `revisao` maior **substitui** o conteúdo e **preserva** o histórico.

## 4. A volta: resumo para o profissional

### Formato `.rjeresumo` (JSON)
```json
{
  "formato": 1, "resumoId": "UUID", "alunoRef": "9f3a…",
  "periodo": { "de": "2026-05-01", "ate": "2026-05-31" },
  "sessoes": [ { "execucaoId": "UUID", "data": "2026-05-03", "treinoId": "b1d2…", "divisao": "A",
                 "esforco": 7, "itens": [ { "exercicio": "Supino reto", "cargaKg": 22.5, "reps": "12", "feito": true } ] } ],
  "mensagem": "Senti dor no ombro no supino."   // texto livre opcional do aluno
}
```
### Importação no profissional
1. "Importar resumo do aluno" (abre arquivo/colar código).
2. Casa por `alunoRef` (guardado no cadastro do aluno; schema novo) — se não achar, **pede para escolher o aluno**.
3. Mostra **prévia** (sessões, cargas, mensagem) e só grava após confirmar.
4. Efeitos: histórico de carga (já existe), anotação em sessão, **sugestão** (não automática) de marcar agendamentos como concluídos.
5. **Idempotente**: `execucaoId`/`resumoId` evitam duplicar se o aluno reenviar.

## 5. LGPD e segurança
- Minimização: o arquivo do treino não carrega dado clínico; o resumo só tem o que o aluno decide enviar.
- Papéis: a relação continua sendo do profissional (controlador); o app do aluno guarda dados **no aparelho do próprio aluno**.
- Aviso na primeira abertura do modo aluno (o que fica no aparelho, o que sai e para quem).
- Integridade: checksum evita arquivo corrompido; **não** prova identidade (sem servidor). Se for necessário, assinatura com chave do profissional (par EC no Keystore) fica como fase futura.
- Revogação: não há como "puxar de volta" um treino enviado; `validoAte` serve como expiração suave.
- Dado do aluno que pede exclusão: apagar localmente no app dele; o histórico já importado pelo profissional segue as regras de exclusão do profissional (já implementadas).

## 6. Casos de borda
- Aluno com iPhone: usa a **página HTML** (já existe). Quando houver app iOS, o formato JSON acima é o contrato comum.
- Dois treinos ativos / trocas de divisão: lista simples, sem "treino ativo" obrigatório.
- Arquivo de versão futura (`formato` > suportado): mensagem pedindo atualização do app.
- Relógio errado do aparelho: datas só informativas; deduplicação usa UUID.
- Catálogo de exercícios: não é enviado; `catalogoId` só aparece se o app do aluno tiver as imagens (fase futura, tamanho do APK).

## 7. Ordem sugerida
1. **Contrato**: serialização/validação de `.rjetreino` e `.rjeresumo`, código de texto, testes JVM (sem UI).
2. **Profissional exporta**: arquivo e código no menu do treino (+ `alunoRef` no cadastro).
3. **Modo aluno**: abrir arquivo/colar código, lista e execução com cronômetro.
4. **Resumo**: gerar no aluno e **importar** no profissional com prévia.
5. QR (geração e leitura) e catálogo com imagens: opcionais.
6. Separar em app próprio, se validar.

## 8. Perguntas em aberto
- Os alunos usam, em sua maioria, Android ou iPhone?
- O retorno do aluno (cargas e esforço) é desejado desde o início ou só a execução?
- Mesmo app com "modo aluno" (rápido) ou app separado desde já?
- Imagens/demonstração dos exercícios são importantes para o aluno?

---

## 9. Decisões tomadas (atualização)

| Pergunta | Decisão |
|---|---|
| Plataforma dos alunos | **Mistura de Android e iPhone** |
| Entrega do modo aluno | **App separado desde já** (não é "modo" dentro do app do profissional) |

### O que isso muda
1. **A recomendação da seção 3 é substituída**: o aluno terá um app próprio ("FitPersonal Treino"), não um modo do app do profissional. O app do profissional **não** ganha telas de aluno.
2. **iPhone**: o app iOS do aluno só existe quando a fase iOS começar. Até lá, o caminho universal é a **página HTML** — e ela passa a ser parte do plano, não só "plano B" (ver 9.2).
3. **O contrato vira o centro do projeto**: `.rjetreino` e `.rjeresumo` precisam ser especificados de forma neutra (JSON versionado + arquivos de teste de exemplo) para Android e iOS implementarem sem divergir.

### 9.1 Estrutura de projetos
```
rje-android/
├── app/            # profissional (existente), applicationId br.com.fitpersonal.app
├── aluno/          # NOVO: app do aluno, applicationId br.com.fitpersonal.treino
└── contrato/       # NOVO: biblioteca Kotlin pura (sem Android): modelos, JSON, checksum,
                    #       código de texto, validação de versão; testes JVM
docs/contrato/      # NOVO: especificação textual + exemplos (golden files) para o iOS
```
- `:app` e `:aluno` dependem de `:contrato`; **não** dependem um do outro.
- Assinaturas e listagens separadas na loja; versionamento do contrato independente (`formato`).
- O app do aluno **não** inclui Room com SQLCipher nem módulos do profissional: menor, mais simples, sem dado de saúde do estúdio.

### 9.2 Página HTML como caminho universal (iPhone e Android sem app)
Evoluir a página gerada hoje para **v2**:
- Embute o `.rjetreino` (JSON) na própria página.
- Checklist e carga usada ficam salvos no navegador (como hoje).
- Botão **"Enviar resumo"**: gera o `.rjeresumo` como **código de texto** e abre o compartilhamento do sistema (`navigator.share`) ou copia para a área de transferência.
- O profissional **cola o código** em "Importar resumo do aluno" — exatamente o mesmo caminho do app do aluno.
- Resultado: um aluno de iPhone tem entrega **e** retorno sem instalar nada; o app do aluno é uma melhoria (cronômetro, histórico, notificações), não um requisito.
- Limite: o armazenamento do navegador pode ser limpo (Safari remove dados de sites sem uso); por isso o resumo deve ser enviado ao fim da semana/sessão.

### 9.3 Ordem revisada
1. **`:contrato`** + especificação + exemplos (sem UI).
2. **Profissional exporta** `.rjetreino` e código de texto; `alunoRef` no cadastro do aluno (migração).
3. **HTML v2** com retorno por código (cobre iPhone imediatamente).
4. **Profissional importa resumo** (prévia, escolha do aluno, idempotência, histórico de carga).
5. **App `:aluno` Android**: receber (arquivo/código), executar com cronômetro, histórico, enviar resumo.
6. **App iOS do aluno** (fase iOS), reutilizando a especificação e os exemplos.
7. Opcionais: QR, catálogo com imagens, assinatura do treino pelo profissional.

### 9.4 Custos e riscos assumidos
- **Dois apps para publicar e manter** (conta de desenvolvedor, políticas de privacidade e de dados de saúde para cada um).
- **Compatibilidade de versões**: o app do aluno precisa tolerar `formato` maior e orientar a atualizar; o do profissional, idem para resumos.
- **Divergência Android/iOS**: mitigada com exemplos de teste ("golden files") que as duas implementações precisam passar.
- **Segurança**: integridade por checksum, sem prova de identidade; assinatura criptográfica fica como evolução.
- **Suporte**: a página HTML depende do navegador do aluno e do armazenamento local dele.

### 9.5 Pontos ainda abertos
- Nome e identidade visual do app do aluno.
- O app do aluno terá notificações de lembrete de treino (local, sem servidor)?
- Retenção: por quanto tempo o app do aluno mantém histórico antes de sugerir enviar/limpar?
