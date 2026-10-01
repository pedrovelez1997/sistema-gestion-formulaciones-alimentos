package com.feedformulator.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    // Formato para guardar en la base de datos (formato ISO)
    private static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Formato para mostrar en la pantalla (formato latino)
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    // Convierte una fecha de formato DB (yyyy-MM-dd) a formato de pantalla (dd/MM/yyyy)
    public static String formatForDisplay(String dbDate) {
        if (dbDate == null || dbDate.isEmpty()) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(dbDate, DB_FORMAT);
            return date.format(DISPLAY_FORMAT);
        } catch (Exception e) {
            // Si falla, devuelve la fecha original
            return dbDate;
        }
    }
    
    // Convierte una fecha de formato de pantalla (dd/MM/yyyy) a formato DB (yyyy-MM-dd)
    public static String formatForDB(String displayDate) {
        if (displayDate == null || displayDate.isEmpty()) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(displayDate, DISPLAY_FORMAT);
            return date.format(DB_FORMAT);
        } catch (Exception e) {
            // Si falla, devuelve la fecha original
            return displayDate;
        }
    }
}