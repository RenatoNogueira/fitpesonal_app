# Política de Privacidade — FitPersonal

**Vigência:** a partir de 19/10/2026 · **Versão do documento:** 1.0

> **Modelo para revisão.** Este texto descreve fielmente o funcionamento atual do aplicativo, mas é um modelo.
> Antes de publicar, preencha os campos entre colchetes e peça a revisão de uma assessoria jurídica (LGPD — Lei 13.709/2018).

## 1. Quem somos

O **FitPersonal** é um aplicativo para profissionais de educação física organizarem alunos, avaliações físicas, treinos e agenda,
e para alunos receberem e executarem os treinos enviados pelo profissional.

- **Desenvolvedor:** [nome do desenvolvedor ou razão social] — CPF/CNPJ [número]
- **Contato para assuntos de privacidade:** [e-mail de contato]

## 2. O papel de cada pessoa (LGPD)

- **O profissional que usa o app** é o **controlador** dos dados dos seus alunos: ele decide quais dados coleta e para quê.
  O aplicativo é a ferramenta que os guarda **no aparelho dele**.
- **O desenvolvedor do FitPersonal não recebe, não armazena e não tem acesso** aos dados de alunos, avaliações, treinos,
  fotos ou mensagens. Não existe conta de usuário nem servidor do FitPersonal com esses dados.
- O **aluno** que usa o modo aluno guarda no próprio aparelho o treino recebido e o registro das suas sessões.

## 3. Quais dados são tratados no aparelho

| Tipo | Exemplos | Para quê |
|---|---|---|
| Identificação e contato do aluno | nome, nascimento, CPF, CEP, telefone, e-mail, Instagram | cadastro e contato |
| Dados de saúde (sensíveis) | medidas corporais, % de gordura, pressão arterial, exames, histórico de saúde, anamnese, PAR-Q, avaliação postural, fotos | avaliação física e prescrição |
| Treino e agenda | exercícios, cargas, sessões, agendamentos, lembretes | acompanhamento |
| Financeiro | mensalidade, dia de vencimento, pagamentos registrados | cobrança e controle |
| Dados do profissional | nome, marca, CREF, cidade, chave Pix, logotipo | relatórios, mensagens e cobranças |
| Modo aluno | hábitos diários, check-in semanal, diário de refeições (texto e fotos) | acompanhamento pessoal |

Tudo isso fica **somente no armazenamento privado do aplicativo**, no aparelho do usuário.

## 4. Como os dados são protegidos

- O banco de dados é **criptografado** (SQLCipher, AES-256), com chave guardada no cofre do Android (Keystore).
- Os **backups** são sempre criptografados com uma senha definida pelo profissional. **A senha não pode ser recuperada.**
- O app oferece bloqueio por biometria/PIN do aparelho e a opção de impedir capturas de tela.
- O backup automático do Android em nuvem está **desativado** para este app (`allowBackup=false`).
- As **fotos** ficam no armazenamento privado do app, protegido pelo sistema, mas não são criptografadas individualmente pelo aplicativo.

## 5. Permissões do aplicativo

| Permissão | Por quê |
|---|---|
| Notificações | lembretes de atendimento, aviso de reavaliação e de vencimento, lembrete de treino do aluno |
| Internet | clima, imagens do catálogo de exercícios e, se você aceitar, estatísticas de uso (seção 6) |
| Câmera | **não pedimos a permissão**: as fotos são feitas pelo app de câmera do próprio sistema |
| Fotos e arquivos | só os arquivos que você escolhe pelo seletor do sistema |

## 6. Serviços de terceiros e comunicação de rede

1. **OpenWeather (clima).** Envia apenas o **nome da cidade** informada no perfil do profissional (e a chave de acesso do app).
   Nenhum dado de aluno. Política: openweathermap.org/privacy-policy.
2. **Imagens do catálogo de exercícios.** Ao ver um exercício do catálogo, o app baixa fotos de raw.githubusercontent.com (GitHub).
   O GitHub pode registrar o endereço IP da requisição, como em qualquer download. Nenhum dado pessoal é enviado.
3. **Estatísticas de uso (Firebase Analytics, do Google) — opcional.** Só são coletadas se você **aceitar** na pergunta inicial ou
   em *Perfil → Segurança e privacidade*, e podem ser desligadas a qualquer momento. Coletamos apenas **quais telas foram abertas,
   quais ações foram concluídas** (por exemplo, "aluno criado") **e em qual tela alguém desistiu de uma tarefa**.
   **Nunca** enviamos nomes, textos digitados, medidas, fotos, mensagens, identificadores de alunos nem o identificador de publicidade
   do aparelho. Política do Google: policies.google.com/privacy.
4. **Compartilhamentos feitos por você.** PDFs, mensagens de WhatsApp, arquivos de treino, backups e sincronização por pasta só saem do
   aparelho por uma ação sua, para o destino que você escolher (WhatsApp, e-mail, pasta na nuvem etc.). A partir daí, o tratamento
   passa a ser do serviço escolhido e de quem recebe.

[Se o aplicativo passar a exibir anúncios ou a ter assinatura, esta seção será atualizada **antes** disso, com o aviso e o pedido de
consentimento exigidos.]

## 7. Base legal e consentimento

- Para **dados de saúde**, a base usual é o **consentimento específico e destacado do aluno** (LGPD, art. 11, I). O app permite registrar,
  no cadastro de cada aluno, que o consentimento foi obtido e a data. **Obter e guardar esse consentimento é responsabilidade do profissional.**
- Para as **estatísticas de uso**, a base é o seu consentimento (opcional e revogável).
- Para a **consulta de clima**, o legítimo interesse de oferecer a função (apenas nome de cidade).

## 8. Por quanto tempo guardamos

Os dados ficam no aparelho **até o profissional excluí-los** ou desinstalar o app. Ao excluir um aluno, o app remove avaliações,
treinos, agenda, pagamentos, fotos e lembretes dele. Cópias já exportadas (PDFs, backups, arquivos enviados) não são apagadas pelo app.
Desinstalar o aplicativo apaga os dados do aparelho; backups feitos antes continuam existindo onde foram guardados.

## 9. Direitos do titular

O titular dos dados (por exemplo, o aluno) pode pedir **confirmação do tratamento, acesso, correção, anonimização, portabilidade,
eliminação e informação sobre compartilhamentos** (LGPD, art. 18). Como os dados do aluno estão com o **profissional**, o pedido
deve ser feito a ele. O profissional corrige os dados no cadastro, exporta os dados do aluno e os exclui pelo próprio aplicativo.
Para dúvidas sobre o aplicativo em si, escreva para [e-mail de contato].

## 10. Crianças e adolescentes

O app é destinado a profissionais e a seus alunos. Dados de menores devem ser tratados com consentimento de um dos pais ou responsável,
obtido pelo profissional.

## 11. Incidentes de segurança

Em caso de perda ou roubo do aparelho, ou acesso indevido aos dados, o profissional deve avaliar a comunicação aos alunos afetados e à
ANPD (LGPD, art. 48). Como o desenvolvedor não tem acesso aos dados, não consegue detectar nem comunicar esses incidentes pelo profissional.

## 12. Mudanças nesta política

Quando algo relevante mudar (por exemplo, um novo serviço de terceiros), esta política e o termo exibido no app serão atualizados, e o
app pedirá um novo aceite. A data de vigência no topo indica a versão atual.

## 13. Contato

[e-mail de contato] · [cidade/UF]
