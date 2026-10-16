package br.com.fitpersonal.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Nomes de colunas mantidos próximos do schema desktop para facilitar o importador do .db legado.

@Entity(tableName = "alunos", indices = [Index("nome")])
data class AlunoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val dataNascimento: String?,      // ISO yyyy-MM-dd
    val sexo: String?,
    val telefone: String?,
    val email: String?,
    val cpf: String?,
    val cep: String?,
    val instagram: String?,
    val objetivo: String?,
    val observacoesMedicas: String?,
    val fotoPerfil: String?,
    val dataCadastro: String,
    val consentimentoLgpdEm: String? = null,
    /** Meta do aluno: nome da [Metrica] e valor-alvo. */
    val metaMetrica: String? = null,
    val metaValor: Double? = null,
    /** Intervalo (dias) entre avaliações; null = sem reavaliação programada. */
    val reavaliacaoDias: Int? = null,
    val mensalidadeCentavos: Long? = null,
    val diaVencimento: Int? = null,
    val pacoteSessoes: Int? = null,
    val pacoteInicio: String? = null,
    /** Token aleatório que identifica o aluno nos arquivos trocados com o app do aluno. */
    val refContrato: String? = null,
)

@Entity(
    tableName = "pagamentos",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno", "competencia")],
)
data class PagamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val competencia: String,          // yyyy-MM
    val valorCentavos: Long,
    val pagoEm: String,               // ISO
)

/** Registra só as mudanças de carga de cada exercício do aluno (base da progressão). */
@Entity(
    tableName = "historico_carga",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno", "exercicio")],
)
data class HistoricoCargaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val exercicio: String,            // nome como digitado (exibição)
    val chave: String,                // CargaRules.chaveExercicio (agrupamento)
    val data: String,                 // ISO
    val cargaKg: Double,
)

@Entity(
    tableName = "agendamentos",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("data"), Index("idAluno")],
)
data class AgendamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val data: String,                 // ISO
    val horario: String,              // HH:mm
    val tipo: String,                 // enum name
    val status: String,
    val anotacao: String? = null,
)

@Entity(
    tableName = "avaliacoes_fisicas",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno")],
)
data class AvaliacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val data: String,
    val pesoKg: Double?,
    val alturaM: Double?,
    val percentualGordura: Double?,
    val massaMagraKg: Double?,
    val massaGordaKg: Double?,
    val ldl: Double?,
    val hdl: Double?,
    val pressaoSistolica: Int?,
    val pressaoDiastolica: Int?,
    val dobrasCutaneas: String?,
    val perimetros: String?,
    val anamnese: String?,
    val condicoes: String,            // nomes do enum separados por vírgula
    val historicoSaude: String?,
    val estiloVida: String?,
    val metas: String?,
    val postura: String?,
    val funcionalMobilidade: String?,
    val cardio: String?,
    val forcaResistencia: String?,
    val fotoFrente: String?,
    val fotoCostas: String?,
    val fotoLateralDir: String?,
    val fotoLateralEsq: String?,
    val protocoloDobras: String?,
    val dobraPeitoral: Double?,
    val dobraAxilarMedia: Double?,
    val dobraTriceps: Double?,
    val dobraSubescapular: Double?,
    val dobraAbdominal: Double?,
    val dobraSuprailiaca: Double?,
    val dobraCoxa: Double?,
    val cinturaCm: Double? = null,
    val quadrilCm: Double? = null,
    val parq: String? = null,
    val assinatura: String? = null,
)

@Entity(tableName = "modelos_treino")
data class ModeloTreinoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val objetivo: String?,
    val dataCriacao: String,
)

@Entity(
    tableName = "exercicios_modelo",
    foreignKeys = [ForeignKey(ModeloTreinoEntity::class, ["id"], ["idModelo"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idModelo")],
)
data class ModeloExercicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idModelo: Long,
    val nome: String,
    val divisao: String,
    val series: String?,
    val repeticoes: String?,
    val carga: String?,
    val descanso: String?,
    val observacoes: String?,
    /** Link de vídeo de demonstração (http/https). */
    val video: String? = null,
)

@Entity(
    tableName = "treinos",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno")],
)
data class TreinoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val nome: String,
    val objetivo: String?,
    val dataCriacao: String,
    /** Identificador estável do treino nos arquivos enviados ao aluno (gerado no primeiro envio). */
    val contratoId: String? = null,
    /** Última revisão enviada (sobe a cada novo envio). */
    val contratoRevisao: Int = 0,
)

@Entity(
    tableName = "exercicios_treino",
    foreignKeys = [ForeignKey(TreinoEntity::class, ["id"], ["idTreino"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idTreino")],
)
data class ExercicioTreinoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idTreino: Long,
    val nome: String,
    val divisao: String,
    val series: String?,
    val repeticoes: String?,
    val carga: String?,
    val descanso: String?,
    val observacoes: String?,
    /** Link de vídeo de demonstração (http/https). */
    val video: String? = null,
)

/** Catálogo normalizado (desktop guardava JSON bruto + LIKE). Busca por LIKE nas colunas; FTS pode vir depois. */
@Entity(tableName = "exercicios_catalogo", indices = [Index("nome"), Index("categoria")])
data class ExercicioCatalogoEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val categoria: String,
    val nivel: String,
    val equipamento: String,
    val musculosPrimarios: String,    // separados por ";"
    val musculosSecundarios: String,
    val instrucoes: String,           // separadas por "\n"
    val imagens: String,
)

@Entity(
    tableName = "lista_espera",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno")],
)
data class ListaEsperaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val diaSemana: Int,               // 1 = segunda … 7 = domingo (ISO)
    val horario: String?,             // HH:mm; null = qualquer horário
    val observacao: String?,
    val criadoEm: String,             // ISO
)

@Entity(
    tableName = "testes_aptidao",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno")],
)
data class TesteAptidaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val data: String,                 // ISO
    val tipo: String,                 // TipoTeste.name
    val valor: Double,
    val observacao: String?,
)

/** Execuções já importadas de resumos do aluno (evita duplicar ao importar o mesmo resumo de novo). */
@Entity(tableName = "execucoes_importadas")
data class ExecucaoImportadaEntity(
    @PrimaryKey val execucaoId: String,
    val idAluno: Long,
    val importadoEm: String,
)

/** Cada resumo importado: o que o aluno contou (sessões, esforço médio e mensagem). */
@Entity(
    tableName = "retornos_aluno",
    foreignKeys = [ForeignKey(AlunoEntity::class, ["id"], ["idAluno"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("idAluno")],
)
data class RetornoAlunoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idAluno: Long,
    val resumoId: String,
    val importadoEm: String,
    val de: String,
    val ate: String,
    val sessoes: Int,
    val esforcoMedio: Double?,
    val mensagem: String?,
)

/**
 * Vídeo próprio de um exercício, gravado ou escolhido pelo profissional. A chave é o nome normalizado
 * (CargaRules.chaveExercicio): o mesmo vídeo vale para todo treino/aluno com esse exercício.
 */
@Entity(tableName = "videos_exercicio")
data class VideoExercicioEntity(
    @PrimaryKey val chave: String,
    val nome: String,
    /** Caminho relativo a filesDir (videos/...). */
    val arquivo: String,
    val duracaoMs: Long,
    val tamanhoBytes: Long,
    val criadoEm: String,
)

/** Meta de composição corporal de um aluno para uma métrica (ex.: % de gordura = 18). Uma por aluno e métrica. */
@Entity(tableName = "metas_composicao", primaryKeys = ["idAluno", "metrica"])
data class MetaComposicaoEntity(
    val idAluno: Long,
    /** Nome do enum `Metrica`. */
    val metrica: String,
    val alvo: Double,
)
