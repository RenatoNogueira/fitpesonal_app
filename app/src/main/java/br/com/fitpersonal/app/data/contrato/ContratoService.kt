package br.com.fitpersonal.app.data.contrato

import android.content.Context
import br.com.fitpersonal.app.data.local.AlunoDao
import br.com.fitpersonal.app.data.local.TreinoDao
import br.com.fitpersonal.app.data.repo.PerfilRepository
import br.com.fitpersonal.app.domain.rules.Saudacao
import br.com.fitpersonal.contrato.AlunoRef
import br.com.fitpersonal.contrato.CodigoTexto
import br.com.fitpersonal.contrato.ContratoJson
import br.com.fitpersonal.contrato.ExercicioRef
import br.com.fitpersonal.contrato.PacoteTreino
import br.com.fitpersonal.contrato.ProfissionalRef
import br.com.fitpersonal.contrato.TreinoRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Treino pronto para ir ao app do aluno: arquivo `.fittreino` e o mesmo conteúdo como código de texto. */
data class PacoteGerado(
    val arquivo: File,
    val codigo: String,
    val revisao: Int,
)

/** Pacote "treino + vídeos" pronto para enviar ao aluno (arquivo `.fittreinopack`). */
data class PacoteComVideosGerado(
    val arquivo: File,
    val revisao: Int,
    val videos: Int,
    val tamanhoBytes: Long,
)

/** Monta o `.fittreino` de um treino salvo (gera os identificadores do contrato na primeira vez). */
@Singleton
class ContratoService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val alunos: AlunoDao,
    private val treinos: TreinoDao,
    private val catalogo: br.com.fitpersonal.app.data.local.CatalogoDao,
    private val perfil: PerfilRepository,
    private val videos: br.com.fitpersonal.app.data.files.VideoExercicioService,
) {

    suspend fun gerar(idTreino: Long, hoje: LocalDate = LocalDate.now()): PacoteGerado = withContext(Dispatchers.IO) {
        val t = treinos.buscar(idTreino) ?: error("Treino não encontrado")
        val a = alunos.buscar(t.idAluno) ?: error("Aluno não encontrado")
        val p = perfil.perfil.first()

        val refAluno = a.refContrato ?: UUID.randomUUID().toString().also { alunos.definirRefContrato(a.id, it) }
        val treinoId = t.contratoId ?: UUID.randomUUID().toString()
        val revisao = t.contratoRevisao + 1
        treinos.definirContrato(t.id, treinoId, revisao)

        val porDivisao = treinos.exerciciosSnapshot(idTreino).groupBy { it.divisao }.mapValues { (_, lista) ->
            lista.map {
                ExercicioRef(
                    nome = it.nome.trim().ifEmpty { "Exercício" },
                    series = it.series, repeticoes = it.repeticoes, carga = it.carga,
                    descanso = it.descanso, obs = it.observacoes, video = it.video, metodo = it.metodo,
                    catalogoId = it.catalogoId ?: catalogo.porNome(it.nome.trim())?.id,
                )
            }
        }

        val pacote = PacoteTreino(
            treinoId = treinoId,
            revisao = revisao,
            emitidoEm = hoje.toString(),
            validoAte = null,
            profissional = ProfissionalRef(
                nome = p.nome.trim().ifEmpty { "Profissional" },
                marca = p.marca.trim(),
                cref = p.cref.trim(),
                contato = p.telefone.trim(),
            ),
            // Minimização: só o primeiro nome do aluno vai no arquivo.
            aluno = AlunoRef(nome = Saudacao.primeiroNome(a.nome).ifEmpty { "Aluno" }, ref = refAluno),
            treino = TreinoRef(nome = t.nome.trim().ifEmpty { "Treino" }, objetivo = t.objetivo, divisoes = porDivisao),
        )

        val json = ContratoJson.serializar(pacote)
        val pasta = File(ctx.cacheDir, "pdf").apply { mkdirs() }
        val arquivo = File(pasta, "treino_${slug(a.nome)}_${slug(t.nome)}.fittreino")
        arquivo.writeText(json, Charsets.UTF_8)
        PacoteGerado(arquivo, CodigoTexto.codificar(json), revisao)
    }

    /**
     * Gera o treino (como [gerar]) e o empacota com os vídeos próprios dos exercícios que os têm.
     * Lança IllegalStateException com mensagem amigável se nenhum exercício tiver vídeo próprio
     * ou se o pacote passar de [LIMITE_BYTES].
     */
    suspend fun gerarComVideos(idTreino: Long): PacoteComVideosGerado = withContext(Dispatchers.IO) {
        val locais = videos.todosSnapshot()
        val exercicios = treinos.exerciciosSnapshot(idTreino)
        val porChave = LinkedHashMap<String, MutableList<String>>()
        exercicios.forEach { e ->
            val chave = br.com.fitpersonal.app.domain.rules.CargaRules.chaveExercicio(e.nome)
            if (chave in locais) porChave.getOrPut(chave) { mutableListOf() } += e.nome.trim().ifEmpty { "Exercício" }
        }
        val lista = porChave.mapNotNull { (chave, nomes) ->
            val arquivo = videos.arquivoDe(locais.getValue(chave))
            if (arquivo.exists()) {
                br.com.fitpersonal.contrato.PacoteVideos.VideoParaPacote(nomes.distinct(), arquivo)
            } else null
        }
        if (lista.isEmpty()) {
            throw IllegalStateException(
                "Nenhum exercício deste treino tem vídeo próprio. Adicione vídeos no editor do exercício.",
            )
        }
        val soma = lista.sumOf { it.arquivo.length() }
        if (soma > LIMITE_BYTES) {
            throw IllegalStateException(
                "Os vídeos somam ${soma / 1_048_576} MB (limite ${LIMITE_BYTES / 1_048_576} MB). " +
                    "Envie parte por link ou remova vídeos.",
            )
        }

        val base = gerar(idTreino)
        val destino = File(base.arquivo.parentFile, base.arquivo.nameWithoutExtension + br.com.fitpersonal.contrato.PacoteVideos.EXTENSAO)
        br.com.fitpersonal.contrato.PacoteVideos.escrever(destino, base.arquivo.readText(Charsets.UTF_8), lista)
        PacoteComVideosGerado(destino, base.revisao, lista.size, destino.length())
    }

    private companion object {
        /** Tamanho máximo de vídeos por pacote (o app do aluno aceita até 600 MB). */
        const val LIMITE_BYTES = 300L * 1024 * 1024
    }

    private fun slug(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_').lowercase().take(30).ifEmpty { "treino" }
}
