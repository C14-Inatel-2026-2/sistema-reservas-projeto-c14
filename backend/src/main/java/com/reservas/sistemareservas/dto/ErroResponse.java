package com.reservas.sistemareservas.dto;

import java.util.List;

public record ErroResponse(int status, String mensagem, List<String> detalhes) {
}
