package com.hotel.ms_reserva.controller;

import com.hotel.ms_reserva.dto.ReservaRequestDTO;
import com.hotel.ms_reserva.dto.ReservaResponseDTO;
import com.hotel.ms_reserva.model.Reserva;
import com.hotel.ms_reserva.service.ReservaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reserva")
@RequiredArgsConstructor
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    // GET para ver todas las reservas
    @GetMapping
    @PreAuthorize("isAuthenticated()") // Cualquier usuario logueado puede listar
    public ResponseEntity<List<Reserva>> listar() {
        log.info("[RESERVA] GET /api/v1/reserva - Mostrando todas las reservas");
        List<Reserva> reservas = reservaService.findAll();
        if (reservas.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservas);
    }

    // GET para buscar reserva por ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {
        log.info("[RESERVA] GET /api/v1/reserva/{}", id);
        Reserva reserva = reservaService.findById(id);
        if (reserva != null) {
            return ResponseEntity.ok(reserva);
        }
        return ResponseEntity.notFound().build();
    }

    // POST para crear una nueva reserva
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> guardar(@Valid @RequestBody ReservaRequestDTO reservaDTO, HttpServletRequest request) {
        log.info("[RESERVA] POST /api/v1/reserva - añadir reserva");

        // se extrae el token
        String token = request.getHeader("Authorization");

        try {
            //  aca se le pasa el token al servicfe
            ReservaResponseDTO nuevaReserva = reservaService.save(reservaDTO, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReserva);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno en el servidor");
        }
    }

    // DELETE para eliminar reserva por ID
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Al igual que tu grupo: Solo el ADMIN puede borrar registros
    public ResponseEntity<?> eliminarReserva(@PathVariable Long id) {
        log.info("[RESERVA] DELETE /api/v1/reserva/{}", id);
        try {
            reservaService.delete(id);
            return ResponseEntity.ok("Reserva eliminada correctamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Reserva no encontrada con id: " + id);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
