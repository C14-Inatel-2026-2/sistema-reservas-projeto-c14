package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.RegraCancelamentoRequest;
import com.reservas.sistemareservas.entity.SalaEstudo;
import com.reservas.sistemareservas.entity.RegraCancelamento;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.RecursoRepository;
import com.reservas.sistemareservas.repository.RegraCancelamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegraCancelamentoServiceTest {

    @Mock
    private RegraCancelamentoRepository regraRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @InjectMocks
    private RegraCancelamentoService service;

    @Test
    void definirCriaRegraParaRecursoSemConfiguracao() {
        SalaEstudo sala = SalaEstudo.builder().id(10L).nome("Sala 1").ativo(true).build();
        RegraCancelamentoRequest request = new RegraCancelamentoRequest(24, 15.0, true, "Até um dia antes");
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(sala));
        when(regraRepository.findByRecursoId(10L)).thenReturn(Optional.empty());
        when(regraRepository.save(any(RegraCancelamento.class))).thenAnswer(invocation -> {
            RegraCancelamento regra = invocation.getArgument(0);
            regra.setId(7L);
            return regra;
        });

        var resposta = service.definir(10L, request);

        assertEquals(7L, resposta.id());
        assertEquals(10L, resposta.recursoId());
        assertEquals(Integer.valueOf(24), resposta.horasMinimasAntecedencia());
        assertEquals(Double.valueOf(15.0), resposta.percentualMulta());
        assertTrue(resposta.permiteCancelamento());
        assertEquals("Até um dia antes", resposta.observacoes());
        verify(regraRepository).save(any(RegraCancelamento.class));
    }

    @Test
    void buscarRegraInexistenteLancaErro() {
        when(regraRepository.findByRecursoId(99L)).thenReturn(Optional.empty());

        EntidadeNaoEncontradaException erro = assertThrows(
                EntidadeNaoEncontradaException.class, () -> service.buscar(99L));

        assertTrue(erro.getMessage().contains("99"));
        verifyNoInteractions(recursoRepository);
    }
}
