package com.reservas.sistemareservas.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipamentoLaboratorioRequest(
        @NotBlank String nome,
        String localizacao,
        @NotBlank String laboratorio,
        @NotBlank String numeroPatrimonio) {
}
