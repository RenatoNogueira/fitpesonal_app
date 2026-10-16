package br.com.fitpersonal.app.ui.alunos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.rules.AlunoInput
import br.com.fitpersonal.app.domain.rules.CampoAluno
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.Masks
import br.com.fitpersonal.app.ui.common.PdfSharer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import android.net.Uri

private val SEXOS = listOf("Feminino", "Masculino", "Outro")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlunoFormScreen(onVoltar: () -> Unit, vm: AlunoFormViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmarExclusao by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var uriCamera by rememberSaveable { mutableStateOf<Uri?>(null) }

    var recortando by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        uriCamera?.let { if (ok) recortando = it }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) recortando = uri
    }
    recortando?.let { uri ->
        br.com.fitpersonal.app.ui.common.RecortarImagemDialog(
            origem = uri,
            proporcaoInicial = 1f,
            onConfirmar = { r -> vm.definirFoto(uri, r); recortando = null },
            onUsarOriginal = { vm.definirFoto(uri); recortando = null },
            onCancelar = { recortando = null },
        )
    }
    LaunchedEffect(s.mensagem) { s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }

    LaunchedEffect(s.concluido) { if (s.concluido) onVoltar() }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(s.exportacao) {
        s.exportacao?.let {
            PdfSharer.compartilhar(ctx, it, "Dados do aluno (LGPD)", "application/json")
            vm.exportacaoConsumida()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (s.editando) "Editar aluno" else "Novo aluno") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                actions = {
                    if (s.editando) IconButton(onClick = { confirmarExclusao = true }) {
                        Icon(Icons.Default.Delete, "Excluir aluno")
                    }
                },
            )
        },
    ) { padding ->
        if (s.carregando) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val i = s.input
            CartaoSecao {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarAluno(i.nome, vm.arquivoFoto(s.foto), tamanho = 88.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                uriCamera = vm.novaUriCamera().also { camera.launch(it) }
                            }) { Text("Câmera") }
                            OutlinedButton(onClick = {
                                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) { Text("Galeria") }
                        }
                        if (s.foto != null) TextButton(onClick = vm::removerFoto) { Text("Remover foto") }
                    }
                }
            }

            CartaoSecao(titulo = "Identificação") {
                Campo("Nome *", i.nome, s.erros[CampoAluno.NOME]) { v -> vm.alterar({ it.copy(nome = v) }, CampoAluno.NOME) }
                Campo(
                    "Data de nascimento", i.dataNascimento, s.erros[CampoAluno.DATA_NASCIMENTO],
                    transform = Masks.DATA, teclado = KeyboardType.Number, max = 8, placeholder = "DD/MM/AAAA",
                ) { v -> vm.alterar({ it.copy(dataNascimento = v) }, CampoAluno.DATA_NASCIMENTO) }
                SexoDropdown(i.sexo) { v -> vm.alterar({ it.copy(sexo = v) }) }
            }

            CartaoSecao(titulo = "Contato") {
                Campo(
                    "Telefone", i.telefone, s.erros[CampoAluno.TELEFONE],
                    transform = Masks.TELEFONE, teclado = KeyboardType.Phone, max = 11,
                ) { v -> vm.alterar({ it.copy(telefone = v) }, CampoAluno.TELEFONE) }
                Campo("E-mail", i.email, s.erros[CampoAluno.EMAIL], teclado = KeyboardType.Email) { v ->
                    vm.alterar({ it.copy(email = v.trim()) }, CampoAluno.EMAIL)
                }
                Campo("Instagram", i.instagram, null, placeholder = "@usuario") { v -> vm.alterar({ it.copy(instagram = v) }) }
            }

            CartaoSecao(titulo = "Documentos") {
                Campo(
                    "CPF", i.cpf, s.erros[CampoAluno.CPF],
                    transform = Masks.CPF, teclado = KeyboardType.Number, max = 11,
                ) { v -> vm.alterar({ it.copy(cpf = v) }, CampoAluno.CPF) }
                Campo(
                    "CEP", i.cep, s.erros[CampoAluno.CEP],
                    transform = Masks.CEP, teclado = KeyboardType.Number, max = 8,
                ) { v -> vm.alterar({ it.copy(cep = v) }, CampoAluno.CEP) }
            }

            CartaoSecao(titulo = "Saúde") {
                Campo("Objetivo", i.objetivo, null) { v -> vm.alterar({ it.copy(objetivo = v) }) }
                Campo("Observações médicas", i.observacoesMedicas, null, linhas = 4) { v ->
                    vm.alterar({ it.copy(observacoesMedicas = v) })
                }
            }

            CartaoSecao(titulo = "Consentimento (LGPD)") {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = i.consentimento,
                        onCheckedChange = { v -> vm.alterar({ it.copy(consentimento = v) }) },
                    )
                    Column(Modifier.padding(top = 12.dp)) {
                        Text("O aluno autorizou o tratamento dos seus dados de saúde")
                        Text(
                            "Marque depois de obter o consentimento do aluno, de preferência por escrito. " +
                                "A data do registro é guardada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }


            Button(onClick = vm::salvar, modifier = Modifier.fillMaxWidth()) { Text("Salvar") }

            if (s.editando) {
                CartaoSecao(titulo = "Privacidade (LGPD)") {
                    Text(
                        "O aluno pode pedir uma cópia dos seus dados ou a eliminação deles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = vm::exportarDados, modifier = Modifier.fillMaxWidth()) {
                        Text("Exportar todos os dados (JSON)")
                    }
                }
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir definitivamente?") },
            text = {
                Text(
                    "Cadastro, avaliações, treinos, agendamentos e todas as fotos deste aluno serão " +
                        "apagados deste aparelho. Cópias em backups já feitos não são alteradas. " +
                        "Esta ação não pode ser desfeita.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmarExclusao = false; vm.excluir() }) { Text("Excluir tudo") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    erro: String?,
    transform: VisualTransformation = VisualTransformation.None,
    teclado: KeyboardType = KeyboardType.Text,
    max: Int = Int.MAX_VALUE,
    linhas: Int = 1,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    val somenteDigitos = teclado == KeyboardType.Number || teclado == KeyboardType.Phone
    OutlinedTextField(
        value = valor,
        onValueChange = { novo ->
            val v = if (somenteDigitos) novo.filter(Char::isDigit) else novo
            onChange(v.take(max))
        },
        label = { Text(rotulo) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        visualTransformation = transform,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = linhas == 1,
        minLines = linhas,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexoDropdown(valor: String, onChange: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sexo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(aberto) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            SEXOS.forEach { opcao ->
                DropdownMenuItem(text = { Text(opcao) }, onClick = { onChange(opcao); aberto = false })
            }
        }
    }
}
