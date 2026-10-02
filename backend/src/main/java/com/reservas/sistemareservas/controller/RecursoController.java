package com.reservas.sistemareservas.controller;

import com.reservas.sistemareservas.dto.EquipamentoLaboratorioRequest;
import com.reservas.sistemareservas.dto.QuadraRequest;
import com.reservas.sistemareservas.dto.RecursoResponse;
import com.reservas.sistemareservas.dto.RegraCancelamentoRequest;
import com.reservas.sistemareservas.dto.RegraCancelamentoResponse;
import com.reservas.sistemareservas.dto.SalaEstudoRequest;
import com.reservas.sistemareservas.service.RecursoService;
import com.reservas.sistemareservas.service.RegraCancelamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoService recursoService;
    private final RegraCancelamentoService regraCancelamentoService;

    @PostMapping("/salas")
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoResponse criarSala(@Valid @RequestBody SalaEstudoRequest request) {
        return recursoService.criarSala(request);
    }

    @PostMapping("/quadras")
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoResponse criarQuadra(@Valid @RequestBody QuadraRequest request) {
        return recursoService.criarQuadra(request);
    }

    @PostMapping("/equipamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoResponse criarEquipamento(@Valid @RequestBody EquipamentoLaboratorioRequest request) {
        return recursoService.criarEquipamento(request);
    }

    @GetMapping
    public List<RecursoResponse> listar() {
        return recursoService.listar();
    }

    @GetMapping("/{id}")
    public RecursoResponse buscar(@PathVariable Long id) {
        return recursoService.buscar(id);
    }

    @PutMapping("/{id}/regra-cancelamento")
    public RegraCancelamentoResponse definirRegra(@PathVariable Long id,
                                                  @Valid @RequestBody RegraCancelamentoRequest request) {
        return regraCancelamentoService.definir(id, request);
    }

    @GetMapping("/{id}/regra-cancelamento")
    public RegraCancelamentoResponse buscarRegra(@PathVariable Long id) {
        return regraCancelamentoService.buscar(id);
    }
}
