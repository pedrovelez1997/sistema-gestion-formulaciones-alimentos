package com.feedformulator.model;

public class DetalleFormulacion {
    private int id;
    private int formulacionId;
    private int materiaPrimaId;
    private String nombreIngrediente;
    private String unidad;
    private double cantidadUsada;
    private double precioUnitario;
    private double subtotal;
    
    // ===== CAMPOS PARA LÍQUIDOS (conversión litros → libras) =====
    private double cantidadOriginal;  // Cantidad en litros (original)
    private String unidadOriginal;     // Unidad original (litro)

    public DetalleFormulacion() {}

    public DetalleFormulacion(int materiaPrimaId, String nombreIngrediente, String unidad,
                               double cantidadUsada, double precioUnitario) {
        this.materiaPrimaId = materiaPrimaId;
        this.nombreIngrediente = nombreIngrediente;
        this.unidad = unidad;
        this.cantidadUsada = cantidadUsada;
        this.precioUnitario = precioUnitario;
        this.cantidadOriginal = cantidadUsada;
        this.unidadOriginal = unidad;
        this.subtotal = cantidadUsada * precioUnitario;
    }

    // ===== CONSTRUCTOR COMPLETO CON CANTIDAD ORIGINAL (para líquidos) =====
    // ===== CONSTRUCTOR COMPLETO CON CANTIDAD ORIGINAL (para líquidos) =====
public DetalleFormulacion(int materiaPrimaId, String nombreIngrediente, String unidad,
                           double cantidadUsada, double precioUnitario, 
                           double cantidadOriginal, String unidadOriginal) {
    this.materiaPrimaId = materiaPrimaId;
    this.nombreIngrediente = nombreIngrediente;
    this.unidad = "lb";  // ← SIEMPRE "lb" para el peso del saco
    this.cantidadUsada = cantidadUsada;  // ← PESO en libras (ej: 5.61)
    this.precioUnitario = precioUnitario;
    this.cantidadOriginal = cantidadOriginal;  // ← LITROS originales (ej: 3.00)
    this.unidadOriginal = unidadOriginal;  // ← "litro"
    // El subtotal se calcula con la cantidad original (litros × precio por libra)
    this.subtotal = cantidadOriginal * precioUnitario;
}

    // ===== GETTERS Y SETTERS =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFormulacionId() { return formulacionId; }
    public void setFormulacionId(int formulacionId) { this.formulacionId = formulacionId; }
    public int getMateriaPrimaId() { return materiaPrimaId; }
    public void setMateriaPrimaId(int materiaPrimaId) { this.materiaPrimaId = materiaPrimaId; }
    public String getNombreIngrediente() { return nombreIngrediente; }
    public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public double getCantidadUsada() { return cantidadUsada; }
    public void setCantidadUsada(double cantidadUsada) { this.cantidadUsada = cantidadUsada; }
    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    
    public double getCantidadOriginal() { return cantidadOriginal; }
    public void setCantidadOriginal(double cantidadOriginal) { this.cantidadOriginal = cantidadOriginal; }
    public String getUnidadOriginal() { return unidadOriginal; }
    public void setUnidadOriginal(String unidadOriginal) { this.unidadOriginal = unidadOriginal; }
}