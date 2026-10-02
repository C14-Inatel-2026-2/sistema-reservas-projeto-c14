package com.reservas.sistemareservas.dto;

import com.reservas.sistemareservas.entity.EquipamentoLaboratorio;
import com.reservas.sistemareservas.entity.Quadra;
import com.reservas.sistemareservas.entity.SalaEstudo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursoResponseTest {

    @Test
    void de_comSalaEstudo_preencheCamposDaSala() {
        SalaEstudo sala = SalaEstudo.builder().id(1L).nome("Sala 1").localizacao("Bloco A")
                .ativo(true).capacidadePessoas(6).possuiProjetor(true).build();

        RecursoResponse resposta = RecursoResponse.de(sala);

        assertEquals("SALA_ESTUDO", resposta.tipo());
        assertEquals(6, resposta.capacidadePessoas());
        assertTrue(resposta.possuiProjetor());
    }

    @Test
    void de_comQuadra_naoPreencheCamposDeOutrosTipos() {
        Quadra quadra = Quadra.builder().id(2L).nome("Quadra 1").ativo(true)
                .modalidade("Futsal").coberta(true).build();

        RecursoResponse resposta = RecursoResponse.de(quadra);

        assertEquals("QUADRA", resposta.tipo());
        assertEquals("Futsal", resposta.modalidade());
        assertNull(resposta.capacidadePessoas());
        assertNull(resposta.numeroPatrimonio());
    }

    @Test
    void de_comEquipamentoInativo_mantemAtivoFalso() {
        EquipamentoLaboratorio equipamento = EquipamentoLaboratorio.builder().id(3L).nome("Microscopio")
                .ativo(false).laboratorio("Lab Bio").numeroPatrimonio("PAT-001").build();

        RecursoResponse resposta = RecursoResponse.de(equipamento);

        assertEquals("EQUIPAMENTO_LABORATORIO", resposta.tipo());
        assertEquals("PAT-001", resposta.numeroPatrimonio());
        assertEquals(false, resposta.ativo());
    }
}
