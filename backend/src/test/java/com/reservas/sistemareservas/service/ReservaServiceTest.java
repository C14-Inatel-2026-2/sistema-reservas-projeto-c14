package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.CancelamentoResponse;
import com.reservas.sistemareservas.dto.ReservaRequest;
import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.entity.RegraCancelamento;
import com.reservas.sistemareservas.entity.Reserva;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.StatusReserva;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.RegraNegocioException;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.RegraCancelamentoRepository;
import com.reservas.sistemareservas.repository.ReservaRepository;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

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
                .senha("123456").tipo(TipoUsuario.ALUNO).build();
        quadra = Quadra.builder().id(10L).nome("Quadra 1").ativo(true)
                .modalidade("Futsal").coberta(true).build();
    }

    private Reserva reservaComecandoEm(LocalDateTime inicio) {
        return Reserva.builder().id(5L).usuario(usuario).recurso(quadra)
                .dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(1))
                .status(StatusReserva.CONFIRMADA).criadoEm(LocalDateTime.now()).build();
    }

    private RegraCancelamento regra(boolean permite, int horas, double multa) {
        return RegraCancelamento.builder().recurso(quadra).permiteCancelamento(permite)
                .horasMinimasAntecedencia(horas).percentualMulta(multa).build();
    }

    @Test
    void criar_comHorarioLivre_salvaReservaPendente() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        ReservaRequest request = new ReservaRequest(1L, 10L, inicio, inicio.plusHours(1));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(quadra));
        when(reservaRepository.existeConflito(anyLong(), any(), any())).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = reservaService.criar(request);

        assertEquals(StatusReserva.PENDENTE, resposta.status());
        assertEquals("Quadra 1", resposta.recursoNome());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void criar_comHorarioOcupado_lancaConflitoENaoSalva() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        ReservaRequest request = new ReservaRequest(1L, 10L, inicio, inicio.plusHours(1));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(quadra));
        when(reservaRepository.existeConflito(anyLong(), any(), any())).thenReturn(true);

        assertThrows(ConflitoException.class, () -> reservaService.criar(request));

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void cancelar_comAntecedenciaSuficiente_naoAplicaMulta() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusDays(3));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));
        when(regraRepository.findByRecursoId(10L)).thenReturn(Optional.of(regra(true, 24, 30.0)));

        CancelamentoResponse resposta = reservaService.cancelar(5L, null);

        assertEquals(0.0, resposta.percentualMultaAplicada());
        assertEquals(StatusReserva.CANCELADA, resposta.reserva().status());
        assertNotNull(reserva.getCanceladoEm());
    }

    @Test
    void cancelar_foraDaAntecedenciaMinima_aplicaMultaDaRegra() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusHours(2));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));
        when(regraRepository.findByRecursoId(10L)).thenReturn(Optional.of(regra(true, 24, 30.0)));

        CancelamentoResponse resposta = reservaService.cancelar(5L, null);

        assertEquals(30.0, resposta.percentualMultaAplicada());
        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
    }

    @Test
    void cancelar_quandoRecursoNaoPermite_lancaRegraNegocioEMantemStatus() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusDays(3));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));
        when(regraRepository.findByRecursoId(10L)).thenReturn(Optional.of(regra(false, 0, 0.0)));

        assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L, null));

        assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
    }
}
