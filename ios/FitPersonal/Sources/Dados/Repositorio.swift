// Repositorio.swift — armazenamento local do aluno (Application Support):
//   pacotes/{treinoId}.json       treino recebido (forma canônica do contrato)
//   videos/{treinoId}/vN.mp4      vídeos do pacote + indice.json (nome do exercício -> arquivo)
//   sessoes.json                  sessões executadas
//
// ATENÇÃO: não compilado/executado (desenvolvido sem Xcode).

import Foundation
import SwiftUI

public enum Recebido {
    case novo(PacoteTreino)
    case atualizado(PacoteTreino)
    case jaTem(PacoteTreino)
    case erro(String)

    var mensagem: String {
        switch self {
        case .novo(let p): return "Treino \"\(p.nome)\" recebido."
        case .atualizado(let p): return "Treino \"\(p.nome)\" atualizado (revisão \(p.revisao))."
        case .jaTem(let p): return "Você já tem esta versão (ou uma mais nova) de \"\(p.nome)\"."
        case .erro(let m): return m
        }
    }
}

@MainActor
public final class Repositorio: ObservableObject {
    @Published public private(set) var treinos: [PacoteTreino] = []
    @Published public private(set) var sessoes: [SessaoRef] = []

    private let fm = FileManager.default
    private let base: URL
    private var pastaPacotes: URL { base.appendingPathComponent("pacotes", isDirectory: true) }
    private var pastaVideos: URL { base.appendingPathComponent("videos", isDirectory: true) }
    private var arquivoSessoes: URL { base.appendingPathComponent("sessoes.json") }

    public init() {
        let suporte = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        base = suporte.appendingPathComponent("FitPersonal", isDirectory: true)
        try? fm.createDirectory(at: pastaPacotes, withIntermediateDirectories: true)
        try? fm.createDirectory(at: pastaVideos, withIntermediateDirectories: true)
        carregar()
    }

    // MARK: leitura

    public func carregar() {
        let arquivos = (try? fm.contentsOfDirectory(at: pastaPacotes, includingPropertiesForKeys: nil)) ?? []
        treinos = arquivos
            .filter { $0.pathExtension == "json" }
            .compactMap { try? String(contentsOf: $0, encoding: .utf8) }
            .compactMap { try? ContratoLeitor.lerPacote($0) }
            .sorted { $0.emitidoEm > $1.emitidoEm }

        if let d = try? Data(contentsOf: arquivoSessoes),
           let lista = try? JSONDecoder().decode([SessaoRef].self, from: d) {
            sessoes = lista
        } else {
            sessoes = []
        }
    }

    public func sessoes(de treinoId: String) -> [SessaoRef] {
        sessoes.filter { $0.treinoId == treinoId }.sorted { $0.data > $1.data }
    }

    private func seguro(_ id: String) -> String { id.filter { $0.isLetter || $0.isNumber || $0 == "-" } }

    // MARK: receber

    /// Aceita o JSON do .rjetreino ou o código de texto (inclusive dentro de uma mensagem).
    @discardableResult
    public func receber(texto: String) -> Recebido {
        let t = texto.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            let json = t.hasPrefix("{") ? t : try CodigoTexto.decodificar(t)
            if ContratoJson.tipo(de: json) == "resumo" { return .erro("Isto é um resumo de treino, não um treino novo.") }
            return try aplicar(json: json, videos: nil)
        } catch {
            return .erro((error as? LocalizedError)?.errorDescription ?? "Não consegui ler este treino.")
        }
    }

    /// Abre um arquivo .rjetreino (JSON) ou .rjetreinopack (zip com vídeos).
    @discardableResult
    public func receber(url: URL) -> Recebido {
        let acessou = url.startAccessingSecurityScopedResource()
        defer { if acessou { url.stopAccessingSecurityScopedResource() } }
        do {
            let cab = try FileHandle(forReadingFrom: url).readData(ofLength: 2)
            if cab == Data([0x50, 0x4B]) {   // "PK"
                let tmp = fm.temporaryDirectory.appendingPathComponent("pacote_\(UUID().uuidString)", isDirectory: true)
                defer { try? fm.removeItem(at: tmp) }
                let extraido = try PacoteVideos.ler(url, pasta: tmp)
                return try aplicar(json: extraido.treinoJson, videos: extraido.videos)
            }
            let texto = try String(contentsOf: url, encoding: .utf8)
            return receber(texto: texto)
        } catch {
            return .erro((error as? LocalizedError)?.errorDescription ?? "Não foi possível abrir o arquivo.")
        }
    }

    private func aplicar(json: String, videos: [VideoExtraido]?) throws -> Recebido {
        let pacote = try ContratoLeitor.lerPacote(json)
        let atual = treinos.first { $0.treinoId == pacote.treinoId }

        if let atual, pacote.revisao <= atual.revisao { return .jaTem(atual) }

        try ContratoJson.serializar(pacote).write(
            to: pastaPacotes.appendingPathComponent("\(seguro(pacote.treinoId)).json"),
            atomically: true, encoding: .utf8
        )

        if let videos {
            let dir = pastaVideos.appendingPathComponent(seguro(pacote.treinoId), isDirectory: true)
            try? fm.removeItem(at: dir)
            try fm.createDirectory(at: dir, withIntermediateDirectories: true)
            var indice: [String: String] = [:]
            for (i, v) in videos.enumerated() {
                let destino = dir.appendingPathComponent("v\(i + 1).mp4")
                try? fm.removeItem(at: destino)
                try fm.copyItem(at: v.arquivo, to: destino)
                v.exercicios.forEach { indice[$0.trimmingCharacters(in: .whitespaces).lowercased()] = destino.lastPathComponent }
            }
            let dados = try JSONSerialization.data(withJSONObject: indice)
            try dados.write(to: dir.appendingPathComponent("indice.json"))
        }

        carregar()
        return atual == nil ? .novo(pacote) : .atualizado(pacote)
    }

    // MARK: vídeos

    public func videoLocal(treinoId: String, exercicio: String) -> URL? {
        let dir = pastaVideos.appendingPathComponent(seguro(treinoId), isDirectory: true)
        guard let d = try? Data(contentsOf: dir.appendingPathComponent("indice.json")),
              let mapa = try? JSONSerialization.jsonObject(with: d) as? [String: String],
              let nome = mapa[exercicio.trimmingCharacters(in: .whitespaces).lowercased()] else { return nil }
        let url = dir.appendingPathComponent(nome)
        return fm.fileExists(atPath: url.path) ? url : nil
    }

    // MARK: sessões e resumo

    public func registrar(_ s: SessaoRef) {
        sessoes.append(s)
        if let d = try? JSONEncoder().encode(sessoes) { try? d.write(to: arquivoSessoes, options: .atomic) }
    }

    public func excluir(treinoId: String) {
        try? fm.removeItem(at: pastaPacotes.appendingPathComponent("\(seguro(treinoId)).json"))
        try? fm.removeItem(at: pastaVideos.appendingPathComponent(seguro(treinoId), isDirectory: true))
        carregar()
    }

    /// Resumo com todas as sessões do treino, pronto para ir ao profissional (nil sem sessões).
    public func resumo(treinoId: String, mensagem: String?) -> Resumo? {
        guard let pacote = treinos.first(where: { $0.treinoId == treinoId }) else { return nil }
        let lista = sessoes(de: treinoId)
        guard !lista.isEmpty else { return nil }
        let datas = lista.map(\.data).sorted()
        let texto = mensagem?.trimmingCharacters(in: .whitespacesAndNewlines)
        return Resumo(
            resumoId: UUID().uuidString.lowercased(), alunoRef: pacote.aluno.ref,
            de: datas.first!, ate: datas.last!, sessoes: lista,
            mensagem: (texto?.isEmpty ?? true) ? nil : texto
        )
    }
}
