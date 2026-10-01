package com.feedformulator.ui;

import com.feedformulator.util.BackupManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

public class BackupPanel extends JPanel {
    
    private DefaultTableModel tableModel;
    private JTable tablaBackups;
    private JLabel lblInfo;
    private JButton btnCrearBackup;
    private JButton btnRestaurar;
    private JButton btnEliminar;
    private JButton btnEliminarAntiguos;
    private JButton btnRefresh;
    
    private static final Color COLOR_PRIMARIO = new Color(41, 128, 185);
    private static final Color COLOR_SECUNDARIO = new Color(46, 204, 113);
    private static final Color COLOR_ALERTA = new Color(231, 76, 60);
    private static final Color COLOR_NARANJA = new Color(230, 126, 34);
    private static final Color FONDO = new Color(240, 245, 248);
    
    public BackupPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelTabla(), BorderLayout.CENTER);
        add(crearPanelInferior(), BorderLayout.SOUTH);
        
        cargarBackups();
    }
    
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        tituloPanel.setOpaque(false);
        
        try {
            ImageIcon iconBackup = new ImageIcon(getClass().getResource("/icons/backup.png"));
            Image imgBackup = iconBackup.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
            JLabel icono = new JLabel(new ImageIcon(imgBackup));
            tituloPanel.add(icono);
        } catch (Exception e) {
            JLabel icono = new JLabel("💾");
            icono.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            tituloPanel.add(icono);
        }
        
        JLabel titulo = new JLabel("  Gestión de Respaldos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(new Color(30, 60, 90));
        tituloPanel.add(titulo);
        
        panel.add(tituloPanel, BorderLayout.WEST);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        
        // ===== ICONO PARA CREAR RESPALDO =====
        try {
            ImageIcon iconAdd = new ImageIcon(getClass().getResource("/icons/add.png"));
            Image imgAdd = iconAdd.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnCrearBackup = new JButton("Crear Respaldo", new ImageIcon(imgAdd));
        } catch (Exception e) {
            btnCrearBackup = new JButton("📦 Crear Respaldo");
        }
        btnCrearBackup.setBackground(COLOR_PRIMARIO);
        btnCrearBackup.setForeground(Color.WHITE);
        btnCrearBackup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCrearBackup.setFocusPainted(false);
        btnCrearBackup.setBorderPainted(false);
        btnCrearBackup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCrearBackup.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnCrearBackup.addActionListener(evt -> crearBackup());
        btnPanel.add(btnCrearBackup);
        
        // ===== ICONO PARA ACTUALIZAR =====
        try {
            ImageIcon iconRefresh = new ImageIcon(getClass().getResource("/icons/refresh.png"));
            Image imgRefresh = iconRefresh.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnRefresh = new JButton("Actualizar", new ImageIcon(imgRefresh));
        } catch (Exception e) {
            btnRefresh = new JButton("🔄 Actualizar");
        }
        btnRefresh.setBackground(new Color(100, 100, 100));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnRefresh.addActionListener(evt -> cargarBackups());
        btnPanel.add(btnRefresh);
        
        panel.add(btnPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JScrollPane crearPanelTabla() {
        String[] columnas = {"#", "Fecha", "Tamaño", "Acciones"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        
        tablaBackups = new JTable(tableModel);
        tablaBackups.setRowHeight(32);
        tablaBackups.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaBackups.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaBackups.getTableHeader().setBackground(new Color(41, 128, 185));
        tablaBackups.getTableHeader().setForeground(Color.WHITE);
        tablaBackups.setSelectionBackground(new Color(200, 220, 240));
        tablaBackups.setShowGrid(true);
        tablaBackups.setGridColor(new Color(220, 225, 230));
        
        tablaBackups.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(new Color(248, 250, 252));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });
        
        tablaBackups.getColumnModel().getColumn(0).setMaxWidth(40);
        tablaBackups.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablaBackups.getColumnModel().getColumn(2).setPreferredWidth(100);
        tablaBackups.getColumnModel().getColumn(3).setPreferredWidth(200);
        
        JScrollPane scroll = new JScrollPane(tablaBackups);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));
        return scroll;
    }
    
    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);
        
        // ===== ICONO PARA RESTAURAR =====
        try {
            ImageIcon iconRestore = new ImageIcon(getClass().getResource("/icons/restore.png"));
            Image imgRestore = iconRestore.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnRestaurar = new JButton("Restaurar Seleccionado", new ImageIcon(imgRestore));
        } catch (Exception e) {
            btnRestaurar = new JButton("🔄 Restaurar Seleccionado");
        }
        btnRestaurar.setBackground(COLOR_NARANJA);
        btnRestaurar.setForeground(Color.WHITE);
        btnRestaurar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRestaurar.setFocusPainted(false);
        btnRestaurar.setBorderPainted(false);
        btnRestaurar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRestaurar.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnRestaurar.addActionListener(evt -> restaurarBackup());
        btnPanel.add(btnRestaurar);
        
        // ===== ICONO PARA ELIMINAR =====
        try {
            ImageIcon iconDelete = new ImageIcon(getClass().getResource("/icons/delete.png"));
            Image imgDelete = iconDelete.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnEliminar = new JButton("Eliminar Seleccionado", new ImageIcon(imgDelete));
        } catch (Exception e) {
            btnEliminar = new JButton("🗑 Eliminar Seleccionado");
        }
        btnEliminar.setBackground(COLOR_ALERTA);
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEliminar.setFocusPainted(false);
        btnEliminar.setBorderPainted(false);
        btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminar.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnEliminar.addActionListener(evt -> eliminarBackup());
        btnPanel.add(btnEliminar);
        
        // ===== ICONO PARA ELIMINAR ANTIGUOS =====
        try {
            ImageIcon iconClear = new ImageIcon(getClass().getResource("/icons/clear.png"));
            Image imgClear = iconClear.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnEliminarAntiguos = new JButton("Eliminar Antiguos (dejar 5)", new ImageIcon(imgClear));
        } catch (Exception e) {
            btnEliminarAntiguos = new JButton("🧹 Eliminar Antiguos (dejar 5)");
        }
        btnEliminarAntiguos.setBackground(new Color(100, 100, 150));
        btnEliminarAntiguos.setForeground(Color.WHITE);
        btnEliminarAntiguos.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEliminarAntiguos.setFocusPainted(false);
        btnEliminarAntiguos.setBorderPainted(false);
        btnEliminarAntiguos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminarAntiguos.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnEliminarAntiguos.addActionListener(evt -> eliminarAntiguos());
        btnPanel.add(btnEliminarAntiguos);
        
        panel.add(btnPanel, BorderLayout.WEST);
        
        lblInfo = new JLabel("📊 Listo");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(new Color(120, 130, 140));
        panel.add(lblInfo, BorderLayout.EAST);
        
        return panel;
    }
    
    // ===== EL RESTO DE MÉTODOS (cargarBackups, crearBackup, restaurarBackup, eliminarBackup, eliminarAntiguos, reiniciarAplicacion) =====
    // MANTÉN TUS MÉTODOS EXISTENTES SIN CAMBIOS AQUÍ
    public void cargarBackups() {
        tableModel.setRowCount(0);
        
        List<BackupManager.BackupInfo> backups = BackupManager.listarBackups();
        
        if (backups.isEmpty()) {
            lblInfo.setText("📊 No hay respaldos guardados");
            return;
        }
        
        int contador = 1;
        for (BackupManager.BackupInfo info : backups) {
            tableModel.addRow(new Object[]{
                contador++,
                info.fecha,
                info.getTamañoFormateado(),
                "📥 Restaurar"
            });
        }
        
        lblInfo.setText(String.format("📊 %d respaldos disponibles", backups.size()));
    }
    
    private void crearBackup() {
        btnCrearBackup.setEnabled(false);
        btnCrearBackup.setText("⏳ Creando...");
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private String rutaBackup;
            private String error;
            
            @Override
            protected Void doInBackground() {
                try {
                    rutaBackup = BackupManager.crearBackup();
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }
            
            @Override
            protected void done() {
                btnCrearBackup.setEnabled(true);
                btnCrearBackup.setText("📦 Crear Respaldo");
                
                if (error != null) {
                    JOptionPane.showMessageDialog(BackupPanel.this, 
                        "Error al crear respaldo: " + error, 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                JOptionPane.showMessageDialog(BackupPanel.this,
                    "✅ Respaldo creado exitosamente!\n\n" +
                    "Ubicación: " + rutaBackup,
                    "Respaldo creado",
                    JOptionPane.INFORMATION_MESSAGE);
                
                cargarBackups();
            }
        };
        
        worker.execute();
    }
    
    private void restaurarBackup() {
        int row = tablaBackups.getSelectedRow();
        if (row < 0) {
            UITheme.showWarning(this, "Seleccione un respaldo para restaurar.");
            return;
        }
        
        List<BackupManager.BackupInfo> backups = BackupManager.listarBackups();
        if (row >= backups.size()) {
            UITheme.showError(this, "Respaldo no encontrado.");
            return;
        }
        
        BackupManager.BackupInfo info = backups.get(row);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ ¿Restaurar respaldo?\n\n" +
            "Fecha: " + info.fecha + "\n" +
            "Tamaño: " + info.getTamañoFormateado() + "\n\n" +
            "ADVERTENCIA: Se reemplazará la base de datos actual.\n" +
            "Se creará un respaldo de seguridad automático.\n" +
            "El programa se reiniciará después de la restauración.",
            "Confirmar restauración",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        btnRestaurar.setEnabled(false);
        btnRestaurar.setText("⏳ Restaurando...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String error;
            
            @Override
            protected Boolean doInBackground() {
                try {
                    return BackupManager.restaurarBackup(info.rutaCompleta);
                } catch (Exception e) {
                    error = e.getMessage();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                btnRestaurar.setEnabled(true);
                btnRestaurar.setText("🔄 Restaurar Seleccionado");
                
                if (error != null) {
                    JOptionPane.showMessageDialog(BackupPanel.this, 
                        "Error al restaurar: " + error, 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                JOptionPane.showMessageDialog(BackupPanel.this,
                    "✅ Respaldo restaurado exitosamente!\n\n" +
                    "El programa se reiniciará para aplicar los cambios.",
                    "Restauración exitosa",
                    JOptionPane.INFORMATION_MESSAGE);
                
                reiniciarAplicacion();
            }
        };
        
        worker.execute();
    }
    
    private void eliminarBackup() {
        int row = tablaBackups.getSelectedRow();
        if (row < 0) {
            UITheme.showWarning(this, "Seleccione un respaldo para eliminar.");
            return;
        }
        
        List<BackupManager.BackupInfo> backups = BackupManager.listarBackups();
        if (row >= backups.size()) {
            UITheme.showError(this, "Respaldo no encontrado.");
            return;
        }
        
        BackupManager.BackupInfo info = backups.get(row);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ ¿Eliminar el respaldo?\n\n" +
            "Fecha: " + info.fecha + "\n" +
            "Tamaño: " + info.getTamañoFormateado() + "\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        if (BackupManager.eliminarBackup(info.rutaCompleta)) {
            UITheme.showSuccess(this, "✅ Respaldo eliminado.");
            cargarBackups();
        } else {
            UITheme.showError(this, "Error al eliminar el respaldo.");
        }
    }
    
    private void eliminarAntiguos() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ ¿Eliminar respaldos antiguos?\n\n" +
            "Se mantendrán los 5 respaldos más recientes.\n" +
            "Los demás serán eliminados.\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        int eliminados = BackupManager.eliminarBackupsAntiguos(5);
        
        JOptionPane.showMessageDialog(this, 
            "✅ Se eliminaron " + eliminados + " respaldos antiguos.\n" +
            "Se mantienen los 5 más recientes.",
            "Limpieza completada",
            JOptionPane.INFORMATION_MESSAGE);
        
        cargarBackups();
    }
    
    private void reiniciarAplicacion() {
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            File currentJar = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            
            if (!currentJar.getName().endsWith(".jar")) {
                UITheme.showSuccess(this, "Reinicie la aplicación manualmente desde NetBeans.");
                return;
            }
            
            ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath());
            builder.start();
            System.exit(0);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "No se pudo reiniciar automáticamente.\n" +
                "Por favor, cierre y abra la aplicación manualmente.",
                "Reinicio manual",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}