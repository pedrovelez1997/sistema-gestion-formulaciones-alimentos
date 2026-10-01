package com.feedformulator.model;

public class Usuario {
    private int id;
    private String nombre;
    private String usuario;
    private String contrasena;
    private String rol; // ADMIN, USUARIO
    private String estado; // ACTIVO, INACTIVO
    private String fechaRegistro;

    public Usuario() {}

    public Usuario(String nombre, String usuario, String contrasena, String rol) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.estado = "ACTIVO";
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isAdmin() {
        return "ADMIN".equals(rol);
    }

    @Override
    public String toString() {
        return usuario + " (" + nombre + ") - " + rol;
    }
}
