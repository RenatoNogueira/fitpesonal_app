import SwiftUI

struct ListaTreinosView: View {
    @EnvironmentObject private var repo: Repositorio
    @State private var colando = false
    @State private var lembrando = false
    @State private var mensagem: String?

    var body: some View {
        NavigationStack {
            Group {
                if repo.treinos.isEmpty {
                    estadoVazio
                } else {
                    List {
                        ForEach(repo.treinos, id: \.treinoId) { t in
                            NavigationLink(value: t.treinoId) { Cartao(pacote: t, sessoes: repo.sessoes(de: t.treinoId).count) }
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                        }
                        .onDelete { idx in idx.map { repo.treinos[$0].treinoId }.forEach(repo.excluir) }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Meus treinos")
            .navigationDestination(for: String.self) { id in
                if let t = repo.treinos.first(where: { $0.treinoId == id }) { ExecutarTreinoView(pacote: t) }
            }
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { lembrando = true } label: { Image(systemName: "bell") }
                        .accessibilityLabel("Lembretes de treino")
                    Button { colando = true } label: { Image(systemName: "doc.on.clipboard") }
                        .accessibilityLabel("Colar código do treino")
                }
            }
            .sheet(isPresented: $colando) {
                ColarCodigoView { texto in
                    mensagem = repo.receber(texto: texto).mensagem
                }
            }
            .sheet(isPresented: $lembrando) { LembretesView() }
            .alert("Treino", isPresented: Binding(get: { mensagem != nil }, set: { if !$0 { mensagem = nil } })) {
                Button("OK", role: .cancel) { mensagem = nil }
            } message: { Text(mensagem ?? "") }
            .task { await reagendarLembretes() }
        }
    }

    private var estadoVazio: some View {
        VStack(spacing: 16) {
            Image(systemName: "figure.strengthtraining.traditional")
                .font(.system(size: 56)).foregroundStyle(Tema.verde)
            Text("Nenhum treino ainda").font(.title2.bold())
            Text("Abra o arquivo .rjetreino que seu professor enviou, ou toque em “Colar código” e cole a mensagem.")
                .font(.callout).multilineTextAlignment(.center).foregroundStyle(.secondary)
            Button { colando = true } label: { Label("Colar código", systemImage: "doc.on.clipboard").padding(.horizontal, 8) }
                .buttonStyle(.borderedProminent)
        }
        .padding(32)
    }

    private func reagendarLembretes() async {
        let ultima = repo.sessoes.map(\.data).max().flatMap { Self.formato.date(from: $0) }
        await Lembretes.reagendar(
            config: LembreteConfig.carregar(),
            nomeTreino: repo.treinos.first?.nome,
            ultimaSessao: ultima
        )
    }

    static let formato: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()
}

private struct Cartao: View {
    let pacote: PacoteTreino
    let sessoes: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(pacote.nome).font(.headline)
            if let o = pacote.objetivo { Text(o).font(.subheadline).foregroundStyle(.secondary) }
            HStack {
                Etiqueta(texto: pacote.profissional.nome)
                Etiqueta(texto: "revisão \(pacote.revisao)")
                Etiqueta(texto: sessoes == 1 ? "1 sessão" : "\(sessoes) sessões")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .cartao()
    }
}
