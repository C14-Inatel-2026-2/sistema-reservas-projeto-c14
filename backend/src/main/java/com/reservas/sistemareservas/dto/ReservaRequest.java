package com.reservas.sistemareservas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservaRequest(
        @NotNull Long usuarioId,
        @NotNull Long recursoId,
        @NotNull LocalDateTime dataHoraInicio,
        @NotNull LocalDateTime dataHoraFim) {
}
