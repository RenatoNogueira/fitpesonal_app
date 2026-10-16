import SwiftUI

@main
struct FitPersonalApp: App {
    @StateObject private var repo = Repositorio()
    @State private var aviso: String?

    var body: some Scene {
        WindowGroup {
            ListaTreinosView()
                .environmentObject(repo)
                .tint(Tema.verde)
                // Arquivo .rjetreino / .rjetreinopack aberto pelo WhatsApp, e-mail ou Arquivos
                .onOpenURL { url in
                    aviso = repo.receber(url: url).mensagem
                }
                .alert("Treino", isPresented: Binding(get: { aviso != nil }, set: { if !$0 { aviso = nil } })) {
                    Button("OK", role: .cancel) { aviso = nil }
                } message: {
                    Text(aviso ?? "")
                }
        }
    }
}
