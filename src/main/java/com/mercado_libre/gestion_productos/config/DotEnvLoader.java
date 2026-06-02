package com.mercado_libre.gestion_productos.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carga variables desde {@code .env}: primero en {@code gestion-inventario-back/.env} (si el
 * directorio de trabajo es la raiz del repo) y si no, en {@code ./.env} (si trabajas dentro del
 * modulo backend).
 * <p>
 * Spring Boot no lee {@code .env} por defecto; sin esto, claves como {@code ADMIN_BOOTSTRAP_*}
 * no llegan a {@code application.properties}.
 */
public final class DotEnvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);

    private DotEnvLoader() {}

    public static void loadIntoSystemProperties() {
        Path wd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        List<Path> dirs = List.of(wd.resolve("gestion-inventario-back"), wd);
        for (Path dir : dirs) {
            Path envFile = dir.resolve(".env");
            if (!Files.isRegularFile(envFile)) {
                continue;
            }
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(dir.toString())
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
                dotenv.entries().forEach(e -> putIfAbsent(e.getKey(), e.getValue()));
                log.info("Variables cargadas desde {}", envFile);
                return;
            } catch (Exception ex) {
                log.warn("No se pudo cargar .env en {}: {}", dir, ex.getMessage());
            }
        }
        log.debug("No se encontro archivo .env en gestion-inventario-back/ ni en {}", wd);
    }

    private static void putIfAbsent(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (System.getenv(key) != null) {
            return;
        }
        if (System.getProperty(key) != null) {
            return;
        }
        String v = value == null ? "" : value.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        System.setProperty(key, v);
    }
}
