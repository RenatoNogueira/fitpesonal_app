// ContratoRJE.swift — contrato de troca (formato 1) para o app iOS.
//
// ATENÇÃO: não foi compilado nem executado (desenvolvido sem Xcode). Traduz docs/contrato/ESPECIFICACAO.md
// e deve ser validado contra docs/contrato/exemplos/ (ver Tests/ContratoTests.swift).

import Foundation
import CryptoKit

// MARK: - Modelos

public struct ProfissionalRef: Equatable, Codable {
    public var nome: String
    public var marca: String = ""
    public var cref: String = ""
    public var contato: String = ""
}

public struct AlunoRef: Equatable, Codable { public var nome: String; public var ref: String }

public struct ExercicioRef: Equatable, Codable, Identifiable {
    public var nome: String
    public var series: String?
    public var repeticoes: String?
    public var carga: String?
    public var descanso: String?
    public var obs: String?
    public var catalogoId: String?
    /// Link (http/https) de vídeo de demonstração; só vai no arquivo quando existe.
    public var video: String?

    public var id: String { nome }
}

public struct PacoteTreino: Equatable, Codable {
    public var treinoId: String
    public var revisao: Int
    public var emitidoEm: String
    public var validoAte: String?
    public var profissional: ProfissionalRef
    public var aluno: AlunoRef
    public var nome: String
    public var objetivo: String?
    /// Chaves "A"..."D"
    public var divisoes: [String: [ExercicioRef]]
}

public struct ItemSessao: Equatable, Codable {
    public var exercicio: String
    public var cargaKg: Double?
    public var reps: String?
    public var feito: Bool
}

public struct SessaoRef: Equatable, Codable {
    public var execucaoId: String
    public var data: String
    public var treinoId: String
    public var divisao: String
    public var esforco: Int?
    public var itens: [ItemSessao]
}

public struct Resumo: Equatable, Codable {
    public var resumoId: String
    public var alunoRef: String
    public var de: String
    public var ate: String
    public var sessoes: [SessaoRef]
    public var mensagem: String?
}

public enum ContratoErro: Error, LocalizedError {
    case jsonInvalido, tipoInesperado, versaoFutura, integridade, campoInvalido(String), codigoInvalido, tamanho

    public var errorDescription: String? {
        switch self {
        case .jsonInvalido: return "O arquivo não é um treino válido."
        case .tipoInesperado: return "Isto não é um treino (pode ser um resumo)."
        case .versaoFutura: return "Este treino foi criado por uma versão mais nova do app. Atualize."
        case .integridade: return "O arquivo parece alterado ou corrompido."
        case .campoInvalido(let c): return "Treino inválido (campo \(c))."
        case .codigoInvalido: return "Não encontrei um código de treino neste texto."
        case .tamanho: return "O arquivo é grande demais."
        }
    }
}

// MARK: - Escrita canônica (ordem de chaves fixa; ver seção 2 da especificação)

enum Canonico {
    static func texto(_ s: String) -> String {
        var out = "\""
        for u in s.unicodeScalars {
            switch u {
            case "\"": out += "\\\""
            case "\\": out += "\\\\"
            case "\n": out += "\\n"
            case "\r": out += "\\r"
            case "\t": out += "\\t"
            default:
                if u.value < 0x20 { out += String(format: "\\u%04x", u.value) } else { out.unicodeScalars.append(u) }
            }
        }
        return out + "\""
    }

    static func nulo(_ s: String?) -> String { s.map(texto) ?? "null" }

    /// Decimais sem zeros à direita e sem notação científica.
    static func decimal(_ d: Double?) -> String {
        guard let d else { return "null" }
        if d == d.rounded(), abs(d) < 1e15 { return String(Int64(d)) }
        return String(d)
    }

    static func pacote(_ p: PacoteTreino) -> String {
        let divs = ["A", "B", "C", "D"].compactMap { d -> String? in
            guard let lista = p.divisoes[d], !lista.isEmpty else { return nil }
            let itens = lista.map { e -> String in
                var s = "{\"nome\":\(texto(e.nome)),\"series\":\(nulo(e.series)),\"repeticoes\":\(nulo(e.repeticoes))," +
                    "\"carga\":\(nulo(e.carga)),\"descanso\":\(nulo(e.descanso)),\"obs\":\(nulo(e.obs))," +
                    "\"catalogoId\":\(nulo(e.catalogoId))"
                if let v = e.video { s += ",\"video\":\(texto(v))" }   // opcional: ausente quando nulo
                return s + "}"
            }.joined(separator: ",")
            return "\(texto(d)):[\(itens)]"
        }.joined(separator: ",")
        return "{\"formato\":1,\"tipo\":\"treino\",\"treinoId\":\(texto(p.treinoId)),\"revisao\":\(p.revisao)," +
            "\"emitidoEm\":\(texto(p.emitidoEm)),\"validoAte\":\(nulo(p.validoAte))," +
            "\"profissional\":{\"nome\":\(texto(p.profissional.nome)),\"marca\":\(texto(p.profissional.marca))," +
            "\"cref\":\(texto(p.profissional.cref)),\"contato\":\(texto(p.profissional.contato))}," +
            "\"aluno\":{\"nome\":\(texto(p.aluno.nome)),\"ref\":\(texto(p.aluno.ref))}," +
            "\"treino\":{\"nome\":\(texto(p.nome)),\"objetivo\":\(nulo(p.objetivo)),\"divisoes\":{\(divs)}}}"
    }

    static func resumo(_ r: Resumo) -> String {
        let sessoes = r.sessoes.map { s -> String in
            let itens = s.itens.map {
                "{\"exercicio\":\(texto($0.exercicio)),\"cargaKg\":\(decimal($0.cargaKg)),\"reps\":\(nulo($0.reps))," +
                "\"feito\":\($0.feito ? "true" : "false")}"
            }.joined(separator: ",")
            return "{\"execucaoId\":\(texto(s.execucaoId)),\"data\":\(texto(s.data)),\"treinoId\":\(texto(s.treinoId))," +
                "\"divisao\":\(texto(s.divisao)),\"esforco\":\(s.esforco.map(String.init) ?? "null"),\"itens\":[\(itens)]}"
        }.joined(separator: ",")
        return "{\"formato\":1,\"tipo\":\"resumo\",\"resumoId\":\(texto(r.resumoId)),\"alunoRef\":\(texto(r.alunoRef))," +
            "\"periodo\":{\"de\":\(texto(r.de)),\"ate\":\(texto(r.ate))},\"sessoes\":[\(sessoes)]," +
            "\"mensagem\":\(nulo(r.mensagem))}"
    }

    static func sha256(_ s: String) -> String {
        SHA256.hash(data: Data(s.utf8)).map { String(format: "%02x", $0) }.joined()
    }

    static func comIntegridade(_ base: String) -> String {
        String(base.dropLast()) + ",\"integridade\":\(texto("sha256:" + sha256(base)))}"
    }
}

// MARK: - Serialização pública

public enum ContratoJson {
    public static func serializar(_ r: Resumo) -> String { Canonico.comIntegridade(Canonico.resumo(r)) }
    public static func serializar(_ p: PacoteTreino) -> String { Canonico.comIntegridade(Canonico.pacote(p)) }

    /// "treino", "resumo" ou nil.
    public static func tipo(de json: String) -> String? {
        guard let o = try? JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any] else { return nil }
        return o["tipo"] as? String
    }
}

// MARK: - Leitura (validação como no Kotlin: formato, tipo, limites e integridade)

public enum ContratoLeitor {
    static let limiteJSON = 512 * 1024
    private static let idValido = try! NSRegularExpression(pattern: "^[A-Za-z0-9_-]{1,64}$")
    private static let dataValida = try! NSRegularExpression(pattern: "^\\d{4}-\\d{2}-\\d{2}$")

    private static func casa(_ re: NSRegularExpression, _ s: String) -> Bool {
        re.firstMatch(in: s, range: NSRange(s.startIndex..., in: s)) != nil
    }

    private static func obrigatorio(_ o: [String: Any], _ k: String, max: Int = 200) throws -> String {
        guard let s = o[k] as? String, !s.isEmpty, s.count <= max else { throw ContratoErro.campoInvalido(k) }
        return s
    }

    private static func opcional(_ o: [String: Any], _ k: String, max: Int = 200) throws -> String? {
        guard let v = o[k], !(v is NSNull) else { return nil }
        guard let s = v as? String, s.count <= max else { throw ContratoErro.campoInvalido(k) }
        return s.isEmpty ? nil : s
    }

    public static func lerPacote(_ json: String) throws -> PacoteTreino {
        guard json.utf8.count <= limiteJSON else { throw ContratoErro.tamanho }
        guard let raiz = try? JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any] else {
            throw ContratoErro.jsonInvalido
        }
        if let f = raiz["formato"] as? Int, f > 1 { throw ContratoErro.versaoFutura }
        guard (raiz["formato"] as? Int) == 1 else { throw ContratoErro.campoInvalido("formato") }
        guard (raiz["tipo"] as? String) == "treino" else { throw ContratoErro.tipoInesperado }

        let treinoId = try obrigatorio(raiz, "treinoId", max: 64)
        guard casa(idValido, treinoId) else { throw ContratoErro.campoInvalido("treinoId") }
        guard let revisao = raiz["revisao"] as? Int, revisao >= 1 else { throw ContratoErro.campoInvalido("revisao") }
        let emitidoEm = try obrigatorio(raiz, "emitidoEm", max: 10)
        guard casa(dataValida, emitidoEm) else { throw ContratoErro.campoInvalido("emitidoEm") }

        guard let prof = raiz["profissional"] as? [String: Any],
              let alu = raiz["aluno"] as? [String: Any],
              let tr = raiz["treino"] as? [String: Any],
              let divs = tr["divisoes"] as? [String: Any] else { throw ContratoErro.jsonInvalido }

        let alunoRef = try obrigatorio(alu, "ref", max: 64)
        guard casa(idValido, alunoRef) else { throw ContratoErro.campoInvalido("aluno.ref") }

        var divisoes: [String: [ExercicioRef]] = [:]
        for d in ["A", "B", "C", "D"] {
            guard let lista = divs[d] as? [[String: Any]] else { continue }
            guard lista.count <= 300 else { throw ContratoErro.tamanho }
            divisoes[d] = try lista.map { e in
                let video = try opcional(e, "video", max: 500).flatMap { v -> String? in
                    let m = v.lowercased()
                    let ok = (m.hasPrefix("http://") || m.hasPrefix("https://")) && !v.contains(where: { $0.isWhitespace })
                    return ok ? v : nil
                }
                return ExercicioRef(
                    nome: try obrigatorio(e, "nome"),
                    series: try opcional(e, "series", max: 40),
                    repeticoes: try opcional(e, "repeticoes", max: 40),
                    carga: try opcional(e, "carga", max: 60),
                    descanso: try opcional(e, "descanso", max: 40),
                    obs: try opcional(e, "obs", max: 1000),
                    catalogoId: try opcional(e, "catalogoId", max: 80),
                    video: video
                )
            }
        }

        let pacote = PacoteTreino(
            treinoId: treinoId, revisao: revisao, emitidoEm: emitidoEm,
            validoAte: try opcional(raiz, "validoAte", max: 10),
            profissional: ProfissionalRef(
                nome: try obrigatorio(prof, "nome"),
                marca: (try opcional(prof, "marca")) ?? "",
                cref: (try opcional(prof, "cref")) ?? "",
                contato: (try opcional(prof, "contato")) ?? ""
            ),
            aluno: AlunoRef(nome: try obrigatorio(alu, "nome"), ref: alunoRef),
            nome: try obrigatorio(tr, "nome"),
            objetivo: try opcional(tr, "objetivo", max: 200),
            divisoes: divisoes
        )

        // Integridade: recalcula o texto canônico e compara quando o arquivo traz o checksum.
        if let esperado = raiz["integridade"] as? String {
            let calculado = "sha256:" + Canonico.sha256(Canonico.pacote(pacote))
            guard esperado == calculado else { throw ContratoErro.integridade }
        }
        return pacote
    }
}

// MARK: - Código de texto (RJE1: DEFLATE bruto + base64url; RJE0: sem compressão)

public enum CodigoTexto {
    static let limite = 512 * 1024

    public static func codificar(json: String) throws -> String {
        let bruto = Data(json.utf8)
        // NSData.compressed(using: .zlib) produz DEFLATE bruto (RFC 1951), sem cabeçalho zlib.
        let comprimido = try (bruto as NSData).compressed(using: .zlib) as Data
        return "RJE1:" + base64url(comprimido)
    }

    public static func decodificar(_ texto: String) throws -> String {
        guard let r = texto.range(of: "RJE[01]:[A-Za-z0-9_-]+", options: .regularExpression) else {
            throw ContratoErro.codigoInvalido
        }
        let achado = String(texto[r])
        guard let dados = dadosBase64url(String(achado.dropFirst(5))) else { throw ContratoErro.codigoInvalido }
        if achado.hasPrefix("RJE0:") {
            guard dados.count <= limite else { throw ContratoErro.tamanho }
            return String(decoding: dados, as: UTF8.self)
        }
        let saida = try (dados as NSData).decompressed(using: .zlib) as Data
        guard saida.count <= limite else { throw ContratoErro.tamanho }
        return String(decoding: saida, as: UTF8.self)
    }

    static func base64url(_ d: Data) -> String {
        d.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    static func dadosBase64url(_ s: String) -> Data? {
        var b = s.replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        while b.count % 4 != 0 { b += "=" }
        return Data(base64Encoded: b)
    }
}
