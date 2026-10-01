package com.feedformulator.model;

import java.util.ArrayList;
import java.util.List;

public class Formulacion {
    private int id;
    private String nombre;
    private double totalLibras;
    private double costoTotal;
    private String fecha;
    private String hora;
    private String estado; // "Disponible" o "Vendido"
    private List<DetalleFormulacion> detalles = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getTotalLibras() { return totalLibras; }
    public void setTotalLibras(double totalLibras) { this.totalLibras = totalLibras; }
    public double getCostoTotal() { return costoTotal; }
    public void setCostoTotal(double costoTotal) { this.costoTotal = costoTotal; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<DetalleFormulacion> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFormulacion> detalles) { this.detalles = detalles; }
    public void addDetalle(DetalleFormulacion d) { this.detalles.add(d); }
    
    @Override
    public String toString() {
        return nombre + " (" + String.format("%.2f", totalLibras) + " lb) - " + estado;
    }
}