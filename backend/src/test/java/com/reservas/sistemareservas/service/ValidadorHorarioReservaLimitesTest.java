package com.reservas.sistemareservas.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorHorarioReservaLimitesTest {

    private final ValidadorHorarioReserva validador = new ValidadorHorarioReserva();

    @Test
    void mesmoHorarioEmDiasDiferentesNaoGeraSobreposicao() {
        LocalDateTime primeiroInicio = LocalDateTime.of(2026, 10, 2, 10, 0);
        LocalDateTime segundoInicio = primeiroInicio.plusDays(1);

        assertFalse(validador.haSobreposicao(
                primeiroInicio, primeiroInicio.plusHours(1),
                segundoInicio, segundoInicio.plusHours(1)));
    }

    @Test
    void umMinutoDeIntersecaoNaViradaDoDiaGeraSobreposicao() {
        LocalDateTime primeiroInicio = LocalDateTime.of(2026, 10, 2, 23, 45);
        LocalDateTime primeiroFim = primeiroInicio.plusMinutes(30);
        LocalDateTime segundoInicio = primeiroFim.minusMinutes(1);

        assertTrue(validador.haSobreposicao(
                primeiroInicio, primeiroFim,
                segundoInicio, segundoInicio.plusMinutes(30)));
    }
}
