package com.mercado_libre.gestion_productos.exception;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String message) {
        super(message);
    }
}
