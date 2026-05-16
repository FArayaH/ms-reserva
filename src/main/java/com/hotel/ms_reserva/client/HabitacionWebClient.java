package com.hotel.ms_reserva.client;

import com.hotel.ms_reserva.dto.HabitacionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class HabitacionWebClient {

    private final WebClient.Builder webClientBuilder;

    public HabitacionDTO obtenerHabitacionPorId(Long habitacionId, String token) {
        log.info("[WEB_CLIENT] Llamando a ms-habitacion para obtener datos de la habitacion ID: {}", habitacionId);

        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://localhost:8083/api/v1/habitaciones/" + habitacionId)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(HabitacionDTO.class)
                    .block();
        } catch (Exception e) {
            log.error("[WEB_CLIENT] Error al comunicarse con ms-habitacion: {}", e.getMessage());
            throw new RuntimeException("No se pudo obtener la informacion de la habitacion con ID: " + habitacionId);
        }
    }
}