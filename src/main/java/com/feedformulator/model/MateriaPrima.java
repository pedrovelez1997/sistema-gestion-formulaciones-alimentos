package com.feedformulator.model;

public class MateriaPrima {
    private int id;
    private String codigo;
    private String nombre;
    private String unidad;
    private double cantidadTotal;
    private double cantidadDisponible;
    private double precioTotal;
    private double precioPorUnidad;
    private String fecha;
    private String hora;
    private Double pesoSaco;
    private Integer numeroSacos;
    private Double librasSueltas;
    private String origen; // "INVENTARIO" o "PEDIDO"
    private String imagenPath; // Ruta en disco de la imagen de fondo subida por el usuario (opcional)
    
    private double costoRealFIFO;
    private double costoDisponibleFIFO;

    public MateriaPrima() {
        this.origen = "INVENTARIO";
    }

    public MateriaPrima(String codigo, String nombre, String unidad, double cantidadTotal, double precioTotal) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.unidad = unidad;
        this.cantidadTotal = cantidadTotal;
        this.cantidadDisponible = cantidadTotal;
        this.precioTotal = precioTotal;
        this.precioPorUnidad = cantidadTotal > 0 ? precioTotal / cantidadTotal : 0;
        this.numeroSacos = 0;
        this.pesoSaco = 0.0;
        this.librasSueltas = 0.0;
        this.origen = "INVENTARIO";
        this.costoRealFIFO = 0;
        this.costoDisponibleFIFO = precioTotal;
    }

    // ===== GETTERS Y SETTERS =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    
    public double getCantidadTotal() { return cantidadTotal; }
    public void setCantidadTotal(double cantidadTotal) { 
        this.cantidadTotal = cantidadTotal; 
        recalcularPrecioPorUnidad();
    }
    
    public double getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(double cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }
    
    public double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(double precioTotal) { 
        this.precioTotal = precioTotal; 
        recalcularPrecioPorUnidad();
    }
    
    public double getPrecioPorUnidad() { return precioPorUnidad; }
    public void setPrecioPorUnidad(double precioPorUnidad) { 
        this.precioPorUnidad = precioPorUnidad; 
    }
    
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    
    public Double getPesoSaco() { return pesoSaco; }
    public void setPesoSaco(Double pesoSaco) { this.pesoSaco = pesoSaco; }
    
    public Integer getNumeroSacos() { return numeroSacos; }
    public void setNumeroSacos(Integer numeroSacos) { this.numeroSacos = numeroSacos; }
    
    public Double getLibrasSueltas() { return librasSueltas; }
    public void setLibrasSueltas(Double librasSueltas) { this.librasSueltas = librasSueltas; }
    
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    
    public String getImagenPath() { return imagenPath; }
    public void setImagenPath(String imagenPath) { this.imagenPath = imagenPath; }
    
    public double getCostoRealFIFO() { return costoRealFIFO; }
    public void setCostoRealFIFO(double costoRealFIFO) { this.costoRealFIFO = costoRealFIFO; }
    
    public double getCostoDisponibleFIFO() { return costoDisponibleFIFO; }
    public void setCostoDisponibleFIFO(double costoDisponibleFIFO) { this.costoDisponibleFIFO = costoDisponibleFIFO; }
    
    private void recalcularPrecioPorUnidad() {
        if (cantidadTotal > 0) {
            this.precioPorUnidad = precioTotal / cantidadTotal;
        }
    }

    @Override
    public String toString() { 
        return codigo + " - " + nombre + " (" + String.format("%.2f", cantidadDisponible) + " " + unidad + " disp.)"; 
    }
}