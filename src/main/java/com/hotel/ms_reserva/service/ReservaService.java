package com.hotel.ms_reserva.service;

import com.hotel.ms_reserva.client.ClienteWebClient;
import com.hotel.ms_reserva.client.HabitacionWebClient;
import com.hotel.ms_reserva.dto.ClienteDTO;
import com.hotel.ms_reserva.dto.HabitacionDTO;
import com.hotel.ms_reserva.dto.ReservaRequestDTO;
import com.hotel.ms_reserva.dto.ReservaResponseDTO;
import com.hotel.ms_reserva.model.Reserva;
import com.hotel.ms_reserva.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteWebClient clienteWebClient;
    private final HabitacionWebClient habitacionWebClient;

    @Transactional(readOnly = true)
    public List<Reserva> findAll() {
        log.info("[RESERVA_SERVICE] Consultando todas las reservas");
        return reservaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reserva findById(Long id) {
        log.info("[RESERVA_SERVICE] Buscando reserva por ID: {}", id);
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La reserva con el ID " + id + " no existe en la base de datos"));
    }

    // Metodo para poder guardar las reglas de negocio
    @Transactional
    public ReservaResponseDTO save(ReservaRequestDTO reservaRequestDTO, String token) {

        log.info("[RESERVA_SERVICE] Validando reglas de negocio para la reserva...");

        log.info("[RESERVA_SERVICE] Conectando con ms-cliente para validar usuario ID: {}", reservaRequestDTO.getUsuarioId());
        ClienteDTO cliente = clienteWebClient.obtenerClientePorId(reservaRequestDTO.getUsuarioId(), token);

        if (cliente == null) {
            log.error("Falla de integracion: El cliente con ID {} no existe en ms-cliente", reservaRequestDTO.getUsuarioId());
            throw new IllegalArgumentException("Error: No se puede crear la reserva porque el usuario proporcionado no existe.");
        }
        log.info("[RESERVA_SERVICE] Cliente verificado exitosamente: {} {}", cliente.getNombre(), cliente.getApellido());

        log.info("[RESERVA_SERVICE] Conectando con ms-habitacion para validar habitacion ID: {}", reservaRequestDTO.getHabitacionId());
        HabitacionDTO habitacion = habitacionWebClient.obtenerHabitacionPorId(reservaRequestDTO.getHabitacionId(), token);

        if (habitacion == null) {
            log.error("Falla de integracion: La habitacion con ID {} no existe en ms-habitacion", reservaRequestDTO.getHabitacionId());
            throw new IllegalArgumentException("Error: No se puede crear la reserva porque la habitacion proporcionada no existe.");
        }

        if (!habitacion.isDisponible()) {
            log.error("Falla de negocio: La habitacion con ID {} no esta disponible", reservaRequestDTO.getHabitacionId());
            throw new IllegalStateException("Error: La habitacion seleccionada no se encuentra disponible actualmente.");
        }
        log.info("[RESERVA_SERVICE] Habitacion verificada exitosamente. Disponible: {}", habitacion.isDisponible());

        // 1. Regla de coherencia de fechas (Salida > Entrada) usando el DTO
        if (reservaRequestDTO.getFechaSalida().isBefore(reservaRequestDTO.getFechaEntrada()) ||
                reservaRequestDTO.getFechaSalida().isEqual(reservaRequestDTO.getFechaEntrada())) {
            log.warn("Falla en regla de fechas: Entrada y Salida no coherentes");
            throw new IllegalArgumentException("Error la fecha de salida debe ser posterior a la fecha de entrada");
        }

        long diasEstadia = ChronoUnit.DAYS.between(reservaRequestDTO.getFechaEntrada(), reservaRequestDTO.getFechaSalida());
        if (diasEstadia > 30) {
            log.warn("Falla en regla de estadia: Intento de reserva por {} dias", diasEstadia);
            throw new IllegalStateException("Error La reserva no puede durar mas de 30 dias.");
        }

        // 2. Regla de negocio para que no spamen reservas usando el DTO
        long reservasPendientes = reservaRepository.countByUsuarioIdAndEstado(reservaRequestDTO.getUsuarioId(), "PENDIENTE");
        if (reservasPendientes >= 3) {
            log.warn("Falla en regla de spam: Usuario {} ya tiene 3 reservas pendientes", reservaRequestDTO.getUsuarioId());
            throw new IllegalStateException("Error El usuario ya tiene el limite maximo de 3 reservas pendientes");
        }

        // 3. Transformo el DTO de entrada a la Entidad (Model) real
        Reserva reserva = new Reserva();
        reserva.setHotelId(reservaRequestDTO.getHotelId());
        reserva.setUsuarioId(reservaRequestDTO.getUsuarioId());
        reserva.setHabitacionId(reservaRequestDTO.getHabitacionId());
        reserva.setFechaEntrada(reservaRequestDTO.getFechaEntrada());
        reserva.setFechaSalida(reservaRequestDTO.getFechaSalida());
        reserva.setCantidadPersonas(reservaRequestDTO.getCantidadPersonas());

        // Regla de Negocio Asegurar que toda reserva nueva nazca como PENDIENTE
        reserva.setEstado("PENDIENTE");

        //  Guardamos en la base de datos
        log.info("[RESERVA_SERVICE] Reglas validadas. Guardando reserva en BD...");
        Reserva reservaGuardada = reservaRepository.save(reserva);

        //  Transformamos la Reserva guardada al DTO de salida y lo retorno
        return new ReservaResponseDTO(
                reservaGuardada.getId(),
                reservaGuardada.getHotelId(),
                reservaGuardada.getUsuarioId(),
                reservaGuardada.getHabitacionId(),
                reservaGuardada.getFechaEntrada(),
                reservaGuardada.getFechaSalida(),
                reservaGuardada.getCantidadPersonas(),
                reservaGuardada.getEstado()
        );
    }

    @Transactional
    public void delete(Long id) {
        log.info("[RESERVA_SERVICE] Verificando eliminacion de reserva ID: {}", id);

        // busca la reserva
        Reserva reservaExistentes = reservaRepository.findById(id)
                .orElseThrow(() -> {
                    // y esto por si es nula
                    log.error("Falla en eliminacion: Reserva ID {} no existe", id);
                    return new IllegalArgumentException("La reserva con el ID " + id + " no existe en la base de datos");
                });

        // Si la reserva existe y no es nula, entramos a validar y borrar

        // Validacion si es PENDIENTE
        if (!reservaExistentes.getEstado().equalsIgnoreCase("PENDIENTE")) {
            log.warn("Falla en eliminacion: Reserva ID {} no esta PENDIENTE (Estado actual: {})", id, reservaExistentes.getEstado());
            throw new IllegalStateException("Solo se pueden eliminar reservas en estado PENDIENTE");
        }

        // y si no hay problemas con la validacion borra
        reservaRepository.deleteById(id);
        log.info("[RESERVA_SERVICE] Reserva ID: {} eliminada correctamente", id);
    }
}