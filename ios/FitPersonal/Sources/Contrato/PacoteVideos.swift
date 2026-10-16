// PacoteVideos.swift — leitura do pacote "treino + vídeos" (.rjetreinopack), um zip com:
//   treino.rjetreino · videos.json · videos/v1.mp4 …
// Leitor mínimo de ZIP (diretório central; métodos 0 = armazenado e 8 = deflate). Sem dependências.
//
// ATENÇÃO: não compilado/executado (desenvolvido sem Xcode). Ver docs/contrato/ESPECIFICACAO.md.

import Foundation

public struct VideoExtraido {
    public var exercicios: [String]
    public var arquivo: URL
}

public struct PacoteExtraido {
    public var treinoJson: String
    public var videos: [VideoExtraido]
}

public enum PacoteVideos {
    static let maxTotal: Int64 = 600 * 1024 * 1024
    static let maxVideos = 40
    static let maxJSON = 512 * 1024
    static let maxIndice = 64 * 1024

    private struct Entrada {
        var nome: String
        var metodo: UInt16
        var tamComprimido: Int
        var tamOriginal: Int
        var deslocamentoLocal: Int
    }

    private static func u16(_ d: Data, _ i: Int) -> Int {
        Int(d[d.startIndex + i]) | (Int(d[d.startIndex + i + 1]) << 8)
    }

    private static func u32(_ d: Data, _ i: Int) -> Int {
        u16(d, i) | (u16(d, i + 2) << 16)
    }

    private static func nomePermitido(_ nome: String) -> Bool {
        if nome == "treino.rjetreino" || nome == "videos.json" { return true }
        guard nome.hasPrefix("videos/v"), nome.hasSuffix(".mp4") else { return false }
        let miolo = nome.dropFirst("videos/v".count).dropLast(".mp4".count)
        return !miolo.isEmpty && miolo.count <= 3 && miolo.allSatisfy(\.isNumber)
    }

    private static func entradas(_ zip: Data) throws -> [Entrada] {
        // Fim do diretório central: assinatura 0x06054b50 nos últimos 64 KB + 22 bytes.
        let minimo = 22
        guard zip.count >= minimo else { throw ContratoErro.jsonInvalido }
        var i = zip.count - minimo
        let limite = max(0, zip.count - minimo - 65_535)
        while i >= limite {
            if u32(zip, i) == 0x0605_4b50 { break }
            i -= 1
        }
        guard i >= limite else { throw ContratoErro.jsonInvalido }

        let total = u16(zip, i + 10)
        var p = u32(zip, i + 16)
        var saida: [Entrada] = []
        for _ in 0..<total {
            guard p + 46 <= zip.count, u32(zip, p) == 0x0201_4b50 else { throw ContratoErro.jsonInvalido }
            let metodo = UInt16(u16(zip, p + 10))
            let tamC = u32(zip, p + 20)
            let tamO = u32(zip, p + 24)
            let nLen = u16(zip, p + 28)
            let eLen = u16(zip, p + 30)
            let cLen = u16(zip, p + 32)
            let local = u32(zip, p + 42)
            guard p + 46 + nLen <= zip.count else { throw ContratoErro.jsonInvalido }
            let bytesNome = zip.subdata(in: (zip.startIndex + p + 46)..<(zip.startIndex + p + 46 + nLen))
            let nome = String(decoding: bytesNome, as: UTF8.self)
            saida.append(Entrada(nome: nome, metodo: metodo, tamComprimido: tamC, tamOriginal: tamO, deslocamentoLocal: local))
            p += 46 + nLen + eLen + cLen
        }
        return saida
    }

    private static func conteudo(_ zip: Data, _ e: Entrada) throws -> Data {
        let l = e.deslocamentoLocal
        guard l + 30 <= zip.count, u32(zip, l) == 0x0403_4b50 else { throw ContratoErro.jsonInvalido }
        let inicio = l + 30 + u16(zip, l + 26) + u16(zip, l + 28)
        guard inicio + e.tamComprimido <= zip.count else { throw ContratoErro.jsonInvalido }
        let bruto = zip.subdata(in: (zip.startIndex + inicio)..<(zip.startIndex + inicio + e.tamComprimido))
        switch e.metodo {
        case 0: return bruto
        case 8: return try (bruto as NSData).decompressed(using: .zlib) as Data
        default: throw ContratoErro.jsonInvalido
        }
    }

    /// Lê o pacote em [url] e grava os vídeos em [pasta] (v1.mp4, v2.mp4…). Ignora qualquer outro nome do zip.
    public static func ler(_ url: URL, pasta: URL) throws -> PacoteExtraido {
        let zip = try Data(contentsOf: url, options: .mappedIfSafe)
        let lista = try entradas(zip).filter { nomePermitido($0.nome) }
        try FileManager.default.createDirectory(at: pasta, withIntermediateDirectories: true)

        var treino: String?
        var indice: [[String: Any]] = []
        var arquivos: [String: URL] = [:]
        var total: Int64 = 0

        for e in lista {
            guard e.tamOriginal >= 0 else { continue }
            switch e.nome {
            case "treino.rjetreino":
                guard e.tamOriginal <= maxJSON else { throw ContratoErro.tamanho }
                treino = String(decoding: try conteudo(zip, e), as: UTF8.self)
            case "videos.json":
                guard e.tamOriginal <= maxIndice else { throw ContratoErro.tamanho }
                let obj = try? JSONSerialization.jsonObject(with: try conteudo(zip, e)) as? [String: Any]
                indice = (obj?["videos"] as? [[String: Any]]) ?? []
            default:
                guard arquivos.count < maxVideos else { continue }
                total += Int64(e.tamOriginal)
                guard total <= maxTotal else { throw ContratoErro.tamanho }
                let nomeLocal = String(e.nome.dropFirst("videos/".count))
                let destino = pasta.appendingPathComponent(nomeLocal)
                try conteudo(zip, e).write(to: destino, options: .atomic)
                arquivos[nomeLocal] = destino
            }
        }
        guard let json = treino else { throw ContratoErro.jsonInvalido }

        var videos: [VideoExtraido] = []
        for item in indice {
            guard let nome = item["arquivo"] as? String, let destino = arquivos[nome],
                  let exs = item["exercicios"] as? [String] else { continue }
            let validos = exs.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
            if !validos.isEmpty { videos.append(VideoExtraido(exercicios: validos, arquivo: destino)) }
        }
        return PacoteExtraido(treinoJson: json, videos: videos)
    }
}
