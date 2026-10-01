package com.feedformulator.model;

public class VentaCredito {
    private int id;
    private int ventaId;
    private int clienteId;
    private int formulacionId;
    private String nombreProducto;
    private double totalLibras;
    private double precioTotal;
    private double abonado;
    private double saldoPendiente;
    private String estado; // PENDIENTE, ABONADO, CANCELADO
    private String fechaVenta;
    private String fechaLimitePago;
    private String notas;
    private Cliente cliente;
    
    // ===== CAMPOS PARA GANANCIA DIFERIDA =====
    private double gananciaDiferida;
    private double gananciaRealizada;

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }
    
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    
    public int getFormulacionId() { return formulacionId; }
    public void setFormulacionId(int formulacionId) { this.formulacionId = formulacionId; }
    
    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    
    public double getTotalLibras() { return totalLibras; }
    public void setTotalLibras(double totalLibras) { this.totalLibras = totalLibras; }
    
    public double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(double precioTotal) { this.precioTotal = precioTotal; }
    
    public double getAbonado() { return abonado; }
    public void setAbonado(double abonado) { this.abonado = abonado; }
    
    public double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(double saldoPendiente) { this.saldoPendiente = saldoPendiente; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(String fechaVenta) { this.fechaVenta = fechaVenta; }
    
    public String getFechaLimitePago() { return fechaLimitePago; }
    public void setFechaLimitePago(String fechaLimitePago) { this.fechaLimitePago = fechaLimitePago; }
    
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    
    // ===== GETTERS Y SETTERS PARA GANANCIA =====
    public double getGananciaDiferida() { return gananciaDiferida; }
    public void setGananciaDiferida(double gananciaDiferida) { this.gananciaDiferida = gananciaDiferida; }
    
    public double getGananciaRealizada() { return gananciaRealizada; }
    public void setGananciaRealizada(double gananciaRealizada) { this.gananciaRealizada = gananciaRealizada; }
    
    /**
     * Calcula la ganancia total del crédito (diferida + realizada)
     */
    public double getGananciaTotal() {
        return gananciaDiferida + gananciaRealizada;
    }
    
    /**
     * Verifica si el crédito está completamente pagado
     */
    public boolean isPagado() {
        return "CANCELADO".equals(estado);
    }
    
    /**
     * Calcula el porcentaje de ganancia realizada vs total
     */
    public double getPorcentajeGananciaRealizada() {
        double total = getGananciaTotal();
        return total > 0 ? (gananciaRealizada / total) * 100 : 0;
    }
    
    @Override
    public String toString() {
        return nombreProducto + " - $" + String.format("%.2f", saldoPendiente) + " pendiente";
    }
}