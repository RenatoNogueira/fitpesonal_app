# Histórico de versões

Formato: [versão] — data — resumo. A versão do app fica em `gradle.properties` (`versionName` / `versionCode`).

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
