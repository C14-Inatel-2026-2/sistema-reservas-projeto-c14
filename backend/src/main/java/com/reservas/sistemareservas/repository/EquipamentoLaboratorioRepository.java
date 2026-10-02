package com.reservas.sistemareservas.repository;

import com.reservas.sistemareservas.entity.EquipamentoLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamentoLaboratorioRepository extends JpaRepository<EquipamentoLaboratorio, Long> {

    boolean existsByNumeroPatrimonio(String numeroPatrimonio);
}
