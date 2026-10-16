package br.com.fitpersonal.app.data.backup

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.room.withTransaction
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.local.*
import br.com.fitpersonal.app.data.repo.PerfilRepository
import br.com.fitpersonal.app.domain.rules.DesktopLegacy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ResumoImportacao(
    val alunosNovos: Int = 0,
    val alunosReaproveitados: Int = 0,
    val avaliacoes: Int = 0,
    val treinos: Int = 0,
    val exercicios: Int = 0,
    val agendamentos: Int = 0,
    val fotos: Int = 0,
    /** Registros que já existiam no app e não foram duplicados. */
    val jaExistiam: Int = 0,
    /** Linhas inválidas (sem aluno, data ou nome) que não puderam ser importadas. */
    val ignorados: Int = 0,
    val perfilAtualizado: Boolean = false,
)

/**
 * Importa um backup do app desktop (.zip com data/rje_avaliacoes.db + media/, ou o .db solto).
 *
 * É ADITIVO e idempotente: não apaga nada do app; alunos já existentes (mesmo CPF, ou nome +
 * nascimento) são reaproveitados e avaliações/treinos/agendamentos repetidos são pulados.
 * Os usuários do desktop não são importados (o app Android tem um único perfil profissional).
 * Antes de gravar, um snapshot de segurança do estado atual é guardado.
 */
@Singleton
class ImportadorDesktop @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: FitDatabase,
    private val perfilRepo: PerfilRepository,
    private val fotos: FotoStorage,
    private val backup: BackupManager,
) {
    private companion object {
        const val DB_ENTRY = "data/rje_avaliacoes.db"
        const val SETTINGS_ENTRY = "data/settings.json"
        const val SQLITE_HEADER = "SQLite format 3\u0000"
    }

    /** Linha de cursor tolerante a colunas ausentes (backups de versões antigas do desktop). */
    private class Linha(private val c: Cursor) {
        fun str(n: String): String? {
            val i = c.getColumnIndex(n)
            return if (i < 0 || c.isNull(i)) null else c.getString(i)
        }
        fun lng(n: String): Long? = str(n)?.trim()?.toDoubleOrNull()?.toLong()
        fun dbl(n: String): Double? = str(n)?.trim()?.replace(',', '.')?.toDoubleOrNull()
    }

    private inline fun SQLiteDatabase.cadaLinha(tabela: String, bloco: (Linha) -> Unit) {
        val cursor = try {
            rawQuery("SELECT * FROM $tabela", null)
        } catch (e: SQLiteException) {
            return   // tabela inexistente em versões antigas
        }
        cursor.use { c ->
            val l = Linha(c)
            while (c.moveToNext()) bloco(l)
        }
    }

    suspend fun importar(origem: Uri): ResumoImportacao = withContext(Dispatchers.IO) {
        val tmp = File(ctx.cacheDir, "import_desktop").apply { deleteRecursively(); mkdirs() }
        val criadas = mutableListOf<String>()
        try {
            val dbFile = prepararOrigem(origem, tmp)
            backup.snapshotDeSeguranca("pre_importacao")

            val src = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                var resumo = ResumoImportacao()
                db.withTransaction { resumo = importarTabelas(src, tmp, criadas) }
                val perfilAtualizado = importarPerfil(src, tmp, criadas)
                resumo.copy(perfilAtualizado = perfilAtualizado)
            } finally {
                src.close()
            }
        } catch (e: Exception) {
            criadas.forEach(fotos::apagar)   // a transação foi revertida: não deixar fotos órfãs
            throw e
        } finally {
            tmp.deleteRecursively()
        }
    }

    // ───────────── origem ─────────────

    private fun prepararOrigem(origem: Uri, tmp: File): File {
        val bruto = File(tmp, "origem.bin")
        ctx.contentResolver.openInputStream(origem)?.use { i -> bruto.outputStream().use { i.copyTo(it) } }
            ?: throw IOException("Não foi possível abrir o arquivo")

        val cabecalho = bruto.inputStream().use { i -> ByteArray(16).also { i.read(it) } }.toString(Charsets.ISO_8859_1)
        if (cabecalho == SQLITE_HEADER) return bruto   // .db solto (sem fotos)

        val base = tmp.canonicalPath + File.separator
        var temDbAndroid = false
        try {
            ZipInputStream(bruto.inputStream().buffered()).use { zis ->
                while (true) {
                    val e = zis.nextEntry ?: break
                    val nome = e.name.replace('\\', '/')
                    if (nome == "rje_avaliacoes.db") temDbAndroid = true
                    val aceita = nome == DB_ENTRY || nome == SETTINGS_ENTRY ||
                        nome.startsWith("media/") || nome.startsWith("data/branding/")
                    if (e.isDirectory || !aceita) continue
                    val alvo = File(tmp, nome)
                    if (!alvo.canonicalPath.startsWith(base)) throw BackupInvalidoException("Arquivo de backup inválido.")
                    alvo.parentFile?.mkdirs()
                    alvo.outputStream().use { zis.copyTo(it) }
                }
            }
        } catch (e: ZipException) {
            throw BackupInvalidoException("O arquivo escolhido não é um .zip nem um banco válido.")
        } finally {
            bruto.delete()
        }

        val dbFile = File(tmp, DB_ENTRY)
        if (!dbFile.exists()) {
            throw BackupInvalidoException(
                if (temDbAndroid) "Este é um backup do app Android. Use \"Restaurar de um backup\"."
                else "O arquivo não contém o banco do RJE Avaliações (data/rje_avaliacoes.db)."
            )
        }
        val ok = dbFile.inputStream().use { i -> ByteArray(16).also { i.read(it) } }
            .toString(Charsets.ISO_8859_1) == SQLITE_HEADER
        if (!ok) throw BackupInvalidoException("O banco de dados do backup está corrompido.")
        return dbFile
    }

    // ───────────── dados ─────────────

    private suspend fun midia(tmp: File, gravado: String?, idAluno: Long, chave: String, criadas: MutableList<String>): String? {
        val rel = DesktopLegacy.caminhoNoZip(gravado) ?: return null
        val arquivo = File(tmp, rel)
        if (!arquivo.isFile || !arquivo.canonicalPath.startsWith(tmp.canonicalPath + File.separator)) return null
        return runCatching { fotos.importar(Uri.fromFile(arquivo), idAluno, chave) }
            .getOrNull()?.also { criadas += it }
    }

    private suspend fun importarTabelas(src: SQLiteDatabase, tmp: File, criadas: MutableList<String>): ResumoImportacao {
        val alunoDao = db.alunoDao()
        val avDao = db.avaliacaoDao()
        val treinoDao = db.treinoDao()
        val agDao = db.agendamentoDao()
        val hoje = LocalDate.now()

        var alunosNovos = 0; var alunosReaprov = 0; var avaliacoes = 0; var treinos = 0
        var exercicios = 0; var agendamentos = 0; var nFotos = 0; var jaExistiam = 0; var ignorados = 0

        // ── alunos ──
        val porChave = alunoDao.todosSnapshot()
            .associateBy { DesktopLegacy.chaveAluno(it.nome, it.dataNascimento?.let(LocalDate::parse), it.cpf) }
            .toMutableMap()
        val mapaAluno = mutableMapOf<Long, Long>()
        val alunosCriados = mutableSetOf<Long>()

        src.cadaLinha("alunos") { r ->
            val idOld = r.lng("id")
            val nome = r.str("nome")?.trim().orEmpty()
            if (idOld == null || nome.isEmpty()) { ignorados++; return@cadaLinha }

            val nasc = DesktopLegacy.data(r.str("data_nascimento"))
            val cpf = DesktopLegacy.digitosOuNulo(r.str("cpf"))
            val existente = porChave[DesktopLegacy.chaveAluno(nome, nasc, cpf)]

            if (existente != null) {
                mapaAluno[idOld] = existente.id
                alunosReaprov++
                if (existente.fotoPerfil == null) {
                    midia(tmp, r.str("foto_perfil"), existente.id, "perfil", criadas)?.let { rel ->
                        alunoDao.atualizar(existente.copy(fotoPerfil = rel)); nFotos++
                    }
                }
            } else {
                val entidade = AlunoEntity(
                    nome = nome,
                    dataNascimento = nasc?.toString(),
                    sexo = DesktopLegacy.sexo(r.str("sexo")),
                    telefone = DesktopLegacy.digitosOuNulo(r.str("telefone")),
                    email = r.str("email")?.trim()?.ifEmpty { null },
                    cpf = cpf,
                    cep = DesktopLegacy.digitosOuNulo(r.str("cep")),
                    instagram = r.str("instagram")?.trim()?.removePrefix("@")?.ifEmpty { null },
                    objetivo = r.str("objetivo")?.trim()?.ifEmpty { null },
                    observacoesMedicas = r.str("observacoes_medicas")?.trim()?.ifEmpty { null },
                    fotoPerfil = null,
                    dataCadastro = (DesktopLegacy.data(r.str("data_cadastro")) ?: hoje).toString(),
                )
                val novoId = alunoDao.inserir(entidade)
                var salvo = entidade.copy(id = novoId)
                midia(tmp, r.str("foto_perfil"), novoId, "perfil", criadas)?.let { rel ->
                    salvo = salvo.copy(fotoPerfil = rel)
                    alunoDao.atualizar(salvo); nFotos++
                }
                porChave[DesktopLegacy.chaveAluno(nome, nasc, cpf)] = salvo
                mapaAluno[idOld] = novoId
                alunosCriados += novoId
                alunosNovos++
            }
        }

        // ── avaliações ──
        val chavesAv = mutableMapOf<Long, MutableSet<String>>()
        src.cadaLinha("avaliacoes_fisicas") { r ->
            val idAluno = r.lng("id_aluno")?.let { mapaAluno[it] }
            val data = DesktopLegacy.data(r.str("data"))
            if (idAluno == null || data == null) { ignorados++; return@cadaLinha }

            val peso = r.dbl("peso"); val altura = r.dbl("altura")
            val chave = "$data|$peso|$altura"
            val vistos = chavesAv.getOrPut(idAluno) {
                if (idAluno in alunosCriados) mutableSetOf()
                else avDao.doAlunoSnapshot(idAluno).map { "${it.data}|${it.pesoKg}|${it.alturaM}" }.toMutableSet()
            }
            if (!vistos.add(chave)) { jaExistiam++; return@cadaLinha }

            val (condicoes, historico) = DesktopLegacy.separarCondicoes(r.str("historico_saude"))
            suspend fun foto(coluna: String, tipo: String): String? =
                midia(tmp, r.str(coluna), idAluno, tipo, criadas)?.also { nFotos++ }

            fun txt(n: String) = r.str(n)?.trim()?.ifEmpty { null }
            avDao.inserir(
                AvaliacaoEntity(
                    idAluno = idAluno, data = data.toString(),
                    pesoKg = peso, alturaM = altura,
                    percentualGordura = r.dbl("percentual_gordura"),
                    massaMagraKg = r.dbl("massa_magra"), massaGordaKg = r.dbl("massa_gorda"),
                    ldl = r.dbl("ldl"), hdl = r.dbl("hdl"),
                    pressaoSistolica = r.dbl("pressao_sistolica")?.toInt(),
                    pressaoDiastolica = r.dbl("pressao_diastolica")?.toInt(),
                    dobrasCutaneas = txt("dobras_cutaneas"), perimetros = txt("perimetros"), anamnese = txt("anamnese"),
                    condicoes = condicoes.joinToString(",") { it.name },
                    historicoSaude = historico, estiloVida = txt("estilo_vida"), metas = txt("metas"),
                    postura = txt("postura"), funcionalMobilidade = txt("funcional_mobilidade"),
                    cardio = txt("cardio"), forcaResistencia = txt("forca_resistencia"),
                    fotoFrente = foto("foto_frente", "frente"),
                    fotoCostas = foto("foto_costas", "costas"),
                    fotoLateralDir = foto("foto_lateral_dir", "lateral_dir"),
                    fotoLateralEsq = foto("foto_lateral_esq", "lateral_esq"),
                    protocoloDobras = null,
                    dobraPeitoral = null, dobraAxilarMedia = null, dobraTriceps = null, dobraSubescapular = null,
                    dobraAbdominal = null, dobraSuprailiaca = null, dobraCoxa = null,
                )
            )
            avaliacoes++
        }

        // ── treinos e exercícios ──
        val chavesTreino = mutableMapOf<Long, MutableSet<String>>()
        val mapaTreino = mutableMapOf<Long, Long>()
        src.cadaLinha("treinos") { r ->
            val idOld = r.lng("id")
            val idAluno = r.lng("id_aluno")?.let { mapaAluno[it] }
            val nome = r.str("nome_do_treino")?.trim().orEmpty()
            if (idOld == null || idAluno == null || nome.isEmpty()) { ignorados++; return@cadaLinha }

            val criacao = DesktopLegacy.data(r.str("data_criacao")) ?: hoje
            val vistos = chavesTreino.getOrPut(idAluno) {
                if (idAluno in alunosCriados) mutableSetOf()
                else treinoDao.doAlunoSnapshot(idAluno).map { "${it.nome}|${it.dataCriacao}" }.toMutableSet()
            }
            if (!vistos.add("$nome|$criacao")) { jaExistiam++; return@cadaLinha }

            mapaTreino[idOld] = treinoDao.inserir(
                TreinoEntity(
                    idAluno = idAluno, nome = nome,
                    objetivo = r.str("objetivo")?.trim()?.ifEmpty { null },
                    dataCriacao = criacao.toString(),
                )
            )
            treinos++
        }

        src.cadaLinha("exercicios_treino") { r ->
            val idTreino = r.lng("id_treino")?.let { mapaTreino[it] } ?: return@cadaLinha   // treino pulado/duplicado
            val nome = r.str("nome_exercicio")?.trim().orEmpty()
            if (nome.isEmpty()) { ignorados++; return@cadaLinha }
            val divisao = r.str("divisao")?.trim()?.uppercase()?.take(1)?.takeIf { it in listOf("A", "B", "C", "D") } ?: "A"
            fun txt(n: String) = r.str(n)?.trim()?.ifEmpty { null }
            treinoDao.inserirExercicio(
                ExercicioTreinoEntity(
                    idTreino = idTreino, nome = nome, divisao = divisao,
                    series = txt("series"), repeticoes = txt("repeticoes"), carga = txt("carga"),
                    descanso = txt("descanso"), observacoes = txt("observacoes"),
                )
            )
            exercicios++
        }

        // ── agendamentos ──
        val chavesAg = mutableMapOf<Long, MutableSet<String>>()
        src.cadaLinha("agendamentos") { r ->
            val idAluno = r.lng("id_aluno")?.let { mapaAluno[it] }
            val data = DesktopLegacy.data(r.str("data"))
            val horario = DesktopLegacy.horario(r.str("horario"))
            val tipo = DesktopLegacy.tipo(r.str("tipo"))
            val status = DesktopLegacy.status(r.str("status")) ?: br.com.fitpersonal.app.domain.model.StatusAgendamento.PENDENTE
            if (idAluno == null || data == null || horario == null || tipo == null) { ignorados++; return@cadaLinha }

            val chave = "$data|$horario|${tipo.name}"
            val vistos = chavesAg.getOrPut(idAluno) {
                if (idAluno in alunosCriados) mutableSetOf()
                else agDao.doAlunoSnapshot(idAluno).map { "${it.data}|${it.horario}|${it.tipo}" }.toMutableSet()
            }
            if (!vistos.add(chave)) { jaExistiam++; return@cadaLinha }

            agDao.inserir(
                AgendamentoEntity(
                    idAluno = idAluno, data = data.toString(), horario = horario.toString(),
                    tipo = tipo.name, status = status.name,
                )
            )
            agendamentos++
        }

        return ResumoImportacao(
            alunosNovos, alunosReaprov, avaliacoes, treinos, exercicios, agendamentos, nFotos, jaExistiam, ignorados,
        )
    }

    // ───────────── perfil ─────────────

    /** Preenche apenas campos em branco do perfil atual; nunca sobrescreve o que o usuário já informou. */
    private suspend fun importarPerfil(src: SQLiteDatabase, tmp: File, criadas: MutableList<String>): Boolean {
        val atual = perfilRepo.perfil.first()

        val settings = File(tmp, SETTINGS_ENTRY).takeIf { it.isFile }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }

        var nome = settings?.optString("professor_nome").orEmpty()
        var cref = settings?.optString("contato_cref").orEmpty()
        var telefone = settings?.optString("contato_telefone").orEmpty()
        var cidade = settings?.optString("cidade").orEmpty()
        val email = settings?.optString("contato_email").orEmpty()
        val marca = settings?.optString("marca_nome").orEmpty()

        // Complemento: primeiro usuário ativo do desktop (admin primeiro).
        runCatching {
            src.rawQuery("SELECT * FROM usuarios WHERE ativo = 1 ORDER BY is_admin DESC, id LIMIT 1", null).use { c ->
                if (c.moveToFirst()) {
                    val l = Linha(c)
                    if (nome.isBlank()) nome = l.str("nome").orEmpty()
                    if (cref.isBlank()) cref = l.str("cref").orEmpty()
                    if (telefone.isBlank()) telefone = l.str("telefone").orEmpty()
                    if (cidade.isBlank()) cidade = l.str("cidade").orEmpty()
                }
            }
        }

        var logo = atual.logo
        if (logo == null) {
            val rel = DesktopLegacy.caminhoNoZip(settings?.optString("logo_path"))
            val f = rel?.let { File(tmp, it) }
            if (f != null && f.isFile && f.canonicalPath.startsWith(tmp.canonicalPath + File.separator)) {
                logo = runCatching { fotos.importarLogo(Uri.fromFile(f)) }.getOrNull()?.also { criadas += it }
            }
        }

        val novo = atual.copy(
            nome = atual.nome.ifBlank { nome.trim() },
            marca = atual.marca.ifBlank { marca.trim() },
            cref = atual.cref.ifBlank { cref.trim() },
            email = atual.email.ifBlank { email.trim() },
            telefone = atual.telefone.ifBlank { DesktopLegacy.digitos(telefone) },
            cidade = atual.cidade.ifBlank { cidade.trim() },
            logo = logo,
        )
        if (novo == atual) return false
        perfilRepo.salvar(novo)
        return true
    }
}
