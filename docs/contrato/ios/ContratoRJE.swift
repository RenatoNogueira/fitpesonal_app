// ContratoRJE.swift — implementação de REFERÊNCIA do contrato (formato 1) para o app iOS do aluno.
//
// ATENÇÃO: este arquivo NÃO foi compilado nem executado (o projeto é desenvolvido sem Xcode).
// Ele traduz docs/contrato/ESPECIFICACAO.md; valide-o contra os arquivos em docs/contrato/exemplos/
// (ler, re-serializar e comparar byte a byte; decodificar os *.codigo.txt).

import Foundation
import CryptoKit

// MARK: - Modelos

public struct ProfissionalRef: Equatable {
    public var nome: String
    public var marca: String = ""
    public var cref: String = ""
    public var contato: String = ""
}

public struct AlunoRef: Equatable { public var nome: String; public var ref: String }

public struct ExercicioRef: Equatable {
    public var nome: String
    public var series: String?
    public var repeticoes: String?
    public var carga: String?
    public var descanso: String?
    public var obs: String?
    public var catalogoId: String?
}

public struct PacoteTreino: Equatable {
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

public struct ItemSessao: Equatable {
    public var exercicio: String
    public var cargaKg: Double?
    public var reps: String?
    public var feito: Bool
}

public struct SessaoRef: Equatable {
    public var execucaoId: String
    public var data: String
    public var treinoId: String
    public var divisao: String
    public var esforco: Int?
    public var itens: [ItemSessao]
}

public struct Resumo: Equatable {
    public var resumoId: String
    public var alunoRef: String
    public var de: String
    public var ate: String
    public var sessoes: [SessaoRef]
    public var mensagem: String?
}

public enum ContratoErro: Error {
    case jsonInvalido, tipoInesperado, versaoFutura, integridade, campoInvalido(String), codigoInvalido, tamanho
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
        return String(d)   // "22.5"; revisar casos com muitas casas contra os exemplos
    }

    static func pacote(_ p: PacoteTreino) -> String {
        let divs = ["A", "B", "C", "D"].compactMap { d -> String? in
            guard let lista = p.divisoes[d], !lista.isEmpty else { return nil }
            let itens = lista.map { e in
                "{\"nome\":\(texto(e.nome)),\"series\":\(nulo(e.series)),\"repeticoes\":\(nulo(e.repeticoes))," +
                "\"carga\":\(nulo(e.carga)),\"descanso\":\(nulo(e.descanso)),\"obs\":\(nulo(e.obs))," +
                "\"catalogoId\":\(nulo(e.catalogoId))}"
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

// MARK: - Leitura (esboço): validar `formato`, `tipo`, limites e `integridade`
//
// Passos (mesmos do Kotlin, ContratoJson.lerPacote / lerResumo):
//  1. JSONSerialization -> dicionário; recusar > 512 KB.
//  2. formato > 1 -> ContratoErro.versaoFutura; tipo diferente do esperado -> tipoInesperado.
//  3. Validar campos (ids [A-Za-z0-9_-]{1,64}, datas yyyy-MM-dd, limites de tamanho, divisões A–D).
//  4. Reconstruir o modelo e recalcular Canonico.pacote/resumo; se houver "integridade",
//     comparar com "sha256:" + sha256(canônico) — divergente => ContratoErro.integridade.
