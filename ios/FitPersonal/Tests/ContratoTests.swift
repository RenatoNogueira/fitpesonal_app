import XCTest
@testable import FitPersonal

/// Valida o contrato contra os exemplos "dourados" gerados pelo app Android (docs/contrato/exemplos).
final class ContratoTests: XCTestCase {

    private func recurso(_ nome: String) throws -> String {
        let bundle = Bundle(for: Self.self)
        // O diretório "exemplos" é copiado como pasta para dentro do bundle de testes.
        let url = bundle.url(forResource: nome, withExtension: nil, subdirectory: "exemplos")
            ?? bundle.url(forResource: nome, withExtension: nil)
        return try String(contentsOf: XCTUnwrap(url), encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func testLeEReserializaOTreino() throws {
        let json = try recurso("treino.rjetreino")
        let pacote = try ContratoLeitor.lerPacote(json)   // valida a integridade
        XCTAssertEqual(ContratoJson.serializar(pacote), json, "re-serialização precisa ser idêntica byte a byte")
    }

    func testCodigoDeTextoDecodificaParaOMesmoJson() throws {
        let codigo = try recurso("treino.codigo.txt")
        let json = try recurso("treino.rjetreino")
        XCTAssertEqual(try CodigoTexto.decodificar(codigo), json)
    }

    func testIdaEVoltaDoCodigo() throws {
        let json = try recurso("treino.rjetreino")
        let codigo = try CodigoTexto.codificar(json: json)
        XCTAssertTrue(codigo.hasPrefix("RJE1:"))
        XCTAssertEqual(try CodigoTexto.decodificar("mensagem " + codigo + " fim"), json)
    }

    func testAdulteracaoQuebraAIntegridade() throws {
        let json = try recurso("treino.rjetreino")
        guard let nome = try ContratoLeitor.lerPacote(json).divisoes.values.first?.first?.nome else {
            return XCTFail("exemplo sem exercícios")
        }
        let adulterado = json.replacingOccurrences(of: nome, with: nome + "X")
        XCTAssertThrowsError(try ContratoLeitor.lerPacote(adulterado))
    }

    func testDescanso() {
        XCTAssertEqual(DescansoRules.segundos("60"), 60)
        XCTAssertEqual(DescansoRules.segundos("1:30"), 90)
        XCTAssertEqual(DescansoRules.segundos("2 min"), 120)
        XCTAssertEqual(DescansoRules.segundos("45s"), 45)
        XCTAssertNil(DescansoRules.segundos(nil))
    }
}
