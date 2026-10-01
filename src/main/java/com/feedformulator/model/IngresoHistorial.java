package com.feedformulator.model;

public class IngresoHistorial {
    private int id;
    private int productoId;
    private String nombreProducto;
    private String unidad;
    private double cantidad;
    private double precioUnitario;
    private double precioTotal;
    private Integer numeroSacos;
    private Double pesoSaco;
    private Double librasSueltas;
    private String fecha;
    private String hora;
    private double cantidadUsada;
    private Integer compraId;
    private Integer pedidoId;
    private String origen;

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    
    public double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(double precioTotal) { this.precioTotal = precioTotal; }
    
    public Integer getNumeroSacos() { return numeroSacos; }
    public void setNumeroSacos(Integer numeroSacos) { this.numeroSacos = numeroSacos; }
    
    public Double getPesoSaco() { return pesoSaco; }
    public void setPesoSaco(Double pesoSaco) { this.pesoSaco = pesoSaco; }
    
    public Double getLibrasSueltas() { return librasSueltas; }
    public void setLibrasSueltas(Double librasSueltas) { this.librasSueltas = librasSueltas; }
    
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    
    public double getCantidadUsada() { return cantidadUsada; }
    public void setCantidadUsada(double cantidadUsada) { this.cantidadUsada = cantidadUsada; }
    
    public Integer getCompraId() { return compraId; }
    public void setCompraId(Integer compraId) { this.compraId = compraId; }
    
    public Integer getPedidoId() { return pedidoId; }
    public void setPedidoId(Integer pedidoId) { this.pedidoId = pedidoId; }
    
    /**
     * Calcula la cantidad disponible de este lote
     */
    public double getCantidadDisponible() {
        return cantidad - cantidadUsada;
    }
    
    /**
     * Verifica si el lote tiene stock disponible
     */
    public boolean tieneStockDisponible() {
        return getCantidadDisponible() > 0;
    }
    
    /**
     * Calcula el porcentaje de uso del lote
     */
    public double getPorcentajeUso() {
        return cantidad > 0 ? (cantidadUsada / cantidad) * 100 : 0;
    }
    
    @Override
    public String toString() {
        return String.format("Lote #%d: %.2f %s (%.2f usados, %.2f disponibles) - $%.4f/unidad",
            id, cantidad, unidad, cantidadUsada, getCantidadDisponible(), precioUnitario);
    }
}