package br.com.fitpersonal.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2: dobras cutâneas numéricas (Pollock 3/7 dobras) na avaliação física. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE avaliacoes_fisicas ADD COLUMN protocoloDobras TEXT")
        listOf(
            "dobraPeitoral", "dobraAxilarMedia", "dobraTriceps", "dobraSubescapular",
            "dobraAbdominal", "dobraSuprailiaca", "dobraCoxa",
        ).forEach { db.execSQL("ALTER TABLE avaliacoes_fisicas ADD COLUMN $it REAL") }
    }
}

/** v2 → v3: registro do consentimento LGPD do aluno. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alunos ADD COLUMN consentimentoLgpdEm TEXT")
    }
}

/** v9 → v10: identificadores do contrato com o app do aluno e importação de resumos. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alunos ADD COLUMN refContrato TEXT")
        db.execSQL("ALTER TABLE treinos ADD COLUMN contratoId TEXT")
        db.execSQL("ALTER TABLE treinos ADD COLUMN contratoRevisao INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS execucoes_importadas (" +
                "execucaoId TEXT NOT NULL, idAluno INTEGER NOT NULL, importadoEm TEXT NOT NULL, " +
                "PRIMARY KEY(execucaoId))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS retornos_aluno (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idAluno INTEGER NOT NULL, " +
                "resumoId TEXT NOT NULL, importadoEm TEXT NOT NULL, de TEXT NOT NULL, ate TEXT NOT NULL, " +
                "sessoes INTEGER NOT NULL, esforcoMedio REAL, mensagem TEXT, " +
                "FOREIGN KEY(idAluno) REFERENCES alunos(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_retornos_aluno_idAluno ON retornos_aluno (idAluno)")
    }
}

/** v8 → v9: lista de espera e testes de aptidão. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS lista_espera (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idAluno INTEGER NOT NULL, " +
                "diaSemana INTEGER NOT NULL, horario TEXT, observacao TEXT, criadoEm TEXT NOT NULL, " +
                "FOREIGN KEY(idAluno) REFERENCES alunos(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lista_espera_idAluno ON lista_espera (idAluno)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS testes_aptidao (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idAluno INTEGER NOT NULL, " +
                "data TEXT NOT NULL, tipo TEXT NOT NULL, valor REAL NOT NULL, observacao TEXT, " +
                "FOREIGN KEY(idAluno) REFERENCES alunos(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_testes_aptidao_idAluno ON testes_aptidao (idAluno)")
    }
}

/** v7 → v8: anotação por sessão e pacote de sessões do aluno. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE agendamentos ADD COLUMN anotacao TEXT")
        db.execSQL("ALTER TABLE alunos ADD COLUMN pacoteSessoes INTEGER")
        db.execSQL("ALTER TABLE alunos ADD COLUMN pacoteInicio TEXT")
    }
}

/** v6 → v7: mensalidade/pagamentos e histórico de carga por exercício. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alunos ADD COLUMN mensalidadeCentavos INTEGER")
        db.execSQL("ALTER TABLE alunos ADD COLUMN diaVencimento INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS pagamentos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idAluno INTEGER NOT NULL, " +
                "competencia TEXT NOT NULL, valorCentavos INTEGER NOT NULL, pagoEm TEXT NOT NULL, " +
                "FOREIGN KEY(idAluno) REFERENCES alunos(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pagamentos_idAluno_competencia ON pagamentos (idAluno, competencia)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS historico_carga (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idAluno INTEGER NOT NULL, " +
                "exercicio TEXT NOT NULL, chave TEXT NOT NULL, data TEXT NOT NULL, cargaKg REAL NOT NULL, " +
                "FOREIGN KEY(idAluno) REFERENCES alunos(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_historico_carga_idAluno_exercicio ON historico_carga (idAluno, exercicio)")
    }
}

/** v5 → v6: PAR-Q e assinatura na avaliação; biblioteca de treinos modelo. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE avaliacoes_fisicas ADD COLUMN parq TEXT")
        db.execSQL("ALTER TABLE avaliacoes_fisicas ADD COLUMN assinatura TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS modelos_treino (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "nome TEXT NOT NULL, objetivo TEXT, dataCriacao TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS exercicios_modelo (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "idModelo INTEGER NOT NULL, nome TEXT NOT NULL, divisao TEXT NOT NULL, " +
                "series TEXT, repeticoes TEXT, carga TEXT, descanso TEXT, observacoes TEXT, " +
                "FOREIGN KEY(idModelo) REFERENCES modelos_treino(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercicios_modelo_idModelo ON exercicios_modelo (idModelo)")
    }
}

/** v4 → v5: meta do aluno e intervalo de reavaliação. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alunos ADD COLUMN metaMetrica TEXT")
        db.execSQL("ALTER TABLE alunos ADD COLUMN metaValor REAL")
        db.execSQL("ALTER TABLE alunos ADD COLUMN reavaliacaoDias INTEGER")
    }
}

/** v3 → v4: circunferências numéricas (cintura e quadril) para o cálculo do RCQ. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE avaliacoes_fisicas ADD COLUMN cinturaCm REAL")
        db.execSQL("ALTER TABLE avaliacoes_fisicas ADD COLUMN quadrilCm REAL")
    }
}

/** v10 → v11: link de vídeo nos exercícios (treino e modelo) e biblioteca de vídeos próprios. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercicios_treino ADD COLUMN video TEXT")
        db.execSQL("ALTER TABLE exercicios_modelo ADD COLUMN video TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS videos_exercicio (" +
                "chave TEXT NOT NULL, nome TEXT NOT NULL, arquivo TEXT NOT NULL, " +
                "duracaoMs INTEGER NOT NULL, tamanhoBytes INTEGER NOT NULL, criadoEm TEXT NOT NULL, " +
                "PRIMARY KEY(chave))"
        )
    }
}

/** v11 → v12: metas de composição corporal por aluno. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS metas_composicao (" +
                "idAluno INTEGER NOT NULL, metrica TEXT NOT NULL, alvo REAL NOT NULL, " +
                "PRIMARY KEY(idAluno, metrica))"
        )
    }
}
