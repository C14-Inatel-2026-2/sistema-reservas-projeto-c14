package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.entity.Reserva;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.StatusReserva;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;
import com.reservas.sistemareservas.exception.RegraNegocioException;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.RegraCancelamentoRepository;
import com.reservas.sistemareservas.repository.ReservaRepository;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceCancelamentoIniciadaTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RecursoRepository recursoRepository;
    @Mock
    private RegraCancelamentoRepository regraRepository;

    private ReservaService reservaService;
    private Usuario usuario;
    private Quadra quadra;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(reservaRepository, usuarioRepository, recursoRepository,
                regraRepository, new ValidadorHorarioReserva());
        usuario = Usuario.builder().id(1L).nome("Ana").email("ana@teste.com")
                .senha("hash").tipo(TipoUsuario.ALUNO).build();
        quadra = Quadra.builder().id(10L).nome("Quadra 1").ativo(true)
                .modalidade("Futsal").coberta(true).build();
    }

    private Reserva dadoReservaComecandoEm(LocalDateTime inicio, StatusReserva status) {
        Reserva reserva = Reserva.builder().id(5L).usuario(usuario).recurso(quadra)
                .dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(2))
                .status(status).criadoEm(LocalDateTime.now().minusDays(1)).build();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));
        return reserva;
    }

    @Test
    void cancelar_reservaJaIniciada_lancaRegraNegocioEMantemDados() {
        Reserva reserva = dadoReservaComecandoEm(LocalDateTime.now().minusMinutes(30), StatusReserva.CONFIRMADA);

        assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L, null));

        assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
        assertNull(reserva.getCanceladoEm());
        verifyNoInteractions(regraRepository);
    }

    @Test
    void cancelar_reservaPendenteJaIniciada_tambemEhRecusada() {
        Reserva reserva = dadoReservaComecandoEm(LocalDateTime.now().minusHours(1), StatusReserva.PENDENTE);

        assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L, null));

        assertEquals(StatusReserva.PENDENTE, reserva.getStatus());
    }

    @Test
    void cancelar_reservaEncerrada_lancaRegraNegocioEPreservaDados() {
        Reserva reserva = dadoReservaComecandoEm(LocalDateTime.now().minusHours(3), StatusReserva.CONFIRMADA);

        assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L, null));

        assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
        assertNull(reserva.getCanceladoEm());
        assertNull(reserva.getMotivoCancelamento());
        verifyNoInteractions(regraRepository);
    }

    @Test
    void cancelar_noInstanteExatoDoInicio_lancaRegraNegocioEPreservaDados() {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 14, 12, 0);

        try (MockedStatic<LocalDateTime> horario = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            horario.when(LocalDateTime::now).thenReturn(agora);
            Reserva reserva = dadoReservaComecandoEm(agora, StatusReserva.CONFIRMADA);

            assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L, null));

            assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
            assertNull(reserva.getCanceladoEm());
            assertNull(reserva.getMotivoCancelamento());
            verifyNoInteractions(regraRepository);
        }
    }

    @Test
    void cancelar_reservaQueAindaNaoComecou_continuaPermitida() {
        Reserva reserva = dadoReservaComecandoEm(LocalDateTime.now().plusMinutes(10), StatusReserva.CONFIRMADA);
        when(regraRepository.findByRecursoId(10L)).thenReturn(Optional.empty());

        reservaService.cancelar(5L, null);

        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
    }
}
