package br.com.fitpersonal.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.R
import br.com.fitpersonal.app.domain.rules.UsoApp

/**
 * Primeira tela do app: a pessoa diz se vai usar como professor (gerencia alunos, avaliações e treinos)
 * ou como aluno (recebe e executa treinos). Pode ser trocado depois.
 */
@Composable
fun EscolhaDeUso(onEscolher: (UsoApp) -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.logo_app),
                contentDescription = "FitPersonal",
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(26.dp)),
            )
            Text(
                "Como você vai usar o FitPersonal?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                "Escolha uma opção para começar. Você pode trocar depois, nas configurações.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))

            OpcaoDeUso(
                titulo = "Sou professor",
                detalhe = "Cadastre alunos, faça avaliações físicas, monte treinos, organize a agenda e envie os treinos para os alunos.",
                icone = { Icon(Icons.Filled.School, contentDescription = null, tint = Color.White) },
                cor = MaterialTheme.colorScheme.primary,
                onClick = { onEscolher(UsoApp.PROFESSOR) },
            )
            OpcaoDeUso(
                titulo = "Sou aluno",
                detalhe = "Receba o treino do seu professor, acompanhe cada exercício, anote as cargas e envie o resumo.",
                icone = { Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White) },
                cor = Color(0xFF0E7490),
                onClick = { onEscolher(UsoApp.ALUNO) },
            )
        }
    }
}

@Composable
private fun OpcaoDeUso(
    titulo: String,
    detalhe: String,
    icone: @Composable () -> Unit,
    cor: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = cor, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { icone() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Text(
                    detalhe,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
