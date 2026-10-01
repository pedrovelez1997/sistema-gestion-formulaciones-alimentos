package com.feedformulator.util;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    
    private static final String APP_FOLDER = System.getenv("APPDATA") + File.separator + "FeedFormulatorPro";
    private static final String DB_NAME = APP_FOLDER + File.separator + "feedformulator.db";
    private static final String BACKUP_FOLDER = APP_FOLDER + File.separator + "backups";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static String crearBackup() throws IOException {
        // Crear carpeta de respaldos si no existe
        File backupDir = new File(BACKUP_FOLDER);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Nombre del archivo de respaldo con fecha y hora
        String timestamp = LocalDateTime.now().format(FORMATO_FECHA);
        String nombreBackup = "backup_" + timestamp + ".zip";
        String rutaBackup = BACKUP_FOLDER + File.separator + nombreBackup;
        
        // Verificar que la base de datos existe
        File dbFile = new File(DB_NAME);
        if (!dbFile.exists()) {
            throw new IOException("La base de datos no existe: " + DB_NAME);
        }
        
        // Crear archivo ZIP
        try (FileOutputStream fos = new FileOutputStream(rutaBackup);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            agregarArchivoAZip(dbFile, zos);
            
            // También respaldar el archivo .db-journal si existe
            File journalFile = new File(DB_NAME + "-journal");
            if (journalFile.exists()) {
                agregarArchivoAZip(journalFile, zos);
            }
            
            // Agregar archivo de información del respaldo
            String info = "Respaldo creado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n";
            info += "Base de datos: " + DB_NAME + "\n";
            info += "Tamaño: " + dbFile.length() + " bytes\n";
            
            ZipEntry infoEntry = new ZipEntry("backup_info.txt");
            zos.putNextEntry(infoEntry);
            zos.write(info.getBytes());
            zos.closeEntry();
        }
        
        return rutaBackup;
    }

    private static void agregarArchivoAZip(File archivo, ZipOutputStream zos) throws IOException {
        ZipEntry entry = new ZipEntry(archivo.getName());
        zos.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(archivo)) {
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesLeidos);
            }
        }
        
        zos.closeEntry();
    }

    public static boolean restaurarBackup(String rutaBackup) throws IOException {
        File backupFile = new File(rutaBackup);
        if (!backupFile.exists()) {
            throw new IOException("El archivo de respaldo no existe: " + rutaBackup);
        }
        
        if (!rutaBackup.toLowerCase().endsWith(".zip")) {
            throw new IOException("El archivo debe ser un ZIP: " + rutaBackup);
        }
        
        // Crear respaldo de seguridad antes de restaurar
        try {
            crearBackup();
        } catch (Exception e) {
            System.out.println("No se pudo crear respaldo de seguridad: " + e.getMessage());
        }
        
        // Extraer el ZIP
        File tempDir = new File(APP_FOLDER + File.separator + "temp_restore");
        if (tempDir.exists()) {
            eliminarDirectorio(tempDir);
        }
        tempDir.mkdirs();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(rutaBackup))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String nombre = entry.getName();
                File archivoDestino = new File(tempDir, nombre);
                
                if (entry.isDirectory()) {
                    archivoDestino.mkdirs();
                    continue;
                }
                
                try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                    byte[] buffer = new byte[4096];
                    int bytesLeidos;
                    while ((bytesLeidos = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesLeidos);
                    }
                }
                
                zis.closeEntry();
            }
        }
        
        // Restaurar la base de datos
        File dbRestaurada = new File(tempDir, "feedformulator.db");
        if (!dbRestaurada.exists()) {
            eliminarDirectorio(tempDir);
            throw new IOException("El respaldo no contiene la base de datos");
        }
        
        File dbActual = new File(DB_NAME);
        if (dbActual.exists()) {
            File dbBackup = new File(DB_NAME + ".old");
            if (dbBackup.exists()) {
                dbBackup.delete();
            }
            Files.copy(dbActual.toPath(), dbBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        Files.copy(dbRestaurada.toPath(), dbActual.toPath(), StandardCopyOption.REPLACE_EXISTING);
        eliminarDirectorio(tempDir);
        
        return true;
    }

    public static List<BackupInfo> listarBackups() {
        List<BackupInfo> backups = new ArrayList<>();
        File backupDir = new File(BACKUP_FOLDER);
        
        if (!backupDir.exists()) {
            return backups;
        }
        
        File[] archivos = backupDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (archivos == null) {
            return backups;
        }
        
        for (File archivo : archivos) {
            String nombre = archivo.getName();
            long tamaño = archivo.length();
            long fechaModificacion = archivo.lastModified();
            
            String fechaStr = "";
            if (nombre.startsWith("backup_") && nombre.endsWith(".zip")) {
                fechaStr = nombre.substring(7, nombre.length() - 4);
                fechaStr = fechaStr.replace("_", " ");
                fechaStr = fechaStr.replace("-", ":");
            }
            
            backups.add(new BackupInfo(nombre, fechaStr, tamaño, fechaModificacion, archivo.getAbsolutePath()));
        }
        
        backups.sort((a, b) -> Long.compare(b.fechaModificacion, a.fechaModificacion));
        return backups;
    }

    public static boolean eliminarBackup(String rutaBackup) {
        File archivo = new File(rutaBackup);
        if (archivo.exists()) {
            return archivo.delete();
        }
        return false;
    }

    public static int eliminarBackupsAntiguos(int mantener) {
        List<BackupInfo> backups = listarBackups();
        int eliminados = 0;
        
        for (int i = mantener; i < backups.size(); i++) {
            BackupInfo info = backups.get(i);
            if (eliminarBackup(info.rutaCompleta)) {
                eliminados++;
            }
        }
        return eliminados;
    }

    private static void eliminarDirectorio(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        eliminarDirectorio(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    public static class BackupInfo {
        public String nombre;
        public String fecha;
        public long tamaño;
        public long fechaModificacion;
        public String rutaCompleta;
        
        public BackupInfo(String nombre, String fecha, long tamaño, long fechaModificacion, String rutaCompleta) {
            this.nombre = nombre;
            this.fecha = fecha;
            this.tamaño = tamaño;
            this.fechaModificacion = fechaModificacion;
            this.rutaCompleta = rutaCompleta;
        }
        
        public String getTamañoFormateado() {
            if (tamaño < 1024) {
                return tamaño + " B";
            } else if (tamaño < 1024 * 1024) {
                return String.format("%.2f KB", tamaño / 1024.0);
            } else {
                return String.format("%.2f MB", tamaño / (1024.0 * 1024.0));
            }
        }
        
        @Override
        public String toString() {
            return fecha + " - " + getTamañoFormateado();
        }
    }
}