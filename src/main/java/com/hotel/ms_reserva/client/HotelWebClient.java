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
                    .uri("http://localhost:8084/api/v1/hoteles/" + id)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(HotelDTO.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }
}