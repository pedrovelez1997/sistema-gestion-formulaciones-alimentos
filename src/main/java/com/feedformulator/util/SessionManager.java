package com.feedformulator.util;

import com.feedformulator.model.Usuario;

public class SessionManager {
    private static Usuario usuarioActual;

    public static void setUsuarioActual(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static boolean isLoggedIn() {
        return usuarioActual != null;
    }

    public static boolean isAdmin() {
        return isLoggedIn() && "ADMIN".equals(usuarioActual.getRol());
    }

    public static void logout() {
        usuarioActual = null;
    }

    public static String getNombreUsuario() {
        return isLoggedIn() ? usuarioActual.getNombre() : "Invitado";
    }
}