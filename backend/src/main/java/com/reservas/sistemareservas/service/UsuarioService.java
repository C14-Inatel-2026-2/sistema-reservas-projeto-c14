package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.UsuarioRequest;
import com.reservas.sistemareservas.dto.UsuarioResponse;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflitoException("Já existe um usuário com o e-mail " + request.email());
        }
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ter no máximo 72 bytes em UTF-8.");
        }
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .tipo(request.tipo())
                .criadoEm(LocalDateTime.now())
                .build();
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", id));
    }
}
