package com.hotel.ms_reserva.config;

import com.hotel.ms_reserva.model.Reserva;
import com.hotel.ms_reserva.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReservaRepository reservaRepository;

    @Override
    public void run(String... args){
        if (reservaRepository.count() > 0){
            log.info("La base de datos ya tiene reservas Omitiendo carga inicial");
            return;
        }
        log.info("La BD de reservas esta vacia. Insertando datos de prueba");
        Reserva res1 = new Reserva();
        res1.setHotelId(1L);
        res1.setUsuarioId(1L);
        res1.setFechaEntrada(LocalDate.now().plusDays(5)); // Entrada en 5 dias
        res1.setFechaSalida(LocalDate.now().plusDays(10)); // Salida en 10 dias
        res1.setCantidadPersonas(2);
        res1.setEstado("PENDIENTE");
        reservaRepository.save(res1);

        Reserva res2 = new Reserva();
        res2.setHotelId(2L);
        res2.setUsuarioId(2L);
        res2.setFechaEntrada(LocalDate.now().plusDays(15)); // Entrada en 15 dias
        res2.setFechaSalida(LocalDate.now().plusDays(20)); // Salida en 20 dias
        res2.setCantidadPersonas(4);
        res2.setEstado("PENDIENTE");
        reservaRepository.save(res2);

        log.info("Se insertaron 2 reservas de prueba de manera exitosa");
    }
}
