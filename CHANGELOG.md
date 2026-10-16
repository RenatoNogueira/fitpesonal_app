# Histórico de versões

Formato: [versão] — data — resumo. A versão do app fica em `gradle.properties` (`versionName` / `versionCode`).

## [Não lançado]
- Arquivos próprios: .fittreino, .fittreinopack e .fitbak (os antigos .rjetreino, .rjetreinopack e .rjebak continuam abrindo).
- Painel de composição corporal (anel gordura x massa magra, variação e tendência) na avaliação e no treino.
- Metas de composição por aluno, com progresso no painel (banco v12).
- Aluno: progressão automática de carga (sugestão), hábitos diários, check-in semanal (segue no resumo) e diário de refeições com fotos.
- Treinos avançados: bi-set, circuito, AMRAP, EMOM e drop-set (banco v13; campo opcional "metodo" no contrato, gravado só quando existe).
- Interface: efeito elástico nas listas, arrasto que acompanha o dedo, prévia do PDF mais leve e com botões no topo.
- iPhone: o campo "metodo" ainda não é lido pelo leitor Swift (treinos com método podem falhar na conferência de integridade até atualizar o ContratoRJE.swift).

## [0.9.0] — 2026-10-15 — versão de testes
Primeira versão versionada. Reúne tudo o que foi desenvolvido até aqui:

- Gestão do professor: alunos, avaliações físicas (PAR-Q, dobras, fotos, assinatura), treinos A/B/C/D, agenda,
  financeiro com Pix e QR Code, relatórios em PDF, metas e reavaliações, backup criptografado e sincronização por pasta.
- Modo aluno no mesmo app: receber treino por arquivo/código/pacote com vídeos, executar, histórico, resumo e lembretes.
- Identidade FitPersonal: abertura animada, ícones, tema verde-limão e cartões em forma de pasta.
- Vídeos nos exercícios (link e vídeo próprio com player embutido) e imagens de demonstração no catálogo.
- Registro local de falhas (Perfil → Dados → Relatório de falhas).
- Projeto iPhone (modo aluno) em `ios/FitPersonal`, ainda não compilado.

Pendências conhecidas: validar compilação/testes de todas as etapas recentes, build release com R8,
fotos criptografadas em disco, vários perfis (estúdio).
