package com.feedformulator.model;

public class HistorialPrecio {
    private int id;
    private int productoId;
    private double precio;
    private String fecha;
    private MateriaPrima producto;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    
    public MateriaPrima getProducto() { return producto; }
    public void setProducto(MateriaPrima producto) { this.producto = producto; }
}