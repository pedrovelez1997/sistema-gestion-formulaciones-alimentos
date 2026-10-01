package com.feedformulator.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private static final String LOG_FILE = System.getProperty("user.home") + 
                                           File.separator + "FeedFormulatorPro" + 
                                           File.separator + "logs" + 
                                           File.separator + "feedformulator.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    static {
        // Crear directorio de logs si no existe
        File logDir = new File(LOG_FILE).getParentFile();
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs();
        }
    }
    
    public static void logInfo(String message) {
        log("INFO", message, null);
    }
    
    public static void logError(String message, Exception e) {
        log("ERROR", message, e);
    }
    
    public static void logWarning(String message) {
        log("WARNING", message, null);
    }
    
    public static void log(String level, String message, Exception e) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("[" + LocalDateTime.now().format(FMT) + "] [" + level + "] " + message);
            if (e != null) {
                pw.println("  Stack trace:");
                e.printStackTrace(pw);
            }
            pw.println("---");
            pw.flush();
        } catch (Exception ex) {
            System.err.println("Error al escribir log: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}