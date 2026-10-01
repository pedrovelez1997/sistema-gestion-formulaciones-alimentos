package com.feedformulator.ui;

import com.feedformulator.dao.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ConfiguracionRedPanel extends JPanel {
    
    private JTextField txtRutaDB;
    private JLabel lblEstado;
    private JButton btnProbarConexion;
    private JButton btnGuardar;
    private JLabel lblRutaActual;
    
    public ConfiguracionRedPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        add(crearTitulo(), BorderLayout.NORTH);
        add(crearFormulario(), BorderLayout.CENTER);
        add(crearPanelBotones(), BorderLayout.SOUTH);
        
        cargarConfiguracionActual();
    }
    
    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("🌐", "Configuración de Red",
            "Configure la ubicación de la base de datos compartida");
    }
    
    private JPanel crearFormulario() {
        JPanel panel = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();
        
        // ===== INFORMACIÓN =====
        JPanel banner = DialogStyler.crearBannerInfo("info",
            "<b>📌 INSTRUCCIONES PARA TRABAJO EN RED</b><br><br>" +
            "1. En la PC principal (Administrador), comparta una carpeta en la red.<br>" +
            "2. En la otra PC, coloque la ruta de red de esa carpeta.<br>" +
            "3. Ejemplo: <b>\\\\PC-ADMIN\\FeedData</b><br><br>" +
            "⚠️ <b>Recomendaciones:</b><br>" +
            "• La PC principal debe estar encendida para que la otra funcione.<br>" +
            "• Ambos usuarios deben tener permisos de lectura/escritura en la carpeta.<br>" +
            "• No trabajen los dos al mismo tiempo en la misma venta para evitar bloqueos."
        );
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(banner, gbc);
        gbc.gridwidth = 1;
        
        // ===== RUTA ACTUAL =====
        int fila = 1;
        gbc.gridx = 0; gbc.gridy = fila;
        panel.add(DialogStyler.crearLabel("Ruta actual:"), gbc);
        gbc.gridx = 1;
        lblRutaActual = new JLabel("Cargando...");
        lblRutaActual.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblRutaActual.setForeground(new Color(34, 139, 34));
        panel.add(lblRutaActual, gbc);
        
        // ===== NUEVA RUTA =====
        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        panel.add(DialogStyler.crearLabel("Nueva ruta:"), gbc);
        gbc.gridx = 1;
        txtRutaDB = DialogStyler.crearCampo(40);
        txtRutaDB.setToolTipText("Ejemplo: C:\\FeedData  o  \\\\PC-ADMIN\\FeedData");
        panel.add(txtRutaDB, gbc);
        
        // ===== ESTADO =====
        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        gbc.gridwidth = 2;
        lblEstado = new JLabel(" ");
        lblEstado.setFont(DialogStyler.FUENTE_INFO);
        panel.add(lblEstado, gbc);
        gbc.gridwidth = 1;
        
        // ===== BOTÓN PARA ABRIR EXPLORADOR =====
        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        gbc.gridwidth = 2;
        JButton btnExaminar = DialogStyler.crearBotonSecundario("📂 Examinar carpeta");
        btnExaminar.addActionListener(e -> examinarCarpeta());
        JPanel panelExaminar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelExaminar.setOpaque(false);
        panelExaminar.add(btnExaminar);
        panel.add(panelExaminar, gbc);
        gbc.gridwidth = 1;
        
        return panel;
    }
    
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        
        btnProbarConexion = DialogStyler.crearBotonSecundario("🔍 Probar Conexión");
        btnProbarConexion.addActionListener(e -> probarConexion());
        
        btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Configuración");
        btnGuardar.addActionListener(e -> guardarConfiguracion());
        
        panel.add(btnProbarConexion);
        panel.add(btnGuardar);
        
        return panel;
    }
    
    private void examinarCarpeta() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Seleccionar carpeta para la base de datos");
        
        // Intentar abrir en la ruta actual
        String rutaActual = txtRutaDB.getText().trim();
        if (!rutaActual.isEmpty()) {
            File dir = new File(rutaActual);
            if (dir.exists() && dir.isDirectory()) {
                fc.setCurrentDirectory(dir);
            }
        }
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String ruta = fc.getSelectedFile().getAbsolutePath();
            txtRutaDB.setText(ruta);
            actualizarEstado("📂 Carpeta seleccionada: " + ruta, new Color(34, 139, 34));
        }
    }
    
    private void cargarConfiguracionActual() {
        String rutaActual = DatabaseManager.getRutaActual();
        lblRutaActual.setText(rutaActual);
        txtRutaDB.setText(rutaActual);
        actualizarEstado("✅ Configuración actual cargada", new Color(34, 139, 34));
    }
    
    private void probarConexion() {
        String ruta = txtRutaDB.getText().trim();
        if (ruta.isEmpty()) {
            actualizarEstado("❌ Ingrese una ruta válida", new Color(180, 40, 40));
            return;
        }
        
        btnProbarConexion.setEnabled(false);
        btnProbarConexion.setText("⏳ Probando...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private String error;
            
            @Override
            protected Void doInBackground() {
                try {
                    File folder = new File(ruta);
                    if (!folder.exists()) {
                        error = "La carpeta no existe: " + ruta;
                        return null;
                    }
                    
                    // Verificar si podemos escribir en la carpeta
                    File testFile = new File(folder, "test_write.tmp");
                    if (testFile.createNewFile()) {
                        testFile.delete();
                    } else {
                        error = "No se puede escribir en la carpeta. Verifique permisos.";
                        return null;
                    }
                    
                    // Probar conexión a la base de datos
                    String url = "jdbc:sqlite:" + ruta + File.separator + "feedformulator.db";
                    java.sql.Connection conn = java.sql.DriverManager.getConnection(url);
                    conn.close();
                    
                } catch (Exception e) {
                    error = "Error: " + e.getMessage();
                }
                return null;
            }
            
            @Override
            protected void done() {
                btnProbarConexion.setEnabled(true);
                btnProbarConexion.setText("🔍 Probar Conexión");
                
                if (error != null) {
                    actualizarEstado("❌ " + error, new Color(180, 40, 40));
                    DialogStyler.mostrarError(ConfiguracionRedPanel.this, error);
                } else {
                    actualizarEstado("✅ Conexión exitosa! La ruta es válida.", new Color(34, 139, 34));
                    DialogStyler.mostrarExito(ConfiguracionRedPanel.this,
                        "✅ Conexión exitosa!<br><br>" +
                        "La ruta es válida y se puede acceder a la base de datos.<br>" +
                        "Si la base de datos no existe, se creará automáticamente.",
                        "Prueba exitosa"
                    );
                }
            }
        };
        worker.execute();
    }
    
    private void guardarConfiguracion() {
        String ruta = txtRutaDB.getText().trim();
        if (ruta.isEmpty()) {
            actualizarEstado("❌ Ingrese una ruta válida", new Color(180, 40, 40));
            return;
        }
        
        btnGuardar.setEnabled(false);
        btnGuardar.setText("⏳ Guardando...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private String error;
            
            @Override
            protected Void doInBackground() {
                try {
                    DatabaseManager.cambiarRutaBaseDatos(ruta);
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }
            
            @Override
            protected void done() {
                btnGuardar.setEnabled(true);
                btnGuardar.setText("💾 Guardar Configuración");
                
                if (error != null) {
                    actualizarEstado("❌ Error: " + error, new Color(180, 40, 40));
                    DialogStyler.mostrarError(ConfiguracionRedPanel.this, error);
                    return;
                }
                
                actualizarEstado("✅ Configuración guardada correctamente.", new Color(34, 139, 34));
                
                // Actualizar la ruta mostrada
                String rutaActual = DatabaseManager.getRutaActual();
                lblRutaActual.setText(rutaActual);
                
                int respuesta = JOptionPane.showConfirmDialog(
                    ConfiguracionRedPanel.this,
                    "✅ Configuración guardada correctamente.\n\n" +
                    "Ruta: " + rutaActual + "\n\n" +
                    "Para que los cambios surtan efecto, debe reiniciar la aplicación.\n\n" +
                    "¿Desea reiniciar ahora?",
                    "Reiniciar aplicación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (respuesta == JOptionPane.YES_OPTION) {
                    reiniciarAplicacion();
                }
            }
        };
        worker.execute();
    }
    
    private void actualizarEstado(String mensaje, Color color) {
        lblEstado.setText(mensaje);
        lblEstado.setForeground(color);
    }
    
    private void reiniciarAplicacion() {
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            File currentJar = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            
            if (!currentJar.getName().endsWith(".jar")) {
                DialogStyler.mostrarInfo(this, 
                    "Reinicie la aplicación manualmente desde NetBeans.", 
                    "Reinicio manual");
                return;
            }
            
            ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath());
            builder.start();
            System.exit(0);
            
        } catch (Exception e) {
            DialogStyler.mostrarInfo(this,
                "No se pudo reiniciar automáticamente.\n" +
                "Por favor, cierre y abra la aplicación manualmente.",
                "Reinicio manual"
            );
        }
    }
}