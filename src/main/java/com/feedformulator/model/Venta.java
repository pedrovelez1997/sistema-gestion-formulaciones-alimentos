package com.feedformulator.model;

public class Venta {
    private int id;
    private int formulacionId;
    private String nombreProducto;
    private double librasVendidas;
    private double costoInversion;
    private double precioVenta;
    private double ganancia;
    private String fecha;
    private String hora;
    private String formaPago = "Efectivo";
    private int clienteId; // 0 = sin cliente asignado (Consumidor Final)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFormulacionId() { return formulacionId; }
    public void setFormulacionId(int formulacionId) { this.formulacionId = formulacionId; }
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public double getLibrasVendidas() { return librasVendidas; }
    public void setLibrasVendidas(double librasVendidas) { this.librasVendidas = librasVendidas; }
    public double getCostoInversion() { return costoInversion; }
    public void setCostoInversion(double costoInversion) { this.costoInversion = costoInversion; }
    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }
    public double getGanancia() { return ganancia; }
    public void setGanancia(double ganancia) { this.ganancia = ganancia; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
}
