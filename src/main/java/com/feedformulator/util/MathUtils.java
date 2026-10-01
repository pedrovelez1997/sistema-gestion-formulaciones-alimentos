package com.feedformulator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {
    
    private static final int DEFAULT_DECIMALES = 2;
    
    /**
     * Redondea un valor a 2 decimales usando HALF_UP.
     */
    public static double redondear(double valor) {
        return redondear(valor, DEFAULT_DECIMALES);
    }
    
    /**
     * Redondea un valor a la cantidad de decimales especificada usando HALF_UP.
     */
    public static double redondear(double valor, int decimales) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return 0;
        }
        return BigDecimal.valueOf(valor)
            .setScale(decimales, RoundingMode.HALF_UP)
            .doubleValue();
    }
    
    /**
     * Suma dos valores y redondea el resultado.
     */
    public static double sumarRedondeado(double a, double b) {
        return redondear(a + b);
    }
    
    /**
     * Multiplica dos valores y redondea el resultado.
     */
    public static double multiplicarRedondeado(double a, double b) {
        return redondear(a * b);
    }
    
    /**
     * Divide dos valores y redondea el resultado.
     */
    public static double dividirRedondeado(double a, double b) {
        if (b == 0) return 0;
        return redondear(a / b);
    }
}