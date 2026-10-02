package com.reservas.sistemareservas.dto;

import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.entity.Reserva;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.StatusReserva;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaResponseTest {

    private final Usuario usuario = Usuario.builder().id(1L).nome("Ana").email("ana@teste.com")
            .senha("123456").tipo(TipoUsuario.ALUNO).build();
    private final Quadra quadra = Quadra.builder().id(10L).nome("Quadra 1").ativo(true)
            .modalidade("Futsal").coberta(false).build();

    @Test
    void de_copiaDadosDeUsuarioERecurso() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 5, 10, 0);
        Reserva reserva = Reserva.builder().id(7L).usuario(usuario).recurso(quadra)
                .dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(1))
                .status(StatusReserva.PENDENTE).criadoEm(inicio.minusDays(1)).build();

        ReservaResponse resposta = ReservaResponse.de(reserva);

        assertEquals(7L, resposta.id());
        assertEquals("Ana", resposta.usuarioNome());
        assertEquals("Quadra 1", resposta.recursoNome());
        assertEquals(StatusReserva.PENDENTE, resposta.status());
        assertNull(resposta.canceladoEm());
    }

    @Test
    void de_semUsuario_lancaNullPointerException() {
        Reserva reserva = Reserva.builder().id(8L).recurso(quadra).status(StatusReserva.PENDENTE).build();

        assertThrows(NullPointerException.class, () -> ReservaResponse.de(reserva));
    }
}
