package com.hotel.ms_reserva.client;

import com.hotel.ms_reserva.dto.HotelDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HotelWebClient {

    private final WebClient.Builder webClientBuilder;

    public HotelWebClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public HotelDTO obtenerHotelPorId(Long id, String token) {
        try {
            return webClientBuilder.build()
                    .get()
                    // IMPORTANTE: Cambia el 8080 por el puerto exacto donde corre ms-hotel
                    // Y ajusta la ruta si tu compañera le puso un nombre distinto a "/api/v1/hoteles/"
                    .uri("http://localhost:8080/api/v1/hoteles/" + id)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(HotelDTO.class)
                    .block();
        } catch (Exception e) {
            return null; // Si no lo encuentra o falla, devuelve null para que el Service lance el error
        }
    }
}