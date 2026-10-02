package com.reservas.sistemareservas.repository;

import com.reservas.sistemareservas.entity.RegraCancelamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegraCancelamentoRepository extends JpaRepository<RegraCancelamento, Long> {

    Optional<RegraCancelamento> findByRecursoId(Long recursoId);
}
