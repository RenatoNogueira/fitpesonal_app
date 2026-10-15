package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento

object AgendaRules {
    /**
     * Conflito = mesmo dia e horário, outro registro, e nenhum dos dois cancelado.
     * No desktop apenas avisa; a UI deve pedir confirmação e permitir salvar.
     */
    fun conflito(novo: Agendamento, existentes: List<Agendamento>): Agendamento? {
        if (novo.status == StatusAgendamento.CANCELADO) return null
        return existentes.firstOrNull {
            it.id != novo.id &&
                it.data == novo.data &&
                it.horario == novo.horario &&
                it.status != StatusAgendamento.CANCELADO
        }
    }
}
