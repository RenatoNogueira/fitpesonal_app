package br.com.fitpersonal.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class TipoAgendamento(val rotulo: String) {
    AVALIACAO("Avaliação"), TREINO("Treino"), CONSULTORIA("Consultoria")
}

enum class StatusAgendamento(val rotulo: String) {
    PENDENTE("Pendente"), CONCLUIDO("Concluído"), CANCELADO("Cancelado"), FALTOU("Faltou")
}

enum class DivisaoTreino { A, B, C, D }

data class Aluno(
    val id: Long = 0,
    val nome: String,
    val dataNascimento: LocalDate? = null,
    val sexo: String? = null,
    val telefone: String? = null,
    val email: String? = null,
    val cpf: String? = null,          // somente dígitos
    val cep: String? = null,          // somente dígitos
    val instagram: String? = null,
    val objetivo: String? = null,
    val observacoesMedicas: String? = null,
    val fotoPerfil: String? = null,   // caminho relativo em filesDir
    val dataCadastro: LocalDate = LocalDate.now(),
    /** Data em que o aluno consentiu (LGPD) com o tratamento de seus dados de saúde; null = não registrado. */
    val consentimentoLgpdEm: LocalDate? = null,
    /** Nome da métrica da meta (enum `Metrica`) e valor-alvo; ambos null = sem meta. */
    val metaMetrica: String? = null,
    val metaValor: Double? = null,
    /** Intervalo de reavaliação em dias; null = não programada. */
    val reavaliacaoDias: Int? = null,
    /** Mensalidade em centavos e dia do vencimento (1–31); ambos null = sem cobrança. */
    val mensalidadeCentavos: Long? = null,
    val diaVencimento: Int? = null,
    /** Pacote de sessões: quantidade contratada e data de início da contagem. */
    val pacoteSessoes: Int? = null,
    val pacoteInicio: LocalDate? = null,
    /** Token do aluno nos arquivos do contrato (null até o primeiro envio). */
    val refContrato: String? = null,
)

data class Pagamento(
    val id: Long = 0,
    val idAluno: Long,
    /** Mês a que o pagamento se refere (yyyy-MM). */
    val competencia: java.time.YearMonth,
    val valorCentavos: Long,
    val pagoEm: LocalDate = LocalDate.now(),
)

enum class CondicaoSaude(val rotulo: String) {
    DIABETES("Diabetes"), HIPERTENSAO("Hipertensão"), LESOES("Lesões"), CIRURGIAS("Cirurgias")
}

data class AvaliacaoFisica(
    val id: Long = 0,
    val idAluno: Long,
    val data: LocalDate = LocalDate.now(),
    val pesoKg: Double? = null,
    val alturaM: Double? = null,
    val percentualGordura: Double? = null,
    val massaMagraKg: Double? = null,
    val massaGordaKg: Double? = null,
    val ldl: Double? = null,
    val hdl: Double? = null,
    val pressaoSistolica: Int? = null,
    val pressaoDiastolica: Int? = null,
    val dobrasCutaneas: String? = null,
    val perimetros: String? = null,
    val anamnese: String? = null,
    val condicoes: Set<CondicaoSaude> = emptySet(),   // antes: prefixo de texto em historico_saude
    val historicoSaude: String? = null,
    val estiloVida: String? = null,
    val metas: String? = null,
    val postura: String? = null,
    val funcionalMobilidade: String? = null,
    val cardio: String? = null,
    val forcaResistencia: String? = null,
    val fotoFrente: String? = null,
    val fotoCostas: String? = null,
    val fotoLateralDir: String? = null,
    val fotoLateralEsq: String? = null,
    val protocoloDobras: ProtocoloDobras? = null,
    val dobrasMm: Map<Dobra, Double> = emptyMap(),
    val cinturaCm: Double? = null,
    val quadrilCm: Double? = null,
    /** Respostas do PAR-Q codificadas (ver `ParqRules`); null = não aplicado. */
    val parq: String? = null,
    /** Caminho relativo da assinatura do aluno (PNG); null = sem assinatura. */
    val assinatura: String? = null,
)

/** Interesse de um aluno em um dia/horário que está sem vaga. */
data class ListaEspera(
    val id: Long = 0,
    val idAluno: Long,
    val diaSemana: java.time.DayOfWeek,
    /** Horário preferido; null = qualquer horário do dia. */
    val horario: LocalTime? = null,
    val observacao: String? = null,
    val criadoEm: LocalDate = LocalDate.now(),
)

/** Treino reutilizável, sem aluno: aplicado a um aluno gera um treino novo. */
data class ModeloTreino(
    val id: Long = 0,
    val nome: String,
    val objetivo: String? = null,
    val dataCriacao: LocalDate = LocalDate.now(),
    val totalExercicios: Int = 0,
)

data class Treino(
    val id: Long = 0,
    val idAluno: Long,
    val nome: String,
    val objetivo: String? = null,
    val dataCriacao: LocalDate = LocalDate.now(),
)

data class ExercicioTreino(
    val id: Long = 0,
    val idTreino: Long,
    val nome: String,
    val divisao: DivisaoTreino = DivisaoTreino.A,
    val series: String? = null,
    val repeticoes: String? = null,
    val carga: String? = null,
    val descanso: String? = null,
    val observacoes: String? = null,
    /** Link de vídeo de demonstração (http/https, já normalizado por VideoRules). */
    val video: String? = null,
)

data class ExercicioCatalogo(
    val id: String,
    val nome: String,
    val categoria: String,
    val nivel: String,
    val equipamento: String,
    val musculosPrimarios: List<String>,
    val musculosSecundarios: List<String>,
    val instrucoes: List<String>,
    val imagens: List<String>,
)

data class Agendamento(
    val id: Long = 0,
    val idAluno: Long,
    val data: LocalDate,
    val horario: LocalTime,
    val tipo: TipoAgendamento,
    val status: StatusAgendamento = StatusAgendamento.PENDENTE,
    /** Anotação livre da sessão (o que foi feito, cargas, observações). */
    val anotacao: String? = null,
)

data class PerfilProfissional(
    val nome: String = "",
    val marca: String = "",
    val cref: String = "",
    val email: String = "",
    val telefone: String = "",
    val cidade: String = "",
    /** Chave Pix usada nas cobranças (CPF/CNPJ, e-mail, telefone com +55 ou chave aleatória). */
    val chavePix: String = "",
    val logo: String? = null,
)
