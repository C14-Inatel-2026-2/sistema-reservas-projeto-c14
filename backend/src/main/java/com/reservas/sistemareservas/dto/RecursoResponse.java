package com.reservas.sistemareservas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.reservas.sistemareservas.entity.EquipamentoLaboratorio;
import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.entity.Recurso;
import com.reservas.sistemareservas.entity.SalaEstudo;
import org.hibernate.Hibernate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecursoResponse(
        Long id,
        String tipo,
        String nome,
        String localizacao,
        boolean ativo,
        Integer capacidadePessoas,
        Boolean possuiProjetor,
        String modalidade,
        Boolean coberta,
        String laboratorio,
        String numeroPatrimonio) {

    public static RecursoResponse de(Recurso recurso) {
        Object r = Hibernate.unproxy(recurso);
        if (r instanceof SalaEstudo s) {
            return new RecursoResponse(s.getId(), "SALA_ESTUDO", s.getNome(), s.getLocalizacao(), s.isAtivo(),
                    s.getCapacidadePessoas(), s.isPossuiProjetor(), null, null, null, null);
        }
        if (r instanceof Quadra q) {
            return new RecursoResponse(q.getId(), "QUADRA", q.getNome(), q.getLocalizacao(), q.isAtivo(),
                    null, null, q.getModalidade(), q.isCoberta(), null, null);
        }
        if (r instanceof EquipamentoLaboratorio e) {
            return new RecursoResponse(e.getId(), "EQUIPAMENTO_LABORATORIO", e.getNome(), e.getLocalizacao(),
                    e.isAtivo(), null, null, null, null, e.getLaboratorio(), e.getNumeroPatrimonio());
        }
        Recurso base = (Recurso) r;
        return new RecursoResponse(base.getId(), null, base.getNome(), base.getLocalizacao(), base.isAtivo(),
                null, null, null, null, null, null);
    }
}
