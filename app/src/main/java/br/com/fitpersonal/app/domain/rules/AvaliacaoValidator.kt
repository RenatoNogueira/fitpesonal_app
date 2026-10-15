package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.CondicaoSaude
import br.com.fitpersonal.app.domain.model.Dobra
import br.com.fitpersonal.app.domain.model.ProtocoloDobras
import java.time.LocalDate

enum class TipoFoto(val rotulo: String, val chave: String) {
    FRENTE("Frente", "frente"),
    COSTAS("Costas", "costas"),
    LATERAL_DIR("Lateral direita", "lateral_dir"),
    LATERAL_ESQ("Lateral esquerda", "lateral_esq"),
}

enum class CampoAvaliacao {
    DATA, PESO, ALTURA, PERCENTUAL, MASSA_MAGRA, MASSA_GORDA, PAS, PAD, LDL, HDL,
    DOBRA_PEITORAL, DOBRA_AXILAR_MEDIA, DOBRA_TRICEPS, DOBRA_SUBESCAPULAR,
    DOBRA_ABDOMINAL, DOBRA_SUPRAILIACA, DOBRA_COXA,
    CINTURA, QUADRIL,
}

fun Dobra.campo(): CampoAvaliacao = when (this) {
    Dobra.PEITORAL -> CampoAvaliacao.DOBRA_PEITORAL
    Dobra.AXILAR_MEDIA -> CampoAvaliacao.DOBRA_AXILAR_MEDIA
    Dobra.TRICEPS -> CampoAvaliacao.DOBRA_TRICEPS
    Dobra.SUBESCAPULAR -> CampoAvaliacao.DOBRA_SUBESCAPULAR
    Dobra.ABDOMINAL -> CampoAvaliacao.DOBRA_ABDOMINAL
    Dobra.SUPRAILIACA -> CampoAvaliacao.DOBRA_SUPRAILIACA
    Dobra.COXA -> CampoAvaliacao.DOBRA_COXA
}

/** Valores crus do formulário (decimais aceitam vírgula; data em ddMMyyyy). */
data class AvaliacaoInput(
    val data: String = "",
    val peso: String = "",
    val altura: String = "",
    val percentualGordura: String = "",
    val massaMagra: String = "",
    val massaGorda: String = "",
    val pressaoSistolica: String = "",
    val pressaoDiastolica: String = "",
    val ldl: String = "",
    val hdl: String = "",
    val condicoes: Set<CondicaoSaude> = emptySet(),
    val historicoSaude: String = "",
    val anamnese: String = "",
    val estiloVida: String = "",
    val metas: String = "",
    val dobrasCutaneas: String = "",
    val perimetros: String = "",
    val postura: String = "",
    val funcionalMobilidade: String = "",
    val cardio: String = "",
    val forcaResistencia: String = "",
    /** Dobras em mm (texto, aceita vírgula) e protocolo escolhido. */
    val dobras: Map<Dobra, String> = emptyMap(),
    val protocolo: ProtocoloDobras = ProtocoloDobras.POLLOCK_3,
    /** Circunferências em cm (texto, aceita vírgula) para o RCQ. */
    val cintura: String = "",
    val quadril: String = "",
    /** PAR-Q codificado por `ParqRules` ("" = não aplicado). */
    val parq: String = "",
)

enum class Gravidade { INFO, ATENCAO, ALTA }

/** Aviso do resumo: título curto, explicação com os valores e gravidade (define cor e ícone do cartão). */
data class AvisoAvaliacao(val titulo: String, val detalhe: String, val gravidade: Gravidade)

/** Resumo "ao vivo" exibido enquanto o usuário digita (ignora campos inválidos). */
data class ResumoAvaliacao(
    val imc: Double? = null,
    val classeImc: ClasseImc? = null,
    val pressao: ClassePressao? = null,
    val pressaoPopular: String? = null,
    val massaGordaKg: Double? = null,
    val massaMagraKg: Double? = null,
    val alertas: List<String> = emptyList(),
    /** Mesmos alertas (mais IMC) com explicação e gravidade, para os cartões do resumo. */
    val avisos: List<AvisoAvaliacao> = emptyList(),
)

sealed interface ResultadoAvaliacao {
    data class Ok(val avaliacao: AvaliacaoFisica) : ResultadoAvaliacao
    data class Erros(val erros: Map<CampoAvaliacao, String>) : ResultadoAvaliacao
}

object AvaliacaoValidator {

    fun parseDecimal(s: String): Double? =
        s.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    /**
     * Altura em metros digitada só com números: "1" vira "1," e "175" vira "1,75" (até 3 dígitos).
     * [anterior] permite apagar: ao tirar o dígito que sobra depois da vírgula, a vírgula some junto.
     */
    fun formatarAltura(novo: String, anterior: String): String {
        val d = novo.filter(Char::isDigit).take(3)
        return when {
            d.isEmpty() -> ""
            d.length == 1 -> if (novo.length < anterior.length) d else "$d,"
            else -> d[0] + "," + d.substring(1)
        }
    }

    fun formatarDecimal(d: Double?): String = when {
        d == null -> ""
        d % 1.0 == 0.0 -> d.toLong().toString()
        else -> d.toString().replace('.', ',')
    }

    fun resumir(i: AvaliacaoInput): ResumoAvaliacao {
        val peso = parseDecimal(i.peso)
        val altura = parseDecimal(i.altura)
        val perc = parseDecimal(i.percentualGordura)
        val imc = AvaliacaoRules.imc(peso, altura)
        val comp = AvaliacaoRules.composicao(peso, perc, parseDecimal(i.massaGorda), parseDecimal(i.massaMagra))
        val pas = i.pressaoSistolica.toIntOrNull()
        val pad = i.pressaoDiastolica.toIntOrNull()
        val pressao = AvaliacaoRules.classificarPressao(pas, pad)
        val alertas = buildList {
            addAll(AvaliacaoRules.alertasLaboratoriais(perc, parseDecimal(i.ldl), parseDecimal(i.hdl)))
            if (pressao?.alerta == true) add(pressao.rotulo)
            if (ParqRules.requerLiberacao(i.parq)) {
                add("PAR-Q com resposta \"sim\": recomenda-se liberação médica antes de iniciar ou intensificar o treino.")
            }
        }
        val ldl = parseDecimal(i.ldl)
        val hdl = parseDecimal(i.hdl)
        val br = java.util.Locale("pt", "BR")
        val avisos = buildList {
            if (imc != null) {
                val classe = AvaliacaoRules.classificarImc(imc)
                val g = when (classe) {
                    ClasseImc.NORMAL -> null
                    ClasseImc.OBESIDADE_2, ClasseImc.OBESIDADE_3 -> Gravidade.ALTA
                    else -> Gravidade.ATENCAO
                }
                if (g != null) add(AvisoAvaliacao(classe.rotulo, "IMC de " + String.format(br, "%.1f", imc) + " kg/m².", g))
            }
            if (perc != null && perc >= 30) {
                add(AvisoAvaliacao("Gordura corporal elevada", "Percentual de " + String.format(br, "%.1f", perc) + "%, acima de 30%.", Gravidade.ATENCAO))
            }
            if (ldl != null) {
                if (ldl >= 160) add(AvisoAvaliacao("LDL alto", "LDL de " + String.format(br, "%.0f", ldl) + " mg/dL (desejável abaixo de 130).", Gravidade.ALTA))
                else if (ldl >= 130) add(AvisoAvaliacao("LDL limítrofe", "LDL de " + String.format(br, "%.0f", ldl) + " mg/dL (desejável abaixo de 130).", Gravidade.ATENCAO))
            }
            if (hdl != null && hdl < 40) {
                add(AvisoAvaliacao("HDL baixo", "HDL de " + String.format(br, "%.0f", hdl) + " mg/dL (desejável acima de 40).", Gravidade.ATENCAO))
            }
            if (pressao?.alerta == true) {
                val g = if (pressao == ClassePressao.CRISE || pressao == ClassePressao.HIPERTENSAO_2) Gravidade.ALTA else Gravidade.ATENCAO
                val medida = if (pas != null && pad != null) "Pressão aferida: $pas/$pad mmHg." else "Pressão fora da faixa normal."
                add(AvisoAvaliacao(pressao.rotulo, medida, g))
            }
            if (ParqRules.requerLiberacao(i.parq)) {
                add(AvisoAvaliacao("PAR-Q com resposta \"sim\"", "Recomenda-se liberação médica antes de iniciar ou intensificar o treino.", Gravidade.ALTA))
            }
        }
        return ResumoAvaliacao(
            imc = imc,
            classeImc = imc?.let(AvaliacaoRules::classificarImc),
            pressao = pressao,
            pressaoPopular = AvaliacaoRules.pressaoPopular(pas, pad),
            massaGordaKg = comp.massaGordaKg,
            massaMagraKg = comp.massaMagraKg,
            alertas = alertas,
            avisos = avisos,
        )
    }

    /**
     * Regras de on_salvar do desktop: data vazia = hoje; altura 0,5–2,5 m; peso 10–400 kg;
     * massa gorda/magra calculadas a partir do % quando ausentes.
     * Fotos não passam por aqui: o chamador as aplica sobre o resultado.
     */
    fun validar(base: AvaliacaoFisica, i: AvaliacaoInput, hoje: LocalDate = LocalDate.now()): ResultadoAvaliacao {
        val erros = linkedMapOf<CampoAvaliacao, String>()

        val data = if (i.data.isEmpty()) hoje else AlunoValidator.parseData(i.data, hoje)
        if (data == null) erros[CampoAvaliacao.DATA] = "Data inválida. Use DD/MM/AAAA."

        fun decimal(texto: String, campo: CampoAvaliacao, faixa: ClosedFloatingPointRange<Double>?, msg: String): Double? {
            if (texto.isBlank()) return null
            val v = parseDecimal(texto)
            if (v == null || (faixa != null && v !in faixa)) { erros[campo] = msg; return null }
            return v
        }

        val peso = decimal(i.peso, CampoAvaliacao.PESO, AvaliacaoRules.PESO_MIN..AvaliacaoRules.PESO_MAX,
            "Informe o peso em kg (10 a 400), ex.: 72,5")
        val altura = decimal(i.altura, CampoAvaliacao.ALTURA, AvaliacaoRules.ALTURA_MIN..AvaliacaoRules.ALTURA_MAX,
            "Informe a altura em metros (0,5 a 2,5), ex.: 1,75")
        val perc = decimal(i.percentualGordura, CampoAvaliacao.PERCENTUAL, 0.0..100.0, "Percentual entre 0 e 100")
        val magra = decimal(i.massaMagra, CampoAvaliacao.MASSA_MAGRA, 0.0..400.0, "Massa magra inválida")
        val gorda = decimal(i.massaGorda, CampoAvaliacao.MASSA_GORDA, 0.0..400.0, "Massa gorda inválida")
        val ldl = decimal(i.ldl, CampoAvaliacao.LDL, 0.0..1000.0, "LDL inválido")
        val hdl = decimal(i.hdl, CampoAvaliacao.HDL, 0.0..1000.0, "HDL inválido")

        fun inteiro(texto: String, campo: CampoAvaliacao, faixa: IntRange, msg: String): Int? {
            if (texto.isBlank()) return null
            val v = texto.toIntOrNull()
            if (v == null || v !in faixa) { erros[campo] = msg; return null }
            return v
        }
        val pas = inteiro(i.pressaoSistolica, CampoAvaliacao.PAS, 40..300, "Sistólica entre 40 e 300 mmHg")
        val pad = inteiro(i.pressaoDiastolica, CampoAvaliacao.PAD, 20..200, "Diastólica entre 20 e 200 mmHg")

        val dobrasMm = buildMap<Dobra, Double> {
            i.dobras.forEach { (d, texto) ->
                val v = decimal(texto, d.campo(), Pollock.DOBRA_MIN_MM..Pollock.DOBRA_MAX_MM,
                    "Dobra entre 1 e 80 mm")
                if (v != null) put(d, v)
            }
        }

        val cintura = decimal(i.cintura, CampoAvaliacao.CINTURA, 30.0..250.0, "Cintura entre 30 e 250 cm")
        val quadril = decimal(i.quadril, CampoAvaliacao.QUADRIL, 40.0..250.0, "Quadril entre 40 e 250 cm")

        if (erros.isNotEmpty()) return ResultadoAvaliacao.Erros(erros)

        val comp = AvaliacaoRules.composicao(peso, perc, gorda, magra)
        fun String.ou() = trim().ifEmpty { null }

        return ResultadoAvaliacao.Ok(
            base.copy(
                data = data!!,
                pesoKg = peso, alturaM = altura, percentualGordura = perc,
                massaGordaKg = comp.massaGordaKg, massaMagraKg = comp.massaMagraKg,
                ldl = ldl, hdl = hdl, pressaoSistolica = pas, pressaoDiastolica = pad,
                condicoes = i.condicoes,
                historicoSaude = i.historicoSaude.ou(), anamnese = i.anamnese.ou(),
                estiloVida = i.estiloVida.ou(), metas = i.metas.ou(),
                dobrasCutaneas = i.dobrasCutaneas.ou(), perimetros = i.perimetros.ou(),
                postura = i.postura.ou(), funcionalMobilidade = i.funcionalMobilidade.ou(),
                cardio = i.cardio.ou(), forcaResistencia = i.forcaResistencia.ou(),
                protocoloDobras = if (dobrasMm.isEmpty()) null else i.protocolo,
                dobrasMm = dobrasMm,
                cinturaCm = cintura,
                quadrilCm = quadril,
                parq = i.parq.takeIf { ParqRules.aplicado(it) },
            )
        )
    }

    fun paraInput(a: AvaliacaoFisica) = AvaliacaoInput(
        data = "%02d%02d%04d".format(a.data.dayOfMonth, a.data.monthValue, a.data.year),
        peso = formatarDecimal(a.pesoKg),
        altura = formatarDecimal(a.alturaM),
        percentualGordura = formatarDecimal(a.percentualGordura),
        massaMagra = formatarDecimal(a.massaMagraKg),
        massaGorda = formatarDecimal(a.massaGordaKg),
        pressaoSistolica = a.pressaoSistolica?.toString().orEmpty(),
        pressaoDiastolica = a.pressaoDiastolica?.toString().orEmpty(),
        ldl = formatarDecimal(a.ldl),
        hdl = formatarDecimal(a.hdl),
        condicoes = a.condicoes,
        historicoSaude = a.historicoSaude.orEmpty(),
        anamnese = a.anamnese.orEmpty(),
        estiloVida = a.estiloVida.orEmpty(),
        metas = a.metas.orEmpty(),
        dobrasCutaneas = a.dobrasCutaneas.orEmpty(),
        perimetros = a.perimetros.orEmpty(),
        postura = a.postura.orEmpty(),
        funcionalMobilidade = a.funcionalMobilidade.orEmpty(),
        cardio = a.cardio.orEmpty(),
        forcaResistencia = a.forcaResistencia.orEmpty(),
        dobras = a.dobrasMm.mapValues { formatarDecimal(it.value) },
        protocolo = a.protocoloDobras ?: ProtocoloDobras.POLLOCK_3,
        cintura = formatarDecimal(a.cinturaCm),
        quadril = formatarDecimal(a.quadrilCm),
        parq = a.parq.orEmpty(),
    )

    fun fotosDe(a: AvaliacaoFisica): Map<TipoFoto, String> = buildMap {
        a.fotoFrente?.let { put(TipoFoto.FRENTE, it) }
        a.fotoCostas?.let { put(TipoFoto.COSTAS, it) }
        a.fotoLateralDir?.let { put(TipoFoto.LATERAL_DIR, it) }
        a.fotoLateralEsq?.let { put(TipoFoto.LATERAL_ESQ, it) }
    }

    fun comFotos(a: AvaliacaoFisica, f: Map<TipoFoto, String>) = a.copy(
        fotoFrente = f[TipoFoto.FRENTE], fotoCostas = f[TipoFoto.COSTAS],
        fotoLateralDir = f[TipoFoto.LATERAL_DIR], fotoLateralEsq = f[TipoFoto.LATERAL_ESQ],
    )
}
