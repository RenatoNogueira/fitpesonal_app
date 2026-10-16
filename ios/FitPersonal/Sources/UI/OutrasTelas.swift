import SwiftUI
import AVKit

// MARK: - Histórico e envio do resumo

struct HistoricoView: View {
    @EnvironmentObject private var repo: Repositorio
    let pacote: PacoteTreino
    @State private var recado = ""

    private var sessoes: [SessaoRef] { repo.sessoes(de: pacote.treinoId) }

    private var codigo: String? {
        guard let r = repo.resumo(treinoId: pacote.treinoId, mensagem: recado) else { return nil }
        let json = ContratoJson.serializar(r)
        return try? CodigoTexto.codificar(json: json)
    }

    var body: some View {
        List {
            Section {
                Text("\(sessoes.count) sessão(ões) registradas").foregroundStyle(.secondary)
            }
            ForEach(sessoes, id: \.execucaoId) { s in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(s.data).font(.headline)
                        Spacer()
                        Etiqueta(texto: "Treino \(s.divisao)")
                    }
                    let feitos = s.itens.filter(\.feito).count
                    Text("\(feitos) de \(s.itens.count) exercícios" + (s.esforco.map { " • esforço \($0)/10" } ?? ""))
                        .font(.subheadline)
                    let cargas = s.itens.filter(\.feito).compactMap { i in
                        i.cargaKg.map { "\(i.exercicio): \(String(format: "%g", $0)) kg" }
                    }
                    if !cargas.isEmpty {
                        Text(cargas.joined(separator: " • ")).font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }
            if !sessoes.isEmpty {
                Section("Enviar resumo ao professor") {
                    TextField("Recado (opcional)", text: $recado, axis: .vertical).lineLimit(1...4)
                    if let codigo {
                        ShareLink(item: "Meu resumo de treino:\n\(codigo)") {
                            Label("Enviar resumo", systemImage: "paperplane.fill")
                        }
                    }
                }
            }
        }
        .navigationTitle("Histórico")
    }
}

// MARK: - Colar código

struct ColarCodigoView: View {
    var aoReceber: (String) -> Void
    @State private var texto = ""
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("Cole a mensagem do professor") {
                    TextEditor(text: $texto).frame(minHeight: 140)
                }
                Button("Colar do que foi copiado") {
                    if let c = UIPasteboard.general.string { texto = c }
                }
            }
            .navigationTitle("Colar código")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Receber") { aoReceber(texto); dismiss() }
                        .disabled(texto.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

// MARK: - Lembretes

struct LembretesView: View {
    @EnvironmentObject private var repo: Repositorio
    @State private var config = LembreteConfig.carregar()
    @Environment(\.dismiss) private var dismiss

    private let dias: [(Int, String)] = [(2, "Seg"), (3, "Ter"), (4, "Qua"), (5, "Qui"), (6, "Sex"), (7, "Sáb"), (1, "Dom")]

    var body: some View {
        NavigationStack {
            Form {
                Toggle("Lembrar de treinar", isOn: $config.ativo)
                if config.ativo {
                    Section("Dias da semana") {
                        HStack {
                            ForEach(dias, id: \.0) { d, nome in
                                let marcado = config.dias.contains(d)
                                Button(nome) {
                                    if marcado { config.dias.remove(d) } else { config.dias.insert(d) }
                                }
                                .buttonStyle(.bordered)
                                .tint(marcado ? Tema.verde : .gray)
                            }
                        }
                    }
                    Section("Horário") {
                        DatePicker(
                            "Hora do aviso",
                            selection: Binding(
                                get: {
                                    Calendar.current.date(
                                        bySettingHour: config.minutos / 60, minute: config.minutos % 60, second: 0, of: Date()
                                    ) ?? Date()
                                },
                                set: {
                                    let c = Calendar.current.dateComponents([.hour, .minute], from: $0)
                                    config.minutos = (c.hour ?? 18) * 60 + (c.minute ?? 0)
                                }
                            ),
                            displayedComponents: .hourAndMinute
                        )
                    }
                    Text("Se você ficar 3 dias sem treinar, também avisamos.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Lembretes")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Salvar") {
                        config.salvar()
                        let ultima = repo.sessoes.map(\.data).max().flatMap { ListaTreinosView.formato.date(from: $0) }
                        Task {
                            await Lembretes.reagendar(config: config, nomeTreino: repo.treinos.first?.nome, ultimaSessao: ultima)
                        }
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Player

struct PlayerVideoView: View {
    let url: URL
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            VideoPlayer(player: AVPlayer(url: url)).ignoresSafeArea()
            Button { dismiss() } label: {
                Image(systemName: "xmark.circle.fill").font(.title).foregroundStyle(.white, .black.opacity(0.5))
            }
            .padding()
            .accessibilityLabel("Fechar")
        }
    }
}
