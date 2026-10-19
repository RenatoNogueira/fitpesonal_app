# Roteiro de publicação — FitPersonal 1.0.0

Marque cada item ao concluir. O que está **em negrito** bloqueia a publicação.

## 0. Antes de tudo: o build novo

A preparação mudou a cadeia de build para atender à Play Store. **Faça isto primeiro** e me avise se aparecer erro:

1. No Android Studio: **File → Sync Project with Gradle Files** (vai baixar o Gradle 8.11.1 e o plugin 8.9.3).
   - Precisa do Android Studio **Meerkat 2024.3.1 Patch 1 ou mais novo** e do **JDK 21** (como antes).
   - Se o Studio pedir para atualizar o plugin ou o Gradle, aceite só as versões acima.
2. **Build → Clean Project** e depois **Rebuild Project**.
3. Rode os testes: **Run → Edit Configurations → JUnit** ou `.\gradlew.bat testDebugUnitTest`.
4. Rode o app e passe o checklist da seção 4.

O que mudou e por quê:

| Mudança | Motivo |
|---|---|
| `versionName 1.0.0`, `versionCode 10` | primeira versão pública; o `versionCode` precisa subir a cada envio |
| `compileSdk`/`targetSdk` **36** (Android 16) | desde 31/08/2026 a Play Store só aceita apps novos e atualizações com API 36 |
| Plugin Android **8.9.3** e Gradle **8.11.1** | mínimo exigido para compilar com a API 36 |
| SQLCipher **4.6.1** | a partir da 4.6.1 as bibliotecas nativas suportam páginas de memória de 16 KB, exigidas pela loja |
| Regras do R8 (`proguard-rules.pro`) | stack traces legíveis no Play Console (com o `mapping.txt`) |

Efeitos do `targetSdk 36` já considerados: o app já usa borda a borda (`enableEdgeToEdge`) e voltar preditivo
(`enableOnBackInvokedCallback`); o travamento em retrato só vale em celulares, e telas grandes giram livremente.

## 1. Assinatura de release

1. Rode **uma vez**: `powershell -ExecutionPolicy Bypass -File scripts\gerar-keystore.ps1`
   - Cria `fitpersonal-release.jks` e `keystore.properties` (senha aleatória, nada aparece em tela). Os dois ficam fora do Git.
2. **Faça cópia dos dois arquivos** em local seguro, fora deste computador. Sem eles não dá para atualizar o app.
3. No Play Console, ative o **Play App Signing** (recomendado): esta chave vira só a *chave de envio*; o Google guarda a definitiva.
4. Gere o pacote: `.\gradlew.bat :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
   Sem o `keystore.properties`, o release sai **sem assinatura** (não serve para a loja).

## 2. Chave do clima

A chave do OpenWeather fica em `local.properties` e **vai embutida no app** (pode ser extraída do APK). Antes de publicar:
crie uma chave **exclusiva do FitPersonal**, com limite de uso baixo, e troque a que foi compartilhada em conversas.
(Ela já foi colada numa conversa: considere-a exposta.)

## 3. Política de privacidade

- Arquivos: `docs/politica-de-privacidade.md` e `docs/politica-de-privacidade.html`.
- **Preencha os campos entre colchetes** (nome/razão social, CPF/CNPJ, e-mail de contato, cidade) e peça **revisão jurídica**.
- A Play Store exige **uma URL pública**. Opção gratuita: GitHub Pages — no repositório, *Settings → Pages → Branch `trabalho`, pasta `/docs`*.
  A URL fica algo como `https://renatonogueira.github.io/fitpesonal_app/politica-de-privacidade.html`.
- Cole a URL no Play Console (*Política do app → Política de Privacidade*).

## 4. Verificação com a minificação (R8) ligada

O tipo de build **`homologacao`** é igual ao release (R8 e redução de recursos ligados), assinado com a chave de debug e com os dados de
teste. É o que deve ser testado em aparelho **antes** de enviar:

```
.\gradlew.bat :app:assembleHomologacao
adb install -r app\build\outputs\apk\homologacao\app-homologacao.apk
```

Rode este roteiro no aparelho (marque tudo; qualquer falha que só acontece aqui costuma ser regra do R8):

- [ ] Primeira abertura: escolha de modo, termo de privacidade, pergunta de estatísticas.
- [ ] Carregar dados de teste (Perfil → Dados) e navegar pelas 5 abas.
- [ ] Criar/editar/excluir aluno, avaliação (com foto), treino e agendamento; ver os avisos de "salvo".
- [ ] Avaliação: etapa **Avaliação física** com as dobras preenchidas (resultado do Pollock).
- [ ] Gerar PDF de avaliação, ficha e comparativo; abrir a pré-visualização e compartilhar.
- [ ] **Backup manual** com senha e **restaurar** em seguida (testa o SQLCipher com o R8).
- [ ] Sincronização por pasta (enviar e receber).
- [ ] Cobrança com Pix e QR Code.
- [ ] Treino com vídeo próprio e **envio ao modo aluno** (arquivo, código e pacote com vídeos); abrir no modo aluno.
- [ ] Modo aluno: executar treino, ver a animação do movimento, hábitos, check-in, diário de refeições com foto.
- [ ] Widget da agenda na tela inicial.
- [ ] Notificações e lembretes (conceda a permissão).
- [ ] Clima no Início; imagens do catálogo com internet e sem internet.
- [ ] Girar o aparelho (celular fica em pé) e, se tiver, testar um tablet.
- [ ] Perfil → Dados → **Relatório de falhas**: deve estar sem falhas fatais.

Se algo falhar só no `homologacao`, me mande o texto do **Relatório de falhas** (já vem legível porque o R8 mantém arquivo e linha).
Para ler stack traces de usuários no Play Console, envie o `mapping.txt` de cada versão
(`app/build/outputs/mapping/release/mapping.txt`; o Play Console pede junto do `.aab`).

## 5. Ficha da loja

**Nome do app (até 30 caracteres):** FitPersonal

**Descrição curta (até 80):** Avaliações físicas, treinos e agenda para personal trainers, e treinos para o aluno.

**Descrição completa (sugestão):**

> O FitPersonal organiza o dia a dia do personal trainer e do aluno, **sem conta e sem enviar dados dos seus alunos para servidores**.
>
> **Para o profissional**
> • Cadastro de alunos com consentimento LGPD
> • Avaliação física completa: medidas, dobras cutâneas (Pollock), PAR-Q, fotos, assinatura e painel de composição corporal
> • Treinos A/B/C/D com catálogo de exercícios, fotos do movimento, vídeos, bi-set, circuito, AMRAP e EMOM
> • Agenda com lembretes, lista de espera e widget
> • Financeiro com cobrança por Pix e QR Code
> • Relatórios em PDF e comparativo antes/depois
> • Backup criptografado e sincronização por pasta
>
> **Para o aluno**
> • Receba o treino por arquivo ou código, execute com cronômetro de descanso e acompanhe seu histórico
> • Hábitos, check-in semanal e diário de refeições
>
> **Privacidade:** o banco de dados é criptografado no aparelho. Estatísticas de uso anônimas são opcionais.
>
> As orientações do aplicativo não substituem a avaliação de um profissional de saúde.

**Categoria:** Saúde e fitness · **Classificação de conteúdo:** responda o questionário (sem violência, sem compras, sem anúncios hoje).
**Material gráfico:** ícone 512×512 (já existe), imagem de destaque 1024×500 e pelo menos 2 capturas de tela de celular (use os dados de teste).

## 6. Formulário "Segurança dos dados" (Play Console)

Respostas coerentes com o app hoje:

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha dados do usuário? | **Sim**, apenas se o usuário aceitar as estatísticas de uso (opcional) |
| Dados coletados | **Interação com o app** (telas e ações) e **identificadores de dispositivo ou outros IDs do Firebase** — coleta **opcional**, finalidade **análise** |
| Dados compartilhados com terceiros | Nenhum dado de alunos. Dados de análise vão ao Google (Firebase) |
| Dados de saúde, fotos, contatos, financeiros | **Não coletados pelo desenvolvedor**: ficam só no aparelho |
| Criptografados em trânsito? | Sim (HTTPS) |
| O usuário pode pedir exclusão? | Sim — os dados estão no aparelho; ele exclui no app ou desinstala. Estatísticas: desligar em Perfil |

Se o app passar a ter **anúncios (AdMob)** ou **assinatura**, este formulário e a política precisam ser atualizados **antes** da versão com anúncios
(ver a anotação de monetização).

## 7. Antes de enviar

- [ ] Sync/Rebuild sem erros; testes unitários verdes.
- [ ] Roteiro da seção 4 completo no `homologacao`.
- [ ] Chave do clima nova e exclusiva; `local.properties` no lugar.
- [ ] `google-services.json` em `app/` **se** quiser as estatísticas (veja `docs/ANALYTICS.md`); sem ele o app roda normalmente.
- [ ] Política de privacidade publicada e preenchida; termo do app revisado (`TermoLgpd`).
- [ ] **Imagens do catálogo:** o repositório free-exercise-db se declara de domínio público, mas a licença cobre o "software" e não cita as
      imagens. Se quiser segurança total, peça confirmação ao autor ou substitua por fotos próprias.
- [ ] Faixa de **teste interno** primeiro (até 100 testadores, sem revisão longa), depois **produção**.
- [ ] Guarde cópia do `.jks`, do `keystore.properties` e do `mapping.txt` de cada versão.

## 8. Depois de publicar

- Aumente o `versionCode` em `gradle.properties` a cada envio (e o `versionName` quando fizer sentido).
- Marque a versão no Git: `git tag v1.0.0 && git push origin v1.0.0`.
- Acompanhe *Android vitals* (falhas e travamentos) no Play Console.
