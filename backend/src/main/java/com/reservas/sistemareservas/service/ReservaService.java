package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.CancelamentoRequest;
import com.reservas.sistemareservas.dto.CancelamentoResponse;
import com.reservas.sistemareservas.dto.ReservaRequest;
import com.reservas.sistemareservas.dto.ReservaResponse;
import com.reservas.sistemareservas.entity.Recurso;
import com.reservas.sistemareservas.entity.RegraCancelamento;
import com.reservas.sistemareservas.entity.Reserva;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.StatusReserva;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.exception.RegraNegocioException;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.RegraCancelamentoRepository;
import com.reservas.sistemareservas.repository.ReservaRepository;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecursoRepository recursoRepository;
    private final RegraCancelamentoRepository regraRepository;
    private final ValidadorHorarioReserva validadorHorario;

    @Transactional
    public ReservaResponse criar(ReservaRequest request) {
        validadorHorario.validarPeriodo(request.dataHoraInicio(), request.dataHoraFim());
        if (!request.dataHoraInicio().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A reserva deve começar em um horário futuro.");
        }

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", request.usuarioId()));
        Recurso recurso = recursoRepository.findById(request.recursoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Recurso", request.recursoId()));

        if (!recurso.isAtivo()) {
            throw new RegraNegocioException("O recurso não está disponível para reservas.");
        }
        if (reservaRepository.existeConflito(recurso.getId(), request.dataHoraInicio(), request.dataHoraFim())) {
            throw new ConflitoException("O recurso já está reservado em parte ou todo o período solicitado.");
        }

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .recurso(recurso)
                .dataHoraInicio(request.dataHoraInicio())
                .dataHoraFim(request.dataHoraFim())
                .status(StatusReserva.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .build();
        return ReservaResponse.de(reservaRepository.save(reserva));
    }

    @Transactional
    public ReservaResponse confirmar(Long id) {
        Reserva reserva = buscarEntidade(id);
        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Apenas reservas pendentes podem ser confirmadas.");
        }
        reserva.setStatus(StatusReserva.CONFIRMADA);
        return ReservaResponse.de(reserva);
    }

    @Transactional
    public CancelamentoResponse cancelar(Long id, CancelamentoRequest request) {
        Reserva reserva = buscarEntidade(id);
        if (reserva.getStatus() == StatusReserva.CANCELADA) {
            throw new RegraNegocioException("A reserva já está cancelada.");
        }

        LocalDateTime agora = LocalDateTime.now();
        double multa = 0;
        RegraCancelamento regra = regraRepository.findByRecursoId(reserva.getRecurso().getId()).orElse(null);
        if (regra != null) {
            if (!regra.isPermiteCancelamento()) {
                throw new RegraNegocioException("Este recurso não permite cancelamento de reservas.");
            }
            long horasRestantes = Duration.between(agora, reserva.getDataHoraInicio()).toHours();
            if (horasRestantes < regra.getHorasMinimasAntecedencia()) {
                multa = regra.getPercentualMulta();
            }
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladoEm(agora);
        reserva.setMotivoCancelamento(request == null ? null : request.motivo());
        return new CancelamentoResponse(ReservaResponse.de(reserva), multa);
    }

    @Transactional(readOnly = true)
    public ReservaResponse buscar(Long id) {
        return ReservaResponse.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(Long usuarioId, Long recursoId) {
        List<Reserva> reservas;
        if (usuarioId != null && recursoId != null) {
            reservas = reservaRepository.findByUsuarioIdAndRecursoId(usuarioId, recursoId);
        } else if (usuarioId != null) {
            reservas = reservaRepository.findByUsuarioId(usuarioId);
        } else if (recursoId != null) {
            reservas = reservaRepository.findByRecursoId(recursoId);
        } else {
            reservas = reservaRepository.findAll();
        }
        return reservas.stream().map(ReservaResponse::de).toList();
    }

    private Reserva buscarEntidade(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Reserva", id));
    }
}
