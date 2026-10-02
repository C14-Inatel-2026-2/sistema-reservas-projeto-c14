package com.reservas.sistemareservas.controller;

import com.reservas.sistemareservas.dto.CancelamentoRequest;
import com.reservas.sistemareservas.dto.CancelamentoResponse;
import com.reservas.sistemareservas.dto.ReservaRequest;
import com.reservas.sistemareservas.dto.ReservaResponse;
import com.reservas.sistemareservas.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@Valid @RequestBody ReservaRequest request) {
        return reservaService.criar(request);
    }

    @GetMapping
    public List<ReservaResponse> listar(@RequestParam(required = false) Long usuarioId,
                                        @RequestParam(required = false) Long recursoId) {
        return reservaService.listar(usuarioId, recursoId);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable Long id) {
        return reservaService.buscar(id);
    }

    @PatchMapping("/{id}/confirmar")
    public ReservaResponse confirmar(@PathVariable Long id) {
        return reservaService.confirmar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public CancelamentoResponse cancelar(@PathVariable Long id,
                                         @RequestBody(required = false) CancelamentoRequest request) {
        return reservaService.cancelar(id, request);
    }
}
