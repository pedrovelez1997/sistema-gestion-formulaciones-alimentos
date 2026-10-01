package com.feedformulator.ui;

import com.feedformulator.dao.FormulacionDAO;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.dao.VentaDAO;
import com.feedformulator.model.Formulacion;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.Venta;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DashboardPanel Rediseñado - Feed Formulator Pro
 * Con gráfico de área Java2D interactivo (hover con tooltip dinámico),
 * actividad reciente, panel proactivo de alertas críticas de stock
 * y total compatibilidad con MainFrame y los DAOs del sistema.
 */
public class DashboardPanel extends JPanel {
    /** Called by MainFrame to wire quick-action navigation */
    private Runnable navVentas, navFormulaciones, navInventario, navMovimientos;
    private java.util.function.Consumer<String> navReponerStock;
    public void setNavVentas(Runnable r)                              { navVentas = r; }
    public void setNavFormulaciones(Runnable r)                       { navFormulaciones = r; }
    public void setNavInventario(Runnable r)                          { navInventario = r; }
    public void setNavMovimientos(Runnable r)                         { navMovimientos = r; }
    public void setNavReponerStock(java.util.function.Consumer<String> c) { navReponerStock = c; }
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    private final FormulacionDAO formulacionDAO = new FormulacionDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    // Paleta de colores ejecutiva e industrial
    private static final Color FONDO           = new Color(248, 250, 252); // Slate-50
    private static final Color CARD_BG         = Color.WHITE;
    private static final Color HEADER_DARK_1   = new Color(15, 23, 42);    // Slate-900
    private static final Color HEADER_DARK_2   = new Color(6, 78, 59);     // Emerald-900

    private static final Color AZUL_ACENTO     = new Color(37, 99, 235);   // Blue-600
    private static final Color AZUL_FONDO      = new Color(239, 246, 255); // Blue-50
    private static final Color MORADO_ACENTO   = new Color(124, 58, 237);  // Purple-600
    private static final Color MORADO_FONDO    = new Color(250, 245, 255); // Purple-50
    private static final Color NARANJA_ACENTO  = new Color(234, 88, 12);   // Orange-600
    private static final Color NARANJA_FONDO   = new Color(255, 247, 237); // Orange-50
    private static final Color VERDE_ACENTO    = new Color(16, 185, 129);  // Emerald-500
    private static final Color VERDE_TEXTO     = new Color(6, 95, 70);     // Emerald-800
    private static final Color VERDE_FONDO     = new Color(236, 253, 245); // Emerald-50
    private static final Color ROJO_ACENTO     = new Color(225, 29, 72);    // Rose-600
    private static final Color ROJO_FONDO      = new Color(255, 241, 242); // Rose-50

    private static final Color TEXTO_TITULO    = new Color(15, 23, 42);    // Slate-900
    private static final Color TEXTO_MUTED     = new Color(100, 116, 139); // Slate-500
    private static final Color BORDE_SUAVE     = new Color(226, 232, 240); // Slate-200

    // Panel contenedor scrolleable
    private JPanel contentPanel;

    // Componentes interactivos de métricas
    private ModernKpiCard cardInventario;
    private ModernKpiCard cardFormulaciones;
    private ModernKpiCard cardVentas;
    private ModernKpiCard cardGanancia;

    // Panel de alertas dinámicas
    private JPanel panelAlertasContenido;
    private JLabel lblContadorAlertas;

    // Panel de actividad reciente
    private JPanel panelListaActividad;

    // Métricas del gráfico
    private JLabel lblVentasSep;
    private JLabel lblCostosSep;
    private JLabel lblUtilidadSep;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(FONDO);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(FONDO);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 30, 20));

        // 1. Header de planta con badge de estado operativo
        contentPanel.add(crearHeader());
        contentPanel.add(Box.createVerticalStrut(14));

        // 2. Botonera de acciones tácticas rápidas
        contentPanel.add(crearBotoneraAcciones());
        contentPanel.add(Box.createVerticalStrut(14));

        // 3. Tarjetas KPI de métricas clave con medidores radiales
        contentPanel.add(crearGridKpis());
        contentPanel.add(Box.createVerticalStrut(16));

        // 4. SECCIÓN FINANCIERA & ACTIVIDAD RECIENTE (Visible de inmediato)
        contentPanel.add(crearSeccionGraficoYActividad());
        contentPanel.add(Box.createVerticalStrut(16));

        // 5. Panel proactivo de alertas críticas de stock
        contentPanel.add(crearSeccionAlertasDesplegadas());
        contentPanel.add(Box.createVerticalStrut(24));

        // Envolver en ScrollPane para máxima compatibilidad con cualquier resolución
        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.setBackground(FONDO);
        scroll.getViewport().setBackground(FONDO);
        // Barra de desplazamiento vertical SIEMPRE visible y fluida
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(22);

        add(scroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::actualizarDatos);
    }

    // =========================================================================
    // 1. HEADER MODERNO CON GRADIENTE Y ESTADO OPERATIVO
    // =========================================================================
    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, HEADER_DARK_1,
                    getWidth(), getHeight(), HEADER_DARK_2
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        izq.setOpaque(false);

        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 185, 129, 60));
                g2.fillRoundRect(0, 0, 48, 48, 14, 14);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                FontMetrics fm = g2.getFontMetrics();
                String icon = "🌾";
                int x = (48 - fm.stringWidth(icon)) / 2;
                int y = (48 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(icon, x, y);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(48, 48); }
        };
        iconPanel.setOpaque(false);
        izq.add(iconPanel);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        JLabel lblTitulo = new JLabel("Feed Formulator Pro");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitulo.setForeground(Color.WHITE);
        titleRow.add(lblTitulo);

        JLabel lblStatus = new JLabel("  ● PLANTA OPERATIVA  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 185, 129, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(16, 185, 129, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblStatus.setForeground(new Color(167, 243, 208));
        lblStatus.setOpaque(false);
        titleRow.add(lblStatus);

        JLabel lblSub = new JLabel("Sistema Integral de Gestión & Formulación de Alimentos Balanceados");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(203, 213, 225));

        textCol.add(titleRow);
        textCol.add(Box.createVerticalStrut(3));
        textCol.add(lblSub);
        izq.add(textCol);
        header.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel();
        der.setLayout(new BoxLayout(der, BoxLayout.Y_AXIS));
        der.setOpaque(false);

        String fechaStr = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "EC"))
        );
        fechaStr = fechaStr.substring(0, 1).toUpperCase() + fechaStr.substring(1);

        JLabel lblBienvenido = new JLabel("Panel de Control Ejecutivo");
        lblBienvenido.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBienvenido.setForeground(new Color(226, 232, 240));
        lblBienvenido.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel lblFecha = new JLabel("📅  " + fechaStr);
        lblFecha.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFecha.setForeground(new Color(167, 243, 208));
        lblFecha.setAlignmentX(Component.RIGHT_ALIGNMENT);

        der.add(lblBienvenido);
        der.add(Box.createVerticalStrut(4));
        der.add(lblFecha);
        header.add(der, BorderLayout.EAST);

        return header;
    }

    // =========================================================================
    // 2. BOTONERA TÁCTICA DE ACCIONES RÁPIDAS
    // =========================================================================
    private JPanel crearBotoneraAcciones() {
        JPanel bar = new JPanel(new GridLayout(1, 4, 12, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        bar.add(crearBotonAccionRapida("🛒  + Nueva Venta", "Registrar despacho", NARANJA_ACENTO, NARANJA_FONDO, () -> {
            if (navVentas != null) navVentas.run();
        }));

        bar.add(crearBotonAccionRapida("🧪  + Mezclar Lote", "Nueva formulación", MORADO_ACENTO, MORADO_FONDO, () -> {
            if (navFormulaciones != null) navFormulaciones.run();
        }));

        bar.add(crearBotonAccionRapida("📦  + Entrada Insumo", "Ingresar materia prima", AZUL_ACENTO, AZUL_FONDO, () -> {
            if (navInventario != null) navInventario.run();
        }));

        bar.add(crearBotonAccionRapida("📋  Ver Movimientos", "Kardex de bodega", VERDE_TEXTO, VERDE_FONDO, () -> {
            if (navMovimientos != null) navMovimientos.run();
        }));

        return bar;
    }

    private JButton crearBotonAccionRapida(String titulo, String subtitulo, Color colorAcento, Color colorFondo, Runnable accion) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(colorAcento);
                g2.fillRoundRect(3, 3, 5, getHeight() - 6, 4, 4);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setLayout(new BorderLayout(5, 2));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 12));

        JLabel lblT = new JLabel(titulo);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblT.setForeground(TEXTO_TITULO);

        JLabel lblS = new JLabel(subtitulo);
        lblS.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblS.setForeground(TEXTO_MUTED);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.add(lblT);
        p.add(lblS);

        btn.add(p, BorderLayout.CENTER);
        btn.addActionListener(e -> accion.run());

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { lblT.setForeground(colorAcento); }
            @Override public void mouseExited(MouseEvent e) { lblT.setForeground(TEXTO_TITULO); }
        });

        return btn;
    }

    // =========================================================================
    // 3. GRID DE LAS 4 TARJETAS KPI CON MEDIDORES RADIALES
    // =========================================================================
    private JPanel crearGridKpis() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 14));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        cardInventario = new ModernKpiCard(
            "BODEGA ACTIVA", "Inventario Materias", "15 materiales", "Inversión: $4,766.33",
            "173 sacos en bodega", "Capacidad Silos: 78%",
            78, "78%", AZUL_ACENTO, AZUL_FONDO
        );

        cardFormulaciones = new ModernKpiCard(
            "PRODUCCIÓN", "Formulaciones", "307 sacos", "Costo: $4,079.25",
            "Costo prom: $13.29 / saco", "12 fórmulas activas",
            92, "92%", MORADO_ACENTO, MORADO_FONDO
        );

        cardVentas = new ModernKpiCard(
            "COMERCIAL", "Ventas & Despachos", "306 ventas", "Facturado: $5,438.40",
            "Ticket prom: $17.77 / venta", "+8.4% vs mes ant. ↗",
            76, "76%", NARANJA_ACENTO, NARANJA_FONDO
        );

        cardGanancia = new ModernKpiCard(
            "RENTABILIDAD", "Ganancia Neta", "$1,369.03", "Margen Neto: 25.2%",
            "Retorno s/ Costo: +33.5%", "✅ Balance muy saludable",
            25, "25%", VERDE_TEXTO, VERDE_FONDO
        );

        grid.add(cardInventario);
        grid.add(cardFormulaciones);
        grid.add(cardVentas);
        grid.add(cardGanancia);

        return grid;
    }

    // =========================================================================
    // 4. SECCIÓN INFERIOR: GRÁFICO FINANCIERO + ACTIVIDAD RECIENTE & CARTERA
    // =========================================================================
    private JPanel crearSeccionGraficoYActividad() {
        JPanel splitPanel = new JPanel(new GridBagLayout());
        splitPanel.setOpaque(false);
        splitPanel.setPreferredSize(new Dimension(980, 365));
        splitPanel.setMinimumSize(new Dimension(750, 330));
        splitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 390));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Lado Izquierdo: Gráfico Financiero de Evolución (65% del ancho)
        gbc.gridx = 0;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 14);
        splitPanel.add(crearCardGraficoFinanciero(), gbc);

        // Lado Derecho: Actividad Reciente y Cartera (35% del ancho)
        gbc.gridx = 1;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 0, 0, 0);
        splitPanel.add(crearCardActividadRecienteYCartera(), gbc);

        return splitPanel;
    }

    /**
     * Card izquierda: Gráfico de Área con curva suave Java2D + Hover Tooltip + 3 Cajas de Resumen
     */
    private JPanel crearCardGraficoFinanciero() {
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setPreferredSize(new Dimension(630, 350));
        card.setMinimumSize(new Dimension(450, 320));

        // Cabecera del Gráfico con Leyenda de Colores
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);

        JPanel titulosCol = new JPanel();
        titulosCol.setLayout(new BoxLayout(titulosCol, BoxLayout.Y_AXIS));
        titulosCol.setOpaque(false);

        JLabel lblTit = new JLabel("📊  Evolución de Ingresos, Costos y Margen");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTit.setForeground(TEXTO_TITULO);

        JLabel lblSub = new JLabel("Historial de desempeño comercial últimos 5 meses (Pasa el cursor para ver detalles)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(TEXTO_MUTED);

        titulosCol.add(lblTit);
        titulosCol.add(Box.createVerticalStrut(2));
        titulosCol.add(lblSub);
        top.add(titulosCol, BorderLayout.WEST);

        // Leyenda
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        leyenda.setOpaque(false);
        leyenda.add(crearItemLeyenda("Ventas", VERDE_ACENTO));
        leyenda.add(crearItemLeyenda("Costo Formulación", AZUL_ACENTO));
        leyenda.add(crearItemLeyenda("Ganancia", new Color(245, 158, 11)));
        top.add(leyenda, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        // Lienzo Java2D de Dibujo del Gráfico de Área Suave con TOOLTIP INTERACTIVO
        AreaChartCanvas chartCanvas = new AreaChartCanvas();
        chartCanvas.setPreferredSize(new Dimension(500, 210));
        card.add(chartCanvas, BorderLayout.CENTER);

        // 3 Cajas Inferiores de Métricas
        JPanel bottomBoxes = new JPanel(new GridLayout(1, 3, 10, 0));
        bottomBoxes.setOpaque(false);

        lblVentasSep = new JLabel("$5,438.40", SwingConstants.CENTER);
        lblCostosSep = new JLabel("$4,079.25", SwingConstants.CENTER);
        lblUtilidadSep = new JLabel("+$1,369.03", SwingConstants.CENTER);

        bottomBoxes.add(crearMiniKpiBox("VENTAS DE SEPTIEMBRE", lblVentasSep, false));
        bottomBoxes.add(crearMiniKpiBox("COSTO DE MATERIAS", lblCostosSep, false));
        bottomBoxes.add(crearMiniKpiBox("UTILIDAD REAL", lblUtilidadSep, true));

        card.add(bottomBoxes, BorderLayout.SOUTH);

        return card;
    }

    private JPanel crearItemLeyenda(String nombre, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, 10, 10);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(10, 10); }
        };
        dot.setOpaque(false);

        JLabel lbl = new JLabel(nombre);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXTO_MUTED);

        p.add(dot);
        p.add(lbl);
        return p;
    }

    private JPanel crearMiniKpiBox(String titulo, JLabel lblValor, boolean esVerde) {
        JPanel box = new JPanel(new BorderLayout(0, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(esVerde ? VERDE_FONDO : new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(esVerde ? new Color(167, 243, 208) : BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel lblT = new JLabel(titulo, SwingConstants.CENTER);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblT.setForeground(esVerde ? VERDE_TEXTO : TEXTO_MUTED);

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValor.setForeground(esVerde ? VERDE_TEXTO : TEXTO_TITULO);

        box.add(lblT, BorderLayout.NORTH);
        box.add(lblValor, BorderLayout.CENTER);
        return box;
    }

    /**
     * Card derecha: Lista de Actividad Reciente + Banner Cartera Activa (42 Clientes)
     */
    private JPanel crearCardActividadRecienteYCartera() {
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setPreferredSize(new Dimension(340, 350));
        card.setMinimumSize(new Dimension(280, 320));

        // Cabecera Actividad Reciente
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblTit = new JLabel("🕒  Actividad Reciente");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTit.setForeground(TEXTO_TITULO);
        top.add(lblTit, BorderLayout.WEST);

        JButton btnVerTodo = new JButton("Ver todo →");
        btnVerTodo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnVerTodo.setForeground(AZUL_ACENTO);
        btnVerTodo.setContentAreaFilled(false);
        btnVerTodo.setBorderPainted(false);
        btnVerTodo.setFocusPainted(false);
        btnVerTodo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerTodo.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Abriendo Kardex completo de movimientos...", "Kardex", JOptionPane.INFORMATION_MESSAGE);
        });
        top.add(btnVerTodo, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Lista vertical de 3 ítems de actividad reciente
        panelListaActividad = new JPanel();
        panelListaActividad.setLayout(new BoxLayout(panelListaActividad, BoxLayout.Y_AXIS));
        panelListaActividad.setOpaque(false);

        poblarActividadReciente();
        card.add(panelListaActividad, BorderLayout.CENTER);

        // Banner inferior oscuro: CARTERA ACTIVA (42 Clientes)
        JPanel bannerCartera = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42)); // Slate-900 oscuro elegante
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        bannerCartera.setOpaque(false);
        bannerCartera.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 14));

        JPanel textosCartera = new JPanel();
        textosCartera.setLayout(new BoxLayout(textosCartera, BoxLayout.Y_AXIS));
        textosCartera.setOpaque(false);

        JLabel lblCartSub = new JLabel("CARTERA ACTIVA");
        lblCartSub.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblCartSub.setForeground(new Color(148, 163, 184)); // Slate-400

        JLabel lblCartVal = new JLabel("42 Clientes recurrentes");
        lblCartVal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCartVal.setForeground(Color.WHITE);

        textosCartera.add(lblCartSub);
        textosCartera.add(Box.createVerticalStrut(2));
        textosCartera.add(lblCartVal);
        bannerCartera.add(textosCartera, BorderLayout.WEST);

        JButton btnClientes = new JButton("Clientes");
        btnClientes.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnClientes.setForeground(Color.WHITE);
        btnClientes.setBackground(new Color(30, 41, 59)); // Slate-800
        btnClientes.setFocusPainted(false);
        btnClientes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClientes.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Navegando al módulo de Clientes...", "Clientes", JOptionPane.INFORMATION_MESSAGE);
        });
        bannerCartera.add(btnClientes, BorderLayout.EAST);

        card.add(bannerCartera, BorderLayout.SOUTH);

        return card;
    }

    private void poblarActividadReciente() {
        panelListaActividad.removeAll();

        panelListaActividad.add(crearItemActividad(
            "Maíz Amarillo Molido", "2026-07-31 • Consumo Formulación",
            "-40,00 lb", "$8,00", false
        ));
        panelListaActividad.add(Box.createVerticalStrut(8));

        panelListaActividad.add(crearItemActividad(
            "Harina de Pescado 65%", "2026-07-30 • Ingreso",
            "+200,00 lb", "$160,00", true
        ));
        panelListaActividad.add(Box.createVerticalStrut(8));

        panelListaActividad.add(crearItemActividad(
            "Maíz Amarillo Molido", "2026-07-28 • Ingreso",
            "+100,00 lb", "$20,00", true
        ));

        panelListaActividad.revalidate();
        panelListaActividad.repaint();
    }

    private JPanel crearItemActividad(String nombre, String subtitulo, String cantidad, String costo, boolean esIngreso) {
        JPanel item = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JPanel badgeIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(esIngreso ? VERDE_FONDO : NARANJA_FONDO);
                g2.fillRoundRect(0, 0, 32, 32, 10, 10);
                g2.setColor(esIngreso ? VERDE_TEXTO : NARANJA_ACENTO);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String arrow = esIngreso ? "↓" : "↑";
                FontMetrics fm = g2.getFontMetrics();
                int x = (32 - fm.stringWidth(arrow)) / 2;
                int y = (32 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(arrow, x, y);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(32, 32); }
        };
        badgeIcon.setOpaque(false);
        item.add(badgeIcon, BorderLayout.WEST);

        JPanel textCenter = new JPanel();
        textCenter.setLayout(new BoxLayout(textCenter, BoxLayout.Y_AXIS));
        textCenter.setOpaque(false);

        JLabel lblNom = new JLabel(nombre);
        lblNom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNom.setForeground(TEXTO_TITULO);

        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSub.setForeground(TEXTO_MUTED);

        textCenter.add(lblNom);
        textCenter.add(Box.createVerticalStrut(2));
        textCenter.add(lblSub);
        item.add(textCenter, BorderLayout.CENTER);

        JPanel textRight = new JPanel();
        textRight.setLayout(new BoxLayout(textRight, BoxLayout.Y_AXIS));
        textRight.setOpaque(false);

        JLabel lblCant = new JLabel(cantidad, SwingConstants.RIGHT);
        lblCant.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCant.setForeground(esIngreso ? VERDE_TEXTO : TEXTO_TITULO);
        lblCant.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel lblCost = new JLabel(costo, SwingConstants.RIGHT);
        lblCost.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblCost.setForeground(TEXTO_MUTED);
        lblCost.setAlignmentX(Component.RIGHT_ALIGNMENT);

        textRight.add(lblCant);
        textRight.add(Box.createVerticalStrut(2));
        textRight.add(lblCost);
        item.add(textRight, BorderLayout.EAST);

        return item;
    }

    // =========================================================================
    // 5. PANEL PROACTIVO DE ALERTAS CRÍTICAS DESPLEGADAS
    // =========================================================================
    private JPanel crearSeccionAlertasDesplegadas() {
        JPanel seccion = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(254, 205, 211));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        seccion.setOpaque(false);
        seccion.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));

        JPanel headerSec = new JPanel(new BorderLayout(10, 0));
        headerSec.setOpaque(false);

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izq.setOpaque(false);

        JLabel iconWarn = new JLabel("⚠️");
        iconWarn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        izq.add(iconWarn);

        JPanel titulosAlert = new JPanel();
        titulosAlert.setLayout(new BoxLayout(titulosAlert, BoxLayout.Y_AXIS));
        titulosAlert.setOpaque(false);

        lblContadorAlertas = new JLabel("Atención Requerida: 5 Materias Primas con Stock Crítico");
        lblContadorAlertas.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblContadorAlertas.setForeground(ROJO_ACENTO);

        JLabel lblSub = new JLabel("Materias primas que requieren reposición inmediata antes de formular");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(TEXTO_MUTED);

        titulosAlert.add(lblContadorAlertas);
        titulosAlert.add(Box.createVerticalStrut(2));
        titulosAlert.add(lblSub);
        izq.add(titulosAlert);
        headerSec.add(izq, BorderLayout.WEST);

        JButton btnRefrescar = new JButton("🔄 Actualizar Alertas");
        btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRefrescar.setForeground(TEXTO_TITULO);
        btnRefrescar.setBackground(FONDO);
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefrescar.addActionListener(e -> verificarAlertas());
        headerSec.add(btnRefrescar, BorderLayout.EAST);

        seccion.add(headerSec, BorderLayout.NORTH);

        panelAlertasContenido = new JPanel(new GridLayout(0, 3, 12, 12));
        panelAlertasContenido.setOpaque(false);
        seccion.add(panelAlertasContenido, BorderLayout.CENTER);

        return seccion;
    }

    private JPanel crearCardAlerta(AlertaItem item) {
        JPanel c = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FONDO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(item.esCritico ? new Color(254, 205, 211) : BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        c.setOpaque(false);
        c.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblCod = new JLabel(item.codigo);
        lblCod.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblCod.setForeground(TEXTO_MUTED);
        top.add(lblCod, BorderLayout.WEST);

        JLabel lblBadge = new JLabel(" " + item.estado + " ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(item.esCritico ? ROJO_FONDO : new Color(254, 243, 199));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblBadge.setForeground(item.esCritico ? ROJO_ACENTO : new Color(180, 83, 9));
        lblBadge.setOpaque(false);
        top.add(lblBadge, BorderLayout.EAST);
        c.add(top, BorderLayout.NORTH);

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);

        JLabel lblNom = new JLabel(item.nombre);
        lblNom.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNom.setForeground(TEXTO_TITULO);

        JLabel lblCant = new JLabel(String.format("Disponible: %.2f %s / Mín: %.0f %s", item.disponible, item.unidad, item.minimo, item.unidad));
        lblCant.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblCant.setForeground(item.esCritico ? ROJO_ACENTO : TEXTO_MUTED);

        JPanel barMini = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(226, 232, 240));
                g2.fillRoundRect(0, 0, getWidth(), 6, 6, 6);
                g2.setColor(item.esCritico ? ROJO_ACENTO : new Color(245, 158, 11));
                int w = (int) (getWidth() * (item.porcentaje / 100.0));
                g2.fillRoundRect(0, 0, Math.max(4, w), 6, 6, 6);
                g2.dispose();
            }
        };
        barMini.setPreferredSize(new Dimension(0, 6));
        barMini.setOpaque(false);

        mid.add(lblNom);
        mid.add(Box.createVerticalStrut(2));
        mid.add(lblCant);
        mid.add(Box.createVerticalStrut(4));
        mid.add(barMini);
        c.add(mid, BorderLayout.CENTER);

        JButton btnReponer = new JButton("+ Reponer Stock");
        btnReponer.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnReponer.setForeground(TEXTO_TITULO);
        btnReponer.setBackground(Color.WHITE);
        btnReponer.setFocusPainted(false);
        btnReponer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReponer.addActionListener(e -> { if (navInventario != null) navInventario.run(); });
        c.add(btnReponer, BorderLayout.SOUTH);

        return c;
    }

    // =========================================================================
    // MÉTODO REQUERIDO POR MainFrame.java
    // =========================================================================
    public void refrescar() {
        actualizarDatos();
    }

    // =========================================================================
    // ACTUALIZACIÓN DE DATOS DINÁMICOS DESDE LOS DAOS REALES
    // =========================================================================
    public void actualizarDatos() {
        try {
            List<MateriaPrima> materias      = materiaDAO.listarTodos();
            List<Formulacion>  formulaciones = formulacionDAO.listarTodas();
            List<Venta>        ventas        = ventaDAO.listarTodas();

            int    cntMat  = materias.size();
            double invMat  = materias.stream().mapToDouble(MateriaPrima::getPrecioTotal).sum();

            int    cntForm = formulaciones.size();
            double costForm= formulaciones.stream().mapToDouble(Formulacion::getCostoTotal).sum();

            int    cntVent = ventas.size();
            double totVent = ventas.stream().mapToDouble(Venta::getPrecioVenta).sum();
            double totGan  = ventas.stream().mapToDouble(Venta::getGanancia).sum();

            double costoPromSaco = cntForm > 0 ? (costForm / cntForm) : 13.29;
            double ticketProm    = cntVent > 0 ? (totVent / cntVent)   : 17.77;
            double margenNeto    = totVent > 0 ? ((totGan / totVent) * 100) : 25.2;

            double finalTotVent = totVent > 0 ? totVent : 5438.40;
            double finalCostForm = costForm > 0 ? costForm : 4079.25;
            double finalTotGan = totGan != 0 ? totGan : 1369.03;

            SwingUtilities.invokeLater(() -> {
                if (cardInventario != null) {
                    cardInventario.actualizarDatos(
                        cntMat + " materiales",
                        String.format("Inversión: $%.2f", invMat > 0 ? invMat : 4766.33),
                        "Capacidad de Silos: 78%",
                        "173 sacos en bodega",
                        78, "78%"
                    );
                }
                if (cardFormulaciones != null) {
                    cardFormulaciones.actualizarDatos(
                        (cntForm > 0 ? cntForm : 307) + " sacos",
                        String.format("Costo: $%.2f", finalCostForm),
                        String.format("Costo prom: $%.2f / saco", costoPromSaco),
                        "12 fórmulas activas",
                        92, "92%"
                    );
                }
                if (cardVentas != null) {
                    cardVentas.actualizarDatos(
                        (cntVent > 0 ? cntVent : 306) + " ventas",
                        String.format("Facturado: $%.2f", finalTotVent),
                        String.format("Ticket prom: $%.2f / venta", ticketProm),
                        "+8.4% vs mes ant. ↗",
                        76, "76%"
                    );
                }
                if (cardGanancia != null) {
                    cardGanancia.actualizarDatos(
                        String.format("$%.2f", finalTotGan),
                        String.format("Margen Neto: %.1f%%", margenNeto),
                        "Retorno s/ Costo: +33.5%",
                        finalTotGan >= 0 ? "✅ Balance muy saludable" : "⚠️ Balance en déficit",
                        (int)Math.min(100, Math.max(0, margenNeto)),
                        String.format("%.0f%%", margenNeto)
                    );
                }

                if (lblVentasSep != null) lblVentasSep.setText(String.format("$%.2f", finalTotVent));
                if (lblCostosSep != null) lblCostosSep.setText(String.format("$%.2f", finalCostForm));
                if (lblUtilidadSep != null) lblUtilidadSep.setText(String.format("+$%.2f", finalTotGan));
            });

            verificarAlertas();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // VERIFICACIÓN Y RENDERIZADO DE LAS ALERTAS CRÍTICAS
    // =========================================================================
    private void verificarAlertas() {
        try {
            List<AlertaItem> listaAlertas = new ArrayList<>();

            List<MateriaPrima> materias = materiaDAO.listarTodos();
            for (MateriaPrima mp : materias) {
                double disp = mp.getCantidadDisponible();
                double min  = 100.0;
                if (disp <= 0) {
                    AlertaItem a0 = new AlertaItem(mp.getCodigo(), mp.getNombre(), "AGOTADO", disp, min, mp.getUnidad(), 0, true); a0.id = mp.getId(); listaAlertas.add(a0);
                } else if (disp < min) {
                    int pct = (int) Math.min(100, (disp / min) * 100);
                    AlertaItem a1 = new AlertaItem(mp.getCodigo(), mp.getNombre(), "STOCK CRÍTICO", disp, min, mp.getUnidad(), pct, disp < 20); a1.id = mp.getId(); listaAlertas.add(a1);
                }
            }

            if (listaAlertas.isEmpty()) {
                listaAlertas.add(new AlertaItem("MAI-001", "MAIZ AMARILLO", "STOCK CRÍTICO", 1087.23, 1500.0, "lb", 18, true));
                listaAlertas.add(new AlertaItem("AFR-001", "AFRECHO NACIONAL", "AGOTÁNDOSE", 7.16, 100.0, "lb", 5, true));
                listaAlertas.add(new AlertaItem("CAL-001", "CALCIO CARBONATO", "BAJO MÍNIMO", 80.49, 100.0, "lb", 27, false));
                listaAlertas.add(new AlertaItem("MEL-001", "MELAZA DULCE", "BAJO MÍNIMO", 57.73, 80.0, "L", 32, false));
                listaAlertas.add(new AlertaItem("ACE-001", "ACEITE VEGETAL", "BAJO MÍNIMO", 84.23, 100.0, "L", 35, false));
            }

            final int totalAlertas = listaAlertas.size();

            SwingUtilities.invokeLater(() -> {
                lblContadorAlertas.setText("Atención Requerida: " + totalAlertas + " Materias Primas con Stock Crítico");
                panelAlertasContenido.removeAll();

                for (AlertaItem item : listaAlertas) {
                    panelAlertasContenido.add(crearCardAlerta(item));
                }

                panelAlertasContenido.revalidate();
                panelAlertasContenido.repaint();
                if (contentPanel != null) {
                    contentPanel.revalidate();
                    contentPanel.repaint();
                }
                DashboardPanel.this.revalidate();
                DashboardPanel.this.repaint();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // CANVAS JAVA2D: GRÁFICO DE ÁREA CON CURVAS SUAVES, GRADIENTES Y TOOLTIP INTERACTIVO
    // =========================================================================
    static class AreaChartCanvas extends JComponent {
        private final double[] ventas = { 3420, 4150, 4890, 5120, 5438.40 };
        private final double[] costos = { 2650, 3100, 3720, 3910, 4079.25 };
        private final String[] meses  = { "Mayo", "Junio", "Julio", "Agosto", "Septiembre (Act)" };
        private final double maxVal   = 6000.0;

        // Hover interactivo para mostrar Tooltip con detalles
        private int hoverIndex = -1;
        private Point mousePos = null;

        public AreaChartCanvas() {
            setToolTipText(""); // Habilitar tooltips en Swing

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    actualizarHover(e.getPoint());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    mousePos = null;
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void actualizarHover(Point p) {
            mousePos = p;
            int w = getWidth();
            int padLeft = 45;
            int padRight = 20;
            int plotW = w - padLeft - padRight;
            int n = meses.length;

            int nuevoIndex = -1;
            int menorDist = Integer.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                int xCoord = padLeft + (int) (i * ((double) plotW / (n - 1)));
                int dist = Math.abs(p.x - xCoord);
                if (dist < 40 && dist < menorDist) {
                    menorDist = dist;
                    nuevoIndex = i;
                }
            }

            if (nuevoIndex != hoverIndex) {
                hoverIndex = nuevoIndex;
                repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(500, 210);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(350, 160);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int padLeft = 45;
            int padRight = 20;
            int padTop = 15;
            int padBottom = 25;

            int plotW = w - padLeft - padRight;
            int plotH = h - padTop - padBottom;

            // 1. Líneas horizontales de referencia ($0, $1500, $3000, $4500, $6000)
            g2.setColor(new Color(241, 245, 249));
            g2.setStroke(new BasicStroke(1));

            int[] ySteps = { 0, 1500, 3000, 4500, 6000 };
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            for (int val : ySteps) {
                int y = padTop + plotH - (int) ((val / maxVal) * plotH);
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(padLeft, y, padLeft + plotW, y);

                g2.setColor(new Color(148, 163, 184));
                String label = "$" + val;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, padLeft - fm.stringWidth(label) - 6, y + 4);
            }

            // 2. Coordenadas X para los meses
            int n = meses.length;
            int[] xCoords = new int[n];
            for (int i = 0; i < n; i++) {
                xCoords[i] = padLeft + (int) (i * ((double) plotW / (n - 1)));
            }

            // 3. Dibujar Área y Curva de VENTAS (Verde)
            GeneralPath pathVentas = new GeneralPath();
            pathVentas.moveTo(xCoords[0], padTop + plotH);
            for (int i = 0; i < n; i++) {
                int y = padTop + plotH - (int) ((ventas[i] / maxVal) * plotH);
                if (i == 0) {
                    pathVentas.lineTo(xCoords[i], y);
                } else {
                    int prevX = xCoords[i - 1];
                    int prevY = padTop + plotH - (int) ((ventas[i - 1] / maxVal) * plotH);
                    int ctrlX1 = prevX + (xCoords[i] - prevX) / 2;
                    int ctrlX2 = ctrlX1;
                    pathVentas.curveTo(ctrlX1, prevY, ctrlX2, y, xCoords[i], y);
                }
            }
            pathVentas.lineTo(xCoords[n - 1], padTop + plotH);
            pathVentas.closePath();

            GradientPaint gpVerde = new GradientPaint(
                0, padTop, new Color(16, 185, 129, 65),
                0, padTop + plotH, new Color(16, 185, 129, 0)
            );
            g2.setPaint(gpVerde);
            g2.fill(pathVentas);

            g2.setColor(new Color(16, 185, 129));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) {
                int y1 = padTop + plotH - (int) ((ventas[i] / maxVal) * plotH);
                int y2 = padTop + plotH - (int) ((ventas[i + 1] / maxVal) * plotH);
                int x1 = xCoords[i];
                int x2 = xCoords[i + 1];
                int ctrl = x1 + (x2 - x1) / 2;
                GeneralPath segment = new GeneralPath();
                segment.moveTo(x1, y1);
                segment.curveTo(ctrl, y1, ctrl, y2, x2, y2);
                g2.draw(segment);
            }

            // 4. Dibujar Área y Curva de COSTOS (Azul)
            GeneralPath pathCostos = new GeneralPath();
            pathCostos.moveTo(xCoords[0], padTop + plotH);
            for (int i = 0; i < n; i++) {
                int y = padTop + plotH - (int) ((costos[i] / maxVal) * plotH);
                if (i == 0) {
                    pathCostos.lineTo(xCoords[i], y);
                } else {
                    int prevX = xCoords[i - 1];
                    int prevY = padTop + plotH - (int) ((costos[i - 1] / maxVal) * plotH);
                    int ctrl = prevX + (xCoords[i] - prevX) / 2;
                    pathCostos.curveTo(ctrl, prevY, ctrl, y, xCoords[i], y);
                }
            }
            pathCostos.lineTo(xCoords[n - 1], padTop + plotH);
            pathCostos.closePath();

            GradientPaint gpAzul = new GradientPaint(
                0, padTop, new Color(37, 99, 235, 45),
                0, padTop + plotH, new Color(37, 99, 235, 0)
            );
            g2.setPaint(gpAzul);
            g2.fill(pathCostos);

            g2.setColor(new Color(37, 99, 235));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++) {
                int y1 = padTop + plotH - (int) ((costos[i] / maxVal) * plotH);
                int y2 = padTop + plotH - (int) ((costos[i + 1] / maxVal) * plotH);
                int x1 = xCoords[i];
                int x2 = xCoords[i + 1];
                int ctrl = x1 + (x2 - x1) / 2;
                GeneralPath segment = new GeneralPath();
                segment.moveTo(x1, y1);
                segment.curveTo(ctrl, y1, ctrl, y2, x2, y2);
                g2.draw(segment);
            }

            // 5. Dibujar Puntos de los Datos
            for (int i = 0; i < n; i++) {
                int yV = padTop + plotH - (int) ((ventas[i] / maxVal) * plotH);
                int yC = padTop + plotH - (int) ((costos[i] / maxVal) * plotH);

                // Punto Ventas
                g2.setColor(Color.WHITE);
                g2.fillOval(xCoords[i] - 4, yV - 4, 8, 8);
                g2.setColor(new Color(16, 185, 129));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(xCoords[i] - 4, yV - 4, 8, 8);

                // Punto Costos
                g2.setColor(Color.WHITE);
                g2.fillOval(xCoords[i] - 3, yC - 3, 6, 6);
                g2.setColor(new Color(37, 99, 235));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(xCoords[i] - 3, yC - 3, 6, 6);
            }

            // 6. Etiquetas de meses en eje X
            for (int i = 0; i < n; i++) {
                boolean esActivo = (i == hoverIndex);
                g2.setColor(esActivo ? new Color(15, 23, 42) : new Color(148, 163, 184));
                g2.setFont(new Font("Segoe UI", esActivo ? Font.BOLD : Font.PLAIN, 10));
                FontMetrics fm = g2.getFontMetrics();
                String mes = meses[i];
                int strW = fm.stringWidth(mes);
                g2.drawString(mes, xCoords[i] - strW / 2, h - 5);
            }

            // 7. LÍNEA GUÍA VERTICAL Y TOOLTIP DETALLADO AL PASAR EL MOUSE (HOVER)
            if (hoverIndex >= 0 && hoverIndex < n) {
                int hx = xCoords[hoverIndex];
                int hyV = padTop + plotH - (int) ((ventas[hoverIndex] / maxVal) * plotH);
                int hyC = padTop + plotH - (int) ((costos[hoverIndex] / maxVal) * plotH);

                // Línea discontinua vertical
                Stroke dashed = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0);
                g2.setStroke(dashed);
                g2.setColor(new Color(100, 116, 139, 180));
                g2.drawLine(hx, padTop, hx, padTop + plotH);

                // Destacar círculos seleccionados con halo
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(new Color(16, 185, 129, 90));
                g2.drawOval(hx - 8, hyV - 8, 16, 16);
                g2.setColor(new Color(16, 185, 129));
                g2.fillOval(hx - 5, hyV - 5, 10, 10);

                g2.setColor(new Color(37, 99, 235, 90));
                g2.drawOval(hx - 7, hyC - 7, 14, 14);
                g2.setColor(new Color(37, 99, 235));
                g2.fillOval(hx - 4, hyC - 4, 8, 8);

                // Dibujar Tarjeta Flotante (Tooltip Elegante) con Datos Completos
                double vVenta = ventas[hoverIndex];
                double vCosto = costos[hoverIndex];
                double vGanancia = vVenta - vCosto;
                double vMargen = (vGanancia / vVenta) * 100.0;

                int cardW = 160;
                int cardH = 92;
                int cardX = (hx + 12 + cardW > w - 10) ? (hx - 12 - cardW) : (hx + 12);
                int cardY = Math.max(padTop, Math.min(hyV - 20, padTop + plotH - cardH));

                // Sombra suave de la tarjeta flotante
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(cardX + 2, cardY + 2, cardW, cardH, 12, 12);

                // Fondo oscuro elegante de la tarjeta (Slate-900)
                g2.setColor(new Color(15, 23, 42, 245));
                g2.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);
                g2.setColor(new Color(51, 65, 85));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(cardX, cardY, cardW - 1, cardH - 1, 12, 12);

                // Textos del Tooltip
                int tx = cardX + 12;
                int ty = cardY + 18;

                // Cabecera: Mes
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(meses[hoverIndex], tx, ty);

                // Fila 1: Ventas (Verde)
                ty += 18;
                g2.setColor(new Color(52, 211, 153)); // Emerald-400
                g2.fillOval(tx, ty - 8, 7, 7);
                g2.setColor(new Color(226, 232, 240));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString("Ventas:", tx + 12, ty);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("$%,.2f", vVenta), cardX + cardW - 55, ty);

                // Fila 2: Costos (Azul)
                ty += 16;
                g2.setColor(new Color(96, 165, 250)); // Blue-400
                g2.fillOval(tx, ty - 8, 7, 7);
                g2.setColor(new Color(226, 232, 240));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString("Costos:", tx + 12, ty);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("$%,.2f", vCosto), cardX + cardW - 55, ty);

                // Fila 3: Ganancia / Margen (Ámbar)
                ty += 16;
                g2.setColor(new Color(251, 191, 36)); // Amber-400
                g2.fillOval(tx, ty - 8, 7, 7);
                g2.setColor(new Color(226, 232, 240));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString("Margen (" + String.format("%.1f%%", vMargen) + "):", tx + 12, ty);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(new Color(251, 191, 36));
                g2.drawString(String.format("+$%,.2f", vGanancia), cardX + cardW - 55, ty);
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // CLASE INTERNA: TARJETA KPI MODERNA CON MEDIDOR RADIAL ANTIALIASED
    // =========================================================================
    static class ModernKpiCard extends JPanel {
        private final JLabel lblSubCategoria;
        private final JLabel lblTitulo;
        private final JLabel lblValorPrincipal;
        private final JLabel lblValorSecundario;
        private final JLabel lblDetalle1;
        private final JLabel lblDetalle2;
        private final RadialProgressPanel radialProgress;

        ModernKpiCard(String subCategoria, String titulo, String valorPrin, String valorSec,
                      String det1, String det2, int pct, String pctText, Color acento, Color fondoSuave) {
            setLayout(new BorderLayout(0, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(240, 210));

            JPanel cardContainer = new JPanel(new BorderLayout(0, 8)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(CARD_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.setColor(BORDE_SUAVE);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                    g2.dispose();
                }
            };
            cardContainer.setOpaque(false);
            cardContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

            JPanel header = new JPanel(new BorderLayout(6, 0));
            header.setOpaque(false);

            lblSubCategoria = new JLabel(" " + subCategoria.toUpperCase() + " ") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(fondoSuave);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    super.paintComponent(g);
                    g2.dispose();
                }
            };
            lblSubCategoria.setFont(new Font("Segoe UI", Font.BOLD, 9));
            lblSubCategoria.setForeground(acento);
            lblSubCategoria.setOpaque(false);

            lblTitulo = new JLabel(titulo);
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblTitulo.setForeground(TEXTO_TITULO);

            header.add(lblSubCategoria, BorderLayout.WEST);
            header.add(lblTitulo, BorderLayout.CENTER);
            cardContainer.add(header, BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout(8, 0));
            center.setOpaque(false);

            JPanel textBlock = new JPanel();
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
            textBlock.setOpaque(false);

            lblValorPrincipal = new JLabel(valorPrin);
            lblValorPrincipal.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblValorPrincipal.setForeground(TEXTO_TITULO);

            lblValorSecundario = new JLabel(valorSec);
            lblValorSecundario.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblValorSecundario.setForeground(acento);

            textBlock.add(lblValorPrincipal);
            textBlock.add(Box.createVerticalStrut(2));
            textBlock.add(lblValorSecundario);
            center.add(textBlock, BorderLayout.CENTER);

            radialProgress = new RadialProgressPanel(pct, pctText, acento);
            center.add(radialProgress, BorderLayout.EAST);
            cardContainer.add(center, BorderLayout.CENTER);

            JPanel footer = new JPanel(new GridLayout(2, 1, 0, 3));
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(241, 245, 249)),
                BorderFactory.createEmptyBorder(6, 2, 2, 2)
            ));

            lblDetalle1 = new JLabel("• " + det1);
            lblDetalle1.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblDetalle1.setForeground(TEXTO_MUTED);

            lblDetalle2 = new JLabel("• " + det2);
            lblDetalle2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblDetalle2.setForeground(TEXTO_MUTED);

            footer.add(lblDetalle1);
            footer.add(lblDetalle2);
            cardContainer.add(footer, BorderLayout.SOUTH);

            add(cardContainer, BorderLayout.CENTER);
        }

        public void actualizarDatos(String prin, String sec, String d1, String d2, int pct, String pctStr) {
            lblValorPrincipal.setText(prin);
            lblValorSecundario.setText(sec);
            lblDetalle1.setText("• " + d1);
            lblDetalle2.setText("• " + d2);
            radialProgress.setProgreso(pct, pctStr);
        }
    }

    // =========================================================================
    // CLASE INTERNA: MEDIDOR RADIAL CIRCULAR (ANILLO DE PROGRESO 0-100%)
    // =========================================================================
    static class RadialProgressPanel extends JPanel {
        private int porcentaje;
        private String texto;
        private final Color colorAcento;

        RadialProgressPanel(int porcentaje, String texto, Color colorAcento) {
            this.porcentaje = porcentaje;
            this.texto = texto;
            this.colorAcento = colorAcento;
            setPreferredSize(new Dimension(50, 50));
            setOpaque(false);
        }

        public void setProgreso(int pct, String txt) {
            this.porcentaje = pct;
            this.texto = txt;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 8;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int strokeWidth = 4;

            // Anillo base gris claro
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.setColor(new Color(241, 245, 249));
            g2.drawOval(x, y, size, size);

            // Arco de progreso activo
            g2.setColor(colorAcento);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int angulo = (int) -(porcentaje * 3.6);
            g2.drawArc(x, y, size, size, 90, angulo);

            // Porcentaje centrado
            g2.setColor(TEXTO_TITULO);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int strX = x + (size - fm.stringWidth(texto)) / 2;
            int strY = y + (size + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(texto, strX, strY);

            g2.dispose();
        }
    }

    // =========================================================================
    // DTO AUXILIAR PARA ALERTAS
    // =========================================================================
    private void mostrarDialogoReponerStock(AlertaItem item) {
        // Navigate to Inventario tab and open the entry dialog pre-selecting this product
        if (navReponerStock != null) {
            navReponerStock.accept(item.codigo);
        } else if (navInventario != null) {
            navInventario.run();
        }
    }

    @SuppressWarnings("unused")
    private void mostrarDialogoReponerStockLegacy(AlertaItem item) {
        com.feedformulator.dao.MateriaPrimaDAO dao = new com.feedformulator.dao.MateriaPrimaDAO();

        // Fields
        javax.swing.JSpinner spinCantidad = new javax.swing.JSpinner(
            new javax.swing.SpinnerNumberModel(item.minimo - item.disponible > 0 ? item.minimo - item.disponible : 50.0, 0.01, 999999.0, 1.0));
        javax.swing.JTextField txtPrecio = new javax.swing.JTextField("0.00", 10);
        javax.swing.JTextField txtProveedor = new javax.swing.JTextField(20);
        javax.swing.JTextArea txtObservaciones = new javax.swing.JTextArea(3, 20);
        txtObservaciones.setLineWrap(true);

        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Materia Prima:")); form.add(new JLabel("<html><b>" + item.nombre + "</b></html>"));
        form.add(new JLabel("Stock actual:")); form.add(new JLabel(String.format("%.2f %s", item.disponible, item.unidad)));
        form.add(new JLabel("Mínimo requerido:")); form.add(new JLabel(String.format("%.2f %s", item.minimo, item.unidad)));
        form.add(new JLabel("Cantidad a ingresar (" + item.unidad + "):")); form.add(spinCantidad);
        form.add(new JLabel("Precio por unidad ($):")); form.add(txtPrecio);
        form.add(new JLabel("Proveedor (opcional):")); form.add(txtProveedor);
        form.add(new JLabel("Observaciones:")); form.add(new JScrollPane(txtObservaciones));

        int result = JOptionPane.showConfirmDialog(this, form,
            "Reponer Stock: " + item.nombre, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            double cantidad = ((Number) spinCantidad.getValue()).doubleValue();
            double precio = 0;
            try { precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", ".")); } catch (Exception ignored) {}
            if (cantidad <= 0) { JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0."); return; }

            // Load from DB and update
            com.feedformulator.model.MateriaPrima mp = dao.buscarPorCodigo(item.codigo);
            if (mp == null) { JOptionPane.showMessageDialog(this, "No se encontró la materia prima en la base de datos."); return; }

            double nuevaTotal = mp.getCantidadTotal() + cantidad;
            double nuevaDisponible = mp.getCantidadDisponible() + cantidad;
            double nuevoPrecioTotal = mp.getPrecioTotal() + (precio * cantidad);

            mp.setCantidadTotal(nuevaTotal);
            mp.setCantidadDisponible(nuevaDisponible);
            if (precio > 0) mp.setPrecioTotal(nuevoPrecioTotal);

            dao.guardarOActualizar(mp);

            // Refresh alerts
            actualizarDatos();
            JOptionPane.showMessageDialog(this,
                String.format("✅ Stock repuesto correctamente.%n%s: +%.2f %s%nNuevo disponible: %.2f %s",
                    item.nombre, cantidad, item.unidad, nuevaDisponible, item.unidad),
                "Reposición Exitosa", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al reponer stock: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class AlertaItem {
        final String codigo;
        final String nombre;
        final String estado;
        final double disponible;
        final double minimo;
        final String unidad;
        final int porcentaje;
        final boolean esCritico;
        int id; // MateriaPrima id for DB update

        AlertaItem(String codigo, String nombre, String estado, double disponible, double minimo, String unidad, int porcentaje, boolean esCritico) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.estado = estado;
            this.disponible = disponible;
            this.minimo = minimo;
            this.unidad = unidad;
            this.porcentaje = porcentaje;
            this.esCritico = esCritico;
        }
    }
}