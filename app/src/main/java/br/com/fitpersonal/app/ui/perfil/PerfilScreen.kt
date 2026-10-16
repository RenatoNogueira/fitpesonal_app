package br.com.fitpersonal.app.ui.perfil

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import br.com.fitpersonal.app.domain.rules.TemaApp
import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.documentfile.provider.DocumentFile
import br.com.fitpersonal.app.domain.rules.LembreteRules
import br.com.fitpersonal.app.work.Notificador
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.DialogoSenha
import br.com.fitpersonal.app.ui.common.Masks
import br.com.fitpersonal.app.domain.rules.TermoLgpd
import br.com.fitpersonal.app.ui.lgpd.TermoConteudo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import coil.compose.AsyncImage
import kotlin.system.exitProcess

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(onVoltar: () -> Unit, vm: PerfilViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmarSaida by remember { mutableStateOf(false) }
    var confirmarPlanilhas by remember { mutableStateOf(false) }

    LaunchedEffect(s.planilha) {
        s.planilha?.let {
            br.com.fitpersonal.app.ui.common.PdfSharer.compartilhar(ctx, it, "Planilhas FitPersonal", "application/zip")
            vm.planilhaConsumida()
        }
    }
    if (confirmarPlanilhas) {
        AlertDialog(
            onDismissRequest = { confirmarPlanilhas = false },
            title = { Text("Exportar dados dos alunos?") },
            text = {
                Text(
                    "O arquivo contém dados pessoais e de saúde (nomes, contatos, medidas) sem criptografia. " +
                        "Só envie a quem tiver autorização e apague depois de usar.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmarPlanilhas = false; vm.exportarPlanilhas() }) { Text("Exportar") } },
            dismissButton = { TextButton(onClick = { confirmarPlanilhas = false }) { Text("Cancelar") } },
        )
    }
    var restaurarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var importarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var definindoSenha by remember { mutableStateOf(false) }
    var trocandoModo by remember { mutableStateOf(false) }
    var relatorioFalhas by remember { mutableStateOf<String?>(null) }
    var verTermo by remember { mutableStateOf(false) }
    var aba by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(s.mensagem) { s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }
    BackHandler(enabled = s.alterado) { confirmarSaida = true }

    var logoParaRecortar by remember { mutableStateOf<android.net.Uri?>(null) }
    val escolherLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logoParaRecortar = uri
    }
    logoParaRecortar?.let { uri ->
        br.com.fitpersonal.app.ui.common.RecortarImagemDialog(
            origem = uri,
            proporcaoInicial = null,
            onConfirmar = { r -> vm.definirLogo(uri, r); logoParaRecortar = null },
            onUsarOriginal = { vm.definirLogo(uri); logoParaRecortar = null },
            onCancelar = { logoParaRecortar = null },
        )
    }
    val criarBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let(vm::exportar)
    }
    val abrirBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restaurarUri = uri
    }
    val escolherPasta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                val nome = DocumentFile.fromTreeUri(ctx, uri)?.name.orEmpty()
                vm.definirPasta(uri.toString(), nome)
            }.onFailure { vm.avisar("Não foi possível usar essa pasta.") }
        }
    }
    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.definirLembretes(true) else vm.avisar("Sem permissão de notificações, os lembretes não aparecem.")
    }
    val abrirDesktop = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importarUri = uri
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil e backup") },
                navigationIcon = {
                    IconButton(onClick = { if (s.alterado) confirmarSaida = true else onVoltar() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        if (s.carregando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val p = s.perfil
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CabecalhoPerfil(
                nome = p.nome,
                marca = p.marca,
                cref = p.cref,
                logo = p.logo?.let(vm::arquivoLogo),
                alterado = s.alterado,
            )

            TabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.background) {
                ABAS_PERFIL.forEachIndexed { i, titulo ->
                    br.com.fitpersonal.app.ui.common.AbaCompacta(
                        selecionada = aba == i,
                        onClick = { aba = i },
                        titulo = titulo,
                    )
                }
            }

            SecaoAnimada(aba == 0) {
            CartaoSecao(titulo = "Dados profissionais") {
            Text(
                "Aparecem no cabeçalho dos PDFs de avaliação, treino e agenda.",
                style = MaterialTheme.typography.bodySmall,
            )

            // Logo
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    if (p.logo != null) {
                        AsyncImage(
                            model = vm.arquivoLogo(p.logo),
                            contentDescription = "Logo",
                            // Crop: a imagem preenche o quadro (o Surface recorta nos cantos), sem bordas vazias.
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text("Sem logo", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = {
                        escolherLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (p.logo == null) "Escolher logo" else "Trocar logo") }
                    if (p.logo != null) TextButton(onClick = vm::removerLogo) { Text("Remover") }
                }
            }

            Campo("Nome do profissional", p.nome, null) { v -> vm.alterar { it.copy(nome = v) } }
            Campo("Marca / estúdio", p.marca, null) { v -> vm.alterar { it.copy(marca = v) } }
            Campo("CREF", p.cref, null) { v -> vm.alterar { it.copy(cref = v) } }
            Campo("E-mail", p.email, s.erros[CampoPerfil.EMAIL], teclado = KeyboardType.Email) { v ->
                vm.alterar(CampoPerfil.EMAIL) { it.copy(email = v.trim()) }
            }
            Campo(
                "Telefone", p.telefone, s.erros[CampoPerfil.TELEFONE],
                transform = Masks.TELEFONE, teclado = KeyboardType.Phone, apenasDigitos = true, max = 11,
            ) { v -> vm.alterar(CampoPerfil.TELEFONE) { it.copy(telefone = v) } }
            Campo("Cidade", p.cidade, null) { v -> vm.alterar { it.copy(cidade = v) } }
            Campo("Chave Pix (vai nas cobranças)", p.chavePix, null) { v -> vm.alterar { it.copy(chavePix = v) } }
            Text(
                "CPF/CNPJ só números, e-mail, telefone no formato +5511999999999 ou chave aleatória. " +
                    "Com a chave, a cobrança por WhatsApp já leva o Pix copia e cola com o valor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = vm::salvar, enabled = s.alterado, modifier = Modifier.fillMaxWidth()) { Text("Salvar perfil") }

            }

            }

            SecaoAnimada(aba == 2) {
            CartaoSecao(titulo = "Segurança e privacidade") {
            Text(
                "O banco de dados é criptografado (AES-256) com uma chave guardada no cofre do Android. " +
                    "Backups são sempre criptografados com a senha abaixo — ela não pode ser recuperada.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (s.senhaBackupDefinida) "Senha do backup: definida" else "Senha do backup: não definida",
                color = if (s.senhaBackupDefinida) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = { definindoSenha = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.senhaBackupDefinida) "Alterar senha do backup" else "Definir senha do backup")
            }
            if (s.config.consentimentoEm > 0) {
                val aceitoEm = Instant.ofEpochMilli(s.config.consentimentoEm).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                Text("Termo de privacidade aceito em $aceitoEm (versão ${s.config.consentimentoVersao}).", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { verTermo = true }) { Text("Ver termo de privacidade") }

            }

            CartaoSecao(titulo = "Proteção do app") {
                val seguro = (ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bloquear ao abrir")
                        Text(
                            if (seguro) "Pede biometria, PIN ou padrão do aparelho (e de novo após 30 s fora do app)."
                            else "Cadastre um bloqueio de tela no Android para usar este recurso.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = s.config.bloqueioAtivo && seguro,
                        enabled = seguro,
                        onCheckedChange = vm::definirBloqueio,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Impedir capturas de tela")
                        Text(
                            "Também esconde o conteúdo na lista de apps recentes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = s.config.bloquearCapturaTela, onCheckedChange = vm::definirBloquearCaptura)
                }
            }

            CartaoSecao(titulo = "Aparência") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TemaApp.entries.forEach { t ->
                        FilterChip(
                            selected = t == s.config.tema,
                            onClick = { vm.definirTema(t) },
                            label = { Text(t.rotulo) },
                        )
                    }
                }
            }

            }

            SecaoAnimada(aba == 1) {
            CartaoSecao(titulo = "Lembretes de agenda") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Avisar antes dos agendamentos", Modifier.weight(1f))
                Switch(
                    checked = s.config.lembretesAtivo,
                    onCheckedChange = { ligar ->
                        when {
                            !ligar -> vm.definirLembretes(false)
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificador.permitido(ctx) ->
                                pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> vm.definirLembretes(true)
                        }
                    },
                )
            }
            if (s.config.lembretesAtivo) {
                Text("Quanto tempo antes", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LembreteRules.ANTECEDENCIAS_MIN.forEach { min ->
                        FilterChip(
                            selected = min == s.config.antecedenciaMin,
                            onClick = { vm.definirAntecedencia(min) },
                            label = { Text(LembreteRules.rotuloAntecedencia(min)) },
                        )
                    }
                }
                if (!Notificador.permitido(ctx)) {
                    Text(
                        "As notificações estão bloqueadas para este app. Libere nas configurações do sistema.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            }

            CartaoSecao(titulo = "Avisos diários") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Aviso diário (por volta das 9h)")
                        Text(
                            "Agenda do dia, mensalidades em atraso, aniversariantes, reavaliações atrasadas ou vencendo e pacotes esgotando. Os ajustes de reavaliação ficam na ficha de cada aluno.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = s.config.reavaliacaoAvisos,
                        onCheckedChange = { ligar ->
                            if (ligar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificador.permitido(ctx)) {
                                pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            vm.definirReavaliacaoAvisos(ligar)
                        },
                    )
                }
            }

            }

            SecaoAnimada(aba == 3) {
            CartaoSecao(titulo = "Modo do aplicativo") {
                Text(
                    "Você está usando o FitPersonal como professor. Para ver os treinos como aluno " +
                        "(receber e executar treinos), troque de modo. Nada é apagado nos dois lados.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = { trocandoModo = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Trocar para o modo aluno")
                }
            }

            if (vm.testeDisponivel) {
                CartaoSecao(titulo = "Dados de teste (só debug/homologação)") {
                    Text(
                        "Cria 5 alunos fictícios com TODOS os campos preenchidos (perfil, avaliações com fotos, PAR-Q e " +
                            "assinatura, treinos, agenda, pagamentos, testes de aptidão e lista de espera). " +
                            "Os nomes começam com \"(Teste)\" e podem ser removidos aqui. Não aparece no app de produção.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = vm::popularDadosDeTeste,
                        enabled = s.ocupado == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Carregar dados de teste") }
                    OutlinedButton(
                        onClick = vm::removerDadosDeTeste,
                        enabled = s.ocupado == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Remover dados de teste") }
                }
            }

            CartaoSecao(titulo = "Relatório de falhas") {
                Text(
                    "Se o app fechar sozinho, o motivo (falha, falta de memória, travamento) fica registrado " +
                        "aqui, só neste aparelho. Compartilhe o relatório para a análise.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = { relatorioFalhas = br.com.fitpersonal.app.core.Falhas.relatorio(ctx) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ver relatório de falhas") }
            }

            CartaoSecao(titulo = "Sincronizar entre aparelhos") {
                Text(
                    "Mantém os mesmos dados em dois aparelhos usando a pasta de backup (Drive ou outra). " +
                        "Use a mesma senha de backup nos dois. Ao trocar de aparelho, toque em sincronizar " +
                        "antes de começar e de novo ao terminar.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (s.config.syncUltimoEm > 0) {
                    val quando = Instant.ofEpochMilli(s.config.syncUltimoEm).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    Text(
                        "Última: $quando — ${s.config.syncMensagem}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = vm::sincronizar,
                    enabled = s.ocupado == null && s.config.pastaBackup != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Sincronizar agora") }
                if (s.config.pastaBackup == null) {
                    Text(
                        "Escolha antes a pasta de destino em \"Backup automático\" (abaixo).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            CartaoSecao(titulo = "Backup manual e restauração") {
            Text(
                "O backup (.fitbak) é criptografado com a sua senha e inclui alunos, avaliações, treinos, agenda, fotos, logo e perfil. " +
                    "Guarde o arquivo fora do celular (Drive, e-mail ou computador); sem a senha ele não abre. " +
                    "Ele contém dados de saúde: proteja-o como dado sensível.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!s.senhaBackupDefinida) {
                Text(
                    "Defina a senha do backup para poder criar backups (toque em \"Definir senha e criar backup\").",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                // Sem senha o botão não fica "morto": leva direto à tela de definir a senha.
                onClick = {
                    if (s.senhaBackupDefinida) criarBackup.launch(vm.nomeBackup()) else definindoSenha = true
                },
                enabled = s.ocupado == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (s.senhaBackupDefinida) "Criar backup agora (escolher onde salvar)" else "Definir senha e criar backup") }
            OutlinedButton(
                onClick = { abrirBackup.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                enabled = s.ocupado == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restaurar de um backup") }

            }

            CartaoSecao(titulo = "Backup automático") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Backup diário (por volta das 3h)", Modifier.weight(1f))
                Switch(checked = s.config.backupAtivo, onCheckedChange = vm::definirBackupAtivo)
            }
            Text(
                if (s.config.pastaBackup != null) "Destino: pasta \"${s.config.pastaBackupNome}\""
                else "Destino: armazenamento interno do app (é apagado se o app for desinstalado)",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Para guardar na nuvem, escolha uma pasta do Google Drive (ou de outro provedor de arquivos " +
                    "instalado). O backup continua criptografado com a sua senha antes de sair do aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { escolherPasta.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("Escolher pasta de destino (Drive ou outra)…")
            }
            if (s.config.pastaBackup != null) {
                TextButton(onClick = { vm.definirPasta(null, "") }) { Text("Voltar ao armazenamento interno") }
            }
            Button(
                onClick = { if (s.senhaBackupDefinida) vm.backupAgora() else definindoSenha = true },
                enabled = s.ocupado == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar agora no destino acima")
            }
            if (s.config.ultimoBackupEm > 0) {
                val quando = Instant.ofEpochMilli(s.config.ultimoBackupEm).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                Text("Último: $quando — ${s.config.ultimoBackupMsg}", style = MaterialTheme.typography.bodySmall)
            }

            }

            CartaoSecao(titulo = "Exportar planilhas") {
                Text(
                    "Gera um .zip com alunos, avaliações, agenda e pagamentos em CSV (abre no Excel e no Google " +
                        "Planilhas). Não inclui CPF nem observações médicas, mas contém dados pessoais: guarde e " +
                        "compartilhe com cuidado.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = { confirmarPlanilhas = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Exportar planilhas (CSV)")
                }
            }

            CartaoSecao(titulo = "Importar do desktop") {
            Text(
                "Traz alunos, avaliações, treinos, agenda, fotos e dados do profissional de um backup (.zip) " +
                    "do RJE Avaliações para Windows. Os dados atuais são mantidos: nada é apagado, e registros " +
                    "que já existem não são duplicados.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = { abrirDesktop.launch(arrayOf("*/*")) },
                enabled = s.ocupado == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Importar backup do desktop") }
            }
            }
        }
    }

    relatorioFalhas?.let { texto ->
        AlertDialog(
            onDismissRequest = { relatorioFalhas = null },
            title = { Text("Relatório de falhas") },
            text = {
                Text(
                    texto,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        br.com.fitpersonal.app.ui.common.PdfSharer.compartilharTexto(ctx, texto, "Relatório de falhas FitPersonal")
                    },
                ) { Text("Compartilhar") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            br.com.fitpersonal.app.core.Falhas.limpar(ctx)
                            relatorioFalhas = null
                        },
                    ) { Text("Limpar") }
                    TextButton(onClick = { relatorioFalhas = null }) { Text("Fechar") }
                }
            },
        )
    }

    s.conflitoSync?.let { remoto ->
        AlertDialog(
            onDismissRequest = vm::cancelarConflito,
            title = { Text("Dados diferentes nos dois lados") },
            text = {
                Text(
                    "A pasta tem uma versão enviada por \"${remoto.nomeDispositivo.ifBlank { "outro aparelho" }}\" " +
                        "e este aparelho também tem alterações novas. Escolha qual manter. " +
                        "O estado atual deste aparelho é guardado antes de qualquer troca.",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.resolverConflito(usarPasta = true) }) { Text("Usar a da pasta") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.resolverConflito(usarPasta = false) }) { Text("Manter deste aparelho") }
                    TextButton(onClick = vm::cancelarConflito) { Text("Cancelar") }
                }
            },
        )
    }

    if (trocandoModo) {
        AlertDialog(
            onDismissRequest = { trocandoModo = false },
            title = { Text("Trocar para o modo aluno?") },
            text = { Text("Seus alunos, avaliações e treinos ficam guardados. Para voltar, use o botão de trocar de modo na tela do aluno.") },
            confirmButton = { TextButton(onClick = { trocandoModo = false; vm.trocarParaAluno() }) { Text("Trocar") } },
            dismissButton = { TextButton(onClick = { trocandoModo = false }) { Text("Cancelar") } },
        )
    }

    if (definindoSenha) {
        DialogoSenha(
            titulo = if (s.senhaBackupDefinida) "Alterar senha do backup" else "Definir senha do backup",
            aviso = "Anote esta senha em local seguro. Ela não pode ser recuperada: sem ela, " +
                "nenhum backup poderá ser aberto. Backups já criados continuam com a senha antiga.",
            confirmar = true,
            textoConfirmar = "Salvar senha",
            onCancelar = { definindoSenha = false },
            onConfirmar = { senha, conf ->
                val erro = vm.definirSenhaBackup(senha, conf)
                if (erro == null) definindoSenha = false
                erro
            },
        )
    }

    s.pedirSenhaPara?.let { uri ->
        DialogoSenha(
            titulo = "Senha do backup",
            aviso = "Este backup é protegido por senha. Informe a senha usada quando ele foi criado.",
            confirmar = false,
            textoConfirmar = "Restaurar",
            onCancelar = vm::cancelarSenhaRestauracao,
            onConfirmar = { senha, _ ->
                vm.restaurar(uri, senha.toCharArray())
                null
            },
        )
    }

    if (verTermo) {
        AlertDialog(
            onDismissRequest = { verTermo = false },
            title = { Text(TermoLgpd.TITULO) },
            text = { Box(Modifier.verticalScroll(rememberScrollState())) { TermoConteudo() } },
            confirmButton = { TextButton(onClick = { verTermo = false }) { Text("Fechar") } },
        )
    }

    // Progresso bloqueante
    s.ocupado?.let { texto ->
        Dialog(texto)
    }

    restaurarUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restaurarUri = null },
            title = { Text("Restaurar backup?") },
            text = {
                Text(
                    "Todos os dados atuais serão substituídos pelos do backup. Uma cópia de segurança do estado " +
                        "atual é guardada antes. O aplicativo será fechado ao terminar.",
                )
            },
            confirmButton = { TextButton(onClick = { restaurarUri = null; vm.restaurar(uri) }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { restaurarUri = null }) { Text("Cancelar") } },
        )
    }

    importarUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importarUri = null },
            title = { Text("Importar backup do desktop?") },
            text = {
                Text(
                    "Os dados do arquivo serão adicionados aos atuais. Nada será apagado e itens repetidos " +
                        "são ignorados. Uma cópia de segurança do estado atual é guardada antes.",
                )
            },
            confirmButton = { TextButton(onClick = { importarUri = null; vm.importarDesktop(uri) }) { Text("Importar") } },
            dismissButton = { TextButton(onClick = { importarUri = null }) { Text("Cancelar") } },
        )
    }

    s.resumoImportacao?.let { r ->
        AlertDialog(
            onDismissRequest = vm::resumoImportacaoVisto,
            title = { Text("Importação concluída") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Alunos novos: ${r.alunosNovos}" +
                        if (r.alunosReaproveitados > 0) " (${r.alunosReaproveitados} já existiam)" else "")
                    Text("Avaliações: ${r.avaliacoes}")
                    Text("Treinos: ${r.treinos} (${r.exercicios} exercícios)")
                    Text("Agendamentos: ${r.agendamentos}")
                    Text("Fotos: ${r.fotos}")
                    if (r.jaExistiam > 0) Text("Registros já existentes ignorados: ${r.jaExistiam}")
                    if (r.ignorados > 0) Text("Linhas inválidas ignoradas: ${r.ignorados}")
                    if (r.perfilAtualizado) Text("Dados do profissional preenchidos a partir do desktop.")
                }
            },
            confirmButton = { TextButton(onClick = vm::resumoImportacaoVisto) { Text("OK") } },
        )
    }

    if (s.restauracaoConcluida) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restauração concluída") },
            text = { Text("Os dados foram restaurados. O aplicativo será fechado — abra-o novamente para continuar.") },
            confirmButton = {
                TextButton(onClick = {
                    ctx.activity()?.finishAffinity()
                    exitProcess(0)
                }) { Text("Fechar app") }
            },
        )
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Descartar alterações?") },
            text = { Text("Há alterações não salvas no perfil.") },
            confirmButton = { TextButton(onClick = { confirmarSaida = false; onVoltar() }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Continuar editando") } },
        )
    }
}

@Composable
private fun Dialog(texto: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(32.dp))
                Text(texto)
            }
        },
    )
}

@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    erro: String?,
    transform: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    teclado: KeyboardType = KeyboardType.Text,
    apenasDigitos: Boolean = false,
    max: Int = Int.MAX_VALUE,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { novo ->
            val v = if (apenasDigitos) novo.filter(Char::isDigit) else novo
            onChange(v.take(max))
        },
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        visualTransformation = transform,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val ABAS_PERFIL = listOf("Perfil", "Avisos", "Segurança", "Dados")

/** Mostra/esconde um grupo de cartões com fade e expansão suaves ao trocar de aba. */
@Composable
private fun SecaoAnimada(visivel: Boolean, conteudo: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(
        visible = visivel,
        enter = fadeIn(tween(160)) + expandVertically(tween(180)),
        exit = fadeOut(tween(90)) + shrinkVertically(tween(150)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = conteudo)
    }
}

/** Cartão de apresentação: logo (ou iniciais), nome, marca e CREF, com aviso de alterações não salvas. */
@Composable
private fun CabecalhoPerfil(
    nome: String,
    marca: String,
    cref: String,
    logo: java.io.File?,
    alterado: Boolean,
) {
    val degrade = br.com.fitpersonal.app.ui.theme.gradienteMarca()
    val sobre = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto
    Row(
        Modifier
            .fillMaxWidth()
            .clip(br.com.fitpersonal.app.ui.common.FolderShape(raio = 26.dp, recorte = 20.dp, fracaoAba = 0.6f))
            .background(br.com.fitpersonal.app.ui.common.PaletaPasta.Lima)
            .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = sobre.copy(alpha = 0.18f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (logo != null && logo.exists()) {
                    AsyncImage(
                        model = logo,
                        contentDescription = "Logo",
                        // Crop + círculo: a foto preenche todo o círculo, sem faixas nas laterais.
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(
                        nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                            .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = sobre,
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                nome.ifBlank { "Seu nome" },
                style = MaterialTheme.typography.titleLarge,
                color = sobre,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (marca.isNotBlank()) {
                Text(marca, style = MaterialTheme.typography.bodyMedium, color = sobre.copy(alpha = 0.9f), maxLines = 1)
            }
            Text(
                if (cref.isNotBlank()) "CREF $cref" else "CREF não informado",
                style = MaterialTheme.typography.bodySmall,
                color = sobre.copy(alpha = 0.85f),
            )
            if (alterado) {
                Text(
                    "Alterações não salvas",
                    style = MaterialTheme.typography.labelMedium,
                    color = sobre,
                )
            }
        }
    }
}
