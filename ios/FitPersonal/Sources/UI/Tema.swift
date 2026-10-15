// Tema.swift — identidade FitPersonal (verde-limão sobre preto) e efeito "Liquid Glass" (iOS 26+),
// com fallback para materiais comuns nas versões anteriores.

import SwiftUI

enum Tema {
    static let verde = Color(red: 0x3A / 255, green: 0x82 / 255, blue: 0x08 / 255)
    static let verdeClaro = Color(red: 0xA3 / 255, green: 0xF5 / 255, blue: 0x3A / 255)
    static let verdeEscuro = Color(red: 0x16 / 255, green: 0x30 / 255, blue: 0x0A / 255)

    static let gradiente = LinearGradient(
        colors: [Color(red: 0x3A / 255, green: 0x7D / 255, blue: 0x0A / 255), verdeEscuro],
        startPoint: .topLeading, endPoint: .bottomTrailing
    )

    /// Cores das divisões A, B, C, D.
    static func cor(divisao: String) -> Color {
        switch divisao.uppercased() {
        case "B": return Color(red: 0x0E / 255, green: 0x74 / 255, blue: 0x90 / 255)
        case "C": return Color(red: 0xC2 / 255, green: 0x41 / 255, blue: 0x0C / 255)
        case "D": return Color(red: 0x6D / 255, green: 0x28 / 255, blue: 0xD9 / 255)
        default: return verde
        }
    }
}

extension View {
    /// Vidro líquido no iOS 26 (SDK com Xcode 26); material translúcido nas demais versões.
    @ViewBuilder
    func vidro<S: Shape>(_ forma: S) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26, *) {
            self.glassEffect(.regular, in: forma)
        } else {
            self.background(.ultraThinMaterial, in: forma)
        }
        #else
        self.background(.ultraThinMaterial, in: forma)
        #endif
    }

    func vidro() -> some View { vidro(Capsule()) }

    /// Cartão padrão das listas.
    func cartao() -> some View {
        self.padding(16)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

struct Etiqueta: View {
    let texto: String
    var body: some View {
        Text(texto)
            .font(.caption.weight(.medium))
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(Color(.tertiarySystemFill), in: Capsule())
    }
}
