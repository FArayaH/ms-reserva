package com.hotel.ms_reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotNull(message = "El ID del hotel es obligatorio")
    private Long hotelId;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "El ID de la habitacion es obligatorio")
    private Long habitacionId;

    //regla de negocio No se puede hacer una en el pasado
    @NotNull(message = "La fecha de entrada es obligatoria")
    @FutureOrPresent(message = "La fecha de entrada debe ser hoy o en el futuro")
    private LocalDate fechaEntrada;

    //regla de negocio La salida debe ser en el futuro
    @NotNull(message = "La fecha de salida es obligatoria")
    @Future(message = "La fecha de salida debe ser en el futuro")
    private LocalDate fechaSalida;

    //Regla de negocio cantidad de personas
    @NotNull(message = "Debe indicar la cantidad de personas")
    @Min(value = 1, message = "La reserva debe ser para al menos 1 persona")
    @Max(value = 10, message = "No se permiten mas de 10 personas por reserva estandar")
    private Integer cantidadPersonas;
}