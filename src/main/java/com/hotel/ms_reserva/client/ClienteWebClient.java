package com.hotel.ms_reserva.client;

import com.hotel.ms_reserva.dto.ClienteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClienteWebClient {

    private final WebClient.Builder webClientBuilder;

    // Método para ir a buscar al cliente usando su ID y el Token de seguridad
    public ClienteDTO obtenerClientePorId(Long clienteId, String token) {
        log.info("[WEB_CLIENT] Llamando a ms-cliente para obtener datos del cliente ID: {}", clienteId);

        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://localhost:8082/api/v1/cliente/" + clienteId)
                    .header("Authorization", token) // Pasamos el candado
                    .retrieve()
                    .bodyToMono(ClienteDTO.class)
                    .block(); // block() hace que espere la respuesta antes de seguir
        } catch (Exception e) {
            log.error("[WEB_CLIENT] Error al comunicarse con ms-cliente: {}", e.getMessage());
            // Si falla, lanzamos un error que tu GlobalExceptionHandler atrapará
            throw new RuntimeException("No se pudo obtener la información del cliente con ID: " + clienteId);
        }
    }
}