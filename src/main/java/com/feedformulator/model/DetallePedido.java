package com.feedformulator.model;

public class DetallePedido {
    private int id;
    private int pedidoId;
    private int materiaPrimaId;
    private String nombreProducto;
    private String unidad;
    private double cantidad;
    private double precioUnitario;
    private double subtotal;
    private Integer numeroSacos;
    private Double pesoSaco;
    private Double librasSueltas;
    private MateriaPrima materiaPrima;

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }
    
    public int getMateriaPrimaId() { return materiaPrimaId; }
    public void setMateriaPrimaId(int materiaPrimaId) { this.materiaPrimaId = materiaPrimaId; }
    
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    
    public Integer getNumeroSacos() { return numeroSacos; }
    public void setNumeroSacos(Integer numeroSacos) { this.numeroSacos = numeroSacos; }
    
    public Double getPesoSaco() { return pesoSaco; }
    public void setPesoSaco(Double pesoSaco) { this.pesoSaco = pesoSaco; }
    
    public Double getLibrasSueltas() { return librasSueltas; }
    public void setLibrasSueltas(Double librasSueltas) { this.librasSueltas = librasSueltas; }
    
    public MateriaPrima getMateriaPrima() { return materiaPrima; }
    public void setMateriaPrima(MateriaPrima materiaPrima) { this.materiaPrima = materiaPrima; }
}