# Desenho: multi-profissional e sincronização

Status: **proposta, nada implementado.** Serve para decidir escopo antes de mexer no banco.

## 1. Ponto de partida (como o app é hoje)

| Peça | Hoje |
|---|---|
| Banco | 1 arquivo Room + SQLCipher (`rje_avaliacoes.db`), chave aleatória guardada no Keystore (alias fixo) |
| Ids | `Long` autoincrement **por aparelho** |
| Arquivos | fotos/logo/assinaturas em `filesDir/fotos/{idAluno}/...` e `branding/` |
| Config | 1 DataStore (`configuracoes`) |
| Segredos | senha do backup no cofre do Keystore |
| Backup | `.rjebak` cifrado com senha (zip: banco + imagens + perfil) |
| Injeção | `FitDatabase`, DAOs e repositórios são `@Singleton` do Hilt |
| Segundo plano | WorkManager (backup noturno, lembretes, avisos), widget Glance |

Três consequências importantes:
1. Os ids não são globais: dois aparelhos criam "aluno 1" diferentes. Sincronizar exige ids globais (UUID).
2. A chave do banco é presa ao aparelho (Keystore). Outro aparelho só abre dados com uma chave derivada de senha.
3. Singletons do Hilt pressupõem **um** banco por processo.

## 2. Cenários (precisam ser separados)

| | Cenário | Exemplo |
|---|---|---|
| A | Vários profissionais no **mesmo aparelho**, dados separados | tablet do estúdio, cada professor com seus alunos |
| B | **Mesmo profissional em 2 aparelhos** | celular + tablet |
| C | **Equipe compartilhando** alunos e agenda | estúdio com recepção e vários professores |

A é local. B pode ser feito sem servidor. C exige servidor, contas e permissões — é outro produto.

## 3. Multi-perfil local (cenário A)

### Opções
| | Banco por perfil | Coluna `idProfissional` num banco só |
|---|---|---|
| Isolamento (LGPD) | forte: arquivo, chave e pastas próprios | fraco: tudo sob uma chave |
| Mudança no código | parametrizar caminhos/alias por perfil ativo | filtro em **toda** query, DAO e migração |
| Troca de perfil | reinicia o processo (ou recria o grafo Hilt) | instantânea |
| Backup/restauração | natural: 1 backup por perfil | restaurar mistura perfis |
| Risco de vazamento entre perfis | baixo | alto (um `WHERE` esquecido) |

**Recomendação: banco por perfil.**

### Como ficaria
- `PerfisLocais` (SharedPreferences simples, **sem dado de saúde**): lista `{id, nomeExibicao, cor}` e `perfilAtivoId`.
- Tudo que hoje é fixo passa a depender do `perfilAtivoId` lido na criação do processo:
  - banco `databases/rje_{id}.db` e alias do Keystore `rje_db_{id}`;
  - imagens em `files/perfis/{id}/...`;
  - DataStore `configuracoes_{id}`;
  - segredo da senha de backup por perfil.
- O perfil existente vira o perfil `padrao` **sem mover dados** (mantém os nomes atuais).
- Troca de perfil: gravar o novo id e reiniciar a Activity/processo (o Hilt reconstrói os singletons). Tela inicial opcional "Quem está usando?".
- Bloqueio do app (biometria/PIN) continua do aparelho; opcionalmente exigir de novo ao trocar de perfil.
- Segundo plano: o backup noturno e os avisos rodam **só para o perfil ativo** (único banco aberto no processo). Alternativa futura: um worker que abre cada perfil em sequência.
- Widget: mostra o perfil ativo (e o nome dele); com bloqueio ligado continua só com contagens.

### Riscos / decisões
- Perfis inativos não entram no backup automático (precisa ficar claro na UI).
- Excluir perfil = apagar banco, chave, pastas e config (confirmação forte; gerar backup antes).
- Notificações de perfis inativos não disparam (limitação do banco único aberto).

## 4. Sincronização

### Opções
| | Como | Prós | Contras |
|---|---|---|---|
| S1 | Backup/restauração manual (já existe) | zero infra, já cifrado | última escrita vence; manual |
| S2 | **Pasta compartilhada** (Drive/Nextcloud via SAF) com arquivos de mudança cifrados | sem servidor próprio; você controla onde estão os dados | exige ids globais, merge e controle de versão |
| S3 | Backend (Firebase/Supabase/próprio) | tempo quase real, contas, permissões | custo, operador de dados sensíveis (LGPD), contas, suporte |
| S4 | Transferência direta (Nearby/QR) | sem nuvem | só pontual, não é sincronização contínua |

**Recomendação em fases:**

**Fase 0 — já possível:** "Mover para outro aparelho" guiado: backup cifrado na pasta do Drive + restaurar no outro. Falta só uma tela/roteiro.

**Fase 1 — sincronização B por pasta (S2):**
- Schema v10: em cada tabela sincronizável, `uuid` (global), `atualizadoEm` (relógio híbrido), `excluido` (tombstone) e `versaoSchema`.
- Cada aparelho tem um `deviceId` e grava arquivos de mudança `{deviceId}-{seq}.chg` (JSON compactado, **cifrado com chave derivada da senha do backup**) na pasta escolhida.
- Importar = aplicar mudanças novas de outros aparelhos; conflito por registro: maior `atualizadoEm`, desempate por `deviceId`.
- Imagens: arquivos nomeados por `uuid`, enviados à parte; tolerar chegada tardia (placeholder).
- Agenda: conflito de horário vira **aviso** (já existe), nunca descarte silencioso.
- Rodar por WorkManager (periódico + ao abrir/sair do app); sempre idempotente.

**Fase 2 — equipe (cenário C):** só se houver demanda real. Exige backend, autenticação, papéis (dono/professor/recepção), auditoria de acesso e contrato de operador de dados.

## 5. Pontos de atenção (LGPD e engenharia)
- Dados de saúde são **sensíveis** (LGPD art. 11): sincronização precisa de cifra ponta a ponta; o provedor da nuvem não pode ler.
- Aparelho novo = novo consentimento/registro de acesso; aparelho perdido: revogar o `deviceId` e trocar a senha de sincronização.
- Relógios errados quebram "última escrita vence": usar relógio híbrido (lógico + físico).
- Versões diferentes do app: recusar mudanças de `versaoSchema` maior e orientar a atualizar.
- Exclusão definitiva (LGPD) precisa propagar por tombstone e **limpar também os arquivos de mudança antigos**.
- Testes: simulação de 2 aparelhos com edições concorrentes, exclusão + edição, imagens atrasadas.

## 6. Ordem sugerida
1. Fase 0 (roteiro "mover de aparelho"): pequeno.
2. Multi-perfil local (banco por perfil): médio, **independente** da sincronização.
3. Ids globais + schema v10 (base da fase 1): grande, mas só mexe em dados, não em UI.
4. Sincronização por pasta (fase 1): grande.
5. Equipe (fase 2): decidir só depois de validar 2–4.

## 7. Perguntas em aberto
- Qual cenário é o real: A, B ou C?
- Aceitam depender de uma pasta em nuvem (Drive etc.) ou querem servidor próprio?
- Perfis inativos precisam de backup/aviso em segundo plano?
- Vale proteger a troca de perfil com PIN/biometria?
