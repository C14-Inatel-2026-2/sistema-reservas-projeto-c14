package com.reservas.sistemareservas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalaEstudoRequest(
        @NotBlank String nome,
        String localizacao,
        @NotNull @Min(1) Integer capacidadePessoas,
        boolean possuiProjetor) {
}
