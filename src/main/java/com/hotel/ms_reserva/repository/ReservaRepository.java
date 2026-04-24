package com.hotel.ms_reserva.repository;

import com.hotel.ms_reserva.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    //Metodo para contar cuantas reservas tiene un usuario en un estado especifico
    Long countByUsuarioIdAndEstado(Long usuarioId, String estado);
}
