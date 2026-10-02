package com.reservas.sistemareservas.config;

import com.reservas.sistemareservas.service.ValidadorHorarioReserva;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidadorHorarioConfig {

    @Bean
    public ValidadorHorarioReserva validadorHorarioReserva() {
        return new ValidadorHorarioReserva();
    }
}
