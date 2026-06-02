package com.mercado_libre.gestion_productos;

import com.mercado_libre.gestion_productos.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GestionProductosApplication {

    public static void main(String[] args) {
        DotEnvLoader.loadIntoSystemProperties();
        SpringApplication.run(GestionProductosApplication.class, args);
    }
}
