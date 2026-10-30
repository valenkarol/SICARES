package co.edu.uniquindio.poo.sicares;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Sprint 3: procesa los recordatorios automáticamente (ServicioNotificaciones)
public class SicaresApplication {

    public static void main(String[] args) {
        SpringApplication.run(SicaresApplication.class, args);
    }

}
