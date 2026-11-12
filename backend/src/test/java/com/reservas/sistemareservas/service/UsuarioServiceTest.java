package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.config.SenhaConfig;
import com.reservas.sistemareservas.dto.UsuarioRequest;
import com.reservas.sistemareservas.dto.UsuarioResponse;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new SenhaConfig().passwordEncoder();

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
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        String hashSalvo = captor.getValue().getSenha();
        assertNotEquals(request.senha(), hashSalvo);
        assertTrue(passwordEncoder.matches(request.senha(), hashSalvo));
        assertFalse(passwordEncoder.matches("senha-incorreta", hashSalvo));
    }

    @Test
    void criar_comMesmaSenha_geraHashesDiferentes() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.criar(new UsuarioRequest("Ana", "ana@teste.com", "123456", TipoUsuario.ALUNO));
        usuarioService.criar(new UsuarioRequest("Bia", "bia@teste.com", "123456", TipoUsuario.ALUNO));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(captor.capture());
        String hashAna = captor.getAllValues().get(0).getSenha();
        String hashBia = captor.getAllValues().get(1).getSenha();
        assertNotEquals(hashAna, hashBia);
        assertTrue(passwordEncoder.matches("123456", hashAna));
        assertTrue(passwordEncoder.matches("123456", hashBia));
    }

    @ParameterizedTest
    @MethodSource("senhasDe72Bytes")
    void criar_comSenhaDe72Bytes_salvaHashValido(String senha) {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.criar(new UsuarioRequest("Ana", "ana@teste.com", senha, TipoUsuario.ALUNO));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertTrue(passwordEncoder.matches(senha, captor.getValue().getSenha()));
    }

    @ParameterizedTest
    @MethodSource("senhasAcimaDe72Bytes")
    void criar_comSenhaAcimaDe72Bytes_lancaErroENaoSalva(String senha) {
        UsuarioRequest request = new UsuarioRequest("Ana", "ana@teste.com", senha, TipoUsuario.ALUNO);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.criar(request));

        assertEquals("A senha deve ter no máximo 72 bytes em UTF-8.", erro.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private static Stream<String> senhasDe72Bytes() {
        return Stream.of("a".repeat(72), "á".repeat(36));
    }

    private static Stream<String> senhasAcimaDe72Bytes() {
        return Stream.of("a".repeat(73), "á".repeat(37));
    }

    @Test
    void criar_comEmailJaCadastrado_lancaConflitoENaoSalva() {
        UsuarioRequest request = new UsuarioRequest("Ana", "ana@teste.com", "123456", TipoUsuario.ALUNO);
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        assertThrows(ConflitoException.class, () -> usuarioService.criar(request));

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void buscar_comIdInexistente_lancaEntidadeNaoEncontrada() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> usuarioService.buscar(99L));
    }
}
