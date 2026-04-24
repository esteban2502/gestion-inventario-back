package com.mercado_libre.gestion_productos.exception;

public class CategoryHasProductsException extends RuntimeException {

    public CategoryHasProductsException(String message) {
        super(message);
    }
}

