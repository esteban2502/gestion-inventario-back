package com.mercado_libre.gestion_productos.util;

import java.util.regex.Pattern;

/**
 * Sanitiza nombres y textos de inventario: permite letras (Unicode), numeros, espacios y
 * {@code . , - + ( ) [ ] { } "} ; elimina el resto.
 */
public final class InventoryTextSanitization {

    private static final Pattern DISALLOWED =
            Pattern.compile("[^\\p{L}\\p{N}\\s.,\\-+()\\[\\]{}\\x22]");

    private InventoryTextSanitization() {}

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        String cleaned = DISALLOWED.matcher(trimmed).replaceAll("");
        return cleaned.trim().replaceAll("\\s+", " ");
    }
}
