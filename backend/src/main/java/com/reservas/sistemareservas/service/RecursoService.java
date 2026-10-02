package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.EquipamentoLaboratorioRequest;
import com.reservas.sistemareservas.dto.QuadraRequest;
import com.reservas.sistemareservas.dto.RecursoResponse;
import com.reservas.sistemareservas.dto.SalaEstudoRequest;
import com.reservas.sistemareservas.entity.EquipamentoLaboratorio;
import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.entity.SalaEstudo;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.EquipamentoLaboratorioRepository;
import com.reservas.sistemareservas.repository.QuadraRepository;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.SalaEstudoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final SalaEstudoRepository salaEstudoRepository;
    private final QuadraRepository quadraRepository;
    private final EquipamentoLaboratorioRepository equipamentoRepository;

    @Transactional
    public RecursoResponse criarSala(SalaEstudoRequest request) {
        SalaEstudo sala = SalaEstudo.builder()
                .nome(request.nome())
                .localizacao(request.localizacao())
                .ativo(true)
                .capacidadePessoas(request.capacidadePessoas())
                .possuiProjetor(request.possuiProjetor())
                .build();
        return RecursoResponse.de(salaEstudoRepository.save(sala));
    }

    @Transactional
    public RecursoResponse criarQuadra(QuadraRequest request) {
        Quadra quadra = Quadra.builder()
                .nome(request.nome())
                .localizacao(request.localizacao())
                .ativo(true)
                .modalidade(request.modalidade())
                .coberta(request.coberta())
                .build();
        return RecursoResponse.de(quadraRepository.save(quadra));
    }

    @Transactional
    public RecursoResponse criarEquipamento(EquipamentoLaboratorioRequest request) {
        if (equipamentoRepository.existsByNumeroPatrimonio(request.numeroPatrimonio())) {
            throw new ConflitoException("Já existe um equipamento com o patrimônio " + request.numeroPatrimonio());
        }
        EquipamentoLaboratorio equipamento = EquipamentoLaboratorio.builder()
                .nome(request.nome())
                .localizacao(request.localizacao())
                .ativo(true)
                .laboratorio(request.laboratorio())
                .numeroPatrimonio(request.numeroPatrimonio())
                .build();
        return RecursoResponse.de(equipamentoRepository.save(equipamento));
    }

    @Transactional(readOnly = true)
    public List<RecursoResponse> listar() {
        return recursoRepository.findAll().stream().map(RecursoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public RecursoResponse buscar(Long id) {
        return recursoRepository.findById(id)
                .map(RecursoResponse::de)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Recurso", id));
    }
}
