package com.feedformulator.ui;

import com.feedformulator.dao.DatabaseManager;
import com.feedformulator.util.SessionManager;
import com.feedformulator.model.Usuario;
import com.formdev.flatlaf.FlatLightLaf;
import java.sql.Statement;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private static final Color COLOR_TAB_BG    = new Color(22, 40, 70);
    private static final Color COLOR_TAB_SEL   = new Color(37, 99, 235);
    private static final Color COLOR_FONDO     = new Color(245, 247, 250);

    private DashboardPanel     dashboardPanel;
    private StockPanel         stockPanel;
    private FormulacionPanel   formulacionPanel;
    private VentasPanel        ventasPanel;
    private ClientesPanel      clientesPanel;
    private ProveedoresPanel   proveedoresPanel;
    private MovimientosPanel   movimientosPanel;
    private ReportesPanel      reportesPanel;
    private ContabilidadPanel  contabilidadPanel;
    private BackupPanel        backupPanel;
    private UsuariosPanel      usuariosPanel;

    private JTabbedPane tabs;

    public MainFrame() {
        super("Feed Formulator Pro - Sistema de Alimentos Balanceados [BUILD-2026-08-01-CLIENTES-TAB-FIX]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setIconImage(crearIcono());
        initUI();
    }

    private void initUI() {
        // ===== MOSTRAR LOGIN ANTES DE INICIAR =====
        Usuario usuario = LoginDialog.mostrarLogin(this);
        if (usuario == null) {
            System.exit(0);
        }

        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO);
        add(crearTabs(), BorderLayout.CENTER);
        add(crearStatusBar(), BorderLayout.SOUTH);

        // Aplicar permisos según rol
        aplicarPermisos();
    }

    private JTabbedPane crearTabs() {
    tabs = new JTabbedPane(JTabbedPane.TOP);
    tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
    tabs.setBackground(COLOR_FONDO);

    // ===== INICIALIZAR TODOS LOS PANELES =====
    dashboardPanel   = new DashboardPanel();
    stockPanel       = new StockPanel();
    formulacionPanel = new FormulacionPanel(stockPanel);
    ventasPanel      = new VentasPanel();
    clientesPanel    = new ClientesPanel();
    proveedoresPanel = new ProveedoresPanel();
    movimientosPanel = new MovimientosPanel();
    reportesPanel    = new ReportesPanel();
    contabilidadPanel = new ContabilidadPanel();
    backupPanel      = new BackupPanel();
    usuariosPanel    = new UsuariosPanel();

    // ===== DEFINICIÓN DE PESTAÑAS =====
    String[][] tabDefs = {
        {"🏠", "Dashboard"},
        {"📦", "Inventario"},
        {"🧪", "Formulaciones"},
        {"💰", "Ventas"},
        {"👥", "Clientes"},
        {"🏢", "Proveedores"},
        {"📋", "Movimientos"},
        {"📊", "Reportes"},
        {"🧮", "Contabilidad"},
        {"💾", "Respaldos"},
        {"👤", "Usuarios"}
    };
    
    JPanel[] panels = {
        dashboardPanel, stockPanel, formulacionPanel, ventasPanel,
        clientesPanel, proveedoresPanel,
        movimientosPanel, reportesPanel, contabilidadPanel, backupPanel, usuariosPanel
    };

    // ===== ICONOS PARA LAS PESTAÑAS =====
    String[] iconFiles = {
        "dashboard.png", "box.png", "formula.png", "sales.png",
        "clients.png", "proveedores.png",
        "movements.png", "report.png", "accounting.png", "backup.png", "user.png"
    };
    
    for (int i = 0; i < panels.length; i++) {
        java.net.URL url = getClass().getResource("/icons/" + iconFiles[i]);
        if (url != null) {
            Image img = new ImageIcon(url).getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            tabs.addTab(tabDefs[i][1], new ImageIcon(img), panels[i]);
        } else {
            tabs.addTab(tabDefs[i][0] + "  " + tabDefs[i][1], panels[i]);
        }
    }

    // ============================================================
    //  NUEVO: AGREGAR PESTAÑA DE CONFIGURACIÓN DE RED
    //  SOLO VISIBLE PARA ADMINISTRADORES
    // ============================================================
    try {
        // Verificar si el usuario actual es administrador
        if (SessionManager.isAdmin()) {
            ConfiguracionRedPanel configRedPanel = new ConfiguracionRedPanel();
            
            // Intentar cargar icono de configuración
            try {
                java.net.URL url = getClass().getResource("/icons/settings.png");
                if (url != null) {
                    Image img = new ImageIcon(url).getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                    tabs.addTab("⚙️ Configuración", new ImageIcon(img), configRedPanel);
                } else {
                    tabs.addTab("⚙️ Configuración", configRedPanel);
                }
            } catch (Exception e) {
                tabs.addTab("⚙️ Configuración", configRedPanel);
            }
            
            System.out.println("✅ Pestaña de Configuración agregada (admin)");
        } else {
            System.out.println("ℹ️ Pestaña de Configuración oculta (usuario normal)");
        }
    } catch (Exception e) {
        System.err.println("⚠️ Error al agregar pestaña de configuración: " + e.getMessage());
    }

    // ============================================================
    //  COLORES DE LAS PESTAÑAS
    // ============================================================
    Color[] tabColors = {
        new Color(37,  99, 235),   // Dashboard
        new Color(59, 130, 246),   // Inventario
        new Color(124, 58, 237),   // Formulaciones
        new Color(234, 88,  12),   // Ventas
        new Color(0,  150, 100),   // Clientes
        new Color(200, 100, 20),   // Proveedores
        new Color(100, 60, 180),   // Movimientos
        new Color(217,119,  6),    // Reportes
        new Color(52,  73, 94),    // Contabilidad
        new Color(5, 150, 105),    // Respaldos
        new Color(0, 80, 200),     // Usuarios
        new Color(180, 100, 50)    // Configuración (si existe)
    };
    
    for (int i = 0; i < tabs.getTabCount(); i++) {
        tabs.setForegroundAt(i, tabColors[i % tabColors.length]);
    }

    // ============================================================
    //  EVENTOS DE CAMBIO DE PESTAÑA
    // ============================================================
    // ── Wire dashboard quick-action buttons to real tabs ──
    dashboardPanel.setNavVentas(()       -> tabs.setSelectedIndex(3));
    dashboardPanel.setNavFormulaciones(() -> tabs.setSelectedIndex(2));
    dashboardPanel.setNavInventario(()   -> tabs.setSelectedIndex(1));
    dashboardPanel.setNavMovimientos(()  -> tabs.setSelectedIndex(6));

    tabs.addChangeListener(e -> {
        int s = tabs.getSelectedIndex();
        if (s >= 0 && s < tabs.getTabCount()) {
            String titulo = tabs.getTitleAt(s);
            
            // Si es la pestaña de Configuración, no hacer nada
            if (titulo != null && titulo.contains("Configuración")) {
                return;
            }
            
            switch (s) {
                case 0: if (dashboardPanel   != null) dashboardPanel.refrescar(); break;
                case 1: if (stockPanel       != null) stockPanel.cargarDatos(); break;
                case 2: if (formulacionPanel != null) formulacionPanel.cargarHistorial(); break;
                case 3: if (ventasPanel      != null) ventasPanel.cargarDatos(); break;
                case 4: if (clientesPanel    != null) clientesPanel.cargarDatos(); break;
                case 5: if (proveedoresPanel != null) proveedoresPanel.cargarDatos(); break;
                case 6: if (movimientosPanel != null) movimientosPanel.cargarDatos(); break;
                case 7: if (reportesPanel    != null) reportesPanel.actualizarReporte(); break;
                case 8: if (contabilidadPanel != null) contabilidadPanel.cargarDatos(); break;
                case 9: if (backupPanel      != null) backupPanel.cargarBackups(); break;
                case 10: if (usuariosPanel    != null) usuariosPanel.cargarDatos(); break;
            }
        }
    });

    return tabs;
}

    private void aplicarPermisos() {
        boolean isAdmin = SessionManager.isAdmin();
        
        // Ocultar pestaña de Usuarios si no es admin
        if (!isAdmin && tabs != null) {
            try {
                tabs.removeTabAt(10); // Usuarios
            } catch (Exception e) {
                // Ignorar
            }
        }
    }

    private JPanel crearStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(22, 40, 70));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(37, 99, 235));
                g2.fillRect(0, 0, getWidth(), 2);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        bar.setPreferredSize(new Dimension(0, 34));

        // ===== IZQUIERDA: Versión y usuario =====
        String usuarioInfo = "🌿  Feed Formulator Pro v2.0  |  Usuario: " + SessionManager.getNombreUsuario();
        if (SessionManager.isAdmin()) {
            usuarioInfo += " 👑 (ADMIN)";
        }
        JLabel izq = new JLabel(usuarioInfo);
        izq.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        izq.setForeground(new Color(148, 163, 184));
        bar.add(izq, BorderLayout.WEST);

        // ===== DERECHA: Estado de conexión y botón logout =====
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        der.setOpaque(false);
        
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dot.setForeground(new Color(5, 150, 105));
        JLabel conn = new JLabel("Conectado");
        conn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        conn.setForeground(new Color(148, 163, 184));
        der.add(dot);
        der.add(conn);
        
        // Botón Cerrar Sesión
        JButton btnLogout = new JButton("🚪 Cerrar Sesión");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnLogout.setForeground(new Color(200, 200, 200));
        btnLogout.setBackground(new Color(60, 60, 80));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btnLogout.addActionListener(e -> cerrarSesion());
        der.add(btnLogout);
        
        bar.add(der, BorderLayout.EAST);

        return bar;
    }

    private void cerrarSesion() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Desea cerrar sesión?\n\nSe cerrará la aplicación.",
            "Cerrar Sesión",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            dispose();
            // Reiniciar aplicación
            try {
                String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                File currentJar = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                
                if (currentJar.getName().endsWith(".jar")) {
                    ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", currentJar.getPath());
                    builder.start();
                } else {
                    // En NetBeans, solo reiniciar la JVM
                    Runtime.getRuntime().exec(new String[]{"java", "-cp", System.getProperty("java.class.path"), MainFrame.class.getName()});
                }
                System.exit(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "No se pudo reiniciar automáticamente.\nPor favor, cierre y abra la aplicación manualmente.",
                    "Reinicio manual",
                    JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        }
    }

    private Image crearIcono() {
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, new Color(22,40,70), 32, 32, new Color(37,99,235));
        g.setPaint(gp);
        g.fillRoundRect(0, 0, 32, 32, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g.drawString("F", 9, 24);
        g.dispose();
        return img;
    }

    // ============================================================
    //  MÉTODOS PARA REFRESCAR DESDE OTROS PANELES
    // ============================================================

    /**
     * Antes este método recargaba las 8 pestañas de la aplicación completas
     * (incluyendo Reportes y Backups, que pueden ser consultas/escaneos pesados)
     * cada vez que se guardaba/editaba/eliminaba CUALQUIER cosa en Inventario.
     * Eso sumaba varios segundos de retraso por cada acción.
     *
     * Cada pestaña YA se recarga sola al seleccionarla (ver el ChangeListener
     * de "tabs" mas arriba), así que no hace falta recargarlas todas por
     * adelantado. Aquí solo refrescamos el Dashboard (resumen/inicio), que es
     * lo único que normalmente se ve reflejado sin necesidad de cambiar de pestaña.
     */
    public void refrescarDashboard() {
        // El Dashboard hace mas de 10 consultas separadas a la base de datos
        // (productos, creditos, pedidos pendientes, formulaciones, ventas...).
        // Si el usuario no esta viendo esa pestaña ahora mismo, no tiene sentido
        // pagar ese costo: ya se refresca solo cuando la selecciona (ver el
        // ChangeListener de "tabs" mas arriba).
        if (dashboardPanel != null && tabs != null && tabs.getSelectedIndex() == 0) {
            dashboardPanel.refrescar();
        }
    }

    public void refrescarFormulaciones() {
        if (formulacionPanel != null) formulacionPanel.cargarHistorial();
    }

    public void refrescarInventario() {
        if (stockPanel != null) stockPanel.cargarDatos();
    }

    public void refrescarVentas() {
        if (ventasPanel != null) ventasPanel.cargarDatos();
    }

    public void refrescarClientes() {
        if (clientesPanel != null) clientesPanel.cargarDatos();
    }

    public void refrescarProveedores() {
        if (proveedoresPanel != null) proveedoresPanel.cargarDatos();
    }

    public void refrescarMovimientos() {
        if (movimientosPanel != null) movimientosPanel.cargarDatos();
    }

    public void refrescarReportes() {
        if (reportesPanel != null) reportesPanel.actualizarReporte();
    }

    public void refrescarContabilidad() {
        if (contabilidadPanel != null) contabilidadPanel.cargarDatos();
    }

    public void refrescarBackups() {
        if (backupPanel != null) backupPanel.cargarBackups();
    }

    public void refrescarUsuarios() {
        if (usuariosPanel != null) usuariosPanel.cargarDatos();
    }

    // ============================================================
    //  MAIN
    // ============================================================

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc",            12);
            UIManager.put("Component.arc",         10);
            UIManager.put("TextComponent.arc",      8);
            UIManager.put("Table.arc",              8);
            UIManager.put("TabbedPane.arc",        10);

            UIManager.put("Button.background",     new ColorUIResource(new Color(37, 99, 235)));
            UIManager.put("Button.foreground",     new ColorUIResource(Color.WHITE));
            UIManager.put("Button.hoverBackground",new ColorUIResource(new Color(29, 78, 216)));

            UIManager.put("TabbedPane.selectedBackground",
                          new ColorUIResource(new Color(219, 234, 254)));
            UIManager.put("TabbedPane.tabHeight",  42);

            Font base = new Font("Segoe UI", Font.PLAIN, 13);
            UIManager.put("Button.font",            base);
            UIManager.put("Label.font",             base);
            UIManager.put("TextField.font",         base);
            UIManager.put("Table.font",             base);
            UIManager.put("TableHeader.font",       new Font("Segoe UI", Font.BOLD, 13));
            UIManager.put("ComboBox.font",          base);
            UIManager.put("CheckBox.font",          base);

        } catch (Exception e) {
            System.err.println("FlatLaf no disponible: " + e.getMessage());
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }

        // ============================================================
        //  PASO 1: Inicializar base de datos (crea todas las tablas)
        // ============================================================
        DatabaseManager.initializeDatabase();

        // ============================================================
        //  PASO 2: Verificar/Crear usuario admin
        // ============================================================
        verificarYRecrearUsuarioAdmin();

        // ============================================================
        //  PASO 3: Iniciar la aplicación
        // ============================================================
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // ============================================================
    //  MÉTODO PARA REPARAR USUARIO ADMIN
    // ============================================================
    // ============================================================
//  MÉTODO PARA VERIFICAR/CREAR USUARIO ADMIN (SIN SOBREESCRIBIR CONTRASEÑA)
// ============================================================
private static void verificarYRecrearUsuarioAdmin() {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    
    try {
        String dbFolder = System.getenv("APPDATA") + File.separator + "FeedFormulatorPro";
        String dbUrl = "jdbc:sqlite:" + dbFolder + File.separator + "feedformulator.db";
        
        conn = DriverManager.getConnection(dbUrl);
        
        // ============================================================
        //  1. VERIFICAR QUE LA TABLA USUARIOS EXISTA
        // ============================================================
        rs = conn.getMetaData().getTables(null, null, "usuarios", null);
        if (!rs.next()) {
            System.out.println("⚠️ Tabla usuarios no existe, creando...");
            rs.close();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE usuarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nombre TEXT NOT NULL," +
                    "usuario TEXT UNIQUE NOT NULL," +
                    "contrasena TEXT NOT NULL," +
                    "rol TEXT DEFAULT 'USUARIO'," +
                    "estado TEXT DEFAULT 'ACTIVO'," +
                    "fecha_registro TEXT NOT NULL)"
                );
                System.out.println("✅ Tabla usuarios creada");
            }
        } else {
            rs.close();
        }
        
        // ============================================================
        //  2. VERIFICAR SI EL USUARIO admin EXISTE
        // ============================================================
        String sql = "SELECT * FROM usuarios WHERE usuario = 'admin'";
        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();
        
        if (rs.next()) {
            // ============================================================
            //  EL USUARIO ADMIN YA EXISTE
            //  SOLO VERIFICAMOS QUE ESTÉ ACTIVO
            //  NO CAMBIAMOS LA CONTRASEÑA
            // ============================================================
            String estado = rs.getString("estado");
            rs.close();
            ps.close();
            
            if (!"ACTIVO".equals(estado)) {
                // Si está inactivo, lo reactivamos (pero mantenemos su contraseña)
                String updateSql = "UPDATE usuarios SET estado = 'ACTIVO' WHERE usuario = 'admin'";
                ps = conn.prepareStatement(updateSql);
                ps.executeUpdate();
                System.out.println("✅ Usuario admin reactivado (contraseña preservada)");
            } else {
                System.out.println("✅ Usuario admin ya existe y está activo (contraseña preservada)");
            }
            
        } else {
            // ============================================================
            //  EL USUARIO admin NO EXISTE → LO CREAMOS
            //  (SOLO OCURRE LA PRIMERA VEZ QUE SE EJECUTA EL PROGRAMA)
            // ============================================================
            rs.close();
            ps.close();
            
            System.out.println("⚠️ Usuario admin no encontrado, creando por primera vez...");
            
            String insertSql = "INSERT INTO usuarios (nombre, usuario, contrasena, rol, estado, fecha_registro) " +
                               "VALUES ('Administrador', 'admin', 'admin123', 'ADMIN', 'ACTIVO', datetime('now'))";
            ps = conn.prepareStatement(insertSql);
            int insertados = ps.executeUpdate();
            if (insertados > 0) {
                System.out.println("✅ Usuario admin creado por primera vez (admin/admin123)");
                System.out.println("   ⚠️ CAMBIE LA CONTRASEÑA POR SEGURIDAD");
            } else {
                System.out.println("⚠️ No se pudo crear el usuario admin");
            }
        }
        
    } catch (SQLException e) {
        System.err.println("❌ Error al verificar usuario admin: " + e.getMessage());
        e.printStackTrace();
    } finally {
        try { if (rs != null) rs.close(); } catch (SQLException e) {}
        try { if (ps != null) ps.close(); } catch (SQLException e) {}
        try { if (conn != null) conn.close(); } catch (SQLException e) {}
    }
}
}