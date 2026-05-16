package com.hotel.ms_reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HabitacionDTO {
    private Long id;
    private Long hotelId;
    private String numeroHabitacion;
    private String tipo;
    private double precio;
    private boolean disponible;
}