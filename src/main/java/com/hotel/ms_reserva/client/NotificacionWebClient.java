package com.hotel.ms_reserva.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Component
public class NotificacionWebClient {

    private final WebClient.Builder webClientBuilder;

    public NotificacionWebClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public void dispararNotificacion(Long reservaId, String token) {
        try {
            // Enviamos el DTO exacto que tu ms-notificacion espera
            Map<String, Long> body = Map.of(
                    "reservaId", reservaId
            );

            webClientBuilder.build().post()
                    .uri("http://localhost:8084/api/v1/notificacion")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

        } catch (Exception e) {
            System.out.println("Error al conectar con ms-notificacion: " + e.getMessage());
        }
    }
}