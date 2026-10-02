package com.reservas.sistemareservas.repository;

import com.reservas.sistemareservas.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioId(Long usuarioId);

    List<Reserva> findByRecursoId(Long recursoId);

    List<Reserva> findByUsuarioIdAndRecursoId(Long usuarioId, Long recursoId);

    @Query("""
            select count(r) > 0 from Reserva r
            where r.recurso.id = :recursoId
              and r.status <> com.reservas.sistemareservas.entity.enums.StatusReserva.CANCELADA
              and r.dataHoraInicio < :fim
              and r.dataHoraFim > :inicio
            """)
    boolean existeConflito(@Param("recursoId") Long recursoId,
                           @Param("inicio") LocalDateTime inicio,
                           @Param("fim") LocalDateTime fim);
}
