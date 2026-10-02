package com.reservas.sistemareservas.service;

import com.reservas.sistemareservas.dto.UsuarioRequest;
import com.reservas.sistemareservas.dto.UsuarioResponse;
import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.exception.ConflitoException;
import com.reservas.sistemareservas.exception.EntidadeNaoEncontradaException;
import com.reservas.sistemareservas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflitoException("Já existe um usuário com o e-mail " + request.email());
        }
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(request.senha())
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
