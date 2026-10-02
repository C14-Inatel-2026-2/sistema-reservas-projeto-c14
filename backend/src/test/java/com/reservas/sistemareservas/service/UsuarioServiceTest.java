package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.UsuarioRequest;
import com.reservas.sistemareservas.dto.UsuarioResponse;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void criar_comEmailNovo_salvaERetornaUsuario() {
        UsuarioRequest request = new UsuarioRequest("Ana", "ana@teste.com", "123456", TipoUsuario.ALUNO);
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioResponse resposta = usuarioService.criar(request);

        assertEquals(1L, resposta.id());
        assertEquals("Ana", resposta.nome());
        assertEquals(TipoUsuario.ALUNO, resposta.tipo());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void criar_comEmailJaCadastrado_lancaConflitoENaoSalva() {
        UsuarioRequest request = new UsuarioRequest("Ana", "ana@teste.com", "123456", TipoUsuario.ALUNO);
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> usuarioService.criar(request));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void buscar_comIdInexistente_lancaEntidadeNaoEncontrada() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> usuarioService.buscar(99L));
    }
}
