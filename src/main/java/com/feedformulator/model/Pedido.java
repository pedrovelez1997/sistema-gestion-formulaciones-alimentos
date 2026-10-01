package com.feedformulator.model;

import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private int id;
    private int proveedorId;
    private String fechaPedido;
    private String estadoPago; // PENDIENTE, ABONADO, CANCELADO
    private String estadoRecepcion; // PENDIENTE, PARCIAL, RECIBIDA
    private double abonado;
    private double saldoPendiente;
    private String notas;
    private Proveedor proveedor;
    private List<DetallePedido> detalles = new ArrayList<>();
    private List<PagoPedido> pagos = new ArrayList<>();

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }
    
    public String getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(String fechaPedido) { this.fechaPedido = fechaPedido; }
    
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    
    public String getEstadoRecepcion() { return estadoRecepcion; }
    public void setEstadoRecepcion(String estadoRecepcion) { this.estadoRecepcion = estadoRecepcion; }
    
    public double getAbonado() { return abonado; }
    public void setAbonado(double abonado) { this.abonado = abonado; }
    
    public double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(double saldoPendiente) { this.saldoPendiente = saldoPendiente; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    
    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
    public void addDetalle(DetallePedido detalle) { this.detalles.add(detalle); }
    
    public List<PagoPedido> getPagos() { return pagos; }
    public void setPagos(List<PagoPedido> pagos) { this.pagos = pagos; }
    public void addPago(PagoPedido pago) { this.pagos.add(pago); }
    
    public double getTotalPedido() {
        return detalles.stream().mapToDouble(DetallePedido::getSubtotal).sum();
    }
    
    public double getCantidadTotal() {
        return detalles.stream().mapToDouble(DetallePedido::getCantidad).sum();
    }
    
    public boolean isPagado() {
        return "CANCELADO".equals(estadoPago);
    }
    
    public boolean isRecibido() {
        return "RECIBIDA".equals(estadoRecepcion);
    }
    
    @Override
    public String toString() {
        return "Pedido #" + id + " - " + (proveedor != null ? proveedor.getNombre() : "Sin proveedor");
    }
}