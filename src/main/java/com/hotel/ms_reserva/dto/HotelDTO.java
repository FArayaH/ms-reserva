package com.hotel.ms_reserva.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelDTO {

    private Long id;
    private String nombre;
    private String ciudad;
    private int categoria;

}