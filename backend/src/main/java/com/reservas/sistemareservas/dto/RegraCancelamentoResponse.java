package com.reservas.sistemareservas.dto;

import com.reservas.sistemareservas.entity.RegraCancelamento;

public record RegraCancelamentoResponse(
        Long id,
        Long recursoId,
        Integer horasMinimasAntecedencia,
        Double percentualMulta,
        boolean permiteCancelamento,
        String observacoes) {

    public static RegraCancelamentoResponse de(RegraCancelamento r) {
        return new RegraCancelamentoResponse(r.getId(), r.getRecurso().getId(), r.getHorasMinimasAntecedencia(),
                r.getPercentualMulta(), r.isPermiteCancelamento(), r.getObservacoes());
    }
}
