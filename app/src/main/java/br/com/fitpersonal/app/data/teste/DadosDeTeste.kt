package br.com.fitpersonal.app.data.teste

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import br.com.fitpersonal.app.BuildConfig
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.local.HistoricoCargaDao
import br.com.fitpersonal.app.data.local.HistoricoCargaEntity
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.data.repo.ListaEsperaRepository
import br.com.fitpersonal.app.data.repo.ModeloTreinoRepository
import br.com.fitpersonal.app.data.repo.PagamentoRepository
import br.com.fitpersonal.app.data.repo.PerfilRepository
import br.com.fitpersonal.app.data.repo.TesteAptidaoRepository
import br.com.fitpersonal.app.data.repo.TreinoRepository
import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.CondicaoSaude
import br.com.fitpersonal.app.domain.model.Dobra
import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioTreino
import br.com.fitpersonal.app.domain.model.ListaEspera
import br.com.fitpersonal.app.domain.model.Pagamento
import br.com.fitpersonal.app.domain.model.PerfilProfissional
import br.com.fitpersonal.app.domain.model.ProtocoloDobras
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.model.Treino
import br.com.fitpersonal.app.domain.rules.CargaRules
import br.com.fitpersonal.app.domain.rules.ParqRules
import br.com.fitpersonal.app.domain.rules.TesteAptidao
import br.com.fitpersonal.app.domain.rules.TipoTeste
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Dados fictícios COMPLETOS (todos os campos preenchidos) para testar o app antes de publicar.
 * Só funciona nos builds `debug` e `homologacao` (`BuildConfig.DADOS_TESTE`): em release as duas funções não fazem nada.
 * Todo aluno criado leva o prefixo [PREFIXO], o que permite removê-los depois sem tocar nos dados reais.
 */
@Singleton
class DadosDeTeste @Inject constructor(
    private val alunos: AlunoRepository,
    private val avaliacoes: AvaliacaoRepository,
    private val treinos: TreinoRepository,
    private val modelos: ModeloTreinoRepository,
    private val agenda: AgendaRepository,
    private val pagamentos: PagamentoRepository,
    private val espera: ListaEsperaRepository,
    private val testes: TesteAptidaoRepository,
    private val historico: HistoricoCargaDao,
    private val fotos: FotoStorage,
    private val perfil: PerfilRepository,
    private val config: ConfiguracoesRepository,
) {
    companion object {
        const val PREFIXO = "(Teste) "
    }

    val disponivel: Boolean get() = BuildConfig.DADOS_TESTE

    /** Cria os alunos de teste e tudo que gira em torno deles. Devolve quantos alunos foram criados (0 = já existiam). */
    suspend fun popular(): Int {
        if (!BuildConfig.DADOS_TESTE) return 0
        if (alunos.observar().first().any { it.nome.startsWith(PREFIXO) }) return 0
        val hoje = LocalDate.now()

        // Perfil e metas do negócio só são preenchidos se ainda estiverem vazios.
        if (perfil.perfil.first().nome.isBlank()) {
            perfil.salvar(
                PerfilProfissional(
                    nome = "Renato Nogueira", marca = "FitPersonal", cref = "123456-G/SP",
                    email = "contato@fitpersonal.teste", telefone = "11987654321", cidade = "São Paulo",
                    chavePix = "contato@fitpersonal.teste",
                ),
            )
        }
        val cfg = config.config.first()
        if (cfg.metaSessoesMes == 0 && cfg.metaReceitaMesCentavos == 0L) config.definirMetasNegocio(40, 1_200_000L)

        val ana = alunos.salvar(
            Aluno(
                nome = PREFIXO + "Ana Beatriz Souza", dataNascimento = hoje.minusYears(28).minusMonths(2),
                sexo = "Feminino", telefone = "11999990001", email = "ana.souza@exemplo.teste",
                cpf = "52998224725", cep = "01310100", instagram = "@ana.souza.fit",
                objetivo = "Emagrecimento e definição", observacoesMedicas = "Rinite alérgica. Sem restrições para exercício.",
                fotoPerfil = foto(0, "perfil", "Ana", Color.rgb(46, 125, 50), 400, 400, "fotos/perfil"),
                dataCadastro = hoje.minusDays(100), consentimentoLgpdEm = hoje.minusDays(99),
                metaMetrica = "PESO", metaValor = 62.0, reavaliacaoDias = 30,
                mensalidadeCentavos = 28_000, diaVencimento = 10,
                pacoteSessoes = 12, pacoteInicio = hoje.minusDays(30),
            ),
        )
        val bruno = alunos.salvar(
            Aluno(
                nome = PREFIXO + "Bruno Lima Ferreira", dataNascimento = hoje.minusYears(41).minusMonths(5),
                sexo = "Masculino", telefone = "11999990002", email = "bruno.lima@exemplo.teste",
                cpf = "11144477735", cep = "04538133", instagram = "@brunolima",
                objetivo = "Hipertrofia e saúde cardiovascular", observacoesMedicas = "Hipertensão controlada com medicação.",
                fotoPerfil = foto(0, "perfil", "Bruno", Color.rgb(21, 101, 192), 400, 400, "fotos/perfil"),
                dataCadastro = hoje.minusDays(120), consentimentoLgpdEm = hoje.minusDays(119),
                metaMetrica = "MASSA_MAGRA", metaValor = 70.0, reavaliacaoDias = 30,
                mensalidadeCentavos = 35_000, diaVencimento = 5,
            ),
        )
        val carla = alunos.salvar(
            Aluno(
                nome = PREFIXO + "Carla Mendes", dataNascimento = hoje.minusYears(55).plusDays(40),
                sexo = "Feminino", telefone = "11999990003", email = "carla.mendes@exemplo.teste",
                cpf = "39053344705", cep = "30130010", instagram = "@carlamendes",
                objetivo = "Mobilidade e qualidade de vida", observacoesMedicas = "Artrose leve no joelho direito; liberação médica anexada.",
                fotoPerfil = foto(0, "perfil", "Carla", Color.rgb(173, 20, 87), 400, 400, "fotos/perfil"),
                dataCadastro = hoje.minusDays(60), consentimentoLgpdEm = hoje.minusDays(60),
                metaMetrica = "CINTURA", metaValor = 84.0, reavaliacaoDias = 45,
                mensalidadeCentavos = 32_000, diaVencimento = 20,
                pacoteSessoes = 10, pacoteInicio = hoje.minusDays(40),
            ),
        )
        val diego = alunos.salvar(
            Aluno(
                // Aniversário hoje: exercita o destaque "Aniversário hoje!" e o aviso diário.
                nome = PREFIXO + "Diego Santos", dataNascimento = hoje.minusYears(19),
                sexo = "Masculino", telefone = "11999990004", email = "diego.santos@exemplo.teste",
                cpf = "86288366757", cep = "80010000", instagram = "@diegosantos",
                objetivo = "Ganho de massa", observacoesMedicas = "Nenhuma.",
                fotoPerfil = foto(0, "perfil", "Diego", Color.rgb(230, 81, 0), 400, 400, "fotos/perfil"),
                dataCadastro = hoje.minusDays(20), consentimentoLgpdEm = hoje.minusDays(20),
                metaMetrica = "PESO", metaValor = 78.0, reavaliacaoDias = 60,
            ),
        )
        val elisa = alunos.salvar(
            Aluno(
                nome = PREFIXO + "Elisa Ramos", dataNascimento = hoje.minusYears(34).plusDays(3),
                sexo = "Outro", telefone = "11999990005", email = "elisa.ramos@exemplo.teste",
                cpf = "15350946056", cep = "22041001", instagram = "@elisaramos",
                objetivo = "Condicionamento para corrida", observacoesMedicas = "Asma induzida por exercício (usa bombinha).",
                fotoPerfil = foto(0, "perfil", "Elisa", Color.rgb(106, 27, 154), 400, 400, "fotos/perfil"),
                dataCadastro = hoje.minusDays(100), consentimentoLgpdEm = hoje.minusDays(100),
                metaMetrica = "PERCENTUAL_GORDURA", metaValor = 22.0, reavaliacaoDias = 30,
                mensalidadeCentavos = 25_000, diaVencimento = 15,
            ),
        )

        // ── Avaliações completas (3 para a Ana, 2 para o Bruno, 1 para os demais) ──
        avaliar(ana, hoje.minusDays(90), 74.0, 1.66, 33.0, 118, 78, 135.0, 52.0, 88.0, 102.0, fotosCompletas = true, parqSim = false)
        avaliar(ana, hoje.minusDays(45), 70.5, 1.66, 30.5, 114, 76, 128.0, 55.0, 84.0, 100.0, fotosCompletas = false, parqSim = false)
        avaliar(ana, hoje.minusDays(3), 67.8, 1.66, 28.0, 110, 72, 118.0, 58.0, 80.5, 98.0, fotosCompletas = true, parqSim = false)
        avaliar(bruno, hoje.minusDays(75), 92.0, 1.78, 27.0, 148, 94, 172.0, 36.0, 98.0, 104.0, fotosCompletas = true, parqSim = true)
        // última avaliação há 50 dias com reavaliação a cada 30: aparece como "reavaliação atrasada"
        avaliar(bruno, hoje.minusDays(50), 89.5, 1.78, 25.0, 142, 90, 160.0, 39.0, 95.0, 103.0, fotosCompletas = true, parqSim = true)
        avaliar(carla, hoje.minusDays(20), 71.0, 1.60, 36.0, 132, 84, 142.0, 48.0, 92.0, 108.0, fotosCompletas = true, parqSim = true)
        avaliar(diego, hoje.minusDays(10), 70.0, 1.80, 14.0, 118, 70, 95.0, 62.0, 78.0, 94.0, fotosCompletas = true, parqSim = false)
        avaliar(elisa, hoje.minusDays(28), 60.0, 1.68, 26.0, 108, 68, 100.0, 60.0, 72.0, 96.0, fotosCompletas = false, parqSim = false)

        // ── Treinos ──
        val historicoAgachamento = listOf(60 to 20.0, 45 to 25.0)
        historicoAgachamento.forEach { (dias, kg) ->
            historico.inserir(
                HistoricoCargaEntity(
                    idAluno = ana, exercicio = "Agachamento livre",
                    chave = CargaRules.chaveExercicio("Agachamento livre"),
                    data = hoje.minusDays(dias.toLong()).toString(), cargaKg = kg,
                ),
            )
        }
        val treinoAna = treinos.salvarCompleto(
            Treino(idAluno = ana, nome = "Hipertrofia ABC", objetivo = "Hipertrofia de membros inferiores e glúteos"),
            listOf(
                ex("Agachamento livre", DivisaoTreino.A, "4", "10", "25 kg", "90s", "Descer até 90°; joelhos alinhados."),
                ex("Leg press 45°", DivisaoTreino.A, "3", "12", "120 kg", "60s", "Não travar os joelhos."),
                ex("Cadeira extensora", DivisaoTreino.A, "3", "15", "35 kg", "60s", "Contração de 1s no topo."),
                ex("Stiff com barra", DivisaoTreino.A, "3", "12", "20 kg", "75s", "Coluna neutra."),
                ex("Supino reto", DivisaoTreino.B, "4", "10", "30 kg", "90s", "Escápulas retraídas.", "BISET"),
                ex("Remada curvada", DivisaoTreino.B, "4", "10", "25 kg", "90s", "Tronco a 45°.", "BISET"),
                ex("Desenvolvimento com halteres", DivisaoTreino.B, "3", "12", "14 kg", "60s", "Sem arquear a lombar."),
                ex("Rosca direta", DivisaoTreino.B, "3", "12", "12 kg", "60s", "Cotovelos fixos."),
                ex("Elevação pélvica", DivisaoTreino.C, "4", "12", "40 kg", "75s", "Pausa de 2s no topo."),
                ex("Abdominal infra", DivisaoTreino.C, "3", "20", "corporal", "45s", "Controlar a descida.", "CIRCUITO"),
                ex("Prancha isométrica", DivisaoTreino.C, "3", "45s", "corporal", "45s", "Quadril alinhado.", "CIRCUITO"),
            ),
        )
        treinos.salvarCompleto(
            Treino(idAluno = bruno, nome = "Full body + cardio", objetivo = "Hipertrofia com controle da pressão"),
            listOf(
                ex("Supino inclinado", DivisaoTreino.A, "3", "10", "40 kg", "90s", "Evitar manobra de Valsalva."),
                ex("Puxada frontal", DivisaoTreino.A, "3", "12", "50 kg", "75s", "Expirar ao puxar."),
                ex("Agachamento no smith", DivisaoTreino.A, "3", "12", "40 kg", "90s", "Respiração contínua."),
                ex("Esteira (intervalado)", DivisaoTreino.B, "1", "20 min", "", "—", "FC máxima 140 bpm.", "EMOM"),
                ex("Remada baixa", DivisaoTreino.B, "3", "12", "45 kg", "75s", "Peito aberto."),
                ex("Elevação lateral", DivisaoTreino.B, "3", "15", "6 kg", "45s", "Sem impulso."),
            ),
        )
        treinos.salvarCompleto(
            Treino(idAluno = carla, nome = "Funcional e mobilidade", objetivo = "Mobilidade articular e equilíbrio"),
            listOf(
                ex("Sentar e levantar da cadeira", DivisaoTreino.A, "3", "10", "corporal", "60s", "Sem dor no joelho."),
                ex("Ponte de glúteos", DivisaoTreino.A, "3", "12", "corporal", "45s", "Amplitude confortável."),
                ex("Remada com elástico", DivisaoTreino.A, "3", "15", "elástico leve", "45s", "Ombros baixos."),
                ex("Equilíbrio unipodal", DivisaoTreino.A, "3", "30s", "corporal", "30s", "Perto de apoio."),
            ),
        )
        modelos.salvarComoModelo(treinoAna, PREFIXO + "Hipertrofia ABC")

        // ── Agenda: concluídos, falta, cancelado, hoje e próximos ──
        ag(ana, -28, "07:00", TipoAgendamento.AVALIACAO, StatusAgendamento.CONCLUIDO, "Avaliação inicial concluída.")
        listOf(-21, -14, -7).forEachIndexed { i, d ->
            ag(ana, d, "07:00", TipoAgendamento.TREINO, StatusAgendamento.CONCLUIDO, "Treino ${'A' + i % 3}: boa execução, carga mantida.")
        }
        ag(ana, 1, "07:00", TipoAgendamento.TREINO, StatusAgendamento.PENDENTE, null)
        ag(ana, 3, "07:00", TipoAgendamento.AVALIACAO, StatusAgendamento.PENDENTE, null)
        ag(bruno, -20, "18:00", TipoAgendamento.TREINO, StatusAgendamento.CONCLUIDO, "Pressão 130/85 antes do treino.")
        ag(bruno, -13, "18:00", TipoAgendamento.TREINO, StatusAgendamento.FALTOU, "Avisou que não conseguiria vir.")
        ag(bruno, -6, "18:00", TipoAgendamento.TREINO, StatusAgendamento.CANCELADO, "Remarcado a pedido do aluno.")
        ag(bruno, 0, "18:00", TipoAgendamento.TREINO, StatusAgendamento.PENDENTE, null)
        ag(bruno, 2, "19:00", TipoAgendamento.CONSULTORIA, StatusAgendamento.PENDENTE, null)
        // Pacote da Carla: 9 de 10 sessões feitas (aparece como "última sessão")
        for (n in 0 until 9) {
            ag(carla, -39 + n * 4, "10:00", TipoAgendamento.TREINO, StatusAgendamento.CONCLUIDO, if (n == 0) "Primeira sessão do pacote." else null)
        }
        ag(carla, 2, "10:00", TipoAgendamento.TREINO, StatusAgendamento.PENDENTE, null)
        ag(diego, -5, "16:00", TipoAgendamento.TREINO, StatusAgendamento.CONCLUIDO, "Primeira semana: adaptação.")
        ag(diego, 0, "09:00", TipoAgendamento.TREINO, StatusAgendamento.PENDENTE, null)
        ag(diego, 4, "16:00", TipoAgendamento.AVALIACAO, StatusAgendamento.PENDENTE, null)
        ag(elisa, -10, "06:30", TipoAgendamento.TREINO, StatusAgendamento.CONCLUIDO, "Corrida leve 5 km.")
        ag(elisa, 5, "06:30", TipoAgendamento.TREINO, StatusAgendamento.PENDENTE, null)

        // ── Pagamentos (Bruno e Elisa ficam em atraso) ──
        val mes = YearMonth.from(hoje)
        for (k in 0..2) {
            val comp = mes.minusMonths(k.toLong())
            pagamentos.registrar(Pagamento(idAluno = ana, competencia = comp, valorCentavos = 28_000, pagoEm = minOf(hoje, comp.atDay(10))))
        }
        for (k in 0..1) {
            val comp = mes.minusMonths(k.toLong())
            pagamentos.registrar(Pagamento(idAluno = carla, competencia = comp, valorCentavos = 32_000, pagoEm = minOf(hoje, comp.atDay(20))))
        }
        pagamentos.registrar(
            Pagamento(idAluno = bruno, competencia = mes.minusMonths(3), valorCentavos = 35_000, pagoEm = mes.minusMonths(3).atDay(5)),
        )

        // ── Lista de espera ──
        espera.adicionar(ListaEspera(idAluno = elisa, diaSemana = DayOfWeek.MONDAY, horario = LocalTime.of(7, 0), observacao = "Prefere antes do trabalho."))
        espera.adicionar(ListaEspera(idAluno = diego, diaSemana = DayOfWeek.SATURDAY, horario = null, observacao = "Qualquer horário pela manhã."))

        // ── Testes de aptidão (todos os tipos, duas datas) ──
        val valores = mapOf(
            TipoTeste.COOPER_12MIN to (1900.0 to 2150.0),
            TipoTeste.SENTAR_ALCANCAR to (22.0 to 27.5),
            TipoTeste.FLEXAO_BRACO to (12.0 to 18.0),
            TipoTeste.ABDOMINAL_1MIN to (28.0 to 36.0),
            TipoTeste.PRANCHA to (50.0 to 75.0),
        )
        valores.forEach { (tipo, par) ->
            testes.registrar(TesteAptidao(idAluno = ana, data = hoje.minusDays(60), tipo = tipo, valor = par.first, observacao = "Linha de base"))
            testes.registrar(TesteAptidao(idAluno = ana, data = hoje.minusDays(5), tipo = tipo, valor = par.second, observacao = "Reteste"))
        }
        testes.registrar(TesteAptidao(idAluno = diego, data = hoje.minusDays(8), tipo = TipoTeste.COOPER_12MIN, valor = 2600.0, observacao = null))

        return 5
    }

    /** Remove os alunos de teste (e, em cascata, avaliações, treinos, agenda e pagamentos), fotos e modelos. */
    suspend fun remover(): Int {
        if (!BuildConfig.DADOS_TESTE) return 0
        val alvo = alunos.observar().first().filter { it.nome.startsWith(PREFIXO) }
        alvo.forEach { a ->
            fotos.arquivo("fotos/${a.id}").deleteRecursively()
            fotos.apagar(a.fotoPerfil)
            alunos.excluir(a)
        }
        modelos.todos.first().filter { it.nome.startsWith(PREFIXO) }.forEach { modelos.excluir(it.id) }
        return alvo.size
    }

    // ───────────────────────── auxiliares ─────────────────────────

    private fun ex(nome: String, d: DivisaoTreino, series: String, rep: String, carga: String, descanso: String, obs: String, metodo: String? = null) =
        ExercicioTreino(
            idTreino = 0, nome = nome, divisao = d, series = series, repeticoes = rep,
            carga = carga.ifBlank { null }, descanso = descanso, observacoes = obs, metodo = metodo,
            video = "https://www.youtube.com/results?search_query=" +
                java.net.URLEncoder.encode("como fazer $nome", "UTF-8"),
        )

    private suspend fun ag(idAluno: Long, dias: Int, hora: String, tipo: TipoAgendamento, status: StatusAgendamento, nota: String?) {
        val (h, m) = hora.split(":").map { it.toInt() }
        agenda.salvar(
            Agendamento(
                idAluno = idAluno, data = LocalDate.now().plusDays(dias.toLong()),
                horario = LocalTime.of(h, m), tipo = tipo, status = status, anotacao = nota,
            ),
        )
    }

    private suspend fun avaliar(
        idAluno: Long, data: LocalDate, peso: Double, altura: Double, perc: Double,
        pas: Int, pad: Int, ldl: Double, hdl: Double, cintura: Double, quadril: Double,
        fotosCompletas: Boolean, parqSim: Boolean,
    ) {
        val gorda = ((peso * perc / 100) * 10).roundToInt() / 10.0
        val magra = ((peso - gorda) * 10).roundToInt() / 10.0
        // Dobras proporcionais ao % de gordura (valores plausíveis, só para teste)
        val fator = perc / 25.0
        val dobras = mapOf(
            Dobra.PEITORAL to 12.0 * fator, Dobra.AXILAR_MEDIA to 14.0 * fator, Dobra.TRICEPS to 18.0 * fator,
            Dobra.SUBESCAPULAR to 17.0 * fator, Dobra.ABDOMINAL to 24.0 * fator, Dobra.SUPRAILIACA to 20.0 * fator,
            Dobra.COXA to 22.0 * fator,
        ).mapValues { (it.value * 10).roundToInt() / 10.0 }
        val resp: List<Boolean?> = ParqRules.PERGUNTAS.indices.map { i -> if (parqSim && i == 0) true else false }
        val frente = if (fotosCompletas) foto(idAluno, "frente", "Frente", Color.rgb(38, 50, 56)) else null
        val costas = if (fotosCompletas) foto(idAluno, "costas", "Costas", Color.rgb(55, 71, 79)) else null
        val latDir = if (fotosCompletas) foto(idAluno, "lateral_dir", "Lateral dir.", Color.rgb(69, 90, 100)) else null
        val latEsq = if (fotosCompletas) foto(idAluno, "lateral_esq", "Lateral esq.", Color.rgb(84, 110, 122)) else null
        avaliacoes.salvar(
            AvaliacaoFisica(
                idAluno = idAluno, data = data, pesoKg = peso, alturaM = altura, percentualGordura = perc,
                massaMagraKg = magra, massaGordaKg = gorda, ldl = ldl, hdl = hdl,
                pressaoSistolica = pas, pressaoDiastolica = pad,
                dobrasCutaneas = dobras.entries.joinToString("; ") { "${it.key.rotulo}: ${it.value} mm" },
                perimetros = "Braço D: 31 cm; Braço E: 30,5 cm; Coxa D: 56 cm; Coxa E: 55,5 cm; Panturrilha: 37 cm; Tórax: 98 cm",
                anamnese = "Sem queixas atuais; sono de 7 h; hidratação regular.",
                condicoes = if (parqSim) setOf(CondicaoSaude.HIPERTENSAO) else emptySet(),
                historicoSaude = "Sem cirurgias recentes. Histórico familiar de diabetes tipo 2.",
                estiloVida = "Trabalho sentado; caminha 20 min/dia; alimentação com excesso de ultraprocessados.",
                metas = "Reduzir % de gordura e aumentar a força nos exercícios compostos.",
                postura = "Leve anteriorização de ombros; hiperlordose lombar discreta.",
                funcionalMobilidade = "Agachamento profundo com compensação em tornozelo; mobilidade de quadril boa.",
                cardio = "Teste de caminhada: FC repouso 68 bpm; recuperação em 1 min adequada.",
                forcaResistencia = "Flexão 14 reps; prancha 55 s; sentar e levantar 5x em 9 s.",
                fotoFrente = frente, fotoCostas = costas, fotoLateralDir = latDir, fotoLateralEsq = latEsq,
                protocoloDobras = ProtocoloDobras.POLLOCK_7, dobrasMm = dobras,
                cinturaCm = cintura, quadrilCm = quadril,
                parq = ParqRules.codificar(resp),
                assinatura = assinatura(idAluno),
            ),
        )
    }

    /** JPEG de teste (gradiente + texto "TESTE"), salvo na pasta de fotos do aluno. Devolve o caminho relativo. */
    private suspend fun foto(
        idAluno: Long, chave: String, rotulo: String, cor: Int,
        largura: Int = 600, altura: Int = 800, pasta: String = "fotos/$idAluno",
    ): String = withContext(Dispatchers.IO) {
        val bmp = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, altura.toFloat(), cor, Color.BLACK, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, largura.toFloat(), altura.toFloat(), p)
        p.shader = null
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.textSize = largura / 10f
        c.drawText("TESTE", largura / 2f, altura / 2f - 10f, p)
        p.textSize = largura / 14f
        c.drawText(rotulo, largura / 2f, altura / 2f + largura / 10f, p)
        val rel = "$pasta/teste_${chave}_${System.nanoTime()}.jpg"
        val arquivo = fotos.arquivo(rel).also { it.parentFile?.mkdirs() }
        arquivo.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        rel
    }

    private suspend fun assinatura(idAluno: Long): String {
        val bmp = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 80); style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND
        }
        val path = Path().apply {
            moveTo(40f, 150f)
            cubicTo(100f, 40f, 150f, 220f, 220f, 110f)
            cubicTo(270f, 40f, 320f, 200f, 380f, 100f)
            cubicTo(430f, 50f, 480f, 180f, 560f, 90f)
        }
        c.drawPath(path, p)
        return fotos.salvarAssinatura(bmp, idAluno)
    }
}
