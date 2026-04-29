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

        // 🛡️ Regla de Negocio: Asegurar que toda reserva nueva nazca como PENDIENTE
        if (reserva.getEstado() == null) {
            reserva.setEstado("PENDIENTE");
        }

        // 1. Regla de coherencia de fechas (Salida > Entrada)
        if (reserva.getFechaSalida().isBefore(reserva.getFechaEntrada()) ||
                reserva.getFechaSalida().isEqual(reserva.getFechaEntrada())) {

            throw new IllegalArgumentException("Error: la fecha de salida debe ser posterior a la fecha de entrada");
        }
    long diasEstadia = ChronoUnit.DAYS.between(reserva.getFechaEntrada(),reserva.getFechaSalida());
        if (diasEstadia > 30) {
        throw new IllegalStateException("Error: La reserva no puede durar más de 30 días.");
        }
        //regla de negocio para que no spamen reservas
    long reservasPendientes = reservaRepository.countByUsuarioIdAndEstado(reserva.getUsuarioId(),"Pendiente");
        if (reservasPendientes >= 3) {
            throw new IllegalStateException("Error: El usuario ya tiene el límite máximo de 3 reservas pendientes");
        }

        return reservaRepository.save(reserva);

    }
    public void delete(Long id) {
        //  busca la reserva
        Reserva reservaExistentes = reservaRepository.findById(id).orElse(null);
        //  Si la reserva existe y no es nula, entramos a validar y borrar
        if (reservaExistentes != null) {

            // Validacion si es PENDIENTE
            if (!reservaExistentes.getEstado().equalsIgnoreCase("PENDIENTE")) {
                throw new IllegalStateException("Solo se pueden eliminar reservas en estado PENDIENTE");
            }
            // y si no hay problemas con la validacion borra
            reservaRepository.deleteById(id);
        } else {
            // y esto por si es nula
            throw new IllegalArgumentException("La reserva con el ID " + id + " no existe en la base de datos");
        }
    }
}
