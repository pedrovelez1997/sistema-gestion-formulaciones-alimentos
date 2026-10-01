package com.feedformulator.util;

public class ValidationUtils {
    
    public static boolean validarEmail(String email) {
        if (email == null || email.isEmpty()) return true;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
    
    public static boolean validarRucCedula(String valor) {
        if (valor == null || valor.isEmpty()) return true;
        return valor.matches("^[0-9]{10,13}$");
    }
    
    public static boolean validarTelefono(String telefono) {
        if (telefono == null || telefono.isEmpty()) return true;
        return telefono.matches("^[0-9]{7,10}$");
    }
    
    public static boolean validarNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) return false;
        return nombre.trim().length() >= 2;
    }
    
    public static boolean validarPrecio(double precio) {
        return precio > 0;
    }
    
    public static boolean validarCantidad(double cantidad) {
        return cantidad > 0;
    }
}