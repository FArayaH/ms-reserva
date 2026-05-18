package com.hotel.ms_reserva.service;

import com.hotel.ms_reserva.client.ClienteWebClient;
import com.hotel.ms_reserva.client.HabitacionWebClient;
import com.hotel.ms_reserva.client.HotelWebClient;
import com.hotel.ms_reserva.client.NotificacionWebClient;
import com.hotel.ms_reserva.dto.ClienteDTO;
import com.hotel.ms_reserva.dto.HabitacionDTO;
import com.hotel.ms_reserva.dto.HotelDTO;
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
    private final HotelWebClient hotelWebClient;
    private final NotificacionWebClient notificacionWebClient;

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

    @Transactional
    public ReservaResponseDTO save(ReservaRequestDTO reservaRequestDTO, String token) {

        log.info("[RESERVA_SERVICE] Validando reglas de negocio para la reserva...");

        // --- 1. VALIDACIÓN DE HOTEL ---
        log.info("[RESERVA_SERVICE] Conectando con ms-hotel para validar hotel ID: {}", reservaRequestDTO.getHotelId());
        HotelDTO hotel = hotelWebClient.obtenerHotelPorId(reservaRequestDTO.getHotelId(), token);

        if (hotel == null) {
            log.error("Falla de integracion: El hotel con ID {} no existe en ms-hotel", reservaRequestDTO.getHotelId());
            throw new IllegalArgumentException("Error: No se puede crear la reserva porque el hotel proporcionado no existe.");
        }
        log.info("[RESERVA_SERVICE] Hotel verificado exitosamente: {}", hotel.getNombre());

        // --- 2. VALIDACIÓN DE CLIENTE ---
        log.info("[RESERVA_SERVICE] Conectando con ms-cliente para validar usuario ID: {}", reservaRequestDTO.getUsuarioId());
        ClienteDTO cliente = clienteWebClient.obtenerClientePorId(reservaRequestDTO.getUsuarioId(), token);

        if (cliente == null) {
            log.error("Falla de integracion: El cliente con ID {} no existe en ms-cliente", reservaRequestDTO.getUsuarioId());
            throw new IllegalArgumentException("Error: No se puede crear la reserva porque el usuario proporcionado no existe.");
        }
        log.info("[RESERVA_SERVICE] Cliente verificado exitosamente: {} {}", cliente.getNombre(), cliente.getApellido());

        // --- 3. VALIDACIÓN DE HABITACIÓN ---
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

        // --- 4. REGLAS DE NEGOCIO INTERNAS (FECHAS Y SPAM) ---
        if (reservaRequestDTO.getFechaSalida().isBefore(reservaRequestDTO.getFechaEntrada()) ||
                reservaRequestDTO.getFechaSalida().isEqual(reservaRequestDTO.getFechaEntrada())) {
            log.warn("Falla en regla de fechas: Entrada y Salida no coherentes");
            throw new IllegalArgumentException("Error: la fecha de salida debe ser posterior a la fecha de entrada");
        }

        long diasEstadia = ChronoUnit.DAYS.between(reservaRequestDTO.getFechaEntrada(), reservaRequestDTO.getFechaSalida());
        if (diasEstadia > 30) {
            log.warn("Falla en regla de estadia: Intento de reserva por {} dias", diasEstadia);
            throw new IllegalStateException("Error: La reserva no puede durar mas de 30 dias.");
        }

        long reservasPendientes = reservaRepository.countByUsuarioIdAndEstado(reservaRequestDTO.getUsuarioId(), "PENDIENTE");
        if (reservasPendientes >= 3) {
            log.warn("Falla en regla de spam: Usuario {} ya tiene 3 reservas pendientes", reservaRequestDTO.getUsuarioId());
            throw new IllegalStateException("Error: El usuario ya tiene el limite maximo de 3 reservas pendientes");
        }

        // --- 5. GUARDADO EN BASE DE DATOS ---
        Reserva reserva = new Reserva();
        reserva.setHotelId(reservaRequestDTO.getHotelId());
        reserva.setUsuarioId(reservaRequestDTO.getUsuarioId());
        reserva.setHabitacionId(reservaRequestDTO.getHabitacionId());
        reserva.setFechaEntrada(reservaRequestDTO.getFechaEntrada());
        reserva.setFechaSalida(reservaRequestDTO.getFechaSalida());
        reserva.setCantidadPersonas(reservaRequestDTO.getCantidadPersonas());

        // Regla de Negocio: Asegurar que toda reserva nueva nazca como PENDIENTE
        reserva.setEstado("PENDIENTE");

        log.info("[RESERVA_SERVICE] Reglas validadas. Guardando reserva en BD...");
        Reserva reservaGuardada = reservaRepository.save(reserva);

        // --- DISPARO AUTOMÁTICO DE LA NOTIFICACIÓN CORREGIDO ---
        try {
            // Se utiliza el ID de la entidad persistida para coordinar con ms-notificacion
            notificacionWebClient.dispararNotificacion(reservaGuardada.getId(), token);
            log.info("[RESERVA_SERVICE] Petición de notificación enviada de forma automática a ms-notificacion para Reserva ID: {}.", reservaGuardada.getId());
        } catch (Exception e) {
            log.error("[RESERVA_SERVICE] La reserva se guardó, pero falló el envío de la notificación: {}", e.getMessage());
        }

        // Transformamos la Reserva guardada al DTO de salida y lo retornamos
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

        Reserva reservaExistente = reservaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Falla en eliminacion: Reserva ID {} no existe", id);
                    return new IllegalArgumentException("La reserva con el ID " + id + " no existe en la base de datos");
                });

        if (!reservaExistente.getEstado().equalsIgnoreCase("PENDIENTE")) {
            log.warn("Falla en eliminacion: Reserva ID {} no esta PENDIENTE (Estado actual: {})", id, reservaExistente.getEstado());
            throw new IllegalStateException("Solo se pueden eliminar reservas en estado PENDIENTE");
        }

        reservaRepository.deleteById(id);
        log.info("[RESERVA_SERVICE] Reserva ID: {} eliminada correctamente", id);
    }

    // Método para actualizar el estado de la reserva (Llamado por ms-checkin y ms-checkout)
    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        log.info(">> Cambiando estado de la reserva ID: {} a {}", id, nuevoEstado);

        // 1. Buscamos la reserva real en la base de datos
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La reserva con ID " + id + " no existe."));

        // 2. Le cambiamos el estado
        reserva.setEstado(nuevoEstado.toUpperCase());

        // 3. Guardamos la actualización
        reservaRepository.save(reserva);
        log.info("<< Estado de la reserva ID: {} actualizado con éxito en la BD", id);
    }
}