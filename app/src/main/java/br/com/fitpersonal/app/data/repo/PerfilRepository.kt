package br.com.fitpersonal.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.fitpersonal.app.domain.model.PerfilProfissional
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.perfilStore by preferencesDataStore(name = "perfil_profissional")

/**
 * Fonte do perfil profissional. Existe para que BackupManager não dependa do DataStore concreto
 * (e possa ser exercitado em testes instrumentados com um perfil em memória).
 */
interface PerfilFonte {
    val perfil: Flow<PerfilProfissional>
    suspend fun salvar(p: PerfilProfissional)
}

/** Marca/contato usados no cabeçalho dos PDFs (equivale ao settings.json do desktop). */
@Singleton
class PerfilRepository @Inject constructor(@ApplicationContext private val ctx: Context) : PerfilFonte {

    private object K {
        val nome = stringPreferencesKey("nome")
        val marca = stringPreferencesKey("marca")
        val cref = stringPreferencesKey("cref")
        val email = stringPreferencesKey("email")
        val telefone = stringPreferencesKey("telefone")
        val cidade = stringPreferencesKey("cidade")
        val chavePix = stringPreferencesKey("chave_pix")
        val logo = stringPreferencesKey("logo")
    }

    override val perfil: Flow<PerfilProfissional> = ctx.perfilStore.data.map { p: Preferences ->
        PerfilProfissional(
            nome = p[K.nome].orEmpty(), marca = p[K.marca].orEmpty(), cref = p[K.cref].orEmpty(),
            email = p[K.email].orEmpty(), telefone = p[K.telefone].orEmpty(), cidade = p[K.cidade].orEmpty(),
            chavePix = p[K.chavePix].orEmpty(),
            logo = p[K.logo]?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun salvar(p: PerfilProfissional) {
        ctx.perfilStore.edit {
            it[K.nome] = p.nome; it[K.marca] = p.marca; it[K.cref] = p.cref
            it[K.email] = p.email; it[K.telefone] = p.telefone; it[K.cidade] = p.cidade
            it[K.chavePix] = p.chavePix
            it[K.logo] = p.logo.orEmpty()
        }
    }
}
