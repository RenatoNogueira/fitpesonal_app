package br.com.fitpersonal.app.data.local

import br.com.fitpersonal.app.domain.model.*
import java.time.LocalDate
import java.time.LocalTime

fun AlunoEntity.toDomain() = Aluno(
    id, nome, dataNascimento?.let(LocalDate::parse), sexo, telefone, email, cpf, cep,
    instagram, objetivo, observacoesMedicas, fotoPerfil, LocalDate.parse(dataCadastro),
    consentimentoLgpdEm?.let(LocalDate::parse),
    metaMetrica, metaValor, reavaliacaoDias,
    mensalidadeCentavos, diaVencimento,
    pacoteSessoes, pacoteInicio?.let(LocalDate::parse),
    refContrato,
)

fun Aluno.toEntity() = AlunoEntity(
    id, nome, dataNascimento?.toString(), sexo, telefone, email, cpf, cep,
    instagram, objetivo, observacoesMedicas, fotoPerfil, dataCadastro.toString(),
    consentimentoLgpdEm?.toString(),
    metaMetrica, metaValor, reavaliacaoDias,
    mensalidadeCentavos, diaVencimento,
    pacoteSessoes, pacoteInicio?.toString(),
    refContrato,
)

fun PagamentoEntity.toDomain() =
    Pagamento(id, idAluno, java.time.YearMonth.parse(competencia), valorCentavos, LocalDate.parse(pagoEm))

fun Pagamento.toEntity() = PagamentoEntity(id, idAluno, competencia.toString(), valorCentavos, pagoEm.toString())

fun AgendamentoEntity.toDomain() = Agendamento(
    id, idAluno, LocalDate.parse(data), LocalTime.parse(horario),
    (TipoAgendamento.entries.firstOrNull { it.name == tipo } ?: TipoAgendamento.TREINO), (StatusAgendamento.entries.firstOrNull { it.name == status } ?: StatusAgendamento.PENDENTE), anotacao,
)

fun Agendamento.toEntity() =
    AgendamentoEntity(id, idAluno, data.toString(), horario.toString(), tipo.name, status.name, anotacao)

fun AvaliacaoEntity.toDomain() = AvaliacaoFisica(
    id, idAluno, LocalDate.parse(data), pesoKg, alturaM, percentualGordura, massaMagraKg, massaGordaKg,
    ldl, hdl, pressaoSistolica, pressaoDiastolica, dobrasCutaneas, perimetros, anamnese,
    condicoes.split(",").filter { it.isNotBlank() }.mapNotNull { n -> CondicaoSaude.entries.firstOrNull { it.name == n } }.toSet(),
    historicoSaude, estiloVida, metas, postura, funcionalMobilidade, cardio, forcaResistencia,
    fotoFrente, fotoCostas, fotoLateralDir, fotoLateralEsq,
    protocoloDobras?.let { n -> ProtocoloDobras.entries.firstOrNull { it.name == n } },
    buildMap {
        dobraPeitoral?.let { put(Dobra.PEITORAL, it) }
        dobraAxilarMedia?.let { put(Dobra.AXILAR_MEDIA, it) }
        dobraTriceps?.let { put(Dobra.TRICEPS, it) }
        dobraSubescapular?.let { put(Dobra.SUBESCAPULAR, it) }
        dobraAbdominal?.let { put(Dobra.ABDOMINAL, it) }
        dobraSuprailiaca?.let { put(Dobra.SUPRAILIACA, it) }
        dobraCoxa?.let { put(Dobra.COXA, it) }
    },
    cinturaCm,
    quadrilCm,
    parq,
    assinatura,
)

fun AvaliacaoFisica.toEntity() = AvaliacaoEntity(
    id, idAluno, data.toString(), pesoKg, alturaM, percentualGordura, massaMagraKg, massaGordaKg,
    ldl, hdl, pressaoSistolica, pressaoDiastolica, dobrasCutaneas, perimetros, anamnese,
    condicoes.joinToString(",") { it.name },
    historicoSaude, estiloVida, metas, postura, funcionalMobilidade, cardio, forcaResistencia,
    fotoFrente, fotoCostas, fotoLateralDir, fotoLateralEsq,
    protocoloDobras?.name,
    dobrasMm[Dobra.PEITORAL], dobrasMm[Dobra.AXILAR_MEDIA], dobrasMm[Dobra.TRICEPS], dobrasMm[Dobra.SUBESCAPULAR],
    dobrasMm[Dobra.ABDOMINAL], dobrasMm[Dobra.SUPRAILIACA], dobrasMm[Dobra.COXA],
    cinturaCm,
    quadrilCm,
    parq,
    assinatura,
)

fun TreinoEntity.toDomain() = Treino(id, idAluno, nome, objetivo, LocalDate.parse(dataCriacao))
fun Treino.toEntity() = TreinoEntity(id, idAluno, nome, objetivo, dataCriacao.toString())

fun ExercicioTreinoEntity.toDomain() = ExercicioTreino(
    id, idTreino, nome, (DivisaoTreino.entries.firstOrNull { it.name == divisao } ?: DivisaoTreino.A), series, repeticoes, carga, descanso, observacoes, video,
)

fun ExercicioTreino.toEntity() = ExercicioTreinoEntity(
    id, idTreino, nome, divisao.name, series, repeticoes, carga, descanso, observacoes, video,
)

fun ExercicioCatalogoEntity.toDomain() = ExercicioCatalogo(
    id, nome, categoria, nivel, equipamento,
    musculosPrimarios.split(";").filter { it.isNotBlank() },
    musculosSecundarios.split(";").filter { it.isNotBlank() },
    instrucoes.split("\n").filter { it.isNotBlank() },
    imagens.split(";").filter { it.isNotBlank() },
)

fun ListaEsperaEntity.toDomain() = ListaEspera(
    id, idAluno, java.time.DayOfWeek.of(diaSemana), horario?.let(LocalTime::parse), observacao, LocalDate.parse(criadoEm),
)

fun ListaEspera.toEntity() = ListaEsperaEntity(
    id, idAluno, diaSemana.value, horario?.toString(), observacao, criadoEm.toString(),
)

fun TesteAptidaoEntity.toDomain() = br.com.fitpersonal.app.domain.rules.TesteAptidao(
    id, idAluno, LocalDate.parse(data), br.com.fitpersonal.app.domain.rules.TipoTeste.valueOf(tipo), valor, observacao,
)

fun br.com.fitpersonal.app.domain.rules.TesteAptidao.toEntity() =
    TesteAptidaoEntity(id, idAluno, data.toString(), tipo.name, valor, observacao)
