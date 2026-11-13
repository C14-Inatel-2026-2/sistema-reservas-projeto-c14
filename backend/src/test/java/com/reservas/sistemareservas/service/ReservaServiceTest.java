package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.CancelamentoRequest;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private void dadoReservaExistente(Reserva reserva) {
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    private void dadoRegraDoRecurso(Optional<RegraCancelamento> regra) {
        when(regraRepository.findByRecursoId(quadra.getId())).thenReturn(regra);
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
        assertEquals(usuario.getId(), resposta.usuarioId());
        assertEquals(usuario.getNome(), resposta.usuarioNome());
        assertEquals(quadra.getId(), resposta.recursoId());
        assertEquals("Quadra 1", resposta.recursoNome());
        assertEquals(inicio, resposta.dataHoraInicio());
        assertEquals(inicio.plusHours(1), resposta.dataHoraFim());
        assertNotNull(resposta.criadoEm());
        assertNull(resposta.canceladoEm());
        assertNull(resposta.motivoCancelamento());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void criar_comInicioNoPassado_lancaRegraNegocioSemConsultarRepositorios() {
        LocalDateTime inicio = LocalDateTime.now().minusHours(2);
        ReservaRequest request = new ReservaRequest(1L, 10L, inicio, inicio.plusHours(1));

        assertThrows(RegraNegocioException.class, () -> reservaService.criar(request));

        verifyNoInteractions(usuarioRepository, recursoRepository, reservaRepository, regraRepository);
    }

    @Test
    void criar_comRecursoInativo_lancaRegraNegocioENaoSalva() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        ReservaRequest request = new ReservaRequest(1L, 10L, inicio, inicio.plusHours(1));
        quadra.setAtivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(quadra));

        assertThrows(RegraNegocioException.class, () -> reservaService.criar(request));

        verifyNoInteractions(reservaRepository, regraRepository);
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
        dadoReservaExistente(reserva);
        dadoRegraDoRecurso(Optional.of(regra(true, 24, 30.0)));

        CancelamentoResponse resposta = reservaService.cancelar(5L, null);

        assertEquals(0.0, resposta.percentualMultaAplicada());
        assertEquals(StatusReserva.CANCELADA, resposta.reserva().status());
        assertNotNull(reserva.getCanceladoEm());
    }

    @Test
    void cancelar_foraDaAntecedenciaMinima_aplicaMultaDaRegra() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusHours(2));
        dadoReservaExistente(reserva);
        dadoRegraDoRecurso(Optional.of(regra(true, 24, 30.0)));

        CancelamentoResponse resposta = reservaService.cancelar(5L, null);

        assertEquals(30.0, resposta.percentualMultaAplicada());
        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
    }

    @Test
    void cancelar_quandoRecursoNaoPermite_lancaRegraNegocioEMantemStatus() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusDays(3));
        dadoReservaExistente(reserva);
        dadoRegraDoRecurso(Optional.of(regra(false, 0, 0.0)));

        assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L, null));

        assertEquals(StatusReserva.CONFIRMADA, reserva.getStatus());
    }

    @Test
    void cancelar_semRegra_naoAplicaMultaERegistraMotivo() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusDays(3));
        dadoReservaExistente(reserva);
        dadoRegraDoRecurso(Optional.empty());

        CancelamentoResponse resposta = reservaService.cancelar(5L,
                new CancelamentoRequest("Mudança de planos"));

        assertEquals(0.0, resposta.percentualMultaAplicada());
        assertEquals(StatusReserva.CANCELADA, resposta.reserva().status());
        assertEquals("Mudança de planos", resposta.reserva().motivoCancelamento());
        assertNotNull(reserva.getCanceladoEm());
        assertEquals(reserva.getCanceladoEm(), resposta.reserva().canceladoEm());
    }

    @Test
    void cancelar_reservaJaCancelada_lancaRegraNegocioEPreservaDados() {
        Reserva reserva = reservaComecandoEm(LocalDateTime.now().plusDays(3));
        LocalDateTime canceladoEm = LocalDateTime.of(2026, 10, 1, 12, 0);
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladoEm(canceladoEm);
        reserva.setMotivoCancelamento("Motivo anterior");
        dadoReservaExistente(reserva);

        assertThrows(RegraNegocioException.class, () -> reservaService.cancelar(5L,
                new CancelamentoRequest("Novo motivo")));

        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
        assertEquals(canceladoEm, reserva.getCanceladoEm());
        assertEquals("Motivo anterior", reserva.getMotivoCancelamento());
        verifyNoInteractions(regraRepository);
    }
}
