package br.com.fitpersonal.app.aluno

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.domain.rules.Refeicao
import br.com.fitpersonal.app.domain.rules.RefeicoesRules
import br.com.fitpersonal.app.domain.rules.TipoRefeicao
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val HORA = DateTimeFormatter.ofPattern("HH:mm")

/** Cartão da lista inicial: quantas refeições hoje e atalho para abrir o diário. */
@Composable
fun CartaoDiario(onAbrir: () -> Unit, versao: Int, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val hoje = remember { LocalDate.now() }
    val hojeCount = remember(versao) { RefeicoesStore(ctx).listar().count { it.data == hoje } }
    Card(
        onClick = onAbrir,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Diário de refeições", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (hojeCount == 0) "Registre o que você comeu, com foto, e mande ao professor"
                    else if (hojeCount == 1) "1 refeição registrada hoje" else "$hojeCount refeições registradas hoje",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.Add, contentDescription = "Abrir diário", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DiarioRefeicoes(onVoltar: () -> Unit, onMensagem: (String) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { RefeicoesStore(ctx) }
    var lista by remember { mutableStateOf(store.listar()) }
    var registrando by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<Refeicao?>(null) }
    val dias = RefeicoesRules.porDia(lista)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Cabecalho(
                titulo = "Diário de refeições",
                subtitulo = if (lista.isEmpty()) "Nenhuma refeição ainda" else "${lista.size} registro(s)",
                inicio = {
                    IconButton(
                        onClick = onVoltar,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                acoes = {
                    IconButton(
                        onClick = { registrando = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) { Icon(Icons.Filled.Add, "Registrar refeição") }
                },
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (dias.isEmpty()) {
                    item {
                        Text(
                            "Toque no + no topo da tela para anotar o que comeu. A foto é opcional e fica só neste aparelho até você enviar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                dias.forEach { (dia, refeicoes) ->
                    item(key = "dia-" + dia) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (dia == LocalDate.now()) "Hoje" else dia.format(DIA),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { enviarDia(ctx, store, dia, lista) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp))
                                Text("  Enviar ao professor")
                            }
                        }
                    }
                    items(refeicoes, key = { it.id }) { r ->
                        ItemRefeicao(r, store, onExcluir = { excluindo = r })
                    }
                }
            }
        }
    }

    if (registrando) {
        RegistrarRefeicaoDialog(
            store = store,
            onSalvo = { lista = store.listar(); registrando = false; onMensagem("Refeição registrada.") },
            onFechar = { registrando = false },
        )
    }

    excluindo?.let { r ->
        AlertDialog(
            onDismissRequest = { excluindo = null },
            title = { Text("Excluir este registro?") },
            text = { Text(r.tipo.rotulo + " às " + r.hora.format(HORA) + " será apagado, junto com a foto.") },
            confirmButton = {
                TextButton(onClick = { store.excluir(r.id); lista = store.listar(); excluindo = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluindo = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ItemRefeicao(r: Refeicao, store: RefeicoesStore, onExcluir: () -> Unit) {
    val foto = remember(r.id) { store.arquivoDaFoto(r) }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (foto != null) {
                AsyncImage(
                    model = foto,
                    contentDescription = "Foto da refeição",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(r.tipo.rotulo + " • " + r.hora.format(HORA), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                r.nota?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onExcluir) { Icon(Icons.Filled.Delete, contentDescription = "Excluir registro") }
        }
    }
}

@Composable
private fun RegistrarRefeicaoDialog(store: RefeicoesStore, onSalvo: () -> Unit, onFechar: () -> Unit) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var tipo by remember { mutableStateOf(RefeicoesRules.sugerirTipo(LocalTime.now())) }
    var nota by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<Uri?>(null) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    // A câmera grava num arquivo temporário (sem exigir permissão CAMERA); o diário guarda uma cópia reduzida.
    var uriCamera by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) foto = uriCamera
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) foto = uri
    }

    AlertDialog(
        onDismissRequest = { if (!salvando) onFechar() },
        title = { Text("Registrar refeição") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(TipoRefeicao.CAFE, TipoRefeicao.ALMOCO, TipoRefeicao.LANCHE).forEach { t ->
                        FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t.rotulo.substringBefore(' ')) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(TipoRefeicao.JANTAR, TipoRefeicao.OUTRO).forEach { t ->
                        FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t.rotulo) })
                    }
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it.take(300) },
                    label = { Text("O que você comeu (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (foto != null) {
                    AsyncImage(
                        model = foto,
                        contentDescription = "Foto escolhida",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val u = FotoStorage(ctx.applicationContext).novaUriCamera()
                            uriCamera = u
                            runCatching { camera.launch(u) }.onFailure { erro = "Não foi possível abrir a câmera." }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp))
                        Text("  Foto")
                    }
                    OutlinedButton(
                        onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(18.dp))
                        Text("  Galeria")
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = !salvando,
                onClick = {
                    salvando = true
                    escopo.launch {
                        val r = withContext(Dispatchers.IO) { runCatching { store.adicionar(tipo, nota, foto) } }
                        salvando = false
                        if (r.isSuccess) onSalvo() else erro = "Não foi possível salvar a foto. Tente outra imagem."
                    }
                },
            ) { Text(if (salvando) "Salvando…" else "Salvar") }
        },
        dismissButton = { TextButton(enabled = !salvando, onClick = onFechar) { Text("Cancelar") } },
    )
}

/** Compartilha o dia: texto resumido e todas as fotos do dia (se houver) pelo app que o aluno escolher. */
private fun enviarDia(ctx: Context, store: RefeicoesStore, dia: LocalDate, todas: List<Refeicao>) {
    val doDia = todas.filter { it.data == dia }
    val texto = RefeicoesRules.textoDoDia(dia, todas)
    val uris = ArrayList(doDia.mapNotNull { store.uriDaFoto(it) })
    val envio = if (uris.isEmpty()) {
        Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_TEXT, texto)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            // Sem o ClipData, alguns apps de destino não recebem a permissão de leitura das fotos.
            clipData = ClipData.newRawUri("Fotos", uris.first()).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    runCatching { ctx.startActivity(Intent.createChooser(envio, "Enviar diário do dia")) }
}
