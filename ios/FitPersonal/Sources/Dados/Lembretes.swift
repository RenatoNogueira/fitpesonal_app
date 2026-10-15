// Lembretes.swift — avisos locais do aluno: "hoje é dia de treinar" (dias e horário escolhidos) e
// retenção ("sentimos sua falta" 3 dias depois da última sessão).
//
// ATENÇÃO: não compilado/executado (desenvolvido sem Xcode). Mesma regra do Android (LembreteAlunoRules).

import Foundation
import UserNotifications

public struct LembreteConfig: Codable, Equatable {
    public var ativo = false
    /// Dias da semana no padrão do Calendar: 1 = domingo … 7 = sábado. Padrão: segunda, quarta, sexta.
    public var dias: Set<Int> = [2, 4, 6]
    /// Minutos desde 00:00.
    public var minutos = 18 * 60

    public init() {}

    static let chave = "lembrete_aluno"

    public static func carregar() -> LembreteConfig {
        guard let d = UserDefaults.standard.data(forKey: chave),
              let c = try? JSONDecoder().decode(LembreteConfig.self, from: d) else { return LembreteConfig() }
        return c
    }

    public func salvar() {
        if let d = try? JSONEncoder().encode(self) { UserDefaults.standard.set(d, forKey: Self.chave) }
    }
}

public enum Lembretes {
    private static let prefixo = "fitpersonal.lembrete."
    static let diasParaRetencao = 3

    /// Reagenda tudo. Chame ao abrir o app, ao salvar a configuração e depois de cada sessão.
    public static func reagendar(config: LembreteConfig, nomeTreino: String?, ultimaSessao: Date?) async {
        let centro = UNUserNotificationCenter.current()
        let pendentes = await centro.pendingNotificationRequests()
        centro.removePendingNotificationRequests(
            withIdentifiers: pendentes.map(\.identifier).filter { $0.hasPrefix(prefixo) }
        )
        guard config.ativo else { return }

        let liberado = (try? await centro.requestAuthorization(options: [.alert, .sound])) ?? false
        guard liberado else { return }

        let hora = config.minutos / 60
        let minuto = config.minutos % 60

        for dia in config.dias {
            let conteudo = UNMutableNotificationContent()
            conteudo.title = "Hoje é dia de treinar 💪"
            conteudo.body = nomeTreino.map { "\($0). Bora?" } ?? "Abra o app e registre sua sessão."
            conteudo.sound = .default
            var comp = DateComponents()
            comp.weekday = dia
            comp.hour = hora
            comp.minute = minuto
            let gatilho = UNCalendarNotificationTrigger(dateMatching: comp, repeats: true)
            try? await centro.add(UNNotificationRequest(identifier: prefixo + "dia\(dia)", content: conteudo, trigger: gatilho))
        }

        // Retenção: um único aviso, 3 dias depois da última sessão, no mesmo horário.
        if let ultima = ultimaSessao,
           let alvo = Calendar.current.date(byAdding: .day, value: diasParaRetencao, to: ultima),
           var comp = Optional(Calendar.current.dateComponents([.year, .month, .day], from: alvo)) {
            comp.hour = hora
            comp.minute = minuto
            if let quando = Calendar.current.date(from: comp), quando > Date() {
                let conteudo = UNMutableNotificationContent()
                conteudo.title = "Sentimos sua falta"
                conteudo.body = "Faz \(diasParaRetencao) dias que você não treina. Que tal retomar hoje?"
                conteudo.sound = .default
                let gatilho = UNCalendarNotificationTrigger(dateMatching: comp, repeats: false)
                try? await centro.add(UNNotificationRequest(identifier: prefixo + "retencao", content: conteudo, trigger: gatilho))
            }
        }
    }
}
