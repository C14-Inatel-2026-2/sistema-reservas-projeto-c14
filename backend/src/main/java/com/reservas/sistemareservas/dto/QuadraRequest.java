package com.reservas.sistemareservas.dto;

import jakarta.validation.constraints.NotBlank;

public record QuadraRequest(
        @NotBlank String nome,
        String localizacao,
        @NotBlank String modalidade,
        boolean coberta) {
}
