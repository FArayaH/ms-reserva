package com.hotel.ms_reserva.service;

import com.hotel.ms_reserva.model.Reserva;
import com.hotel.ms_reserva.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservaService {
    @Autowired
    private ReservaRepository reservaRepository;

    public List<Reserva> findAll() {
        return reservaRepository.findAll();
    }

    public Reserva findById(Long id) {
        return reservaRepository.findById(id).orElse(null);
    }
    //Metodo para poder guardar las reglas de negocio
    public Reserva save(Reserva reserva) {
        // 1. Regla de coherencia de fechas (Salida > Entrada)
        if (reserva.getFechaSalida().isBefore(reserva.getFechaEntrada()) ||
                reserva.getFechaSalida().isEqual(reserva.getFechaEntrada())) {

            throw new IllegalArgumentException("Error: la fecha de salida debe ser posterior a la fecha de entrada");
        }
    long diasEstadia = ChronoUnit.DAYS.between(reserva.getFechaEntrada(),reserva.getFechaSalida());
        if (diasEstadia > 30) {
        throw new IllegalStateException("Error el usuario ya tiene el maximo de 3 reservas pendientes");
        }
        //regla de negocio para que no spamen reservas
    long reservasPendientes = reservaRepository.countByUsuarioIdAndEstado(reserva.getUsuarioId(),"Pendiente");
        if (reservasPendientes >= 3) {
            throw new IllegalStateException("Error el usuario ya tiene el limite maximo de 3 reservas pendientes");
        }

        return reservaRepository.save(reserva);

    }
        public void delete(Long id) {
        Reserva reservaExistentes = reservaRepository.findById(id).orElse(null);

        if (reservaExistentes == null) {
           if(!reservaExistentes.getEstado().equalsIgnoreCase("Pendiente")) {
               throw new IllegalStateException("Solo se pueden elminar reservas en estados Pendiente");
           }
           reservaRepository.deleteById(id);
        }else{
            throw new IllegalArgumentException("La reserva que intenta borrar no existe");
        }
        }
}
