package br.com.fitpersonal.app.report

import android.graphics.Bitmap
import br.com.fitpersonal.app.data.local.AgendamentoComAluno
import br.com.fitpersonal.app.domain.model.*
import br.com.fitpersonal.app.domain.rules.AnalisePollock
import br.com.fitpersonal.app.domain.rules.AvaliacaoRules
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.ParqRules
import br.com.fitpersonal.app.domain.rules.Pollock
import br.com.fitpersonal.app.domain.rules.ResumoMensal
import br.com.fitpersonal.app.domain.rules.ClassePressao
import br.com.fitpersonal.app.domain.rules.TipoFoto
import br.com.fitpersonal.app.domain.rules.TreinoRules
import br.com.fitpersonal.app.domain.rules.Validators
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val PT_BR = Locale("pt", "BR")

private fun num(v: Double?, casas: Int = 1): String = when {
    v == null -> "-"
    v % 1.0 == 0.0 -> v.toLong().toString()
    else -> String.format(PT_BR, "%.${casas}f", v)
}

private fun contato(p: PerfilProfissional): String = listOf(
    p.email, p.telefone, p.cidade, if (p.cref.isNotBlank()) "CREF: ${p.cref}" else "",
).filter { it.isNotBlank() }.joinToString(" | ")

private fun cabecalho(titulo: String, p: PerfilProfissional, logo: Bitmap?) =
    Cabecalho(titulo = titulo, marca = p.marca.ifBlank { p.nome }, contato = contato(p), logo = logo)

private fun linhaProfessor(p: PerfilProfissional): String {
    val nome = p.nome.ifBlank { "____________________" }
    return "Professor(a): $nome" + if (p.cref.isNotBlank()) " (CREF: ${p.cref})" else ""
}

private fun dica(c: ClassePressao): String = when (c) {
    ClassePressao.HIPOTENSAO ->
        "Mantenha-se hidratado, evite levantar bruscamente e prefira refeições menores e mais frequentes. Avalie com o médico se houver tontura."
    ClassePressao.NORMAL ->
        "Valores adequados. Mantenha hábitos saudáveis e a rotina de exercícios."
    ClassePressao.ELEVADA ->
        "Atenção aos fatores de risco. Alimentação equilibrada, menos sódio e consistência nos treinos ajudam a manter a pressão sob controle."
    ClassePressao.HIPERTENSAO_1 ->
        "Monitore regularmente e reduza o sódio. Atividade aeróbica regular é recomendada, com acompanhamento médico."
    ClassePressao.HIPERTENSAO_2 ->
        "Acompanhamento médico rigoroso é indispensável. Controle cuidadosamente carga, intervalos e esforço nos treinos."
    ClassePressao.CRISE ->
        "Valores criticamente altos. Procure atendimento médico imediato e suspenda os treinos até liberação."
}

object AvaliacaoPdf {

    fun gerar(
        a: AvaliacaoFisica,
        aluno: Aluno,
        perfil: PerfilProfissional,
        logo: Bitmap?,
        fotos: (String) -> Bitmap?,
        saida: OutputStream,
    ) {
        val w = PdfWriter(paisagem = false, cabecalho = cabecalho("Avaliação Física", perfil, logo))

        w.texto(linhaProfessor(perfil))
        w.texto("Aluno(a): ${aluno.nome}" + (aluno.sexo?.let { " ($it)" } ?: ""))
        val docs = listOfNotNull(
            aluno.cpf?.takeIf { it.isNotBlank() }?.let { "CPF: ${Validators.formatarCpf(it)}" },
            aluno.cep?.takeIf { it.isNotBlank() }?.let { "CEP: $it" },
        ).joinToString(" | ")
        w.texto(docs)
        w.texto("Data: ${a.data.format(DATA_BR)}")

        w.secao("Dados antropométricos")
        val imc = AvaliacaoRules.imc(a.pesoKg, a.alturaM)
        w.campo("Peso", "${num(a.pesoKg)} kg")
        w.campo("Altura", "${num(a.alturaM, 2)} m")
        w.campo("IMC", imc?.let { "%s — %s".format(num(it, 2), AvaliacaoRules.classificarImc(it).rotulo) } ?: "-")
        w.campo("% de gordura", num(a.percentualGordura))
        w.campo("Massa magra", "${num(a.massaMagraKg)} kg")
        w.campo("Massa gorda", "${num(a.massaGordaKg)} kg")
        if (a.cinturaCm != null || a.quadrilCm != null) {
            w.campo("Cintura", "${num(a.cinturaCm)} cm")
            w.campo("Quadril", "${num(a.quadrilCm)} cm")
            EvolucaoRules.rcq(a.cinturaCm, a.quadrilCm)?.let { r ->
                val classe = EvolucaoRules.classificarRcq(r, SexoBiologico.deTexto(aluno.sexo))
                w.campo("RCQ", num(r, 2) + (classe?.let { " — " + it.rotulo } ?: ""))
            }
        }

        if (a.ldl != null || a.hdl != null) {
            w.campo("Perfil lipídico", "LDL ${num(a.ldl, 0)} mg/dL | HDL ${num(a.hdl, 0)} mg/dL")
        }

        val classe = AvaliacaoRules.classificarPressao(a.pressaoSistolica, a.pressaoDiastolica)
        if (classe != null) {
            val popular = AvaliacaoRules.pressaoPopular(a.pressaoSistolica, a.pressaoDiastolica)?.let { " ($it)" }.orEmpty()
            w.espaco(4f)
            w.texto(
                "PA: ${a.pressaoSistolica ?: "-"}/${a.pressaoDiastolica ?: "-"} mmHg$popular — ${classe.rotulo}",
                enfatizar = true,
            )
            w.texto(dica(classe), tamanho = 9f)
        }

        val alertas = AvaliacaoRules.alertasLaboratoriais(a.percentualGordura, a.ldl, a.hdl)
        if (alertas.isNotEmpty()) w.campo("Alertas", alertas.joinToString("; "))

        dobras(w, a, aluno)
        bloco(w, "Perímetros", a.perimetros, sempre = true)
        bloco(w, "Anamnese", a.anamnese, sempre = true)

        val condicoes = a.condicoes.joinToString(", ") { it.rotulo }
        if (condicoes.isNotEmpty() || listOf(a.historicoSaude, a.estiloVida, a.metas).any { !it.isNullOrBlank() }) {
            w.secao("Entrevista inicial")
            if (condicoes.isNotEmpty()) w.campo("Condições", condicoes)
            a.historicoSaude?.takeIf { it.isNotBlank() }?.let { w.campo("Histórico de saúde", it) }
            a.estiloVida?.takeIf { it.isNotBlank() }?.let { w.campo("Estilo de vida", it) }
            a.metas?.takeIf { it.isNotBlank() }?.let { w.campo("Metas", it) }
        }

        if (ParqRules.aplicado(a.parq)) {
            w.secao("PAR-Q (prontidão para atividade física)")
            val respostas = ParqRules.decodificar(a.parq)
            w.tabela(
                listOf("Pergunta" to 82f, "Resposta" to 18f),
                ParqRules.PERGUNTAS.mapIndexed { i, p ->
                    listOf(p, when (respostas[i]) { true -> "Sim"; false -> "Não"; null -> "-" })
                },
            )
            if (ParqRules.requerLiberacao(a.parq)) {
                w.texto(
                    "Atenção: há resposta \"sim\". Recomenda-se liberação médica antes de iniciar ou intensificar o treino.",
                    enfatizar = true,
                )
            } else {
                w.texto("Sem respostas \"sim\" no PAR-Q.", tamanho = 9f)
            }
        }

        bloco(w, "Avaliação postural", a.postura)
        bloco(w, "Funcional e mobilidade", a.funcionalMobilidade)
        if (!a.cardio.isNullOrBlank() || !a.forcaResistencia.isNullOrBlank()) {
            w.secao("Cardiorrespiratório e força")
            a.cardio?.takeIf { it.isNotBlank() }?.let { w.campo("Cardiorrespiratório", it) }
            a.forcaResistencia?.takeIf { it.isNotBlank() }?.let { w.campo("Força/Resistência", it) }
        }

        val legendas = mapOf(
            TipoFoto.FRENTE to a.fotoFrente, TipoFoto.COSTAS to a.fotoCostas,
            TipoFoto.LATERAL_DIR to a.fotoLateralDir, TipoFoto.LATERAL_ESQ to a.fotoLateralEsq,
        )
        val imagens = legendas.mapNotNull { (tipo, caminho) ->
            caminho?.let(fotos)?.let { "Vista ${tipo.rotulo.lowercase(PT_BR)}" to it }
        }
        if (imagens.isNotEmpty()) {
            w.secao("Fotos")
            w.imagens(imagens)
        }
        a.assinatura?.let(fotos)?.let { sig ->
            w.secao("Ciência e assinatura do aluno")
            w.texto("Declaro que as informações prestadas nesta avaliação são verdadeiras.", tamanho = 9f)
            w.imagens(listOf("${aluno.nome} — ${a.data.format(DATA_BR)}" to sig), porLinha = 2)
        }
        w.finalizar(saida)
    }

    /** Dobras numéricas (mm) com o resultado de Pollock/Siri quando há dados suficientes; senão, só as observações. */
    private fun dobras(w: PdfWriter, a: AvaliacaoFisica, aluno: Aluno) {
        w.secao("Dobras cutâneas")
        val protocolo = a.protocoloDobras
        if (protocolo != null && a.dobrasMm.isNotEmpty()) {
            w.tabela(
                listOf("Ponto" to 60f, "Medida (mm)" to 40f),
                Dobra.entries.filter { it in a.dobrasMm }.map { listOf(it.rotulo, num(a.dobrasMm[it])) },
            )
            when (val an = Pollock.analisar(
                protocolo, a.dobrasMm, SexoBiologico.deTexto(aluno.sexo), aluno.dataNascimento, a.data,
            )) {
                is AnalisePollock.Calculada -> {
                    val r = an.resultado
                    w.texto(
                        "${protocolo.rotulo}: soma ${num(r.somaDobrasMm)} mm • densidade ${num(r.densidadeCorporal, 4)} g/mL " +
                            "• % gordura (Siri) ${num(r.percentualGordura)}%",
                        enfatizar = true,
                    )
                    an.avisos.forEach { w.texto("Atenção: $it", tamanho = 9f) }
                }
                is AnalisePollock.Pendente -> w.texto("${protocolo.rotulo}: cálculo não realizado — ${an.motivo}", tamanho = 9f)
            }
        }
        w.texto(a.dobrasCutaneas?.takeIf { it.isNotBlank() } ?: if (a.dobrasMm.isEmpty()) "-" else "")
    }

    private fun bloco(w: PdfWriter, titulo: String, conteudo: String?, sempre: Boolean = false) {
        if (conteudo.isNullOrBlank() && !sempre) return
        w.secao(titulo)
        w.texto(conteudo?.takeIf { it.isNotBlank() } ?: "-")
    }
}

object TreinoPdf {

    fun gerar(
        treino: Treino,
        exercicios: List<ExercicioTreino>,
        aluno: Aluno,
        perfil: PerfilProfissional,
        logo: Bitmap?,
        saida: OutputStream,
    ) {
        val w = PdfWriter(paisagem = true, cabecalho = cabecalho("FICHA DE TREINO", perfil, logo))

        w.texto(linhaProfessor(perfil))
        w.texto("Aluno(a): ${aluno.nome}")
        w.texto("Treino: ${treino.nome}" + (treino.objetivo?.takeIf { it.isNotBlank() }?.let { " — Objetivo: $it" } ?: ""))
        w.texto("Data: ${treino.dataCriacao.format(DATA_BR)}")

        val grupos = TreinoRules.agruparPorDivisao(exercicios).filterValues { it.isNotEmpty() }
        if (grupos.isEmpty()) {
            w.espaco()
            w.texto("Nenhum exercício cadastrado neste treino.")
        }
        val colunas = listOf(
            "Exercício" to 34f, "Séries" to 8f, "Reps" to 9f, "Carga" to 12f, "Descanso" to 12f, "Observações" to 25f,
        )
        grupos.forEach { (divisao, lista) ->
            w.secao("Treino ${divisao.name}")
            w.tabela(
                colunas,
                lista.map { listOf(it.nome, it.series.orEmpty(), it.repeticoes.orEmpty(), it.carga.orEmpty(), it.descanso.orEmpty(), it.observacoes.orEmpty()) },
            )
        }

        w.secao("Ativação e cardio")
        w.linhasEmBranco("Ativações", 2)
        w.linhasEmBranco("Cardio", 2)
        w.finalizar(saida)
    }
}

object AgendaPdf {

    fun gerar(
        dia: LocalDate,
        itens: List<AgendamentoComAluno>,
        perfil: PerfilProfissional,
        logo: Bitmap?,
        saida: OutputStream,
    ) {
        val w = PdfWriter(paisagem = false, cabecalho = cabecalho("Agenda do Dia", perfil, logo))
        w.texto(linhaProfessor(perfil))
        w.texto("Data: ${dia.format(DATA_BR)}")
        w.espaco()

        if (itens.isEmpty()) {
            w.texto("Nenhum agendamento para este dia.")
        } else {
            w.tabela(
                listOf("Horário" to 12f, "Aluno" to 38f, "CPF" to 20f, "Tipo" to 16f, "Status" to 14f),
                itens.map {
                    listOf(
                        it.horario,
                        it.nomeAluno,
                        it.cpfAluno?.let(Validators::formatarCpf).orEmpty(),
                        (TipoAgendamento.entries.firstOrNull { e -> e.name == it.tipo } ?: TipoAgendamento.TREINO).rotulo,
                        (StatusAgendamento.entries.firstOrNull { e -> e.name == it.status } ?: StatusAgendamento.PENDENTE).rotulo,
                    )
                },
            )
            val pendentes = itens.count { it.status == StatusAgendamento.PENDENTE.name }
            w.texto("Total: ${itens.size} agendamento(s) • $pendentes pendente(s)", enfatizar = true)
        }
        w.finalizar(saida)
    }
}

/** Resumo do mês: sessões por status/tipo/aluno e taxa de comparecimento. */
object RelatorioMensalPdf {

    fun gerar(
        mes: java.time.YearMonth,
        resumo: ResumoMensal,
        avaliacoesNoMes: Int,
        perfil: PerfilProfissional,
        logo: Bitmap?,
        saida: OutputStream,
        /** Pagamentos da competência (nome do aluno, valor em centavos, data); vazio = sem a seção. */
        recebimentos: List<Triple<String, Long, String>> = emptyList(),
    ) {
        val w = PdfWriter(paisagem = false, cabecalho = cabecalho("Relatório Mensal", perfil, logo))
        val nomeMes = mes.month.getDisplayName(java.time.format.TextStyle.FULL, PT_BR)
        w.texto(linhaProfessor(perfil))
        w.texto("Período: $nomeMes de ${mes.year}")

        w.secao("Resumo")
        if (resumo.total == 0 && avaliacoesNoMes == 0) {
            w.texto("Nenhuma atividade registrada neste mês.")
        } else {
            w.tabela(
                listOf("Indicador" to 60f, "Valor" to 40f),
                listOf(
                    listOf("Agendamentos no mês", resumo.total.toString()),
                    listOf("Concluídos", resumo.concluidos.toString()),
                    listOf("Cancelados", resumo.cancelados.toString()),
                    listOf("Faltas", resumo.faltas.toString()),
                    listOf("Pendentes", resumo.pendentes.toString()),
                    listOf(
                        "Comparecimento (concluídos / concluídos + cancelados + faltas)",
                        resumo.taxaComparecimento?.let { String.format(PT_BR, "%.0f%%", it * 100) } ?: "-",
                    ),
                    listOf("Avaliações físicas realizadas", avaliacoesNoMes.toString()),
                ),
            )
        }

        if (resumo.porTipo.isNotEmpty()) {
            w.secao("Por tipo")
            w.tabela(
                listOf("Tipo" to 60f, "Quantidade" to 40f),
                resumo.porTipo.map { (t, n) -> listOf(t.rotulo, n.toString()) },
            )
        }

        if (recebimentos.isNotEmpty()) {
            w.secao("Recebimentos da competência")
            w.tabela(
                listOf("Aluno" to 50f, "Valor" to 25f, "Pago em" to 25f),
                recebimentos.map { (nome, centavos, data) ->
                    listOf(nome, FinanceiroRules.formatarReais(centavos), data)
                },
            )
            w.texto(
                "Total recebido: " + FinanceiroRules.formatarReais(recebimentos.sumOf { it.second }),
                enfatizar = true,
            )
        }

        if (resumo.porAluno.isNotEmpty()) {
            w.secao("Por aluno")
            w.tabela(
                listOf("Aluno" to 46f, "Concluídos" to 18f, "Cancelados" to 18f, "Pendentes" to 18f),
                resumo.porAluno.map {
                    listOf(it.nome, it.concluidos.toString(), it.cancelados.toString(), it.pendentes.toString())
                },
            )
        }
        w.finalizar(saida)
    }
}

/** Comparativo entre duas avaliações do mesmo aluno: tabela de métricas com variação e sentido. */
object ComparativoPdf {

    fun gerar(
        aluno: Aluno,
        antes: AvaliacaoFisica,
        depois: AvaliacaoFisica,
        perfil: PerfilProfissional,
        logo: Bitmap?,
        saida: OutputStream,
        /** Fotos posturais já legendadas, em pares antes/depois (2 por linha). Vazio = sem a seção. */
        fotos: List<Pair<String, Bitmap>> = emptyList(),
    ) {
        val w = PdfWriter(paisagem = false, cabecalho = cabecalho("Comparativo de Avaliações", perfil, logo))

        w.texto(linhaProfessor(perfil))
        w.texto("Aluno(a): ${aluno.nome}")
        w.texto("Período: ${antes.data.format(DATA_BR)} → ${depois.data.format(DATA_BR)}")
        val dias = java.time.temporal.ChronoUnit.DAYS.between(antes.data, depois.data)
        if (dias > 0) w.texto("Intervalo: $dias dia(s)")

        w.secao("Resultados")
        val linhas = EvolucaoRules.comparar(antes, depois)
        if (linhas.isEmpty()) {
            w.texto("As duas avaliações não têm medidas em comum para comparar.")
        } else {
            w.tabela(
                listOf("Medida" to 30f, "Antes" to 20f, "Depois" to 20f, "Variação" to 30f),
                linhas.map { l ->
                    val un = if (l.metrica.unidade.isNotEmpty()) " ${l.metrica.unidade}" else ""
                    val variacao = l.variacao?.let { v ->
                        val marca = when (v.sentido) {
                            SentidoVariacao.MELHOROU -> " (melhorou)"
                            SentidoVariacao.PIOROU -> " (piorou)"
                            SentidoVariacao.IGUAL -> " (igual)"
                            SentidoVariacao.NEUTRO -> ""
                        }
                        EvolucaoRules.formatarDelta(v, l.metrica) + marca
                    } ?: "-"
                    listOf(
                        l.metrica.rotulo,
                        l.antes?.let { num(it, l.metrica.casas) + un } ?: "-",
                        l.depois?.let { num(it, l.metrica.casas) + un } ?: "-",
                        variacao,
                    )
                },
            )
        }
        if (fotos.isNotEmpty()) {
            w.secao("Fotos posturais (antes à esquerda, depois à direita)")
            w.imagens(fotos, porLinha = 2)
        }
        w.texto(
            "Observação: \"melhorou/piorou\" considera menor valor como melhor apenas para % de gordura, massa gorda, " +
                "cintura e RCQ. Peso e IMC dependem do objetivo do aluno e não recebem juízo.",
            tamanho = 8.5f,
        )
        w.finalizar(saida)
    }
}

/** Recibo simples de mensalidade (não substitui nota fiscal). */
object ReciboPdf {

    fun gerar(
        pagamento: Pagamento,
        aluno: Aluno,
        perfil: PerfilProfissional,
        logo: Bitmap?,
        saida: OutputStream,
    ) {
        val w = PdfWriter(paisagem = false, cabecalho = cabecalho("Recibo de Pagamento", perfil, logo))
        val valor = FinanceiroRules.formatarReais(pagamento.valorCentavos)
        val extenso = br.com.fitpersonal.app.domain.rules.ExtensoRules.reais(pagamento.valorCentavos)

        w.espaco(8f)
        w.texto("Recibo nº %06d".format(pagamento.id), enfatizar = true, tamanho = 13f)
        w.texto("Valor: $valor", enfatizar = true, tamanho = 16f)
        w.espaco(10f)

        val cpf = aluno.cpf?.takeIf { it.isNotBlank() }?.let { ", CPF ${Validators.formatarCpf(it)}" }.orEmpty()
        w.texto(
            "Recebi de ${aluno.nome}$cpf a importância de $valor ($extenso), referente à mensalidade de " +
                "${FinanceiroRules.rotuloCompetencia(pagamento.competencia)}.",
            tamanho = 12f,
        )
        w.espaco(10f)
        w.texto("Data do pagamento: ${pagamento.pagoEm.format(DATA_BR)}")

        w.espaco(40f)
        w.texto("______________________________________")
        w.texto(perfil.nome.ifBlank { "Profissional responsável" } + if (perfil.cref.isNotBlank()) " — CREF ${perfil.cref}" else "")
        if (perfil.marca.isNotBlank()) w.texto(perfil.marca, tamanho = 9f)

        w.espaco(14f)
        w.texto("Documento de quitação simples; não substitui nota fiscal de serviços.", tamanho = 8.5f)
        w.finalizar(saida)
    }
}
