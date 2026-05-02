package com.hotel.ms_reserva.service;

import com.hotel.ms_reserva.dto.ReservaRequestDTO;
import com.hotel.ms_reserva.dto.ReservaResponseDTO;
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
    public ReservaResponseDTO save(ReservaRequestDTO reservaRequestDTO) {

        // 1. Regla de coherencia de fechas (Salida > Entrada) usando el DTO
        if (reservaRequestDTO.getFechaSalida().isBefore(reservaRequestDTO.getFechaEntrada()) ||
                reservaRequestDTO.getFechaSalida().isEqual(reservaRequestDTO.getFechaEntrada())) {
            throw new IllegalArgumentException("Error la fecha de salida debe ser posterior a la fecha de entrada");
        }

        long diasEstadia = ChronoUnit.DAYS.between(reservaRequestDTO.getFechaEntrada(), reservaRequestDTO.getFechaSalida());
        if (diasEstadia > 30) {
            throw new IllegalStateException("Error La reserva no puede durar más de 30 días.");
        }

        // 2. Regla de negocio para que no spamen reservas usando el DTO
        long reservasPendientes = reservaRepository.countByUsuarioIdAndEstado(reservaRequestDTO.getUsuarioId(), "PENDIENTE");
        if (reservasPendientes >= 3) {
            throw new IllegalStateException("Error El usuario ya tiene el límite máximo de 3 reservas pendientes");
        }

        // 3. Transformo el DTO de entrada a la Entidad (Model) real
        Reserva reserva = new Reserva();
        reserva.setHotelId(reservaRequestDTO.getHotelId());
        reserva.setUsuarioId(reservaRequestDTO.getUsuarioId());
        reserva.setFechaEntrada(reservaRequestDTO.getFechaEntrada());
        reserva.setFechaSalida(reservaRequestDTO.getFechaSalida());
        reserva.setCantidadPersonas(reservaRequestDTO.getCantidadPersonas());

        // Regla de Negocio Asegurar que toda reserva nueva nazca como PENDIENTE
        reserva.setEstado("PENDIENTE");

        //  Guardamos en la base de datos
        Reserva reservaGuardada = reservaRepository.save(reserva);

        //  Transformamos la Reserva guardada al DTO de salida y lo retorno
        return new ReservaResponseDTO(
                reservaGuardada.getId(),
                reservaGuardada.getHotelId(),
                reservaGuardada.getUsuarioId(),
                reservaGuardada.getFechaEntrada(),
                reservaGuardada.getFechaSalida(),
                reservaGuardada.getCantidadPersonas(),
                reservaGuardada.getEstado()
        );
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
