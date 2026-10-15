package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Aluno
import java.time.DateTimeException
import java.time.LocalDate

enum class CampoAluno { NOME, DATA_NASCIMENTO, TELEFONE, EMAIL, CPF, CEP }

/** Valores crus do formulário; máscaras ficam na UI, aqui só dígitos onde aplicável. */
data class AlunoInput(
    val nome: String = "",
    val dataNascimento: String = "",   // ddMMyyyy (somente dígitos)
    val sexo: String = "",
    val telefone: String = "",         // dígitos
    val email: String = "",
    val cpf: String = "",              // dígitos
    val cep: String = "",              // dígitos
    val instagram: String = "",
    val objetivo: String = "",
    val observacoesMedicas: String = "",
    /** O aluno autorizou o tratamento dos dados de saúde (LGPD, art. 11, I). */
    val consentimento: Boolean = false,
)

sealed interface ResultadoAluno {
    data class Ok(val aluno: Aluno) : ResultadoAluno
    data class Erros(val erros: Map<CampoAluno, String>) : ResultadoAluno
}

/** Regras de gui/alunos_view.py on_salvar. */
object AlunoValidator {

    fun parseData(ddMMyyyy: String, hoje: LocalDate = LocalDate.now()): LocalDate? {
        if (ddMMyyyy.length != 8) return null
        return try {
            val d = LocalDate.of(
                ddMMyyyy.substring(4).toInt(), ddMMyyyy.substring(2, 4).toInt(), ddMMyyyy.substring(0, 2).toInt(),
            )
            if (d.isAfter(hoje) || d.year < 1900) null else d
        } catch (e: DateTimeException) { null }
    }

    fun validar(base: Aluno?, input: AlunoInput, hoje: LocalDate = LocalDate.now()): ResultadoAluno {
        val erros = linkedMapOf<CampoAluno, String>()

        val nome = input.nome.trim()
        if (nome.isEmpty()) erros[CampoAluno.NOME] = "O nome do aluno é obrigatório."

        var nascimento: LocalDate? = null
        if (input.dataNascimento.isNotEmpty()) {
            nascimento = parseData(input.dataNascimento, hoje)
            if (nascimento == null) erros[CampoAluno.DATA_NASCIMENTO] = "Data inválida. Use DD/MM/AAAA."
        }
        if (input.telefone.isNotEmpty() && !Validators.telefoneValido(input.telefone))
            erros[CampoAluno.TELEFONE] = "Informe DDD + número (10 ou 11 dígitos)."
        if (input.email.isNotBlank() && !Validators.emailValido(input.email))
            erros[CampoAluno.EMAIL] = "E-mail inválido."
        if (input.cpf.isNotEmpty() && !Validators.cpfValido(input.cpf))
            erros[CampoAluno.CPF] = "CPF inválido ou incompleto."
        if (input.cep.isNotEmpty() && !Validators.cepValido(input.cep))
            erros[CampoAluno.CEP] = "CEP deve ter 8 dígitos."

        if (erros.isNotEmpty()) return ResultadoAluno.Erros(erros)

        fun String.ou() = trim().ifEmpty { null }
        val aluno = (base ?: Aluno(nome = nome, dataCadastro = hoje)).copy(
            nome = nome,
            dataNascimento = nascimento,
            sexo = input.sexo.ou(),
            telefone = input.telefone.ou(),
            email = input.email.ou(),
            cpf = input.cpf.ou(),
            cep = input.cep.ou(),
            instagram = input.instagram.ou()?.removePrefix("@"),
            objetivo = input.objetivo.ou(),
            observacoesMedicas = input.observacoesMedicas.ou(),
            // Marcar registra a data (uma vez); desmarcar revoga; manter preserva a data original.
            consentimentoLgpdEm = when {
                !input.consentimento -> null
                base?.consentimentoLgpdEm != null -> base.consentimentoLgpdEm
                else -> hoje
            },
        )
        return ResultadoAluno.Ok(aluno)
    }

    fun paraInput(a: Aluno) = AlunoInput(
        nome = a.nome,
        dataNascimento = a.dataNascimento?.let { "%02d%02d%04d".format(it.dayOfMonth, it.monthValue, it.year) }.orEmpty(),
        sexo = a.sexo.orEmpty(),
        telefone = Validators.somenteDigitos(a.telefone),
        email = a.email.orEmpty(),
        cpf = Validators.somenteDigitos(a.cpf),
        cep = Validators.somenteDigitos(a.cep),
        instagram = a.instagram.orEmpty(),
        objetivo = a.objetivo.orEmpty(),
        observacoesMedicas = a.observacoesMedicas.orEmpty(),
        consentimento = a.consentimentoLgpdEm != null,
    )
}
