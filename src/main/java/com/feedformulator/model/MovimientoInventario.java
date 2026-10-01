package com.feedformulator.model;

public class MovimientoInventario {
    private int id;
    private int materiaPrimaId;
    private String nombreProducto;
    private String unidad;
    private String tipoMovimiento; // "INGRESO", "USO", "SALIDA"
    private double cantidad;
    private double precioPorUnidad;
    private double subtotal;
    private Integer sacosAfectados;
    private Double librasSueltasAfectadas;
    private String fecha;
    private String hora;
    private String referencia;
    private Integer compraId;
    private Integer pedidoId;  // ===== NUEVO =====

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMateriaPrimaId() { return materiaPrimaId; }
    public void setMateriaPrimaId(int materiaPrimaId) { this.materiaPrimaId = materiaPrimaId; }
    
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    
    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    
    public double getPrecioPorUnidad() { return precioPorUnidad; }
    public void setPrecioPorUnidad(double precioPorUnidad) { this.precioPorUnidad = precioPorUnidad; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    
    public Integer getSacosAfectados() { return sacosAfectados; }
    public void setSacosAfectados(Integer sacosAfectados) { this.sacosAfectados = sacosAfectados; }
    
    public Double getLibrasSueltasAfectadas() { return librasSueltasAfectadas; }
    public void setLibrasSueltasAfectadas(Double librasSueltasAfectadas) { this.librasSueltasAfectadas = librasSueltasAfectadas; }
    
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    
    public Integer getCompraId() { return compraId; }
    public void setCompraId(Integer compraId) { this.compraId = compraId; }
    
    public Integer getPedidoId() { return pedidoId; }  // ===== NUEVO =====
    public void setPedidoId(Integer pedidoId) { this.pedidoId = pedidoId; }  // ===== NUEVO =====
}