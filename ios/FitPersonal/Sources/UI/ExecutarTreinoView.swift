import SwiftUI
import AVKit

struct ExecutarTreinoView: View {
    @EnvironmentObject private var repo: Repositorio
    let pacote: PacoteTreino

    @State private var divisao: String = ""
    @State private var feitos: Set<String> = []
    @State private var cargas: [String: String] = [:]
    @State private var esforco: Double = 0
    @State private var descansoRestante = 0
    @State private var descansoTotal = 0
    @State private var finalizando = false
    @State private var assistindo: URL?
    @Environment(\.dismiss) private var dismiss

    private let relogio = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private var divisoes: [String] {
        ["A", "B", "C", "D"].filter { !(pacote.divisoes[$0] ?? []).isEmpty }
    }

    private var exercicios: [ExercicioRef] { pacote.divisoes[divisao] ?? [] }

    private func chave(_ i: Int) -> String { "\(divisao)\(i)" }

    private func cargaInicial(_ e: ExercicioRef) -> String {
        guard let r = e.carga?.range(of: "\\d+([.,]\\d+)?", options: .regularExpression) else { return "" }
        return String(e.carga![r])
    }

    var body: some View {
        VStack(spacing: 0) {
            if divisoes.count > 1 {
                Picker("Divisão", selection: $divisao) {
                    ForEach(divisoes, id: \.self) { Text("Treino \($0)").tag($0) }
                }
                .pickerStyle(.segmented)
                .padding()
            }

            ProgressView(value: Double(feitosNaDivisao), total: Double(max(exercicios.count, 1)))
                .tint(Tema.cor(divisao: divisao))
                .padding(.horizontal)

            List {
                ForEach(Array(exercicios.enumerated()), id: \.offset) { i, e in
                    linha(i, e)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }
            .listStyle(.plain)

            rodape
        }
        .navigationTitle(pacote.nome)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            NavigationLink { HistoricoView(pacote: pacote) } label: { Image(systemName: "clock.arrow.circlepath") }
                .accessibilityLabel("Histórico")
        }
        .onAppear { if divisao.isEmpty { divisao = divisoes.first ?? "A" } }
        .onReceive(relogio) { _ in if descansoRestante > 0 { descansoRestante -= 1 } }
        .sheet(item: Binding(get: { assistindo.map(ArquivoVideo.init) }, set: { assistindo = $0?.url })) { v in
            PlayerVideoView(url: v.url)
        }
        .confirmationDialog("Finalizar sessão?", isPresented: $finalizando, titleVisibility: .visible) {
            Button("Finalizar (\(feitosNaDivisao) de \(exercicios.count) feitos)") { registrar() }
            Button("Continuar", role: .cancel) {}
        }
    }

    private var feitosNaDivisao: Int {
        exercicios.indices.filter { feitos.contains(chave($0)) }.count
    }

    @ViewBuilder
    private func linha(_ i: Int, _ e: ExercicioRef) -> some View {
        let k = chave(i)
        let feito = feitos.contains(k)
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("\(i + 1). \(e.nome)").font(.headline)
                    HStack {
                        if let s = e.series { Etiqueta(texto: "\(s) séries") }
                        if let r = e.repeticoes { Etiqueta(texto: "\(r) reps") }
                        if let d = e.descanso { Etiqueta(texto: "descanso \(d)") }
                    }
                    if let o = e.obs { Text(o).font(.footnote).foregroundStyle(.secondary) }
                }
                Spacer()
                Button {
                    if feito { feitos.remove(k) } else { feitos.insert(k) }
                } label: {
                    Image(systemName: feito ? "checkmark.circle.fill" : "circle")
                        .font(.title).foregroundStyle(feito ? Tema.verde : .secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(feito ? "Feito" : "Marcar como feito")
            }

            HStack {
                TextField("Carga (kg)", text: Binding(
                    get: { cargas[k] ?? cargaInicial(e) },
                    set: { cargas[k] = String($0.filter { "0123456789,.".contains($0) }.prefix(6)) }
                ))
                .keyboardType(.decimalPad)
                .textFieldStyle(.roundedBorder)
                if let seg = DescansoRules.segundos(e.descanso) {
                    Button {
                        descansoTotal = seg
                        descansoRestante = seg
                    } label: { Label("\(seg) s", systemImage: "timer") }
                        .buttonStyle(.bordered)
                }
            }

            if let local = repo.videoLocal(treinoId: pacote.treinoId, exercicio: e.nome) {
                Button { assistindo = local } label: {
                    Label("Assistir vídeo do exercício", systemImage: "play.rectangle.fill")
                }.buttonStyle(.borderedProminent)
            } else if let link = e.video, let url = URL(string: link) {
                Link(destination: url) { Label("Ver vídeo do exercício", systemImage: "play.rectangle") }
                    .buttonStyle(.bordered)
            }
        }
        .cartao()
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(feito ? Tema.verde : .clear, lineWidth: 2)
        )
    }

    private var rodape: some View {
        VStack(spacing: 10) {
            if descansoRestante > 0 {
                HStack {
                    Image(systemName: "timer")
                    Text("Descanso \(descansoRestante / 60):\(String(format: "%02d", descansoRestante % 60))")
                        .font(.headline)
                    Spacer()
                    Button("Pular") { descansoRestante = 0 }
                }
                ProgressView(value: Double(descansoRestante), total: Double(max(descansoTotal, 1)))
            }
            HStack {
                Text(esforco == 0 ? "Esforço da sessão (opcional)" : "Esforço: \(Int(esforco))/10")
                    .font(.footnote)
                Slider(value: $esforco, in: 0...10, step: 1)
            }
            Button { finalizando = true } label: {
                Text("Finalizar sessão \(divisao)").frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .vidro(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .padding(.horizontal)
        .padding(.bottom, 8)
    }

    private func registrar() {
        let itens = exercicios.enumerated().map { i, e -> ItemSessao in
            let texto = (cargas[chave(i)] ?? cargaInicial(e)).replacingOccurrences(of: ",", with: ".")
            let kg = Double(texto).flatMap { $0 > 0 && $0 <= 2000 ? $0 : nil }
            return ItemSessao(exercicio: e.nome, cargaKg: kg, reps: nil, feito: feitos.contains(chave(i)))
        }
        let hoje = ListaTreinosView.formato.string(from: Date())
        repo.registrar(SessaoRef(
            execucaoId: UUID().uuidString.lowercased(), data: hoje, treinoId: pacote.treinoId,
            divisao: divisao, esforco: esforco >= 1 ? Int(esforco) : nil, itens: itens
        ))
        exercicios.indices.forEach { feitos.remove(chave($0)) }
        esforco = 0
        Task {
            await Lembretes.reagendar(
                config: LembreteConfig.carregar(), nomeTreino: pacote.nome, ultimaSessao: Date()
            )
        }
        dismiss()
    }
}

private struct ArquivoVideo: Identifiable {
    let url: URL
    var id: String { url.path }
}

/// Segundos de descanso a partir do texto do professor ("60", "60s", "1:30", "2 min"); nil se não houver.
enum DescansoRules {
    static func segundos(_ texto: String?) -> Int? {
        guard let t = texto?.lowercased().trimmingCharacters(in: .whitespaces), !t.isEmpty else { return nil }
        if let r = t.range(of: "(\\d+):(\\d{1,2})", options: .regularExpression) {
            let partes = t[r].split(separator: ":").compactMap { Int($0) }
            if partes.count == 2 { return min(max(partes[0] * 60 + partes[1], 10), 600) }
        }
        let numeros = t.split(whereSeparator: { !$0.isNumber }).compactMap { Int($0) }
        guard let n = numeros.first else { return nil }
        let bruto: Int
        if t.contains("min") { bruto = n * 60 + (numeros.count > 1 ? numeros[1] : 0) }
        else if t.contains("s") { bruto = n }
        else if (1...5).contains(n) { bruto = n * 60 }
        else { bruto = n }
        return min(max(bruto, 10), 600)
    }
}
