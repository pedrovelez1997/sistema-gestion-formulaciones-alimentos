package com.feedformulator.model;

public class Proveedor {
    private int id;
    private String codigo;
    private String nombre;
    private String telefono;
    private String email;
    private String direccion;
    private String rucCedula;
    private String contacto;
    private String tipoProveedor; // MATERIA_PRIMA, SERVICIOS, EQUIPOS
    private String fechaRegistro;
    private String estado; // ACTIVO, INACTIVO

    public Proveedor() {}

    public Proveedor(String nombre, String telefono, String tipoProveedor) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.tipoProveedor = tipoProveedor;
        this.estado = "ACTIVO";
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getRucCedula() { return rucCedula; }
    public void setRucCedula(String rucCedula) { this.rucCedula = rucCedula; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
    public String getTipoProveedor() { return tipoProveedor; }
    public void setTipoProveedor(String tipoProveedor) { this.tipoProveedor = tipoProveedor; }
    public String getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(String fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + tipoProveedor + ")";
    }
}