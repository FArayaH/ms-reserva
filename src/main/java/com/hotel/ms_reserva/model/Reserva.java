package com.hotel.ms_reserva.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "reserva")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long hotelId;
    private Long usuarioId;
    private Long habitacionId;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private String estado = "PENDIENTE";
    private Integer cantidadPersonas;

}