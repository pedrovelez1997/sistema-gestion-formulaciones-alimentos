package com.feedformulator.util;

import java.util.HashMap;
import java.util.Map;

public class DensidadManager {
    
    // Tabla de densidades (litros → libras)
    // 1 litro = X libras
    private static final Map<String, Double> DENSIDADES = new HashMap<>();
    
    static {
        // Aceites
        DENSIDADES.put("ACEITE", 1.87);      // Aceite vegetal ~0.92 kg/L = 1.87 lb/L
        DENSIDADES.put("ACEITE DE SOYA", 1.87);
        DENSIDADES.put("ACEITE DE PALMA", 1.84);
        DENSIDADES.put("ACEITE DE PESCADO", 1.93);
        DENSIDADES.put("ACEITE DE GIRASOL", 1.87);
        
        // Melazas y jarabes
        DENSIDADES.put("MELAZA", 2.87);      // Melaza de caña ~1.4 kg/L = 2.87 lb/L
        DENSIDADES.put("MELAZA DE CAÑA", 2.87);
        DENSIDADES.put("MELAZA DE REMOLACHA", 2.87);
        DENSIDADES.put("JARABE", 2.65);
        
        // Líquidos varios
        DENSIDADES.put("AGUA", 2.204);       // 1 L = 2.204 lb
        DENSIDADES.put("LECHE", 2.27);
        DENSIDADES.put("SUERO DE LECHE", 2.22);
        DENSIDADES.put("GRASA", 1.89);
        DENSIDADES.put("GRASA ANIMAL", 1.89);
        DENSIDADES.put("SEBO", 1.89);
        
        // Otros líquidos comunes en alimentación animal
        DENSIDADES.put("VINAGRE", 2.20);
        DENSIDADES.put("PROPILENGLICOL", 2.28);
        DENSIDADES.put("GLICERINA", 2.73);
    }
    
    public static double getDensidad(String nombre) {
        // Buscar coincidencia exacta
        if (DENSIDADES.containsKey(nombre.toUpperCase())) {
            return DENSIDADES.get(nombre.toUpperCase());
        }
        
        // Buscar por coincidencia parcial (ej: "ACEITE DE SOYA" contiene "ACEITE")
        for (Map.Entry<String, Double> entry : DENSIDADES.entrySet()) {
            if (nombre.toUpperCase().contains(entry.getKey()) || 
                entry.getKey().contains(nombre.toUpperCase())) {
                return entry.getValue();
            }
        }
        
        // Valor por defecto para líquidos (agua)
        return 2.204; // 1 L = 2.204 lb
    }
    
    public static double convertirLitrosALibras(double litros, String nombre) {
        double densidad = getDensidad(nombre);
        return litros * densidad;
    }
    
    public static double convertirLibrasALitros(double libras, String nombre) {
        double densidad = getDensidad(nombre);
        return densidad > 0 ? libras / densidad : 0;
    }
    
    public static String getInfoDensidad(String nombre) {
        double densidad = getDensidad(nombre);
        return String.format("1 %s = %.2f lb", nombre, densidad);
    }
}