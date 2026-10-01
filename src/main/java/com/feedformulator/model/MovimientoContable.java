package com.feedformulator.model;

public class MovimientoContable {
    private int id;
    // Tipo: "INGRESO", "GASTO", "REINVERSION", "CUENTA_POR_COBRAR"
    private String tipo;
    private String categoria;
    private String descripcion;
    private double monto;
    private String fecha;
    // Estado solo aplica de forma útil a CUENTA_POR_COBRAR: "PENDIENTE" / "COBRADO"
    // Para el resto de tipos queda simplemente en "ACTIVO".
    private String estado;
    private String notas;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
