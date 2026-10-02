package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.RegraCancelamentoRequest;
import com.reservas.sistemareservas.dto.RegraCancelamentoResponse;
import com.reservas.sistemareservas.entity.Recurso;
import com.reservas.sistemareservas.entity.RegraCancelamento;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.RegraCancelamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegraCancelamentoService {

    private final RegraCancelamentoRepository regraRepository;
    private final RecursoRepository recursoRepository;

    @Transactional
    public RegraCancelamentoResponse definir(Long recursoId, RegraCancelamentoRequest request) {
        Recurso recurso = recursoRepository.findById(recursoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Recurso", recursoId));

        RegraCancelamento regra = regraRepository.findByRecursoId(recursoId)
                .orElseGet(() -> RegraCancelamento.builder().recurso(recurso).build());
        regra.setHorasMinimasAntecedencia(request.horasMinimasAntecedencia());
        regra.setPercentualMulta(request.percentualMulta());
        regra.setPermiteCancelamento(request.permiteCancelamento());
        regra.setObservacoes(request.observacoes());
        return RegraCancelamentoResponse.de(regraRepository.save(regra));
    }

    @Transactional(readOnly = true)
    public RegraCancelamentoResponse buscar(Long recursoId) {
        return regraRepository.findByRecursoId(recursoId)
                .map(RegraCancelamentoResponse::de)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Regra de cancelamento do recurso", recursoId));
    }
}
