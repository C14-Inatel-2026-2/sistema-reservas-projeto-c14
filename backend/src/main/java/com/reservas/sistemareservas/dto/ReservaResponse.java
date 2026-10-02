package com.reservas.sistemareservas.dto;

import com.reservas.sistemareservas.entity.Reserva;
import com.reservas.sistemareservas.entity.enums.StatusReserva;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        Long recursoId,
        String recursoNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        StatusReserva status,
        LocalDateTime criadoEm,
        LocalDateTime canceladoEm,
        String motivoCancelamento) {

    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(),
                r.getUsuario().getId(), r.getUsuario().getNome(),
                r.getRecurso().getId(), r.getRecurso().getNome(),
                r.getDataHoraInicio(), r.getDataHoraFim(), r.getStatus(),
                r.getCriadoEm(), r.getCanceladoEm(), r.getMotivoCancelamento());
    }
}
