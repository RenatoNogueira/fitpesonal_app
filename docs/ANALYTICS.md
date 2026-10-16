# Estatísticas de uso (Firebase Analytics)

Objetivo: entender **quais telas as pessoas usam** e **em qual momento desistem de uma tarefa**, sem coletar nenhum dado de aluno.

## Como funciona

- **Opt-in:** a coleta nasce **desligada** (manifesto) e só liga se o usuário aceitar na pergunta inicial ou em
  *Perfil → Segurança e privacidade → Ajudar a melhorar o app*. A escolha pode ser desfeita a qualquer momento.
- **Inerte sem configuração:** sem `app/google-services.json` o app compila e roda normalmente; o Firebase não
  inicializa, a pergunta não aparece e o interruptor do Perfil fica oculto.
- **Sem dados pessoais:** só nomes fixos de telas e de ações. Nunca nomes, textos digitados, ids, medidas, fotos,
  mensagens de erro ou o identificador de publicidade (coleta de AD ID/SSAID e sinais de anúncios desligados no manifesto).
- Código: `core/Analitica.kt` (envio), `domain/rules/AnaliticaRules.kt` (nomes seguros, testado),
  `ui/lgpd/PerguntaAnalitica.kt` (pergunta única), chave em `ConfiguracoesRepository` (`analiticaAtiva`).
- O termo de privacidade (`TermoLgpd`, versão 3) descreve a coleta; quem já tinha aceitado a versão anterior vê o termo de novo.

## Dicionário de eventos

| Evento | Quando | Parâmetros |
|---|---|---|
| `screen_view` | Uma tela abre (profissional e aluno) | `screen_name` (ex.: `alunos`, `aluno`, `avaliacao`, `aluno_treino`) |
| `acao_concluida` | Salvou, editou ou excluiu algo (mesmos avisos que o usuário vê) | `acao` (ex.: `aluno_criado`, `treino_excluido`) |
| `tela_abandonada` | Saiu de um formulário (aluno, agendamento, avaliação, treino) sem concluir nada | `tela`, `segundos` (tempo na tela, até 3600) |

Perguntas que isso responde: *qual tela é mais aberta*, *em qual formulário mais gente desiste* (e depois de quanto tempo),
*quais ações são mais concluídas*. O funil "abriu formulário → concluiu" sai de `screen_view` × `acao_concluida`.

## Ativar no seu projeto (uma vez)

1. Acesse o [Console do Firebase](https://console.firebase.google.com) → **Adicionar projeto** (pode desativar o
   Google Analytics para anúncios; para este app basta o Analytics padrão).
2. Dentro do projeto: **Adicionar app → Android**, nome do pacote **`br.com.fitpersonal.app`** (os tipos de build
   debug, homologação e release usam o mesmo pacote).
3. Baixe o **`google-services.json`** e coloque em **`app/google-services.json`**. O arquivo está no `.gitignore`:
   cada ambiente baixa o seu.
4. No Android Studio: **Sync Project with Gradle Files** e rode o app. Aceite a pergunta de estatísticas.
5. Para ver os eventos na hora: no Console, **Analytics → DebugView**, e no aparelho:
   `adb shell setprop debug.firebase.analytics.app br.com.fitpersonal.app`.
   Os relatórios normais levam algumas horas para aparecer.

## Antes de publicar

- Informe o uso do Firebase Analytics na **política de privacidade** e no formulário **Segurança dos dados** da Play Store
  (dados de "interação com o app", não vinculados à identidade, coleta opcional).
- Revise o texto do termo com assessoria jurídica (LGPD).
- O modo aluno só pergunta uma vez; quem quiser desligar depois pode alternar para o modo professor e usar o Perfil.
