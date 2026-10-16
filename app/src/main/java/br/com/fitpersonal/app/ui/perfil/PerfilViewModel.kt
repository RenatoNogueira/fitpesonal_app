package br.com.fitpersonal.app.ui.perfil

import br.com.fitpersonal.app.core.TratadorDeErros
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.backup.BackupInvalidoException
import br.com.fitpersonal.app.data.backup.BackupManager
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.repo.PerfilRepository
import br.com.fitpersonal.app.domain.model.PerfilProfissional
import br.com.fitpersonal.app.domain.rules.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import br.com.fitpersonal.app.data.crypto.SenhaIncorretaException
import br.com.fitpersonal.app.domain.rules.SenhaBackupRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class CampoPerfil { EMAIL, TELEFONE }

data class PerfilState(
    val carregando: Boolean = true,
    val perfil: PerfilProfissional = PerfilProfissional(),
    val erros: Map<CampoPerfil, String> = emptyMap(),
    val alterado: Boolean = false,
    /** Texto de progresso enquanto backup/restauração rodam. */
    val ocupado: String? = null,
    val mensagem: String? = null,
    /** Restauração concluída: o app precisa ser fechado e reaberto. */
    val restauracaoConcluida: Boolean = false,
    /** Resultado da última importação do desktop (exibido em diálogo até ser dispensado). */
    val resumoImportacao: br.com.fitpersonal.app.data.backup.ResumoImportacao? = null,
    val config: br.com.fitpersonal.app.data.repo.Configuracoes = br.com.fitpersonal.app.data.repo.Configuracoes(),
    /** Há senha de backup guardada (sem ela, backups manuais e automáticos ficam bloqueados). */
    val senhaBackupDefinida: Boolean = false,
    /** Backup cifrado selecionado para restaurar que aguarda a senha do usuário. */
    val pedirSenhaPara: Uri? = null,
    /** Zip de planilhas pronto para compartilhar (consumido pela tela). */
    val planilha: java.io.File? = null,
    /** Conflito de sincronização aguardando a escolha do usuário. */
    val conflitoSync: br.com.fitpersonal.app.data.backup.MetaSync? = null,
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val repo: PerfilRepository,
    private val fotos: FotoStorage,
    private val backup: BackupManager,
    private val importador: br.com.fitpersonal.app.data.backup.ImportadorDesktop,
    private val configRepo: br.com.fitpersonal.app.data.repo.ConfiguracoesRepository,
    private val trabalhos: br.com.fitpersonal.app.work.TrabalhosAgendados,
    private val lembretes: br.com.fitpersonal.app.work.LembreteAgendador,
    private val automatico: br.com.fitpersonal.app.data.backup.BackupAutomatico,
    private val exportacao: br.com.fitpersonal.app.data.privacidade.ExportacaoService,
    private val dadosTeste: br.com.fitpersonal.app.data.teste.DadosDeTeste,
    private val sincronizacao: br.com.fitpersonal.app.data.backup.SincronizacaoPasta,
) : ViewModel() {

    // ── sincronização entre aparelhos (pasta compartilhada) ──
    fun sincronizar() = rodarSync { sincronizacao.sincronizar() }
    fun resolverConflito(usarPasta: Boolean) {
        _state.update { it.copy(conflitoSync = null) }
        rodarSync { if (usarPasta) sincronizacao.usarDaPasta() else sincronizacao.enviarEsteAparelho() }
    }
    fun cancelarConflito() = _state.update { it.copy(conflitoSync = null) }

    private fun rodarSync(bloco: suspend () -> br.com.fitpersonal.app.data.backup.ResultadoSync) {
        _state.update { it.copy(ocupado = "Sincronizando…") }
        viewModelScope.launch(TratadorDeErros) {
            val r = bloco()
            _state.update { s ->
                when (r) {
                    is br.com.fitpersonal.app.data.backup.ResultadoSync.JaSincronizado ->
                        s.copy(ocupado = null, mensagem = "Tudo já está sincronizado.")
                    is br.com.fitpersonal.app.data.backup.ResultadoSync.Enviado ->
                        s.copy(ocupado = null, mensagem = "Dados enviados para a pasta (versão ${r.versao}).")
                    is br.com.fitpersonal.app.data.backup.ResultadoSync.Recebido ->
                        s.copy(ocupado = null, restauracaoConcluida = true)
                    is br.com.fitpersonal.app.data.backup.ResultadoSync.Conflito ->
                        s.copy(ocupado = null, conflitoSync = r.remoto)
                    is br.com.fitpersonal.app.data.backup.ResultadoSync.Erro ->
                        s.copy(ocupado = null, mensagem = r.mensagem)
                }
            }
        }
    }

    /** Só em build de depuração: carrega/remove dados fictícios completos para testes. */
    val testeDisponivel: Boolean get() = dadosTeste.disponivel

    fun popularDadosDeTeste() {
        _state.update { it.copy(ocupado = "Criando dados de teste…") }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { dadosTeste.popular() }
                .onSuccess { n ->
                    val msg = if (n > 0) "$n alunos de teste criados, com avaliações, treinos, agenda e pagamentos."
                    else "Os dados de teste já existem. Remova-os antes de criar de novo."
                    _state.update { it.copy(ocupado = null, mensagem = msg) }
                }
                .onFailure { e -> _state.update { it.copy(ocupado = null, mensagem = "Falha nos dados de teste: ${e.message}") } }
        }
    }

    fun removerDadosDeTeste() {
        _state.update { it.copy(ocupado = "Removendo dados de teste…") }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { dadosTeste.remover() }
                .onSuccess { n -> _state.update { it.copy(ocupado = null, mensagem = "$n alunos de teste removidos.") } }
                .onFailure { e -> _state.update { it.copy(ocupado = null, mensagem = "Falha ao remover: ${e.message}") } }
        }
    }

    /** Muda o app para o modo aluno. Os dados do professor continuam guardados e voltam ao trocar de novo. */
    fun trocarParaAluno() {
        viewModelScope.launch(TratadorDeErros) { configRepo.definirUsoApp(br.com.fitpersonal.app.domain.rules.UsoApp.ALUNO) }
    }

    fun exportarPlanilhas() {
        _state.update { it.copy(ocupado = "Gerando planilhas…") }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { exportacao.exportarPlanilhas() }
                .onSuccess { f -> _state.update { it.copy(ocupado = null, planilha = f) } }
                .onFailure { e ->
                    _state.update { it.copy(ocupado = null, mensagem = "Não foi possível exportar: ${e.message}") }
                }
        }
    }

    fun planilhaConsumida() = _state.update { it.copy(planilha = null) }

    private val _state = MutableStateFlow(PerfilState())
    val state: StateFlow<PerfilState> = _state

    private var logoPersistida: String? = null
    /** Logo importada nesta sessão e ainda não salva (apagada se o usuário sair sem salvar). */
    private var logoNova: String? = null
    private var salvou = false

    init {
        viewModelScope.launch(TratadorDeErros) {
            configRepo.config.collect { c -> _state.update { it.copy(config = c) } }
        }
    }

    init {
        viewModelScope.launch(TratadorDeErros) {
            val definida = withContext(Dispatchers.IO) { backup.temSenha() }
            _state.update { it.copy(senhaBackupDefinida = definida) }
        }
    }

    init {
        viewModelScope.launch(TratadorDeErros) {
            val p = repo.perfil.first()
            logoPersistida = p.logo
            _state.update { it.copy(carregando = false, perfil = p) }
        }
    }

    fun alterar(campo: CampoPerfil? = null, transform: (PerfilProfissional) -> PerfilProfissional) {
        _state.update { s ->
            s.copy(perfil = transform(s.perfil), alterado = true, erros = campo?.let { s.erros - it } ?: s.erros)
        }
    }

    fun arquivoLogo(rel: String): File = fotos.arquivo(rel)
    fun mensagemLida() = _state.update { it.copy(mensagem = null) }

    // ── logo ──
    fun definirLogo(uri: Uri, recorte: android.graphics.RectF? = null) {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { fotos.importarLogo(uri, recorte) }
                .onSuccess { rel ->
                    logoNova?.let(fotos::apagar)
                    logoNova = rel
                    salvou = false
                    alterar { it.copy(logo = rel) }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível usar a imagem: ${e.message}") } }
        }
    }

    fun removerLogo() {
        logoNova?.let(fotos::apagar)
        logoNova = null
        alterar { it.copy(logo = null) }
    }

    // ── salvar ──
    fun salvar() {
        val p = _state.value.perfil
        val erros = buildMap<CampoPerfil, String> {
            if (p.email.isNotBlank() && !Validators.emailValido(p.email)) put(CampoPerfil.EMAIL, "E-mail inválido.")
            if (p.telefone.isNotBlank() && !Validators.telefoneValido(p.telefone))
                put(CampoPerfil.TELEFONE, "Informe DDD + número (10 ou 11 dígitos).")
        }
        if (erros.isNotEmpty()) {
            _state.update { it.copy(erros = erros) }
            return
        }
        val limpo = p.copy(
            nome = p.nome.trim(), marca = p.marca.trim(), cref = p.cref.trim(),
            email = p.email.trim(), cidade = p.cidade.trim(),
            chavePix = br.com.fitpersonal.app.domain.rules.PixRules.normalizarChave(p.chavePix),
        )
        viewModelScope.launch(TratadorDeErros) {
            repo.salvar(limpo)
            if (logoPersistida != null && logoPersistida != limpo.logo) fotos.apagar(logoPersistida)
            logoPersistida = limpo.logo
            logoNova = null
            salvou = true
            _state.update { it.copy(perfil = limpo, alterado = false, mensagem = "Perfil salvo") }
        }
    }

    // ── backup ──
    fun nomeBackup() = backup.nomeSugerido()

    fun exportar(destino: Uri) {
        _state.update { it.copy(ocupado = "Criando backup…") }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { backup.exportar(destino, incluirVideos = true) }
                .onSuccess { r ->
                    val mb = r.bytes / 1_048_576.0
                    _state.update { it.copy(ocupado = null, mensagem = "Backup criado (${r.arquivos} arquivos, %.1f MB)".format(mb)) }
                }
                .onFailure { e -> _state.update { it.copy(ocupado = null, mensagem = "Falha no backup: ${e.message}") } }
        }
    }

    fun importarDesktop(origem: Uri) {
        _state.update { it.copy(ocupado = "Importando dados do desktop…") }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { importador.importar(origem) }
                .onSuccess { r ->
                    runCatching { lembretes.reagendarTodos() }   // novos agendamentos importados
                    // O perfil pode ter sido preenchido pela importação: recarrega se não há edição pendente.
                    val atualizado = repo.perfil.first()
                    if (!_state.value.alterado) logoPersistida = atualizado.logo
                    _state.update {
                        it.copy(
                            ocupado = null, resumoImportacao = r,
                            perfil = if (it.alterado) it.perfil else atualizado,
                        )
                    }
                }
                .onFailure { e ->
                    val msg = if (e is BackupInvalidoException) e.message else "Falha na importação: ${e.message}"
                    _state.update { it.copy(ocupado = null, mensagem = msg) }
                }
        }
    }

    // ── backup automático e lembretes ──
    fun definirBackupAtivo(ativo: Boolean) {
        viewModelScope.launch(TratadorDeErros) {
            configRepo.definirBackupAtivo(ativo)
            trabalhos.sincronizarBackupDiario(ativo)
        }
    }

    fun definirPasta(uri: String?, nome: String) {
        viewModelScope.launch(TratadorDeErros) { configRepo.definirPasta(uri, nome) }
    }

    fun backupAgora() {
        _state.update { it.copy(ocupado = "Criando backup…") }
        viewModelScope.launch(TratadorDeErros) {
            val r = automatico.executar()
            _state.update { it.copy(ocupado = null, mensagem = r.mensagem) }
        }
    }

    fun definirReavaliacaoAvisos(ativo: Boolean) {
        viewModelScope.launch(TratadorDeErros) {
            configRepo.definirReavaliacaoAvisos(ativo)
            trabalhos.sincronizarAvisoReavaliacao(ativo)
        }
    }

    fun definirBloqueio(ativo: Boolean) {
        viewModelScope.launch(TratadorDeErros) { configRepo.definirBloqueio(ativo) }
    }

    fun definirAnalitica(ativo: Boolean) {
        viewModelScope.launch(TratadorDeErros) { configRepo.definirAnalitica(ativo) }
    }

    fun definirBloquearCaptura(ativo: Boolean) {
        viewModelScope.launch(TratadorDeErros) { configRepo.definirBloquearCaptura(ativo) }
    }

    fun definirTema(tema: br.com.fitpersonal.app.domain.rules.TemaApp) {
        viewModelScope.launch(TratadorDeErros) { configRepo.definirTema(tema) }
    }

    fun definirLembretes(ativo: Boolean) {
        viewModelScope.launch(TratadorDeErros) {
            configRepo.definirLembretes(ativo)
            lembretes.reagendarTodos()
        }
    }

    fun definirAntecedencia(min: Int) {
        viewModelScope.launch(TratadorDeErros) {
            configRepo.definirAntecedencia(min)
            lembretes.reagendarTodos()
        }
    }

    fun avisar(texto: String) = _state.update { it.copy(mensagem = texto) }

    fun resumoImportacaoVisto() = _state.update { it.copy(resumoImportacao = null) }

    /** Valida e grava a senha do backup. Retorna a mensagem de erro (para o diálogo) ou null se salvou. */
    fun definirSenhaBackup(senha: String, confirmacao: String): String? {
        SenhaBackupRules.validar(senha, confirmacao)?.let { return it }
        val chars = senha.toCharArray()
        viewModelScope.launch(TratadorDeErros) {
            withContext(Dispatchers.IO) { backup.definirSenha(chars) }
            chars.fill('0')
            _state.update { it.copy(senhaBackupDefinida = true, mensagem = "Senha do backup definida") }
        }
        return null
    }

    fun cancelarSenhaRestauracao() = _state.update { it.copy(pedirSenhaPara = null) }

    /** Restaura de [origem]; se o arquivo for cifrado e [senha] ainda não foi informada, pede a senha. */
    fun restaurar(origem: Uri, senha: CharArray? = null) {
        _state.update { it.copy(ocupado = "Restaurando backup…", pedirSenhaPara = null) }
        viewModelScope.launch(TratadorDeErros) {
            val cifrado = runCatching { backup.ehCifrado(origem) }.getOrDefault(false)
            if (cifrado && senha == null) {
                _state.update { it.copy(ocupado = null, pedirSenhaPara = origem) }
                return@launch
            }
            runCatching { backup.restaurar(origem, senha) }
                .onSuccess { _state.update { it.copy(ocupado = null, restauracaoConcluida = true) } }
                .onFailure { e ->
                    senha?.fill('0')
                    when (e) {
                        is SenhaIncorretaException ->
                            _state.update { it.copy(ocupado = null, pedirSenhaPara = origem, mensagem = "Senha incorreta.") }
                        is BackupInvalidoException ->
                            _state.update { it.copy(ocupado = null, mensagem = e.message) }
                        else ->
                            _state.update { it.copy(ocupado = null, mensagem = "Falha na restauração: ${e.message}") }
                    }
                }
        }
    }

    override fun onCleared() {
        if (!salvou) logoNova?.let(fotos::apagar)
    }
}
