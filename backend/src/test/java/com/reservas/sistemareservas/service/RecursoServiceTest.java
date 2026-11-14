package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.RecursoResponse;
import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.EquipamentoLaboratorioRepository;
import com.reservas.sistemareservas.repository.QuadraRepository;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.SalaEstudoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecursoServiceTest {

    @Mock
    private RecursoRepository recursoRepository;
    @Mock
    private SalaEstudoRepository salaEstudoRepository;
    @Mock
    private QuadraRepository quadraRepository;
    @Mock
    private EquipamentoLaboratorioRepository equipamentoRepository;

    private RecursoService recursoService;

    @BeforeEach
    void setUp() {
        recursoService = new RecursoService(recursoRepository, salaEstudoRepository, quadraRepository,
                equipamentoRepository);
    }

    @Test
    void desativar_recursoAtivo_marcaComoInativoEMantemDados() {
        Quadra quadra = Quadra.builder().id(10L).nome("Quadra 1").ativo(true)
                .modalidade("Futsal").coberta(true).build();
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(quadra));

        RecursoResponse resposta = recursoService.desativar(10L);

        assertFalse(resposta.ativo());
        assertFalse(quadra.isAtivo());
        assertEquals("Quadra 1", resposta.nome());
        assertEquals("QUADRA", resposta.tipo());
    }

    @Test
    void ativar_recursoInativo_voltaAFicarDisponivelEMantemDados() {
        Quadra quadra = Quadra.builder().id(10L).nome("Quadra 1").ativo(false)
                .modalidade("Futsal").coberta(true).build();
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(quadra));

        RecursoResponse resposta = recursoService.ativar(10L);

        assertTrue(resposta.ativo());
        assertTrue(quadra.isAtivo());
        assertEquals("Quadra 1", resposta.nome());
        assertEquals("QUADRA", resposta.tipo());
    }

    @Test
    void ativar_recursoInexistente_lancaNaoEncontrado() {
        when(recursoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> recursoService.ativar(99L));
    }

    @Test
    void desativar_recursoInexistente_lancaNaoEncontrado() {
        when(recursoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> recursoService.desativar(99L));
    }
}
