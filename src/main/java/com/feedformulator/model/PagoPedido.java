package com.feedformulator.model;

public class PagoPedido {
    private int id;
    private int pedidoId;
    private int proveedorId;
    private double monto;
    private String formaPago; // EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE
    private String fechaPago;
    private String horaPago;
    private String referencia;
    private Proveedor proveedor;

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getPedidoId() { return pedidoId; }
    public void setPedidoId(int pedidoId) { this.pedidoId = pedidoId; }
    
    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }
    
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    
    public String getFechaPago() { return fechaPago; }
    public void setFechaPago(String fechaPago) { this.fechaPago = fechaPago; }
    
    public String getHoraPago() { return horaPago; }
    public void setHoraPago(String horaPago) { this.horaPago = horaPago; }
    
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
}