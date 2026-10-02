package com.reservas.sistemareservas.exception;

public class EntidadeNaoEncontradaException extends RuntimeException {

    public EntidadeNaoEncontradaException(String entidade, Long id) {
        super(entidade + " não encontrado(a) com id " + id);
    }
}
