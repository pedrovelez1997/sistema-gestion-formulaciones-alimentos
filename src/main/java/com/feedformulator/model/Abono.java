package com.feedformulator.model;

public class Abono {
    private int id;
    private int ventaCreditoId;
    private int clienteId;
    private double monto;
    private String formaPago; // EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE
    private String fechaAbono;
    private String horaAbono;
    private String referencia;
    private Cliente cliente;

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVentaCreditoId() { return ventaCreditoId; }
    public void setVentaCreditoId(int ventaCreditoId) { this.ventaCreditoId = ventaCreditoId; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public String getFechaAbono() { return fechaAbono; }
    public void setFechaAbono(String fechaAbono) { this.fechaAbono = fechaAbono; }
    public String getHoraAbono() { return horaAbono; }
    public void setHoraAbono(String horaAbono) { this.horaAbono = horaAbono; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}