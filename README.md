# FitPersonal — Android

Versão Android do RJE Avaliações (desktop), agora chamada FitPersonal, em Kotlin + Jetpack Compose.
Especificação de origem (engenharia reversa do app desktop): [`../ENGENHARIA_REVERSA_ANDROID.md`](../ENGENHARIA_REVERSA_ANDROID.md).

## Funcionalidades
- **Alunos**: lista com busca, formulário com máscaras e validações (CPF, telefone, e-mail, CEP, data); foto de perfil (câmera/galeria) e avatar com iniciais; registro do consentimento LGPD do aluno.
- **Dashboard**: KPIs, próximos agendamentos, aniversariantes do mês (atalho "Parabenizar" via WhatsApp no dia) e **clima** (OpenWeather) pela cidade do perfil.
- **Avaliações**: formulário em 4 etapas — medidas (IMC/OMS), saúde (PA, lipídios, condições), avaliação física (dobras numéricas **Pollock 3/7** + Siri) e fotos posturais.
- **Treinos**: abas A/B/C/D, catálogo de 873 exercícios com busca, duplicar, excluir.
- **Agenda**: navegação por dia, conclusão rápida, **aviso de conflito de horário**.
- **PDFs** de avaliação, ficha de treino e agenda do dia, com compartilhamento.
- **Perfil** do profissional (marca, logo, CREF, contato) usado no cabeçalho dos PDFs.
- **Backup/restauração** `.rjebak` cifrado com senha (AES-256-GCM, PBKDF2) e snapshot de segurança; **backup automático diário** (WorkManager, ~3h, pasta à escolha via SAF, mantém 7).
- **Lembretes de agendamento** por notificação (15 min a 1 dia antes).
- **Importador** do backup `.zip` do desktop (aditivo, sem duplicar registros).
- **Segurança/LGPD**: banco SQLCipher (chave no Android Keystore), termo de privacidade com aceite versionado e **recuperação** quando a chave do Keystore é perdida (o banco ilegível é guardado de lado e o app oferece restaurar um backup ou começar do zero).
- **Ficha do aluno**: ao tocar num aluno abre um painel com cabeçalho (idade, objetivo), ligar/WhatsApp, última avaliação com variação, atalhos (avaliar, treinar, agendar) e histórico de avaliações, treinos e agenda; a edição do cadastro fica no lápis.
- **Evolução**: gráficos de peso, IMC, % de gordura, massas, cintura e RCQ ao longo das avaliações; **comparativo** entre duas avaliações com PDF para compartilhar. A avaliação ganhou cintura/quadril (RCQ, banco v4).
- **Metas e reavaliação**: meta por aluno (peso, % de gordura, massas ou cintura) com barra de progresso e aviso de meta atingida; intervalo de reavaliação (15–90 dias) com painel no início dos alunos atrasados/vencendo e atalho "Agendar" (banco v5).
- **Fotos antes/depois**: fotos posturais de duas avaliações lado a lado na tela de Evolução e incluídas no PDF do comparativo (posições presentes nas duas).
- **Recorrência e relatório**: agendamento semanal repetido (4, 8 ou 12 semanas; horários já ocupados são pulados e avisados) e **relatório mensal** em PDF (sessões por status/tipo/aluno, comparecimento, avaliações) pela Agenda.
- **PAR-Q e assinatura**: questionário de 7 perguntas na etapa Saúde da avaliação, com alerta de liberação médica quando há "sim"; assinatura do aluno desenhada na tela, anexada ao PDF (banco v6).
- **Modelos de treino**: "Salvar como modelo" no editor, biblioteca em Treinos → Modelos e aplicação a qualquer aluno (cria um treino novo).
- **Aviso de reavaliação**: notificação diária (~9h) com alunos atrasados ou vencendo (liga/desliga em Perfil).
- **Busca**: ícone no início busca alunos (nome, objetivo, telefone, e-mail) e treinos, sem acento; a lista de alunos ganhou filtros (reavaliação, aniversário no mês, sem avaliação, sem consentimento).
- **Progressão de carga**: cada mudança de carga (kg) digitada nos exercícios entra no histórico do aluno; tela com gráfico por exercício, variação e sugestão de aumento (+5%, arredondado a 0,5 kg) quando a carga está parada há 21 dias.
- **Financeiro**: mensalidade e dia de vencimento por aluno, registro/desfazer de pagamento por competência, situação (em dia, a vencer, em atraso), painel de inadimplência no início e recebimentos no relatório mensal (banco v7).
- **WhatsApp**: mensagens prontas (lembrete de agendamento, cobrança, treino atualizado) abertas no WhatsApp a partir da ficha, da agenda e do início; o app só abre a conversa, o envio é do usuário.
- **Planilhas**: Perfil → "Exportar planilhas" gera um .zip com alunos, avaliações, agenda e pagamentos em CSV (Excel pt-BR, UTF-8, `;`), sem CPF nem observações médicas.
- **Anotações por sessão**: campo de anotação em cada agendamento; as concluídas com nota aparecem no histórico da ficha (banco v8).
- **Pacotes de sessões**: pacote por aluno (4–24 sessões ou outro número); o saldo baixa a cada treino/consultoria concluído (avaliação física não consome), com aviso de renovação por WhatsApp e alerta no início.
- **Metas do mês**: meta de sessões concluídas e de receita, com progresso e comparação com o mês anterior no início.
- **Widget**: "Agenda de hoje" na tela inicial (Glance) com os pendentes do dia; com o bloqueio do app ligado mostra só a contagem. Atualiza a cada ~30 min e ao sair do app.
- **Faltas e lista de espera**: novo status "Faltou" (botão na agenda), frequência na ficha (faltas, taxa e faltas seguidas) e faltas no relatório mensal; lista de espera por dia da semana/horário com atalho para agendar (banco v9).
- **Linha do tempo de fotos**: galeria cronológica das fotos posturais por posição, com zoom e sobreposição ajustável sobre a foto mais antiga da mesma posição.
- **Testes de aptidão**: Cooper 12 min (com VO₂ máx estimado), sentar e alcançar, flexão, abdominal e prancha, com histórico e gráfico. O app registra desempenho; não classifica por faixa etária.
- **Backup na nuvem**: a pasta do backup automático pode ser do Google Drive (ou outro provedor de arquivos) pelo seletor do Android; o arquivo `.rjebak` já sai criptografado.
- **Gestos e animações**: arrastar para o lado troca de aba (esquerda = próxima), transições deslizantes entre telas e entre abas, voltar preditivo do Android 14+ ligado, itens de lista e cartões que se reorganizam com animação.
- **Perfil reorganizado**: cabeçalho com logo/iniciais, nome, marca e CREF e quatro abas (Perfil, Avisos, Segurança, Dados) com troca animada.
- **Aniversariantes**: destaque em degradê para quem faz aniversário hoje (com botão de parabéns), cartões com dia, idade e proximidade ("amanhã", "em 5 dias") e os que já passaram esmaecidos.
- **Telas principais renovadas**: Alunos, Agenda, Treinos e Avaliações com cabeçalho em degradê e pílulas de resumo, barra de acento colorida por estado nos itens (reavaliação/mensalidade, status da sessão, IMC), destaque do "próximo" agendamento, contagem de exercícios por treino e variação de peso entre avaliações.
- **Ficha, novo treino e novo agendamento renovados**: ficha com cabeçalho em degradê (avatar com anel, contato em botões redondos), atalhos coloridos e abas Resumo/Histórico/Financeiro; editor de treino com resumo ao vivo, divisões A–D coloridas e exercícios numerados; agendamento com resumo ao vivo, atalhos de data e horário, tipo em blocos com ícone e botão Salvar fixo.
- **Recibo e pré-visualização**: recibo em PDF de cada mensalidade paga (valor por extenso, não substitui nota fiscal) e pré-visualização do PDF dentro do app antes de compartilhar ou enviar pelo WhatsApp (vale para avaliação, treino, agenda, comparativo, relatório e recibo).
- **Busca ampliada**: além de alunos e treinos, busca avaliações (aluno, data, anamnese, metas…) e agendamentos (aluno, data, tipo, status, anotação).
- **Avisos**: o aviso diário também resume pacotes esgotados ou na última sessão; o widget da agenda atualiza a cada alteração de agendamento; na progressão de carga há o botão "Aplicar" que grava a sugestão nos treinos do aluno.
- **Enviar treino ao aluno** (sem o aluno instalar nada): no menu do treino, "Enviar ao aluno (página)" gera uma página HTML única que abre no navegador do celular, funciona offline e deixa o aluno marcar os exercícios feitos (ficam salvos só no aparelho dele); "Enviar como texto" monta o treino formatado para o WhatsApp; o PDF segue disponível.
- **Contrato com o aluno** (`:contrato`, `docs/contrato/`): formato `.rjetreino`/`.rjeresumo` em JSON versionado com checksum, código de texto colável e exemplos para outras plataformas. O profissional envia o treino como arquivo ou código ("Enviar para o app do aluno"), a página HTML passou a registrar sessões e devolver o resumo por código, e a ficha tem "Retornos do aluno" com importação por código/arquivo (prévia, sem duplicar, cargas vão para a progressão).
- **Um app, dois modos**: na primeira abertura a pessoa escolhe **professor** ou **aluno** (tela de escolha). Receber um `.rjetreino`/`.rjetreinopack` antes de escolher já ativa o modo aluno. A troca fica em Perfil → Dados (professor) e no ícone de trocar na tela do aluno. Os dados dos dois modos ficam separados e preservados.
- **Modo aluno** (em `app/.../aluno`; o módulo `:aluno` foi removido): recebe `.rjetreino` (abrir/compartilhar/colar código), executa com checklist, carga, cronômetro de descanso e esforço, guarda histórico no aparelho e envia o resumo por código. Referência para iOS em `docs/contrato/ios/` (não compilada).
- **Novo exercício, catálogo e nova avaliação renovados**: diálogo do exercício com faixa na cor da divisão e atalhos de séries/repetições/descanso; catálogo com atalhos por grupo muscular, cartões com músculos, categoria e equipamento, instruções expansíveis e barra de adicionar fixa; avaliação com cabeçalho de progresso (IMC e alertas ao vivo), etapas animadas e botões fixos.
- **Identidade FitPersonal**: nome, logo (abertura animada, ícone e splash) e paleta verde-limão sobre preto, claro e escuro.
- **Recorte de foto** ao escolher/tirar foto (aluno, avaliação e logo): proporções livre, 1:1, 3:4 e 4:3.
- **Modo treino** (menu do treino → "Iniciar modo treino"): marca séries, cronômetro de descanso com vibração, ajuste de carga (±1 kg, grava e alimenta a progressão) e tela sempre acesa.
- **Avisos diários (~9h)**: agenda do dia, mensalidades em atraso e aniversariantes, junto com reavaliações e pacotes.
- **Vídeos nos exercícios**: link (YouTube etc.) por exercício, que viaja no treino para o app do aluno, na página e no texto; e **vídeo próprio** gravado ou da galeria (até 5 min / 150 MB), guardado no aparelho por nome de exercício, com assistir e compartilhar. O vídeo próprio entra só no backup manual (não no automático nem na sincronização). Para o aluno: menu do treino → **Enviar treino com vídeos** gera um `.rjetreinopack` (zip com o treino e os vídeos, até 300 MB); o app do aluno abre o arquivo e mostra **Assistir vídeo do exercício** com player embutido.
- **Lembretes do aluno**: no modo aluno, sino na lista de treinos para escolher dias e horário; avisa “hoje é dia de treinar” (com a próxima divisão) e, depois de 3 dias sem treinar, “sentimos sua falta”.
- **Aviso de vencimento**: o resumo diário das 9h agora também avisa mensalidades que vencem em até 3 dias, para enviar o Pix antes do atraso.
- **Comparador antes/depois**: em Evolução, “Comparar deslizando” sobrepõe as duas fotos da mesma posição com um marcador arrastável.
- **Imagens no catálogo**: ao expandir um exercício do catálogo aparecem as fotos de demonstração (free-exercise-db, domínio público), baixadas sob demanda e guardadas em cache.
- **Sincronização entre aparelhos**: Perfil → Dados → “Sincronizar agora” usa a pasta de backup (Drive etc.): recebe, envia ou pede para escolher em caso de conflito. É “último a enviar vence” (não mescla registro a registro) e exige a mesma senha de backup nos dois aparelhos.
- **iPhone (modo aluno)**: projeto SwiftUI em `ios/FitPersonal` (XcodeGen), com Liquid Glass no iOS 26. **Não foi compilado** — veja `ios/FitPersonal/README.md`.
- **Proteção**: bloqueio ao abrir com biometria/PIN/padrão do aparelho (trava de novo após 30 s fora do app), opção de impedir capturas de tela, **exportação** de todos os dados de um aluno (JSON) e **exclusão definitiva** (registros, fotos, lembretes e PDFs em cache).
- **Acabamento**: ícone adaptativo (com versão monocromática), splash, tema claro/escuro/automático e cartão de "primeiros passos" no início.

## Design
- **Tema** em `ui/theme/Theme.kt`: paleta verde-limão/preto da logo FitPersonal (nomes Teal*/Cyan* são históricos) + índigo/âmbar de apoio, claro e escuro, tipografia com hierarquia mais marcada e cantos de 12–32 dp. Para trocar a identidade visual, mude só `Paleta` e os esquemas de cor desse arquivo (cores dinâmicas do Android 12+ ficam desligadas por padrão).
- **Componentes** em `ui/common/` (`Componentes.kt`, `Agenda.kt`, `SeletorAluno.kt`): `CartaoSecao`, `CartaoItem`, `IndicadorKpi`, `EstadoVazio`, `CampoBusca`, `ChipStatus`, `IndicadorEtapas`, `BlocoHorario`.
- **Telas**: Início com saudação, ações rápidas, KPIs coloridos, agenda em cartões e aniversariantes em carrossel; Agenda com faixa semanal; listas com estados vazios; formulários (aluno, agendamento, treino, perfil) agrupados em cartões; avaliação com indicador de etapas; termo LGPD e tela de recuperação com o mesmo visual.

## Estrutura (`app/src/main/java/br/com/fitpersonal/app`)
| Pasta | Conteúdo |
|---|---|
| `domain/` | Modelos e regras puras, sem Android (IMC, PA, Pollock, conflitos de agenda, validadores, termo LGPD) |
| `data/local` | Room v9 + SQLCipher, DAOs, mappers, migrações 1→…→9, seeder do catálogo, recuperação de banco |
| `data/privacidade` | Exportação e exclusão definitiva dos dados de um aluno (LGPD) |
| `data/repo` | Repositórios (alunos, agenda, avaliações, treinos, catálogo, perfil, configurações, clima) |
| `data/crypto` | Cofre de segredos (Keystore) e cifra de arquivos de backup |
| `data/backup` | Backup/restauração, backup automático, importador do desktop |
| `data/files`, `data/remote` | Armazenamento de imagens; cliente do OpenWeather |
| `report/` | Gerador de PDF (`PdfDocument`) |
| `work/` | WorkManager: backup noturno, lembretes, notificações |
| `ui/` | Compose + Navigation: home, alunos, ficha, evolucao, agenda, treinos, avaliações, perfil, lgpd, protecao, recuperacao, common |
| `di/` | Módulos Hilt |

Testes: `src/test` (JVM: regras de domínio, cifra, parser do clima, política de recuperação) e `src/androidTest` (aparelho: migração SQLCipher, recuperação de chave, backup/restauração).

## Como rodar
1. Abra a pasta `rje-android` no **Android Studio** (Ladybug ou mais novo), com **JDK 17**. O projeto traz `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9); o IDE baixa a distribuição na primeira sincronização.
2. *(Opcional)* **Clima**: copie `local.properties.sample` para `local.properties` e preencha `OPENWEATHER_API_KEY`. Sem chave o cartão de clima simplesmente não aparece.
3. **Sync** → **Run** em um aparelho/emulador API 26+.
4. Linha de comando (depois de gerar o wrapper uma vez com `gradle wrapper --gradle-version 8.9`):
   ```bash
   ./gradlew :app:testDebugUnitTest          # testes de domínio/JVM
   ./gradlew :app:assembleDebug              # APK de debug
   ./gradlew :app:connectedDebugAndroidTest  # testes em aparelho (usam pasta temporária e alias próprio do Keystore)
   ```

## Publicar (release)
1. Gere uma chave de assinatura (guarde o `.jks` e as senhas fora do repositório — perder a chave impede atualizar o app na loja):
   ```bash
   keytool -genkeypair -v -keystore rje-release.jks -alias rje -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copie `keystore.properties.sample` para `keystore.properties` (já no `.gitignore`) e preencha. Alternativa: variáveis de ambiente `RJE_STORE_FILE`, `RJE_STORE_PASSWORD`, `RJE_KEY_ALIAS`, `RJE_KEY_PASSWORD`.
3. Versão: `-PversionCode=2 -PversionName=1.0.0` (o `versionCode` precisa subir a cada envio).
4. Gere o pacote da loja ou o APK:
   ```bash
   ./gradlew :app:bundleRelease     # app/build/outputs/bundle/release/*.aab
   ./gradlew :app:assembleRelease   # APK assinado, se houver keystore.properties
   ```
   Sem os dados de assinatura o release sai **não assinado**. O R8 (minify + shrink de recursos) está ligado: teste o release em aparelho antes de publicar.
5. Antes de enviar: revisar o termo LGPD, política de privacidade da loja (dados de saúde, câmera, notificações, internet só para o clima) e a chave do OpenWeather.

## Premissas
- Um profissional por instalação; sem login nem sincronização.
- Dados 100% locais. Rede só para: clima (OpenWeather: nome da cidade e chave), imagens do catálogo de exercícios (baixadas de raw.githubusercontent.com) e, **se o usuário aceitar**, estatísticas de uso anônimas (Firebase). `allowBackup=false`.

## Estado de verificação — leia antes de publicar
O projeto é compilado e testado no Android Studio pela equipe; muitas etapas recentes foram escritas por revisão estática, então rode **Clean/Rebuild, os testes unitários e um teste completo em aparelho** (tipo de build `homologacao`, que liga o R8 como no release) antes de publicar. Veja docs/PUBLICACAO.md. Pontos de maior risco:
- API do SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`): `openDatabase`, `rawExecSQL`, `rawQuery`, `SupportOpenHelperFactory` (usadas em `BancoCriptografado.kt` e `AppModule.kt`).
- Assinaturas do `androidx.sqlite` nos testes instrumentados (`SupportSQLiteOpenHelper.Callback`).
- Layout dos PDFs, restauração de backup, importador do desktop, câmera/galeria e lembretes em Doze nunca rodaram em aparelho.
- Fórmulas de Pollock/Siri e textos de saúde: validar com profissional da área.

## Pendências conhecidas
- Fotos (posturais e de perfil) ficam no armazenamento privado do app, mas não são criptografadas pelo app.
- A senha do backup não tem recuperação (por desenho).
- O termo de privacidade (`domain/rules/TermoLgpd.kt`) é um texto-modelo: revisar com assessoria jurídica antes de distribuir.
- A chave do OpenWeather vai embutida no APK (use uma chave dedicada e de baixo limite).
- Testes instrumentados para PDF, importador do desktop, WorkManager e telas Compose.
- O ícone atual (haltere sobre fundo teal) é provisório: troque `res/drawable/ic_launcher_foreground.xml` pela arte final.
- O bloqueio usa a credencial do aparelho (`KeyguardManager`): se o aparelho não tem bloqueio de tela, o recurso fica indisponível. Exportações e backups compartilhados saem do app e deixam de ser protegidos por ele.
- Exclusão definitiva não altera backups `.rjebak` já gerados nem cópias que o usuário compartilhou.
- Telas novas (ficha, evolução, metas, recorrência) e as migrações 3→4 e 4→5 ainda não foram compiladas/testadas em aparelho; `schemas/…/4.json` e `5.json` são gerados no primeiro build.
- O PAR-Q é triagem de apoio (texto padrão, não diagnóstico); a assinatura é uma imagem, sem valor de assinatura digital qualificada. Modelos de treino não guardam histórico de progressão de carga. A busca é em memória (adequada para centenas de alunos).
- Backup na nuvem depende do app do provedor (ex.: Google Drive) estar instalado e conectado; se a pasta ficar indisponível o backup cai no armazenamento interno e o status mostra o aviso.
- Migração 8→9 cria `lista_espera` e `testes_aptidao` manualmente; confira `schemas/…/9.json` no primeiro build.
- O widget usa `androidx.glance:glance-appwidget:1.1.1` (nova dependência): confirme no primeiro sync. Ele não atualiza em tempo real a cada mudança na agenda; use o botão de atualizar do launcher ou reabra/saia do app.
- Pacote: a contagem usa a data de início do pacote (renovar zera o consumo); metas do mês contam todos os concluídos, inclusive avaliações.
- Financeiro é controle simples (sem integração bancária, Pix ou recibo fiscal); o histórico de carga só lê o primeiro número do campo "carga" (ex.: "20 kg"). A planilha CSV sai sem criptografia: o app avisa antes de gerar.
- Migrações 5→6 e 6→7 criam tabelas manualmente: confira o `schemas/…/6.json` gerado contra a migração se o Room reclamar de esquema. O relatório mensal considera "comparecimento" = concluídos ÷ (concluídos + cancelados); pendentes não entram na taxa.
