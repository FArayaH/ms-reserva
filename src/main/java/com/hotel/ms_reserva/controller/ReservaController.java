package com.hotel.ms_reserva.controller;

import com.hotel.ms_reserva.model.Reserva;
import com.hotel.ms_reserva.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reserva")
public class ReservaController {
    @Autowired
    private ReservaService reservaService;

    // esto es para listar todas las reservas
    @GetMapping
    public ResponseEntity<List<Reserva>> listar(){
        List<Reserva> reservas = reservaService.findAll();
        if(reservas.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservas);

    }

    // Esto es para buscar por ID
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id){
        Reserva reserva = reservaService.findById(id);
        if(reserva != null){
            return ResponseEntity.ok(reserva);
        }
        return ResponseEntity.notFound().build();
    }

    // Esto es para crear una reserva y aqui se van activar las reglas de negocio
    @PostMapping
    public ResponseEntity<?> guardar(@Valid @RequestBody Reserva reserva,org.springframework.validation.BindingResult result){

        if(result.hasErrors()){
            String mensajeError = result.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensajeError);
        }

        try {
            Reserva nuevaReserva = reservaService.save(reserva);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReserva);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // aca captura los throw new que creamos en el service
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno en el servidor");
        }
    }
    // Y este es para poder eliminar la reservs
    @DeleteMapping("/{id}")
    public ResponseEntity<?> elimnarReserva(@PathVariable Long id){
        try {
            reservaService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e){
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

}
