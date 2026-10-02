package com.reservas.sistemareservas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RegraCancelamentoRequest(
        @NotNull @Min(0) Integer horasMinimasAntecedencia,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double percentualMulta,
        @NotNull Boolean permiteCancelamento,
        String observacoes) {
}
