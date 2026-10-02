package com.reservas.sistemareservas.dto;

import com.reservas.sistemareservas.entity.Usuario;
import com.reservas.sistemareservas.entity.enums.TipoUsuario;

import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String nome, String email, TipoUsuario tipo, LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTipo(), u.getCriadoEm());
    }
}
