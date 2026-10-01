package com.feedformulator.ui;

import com.feedformulator.dao.VentaDAO;
import com.feedformulator.dao.VentaCreditoDAO;
import com.feedformulator.dao.FormulacionDAO;
import com.feedformulator.model.Venta;
import com.feedformulator.model.VentaCredito;
import com.feedformulator.model.Formulacion;
import com.feedformulator.model.DetalleFormulacion;
import com.feedformulator.report.ReporteGenerator;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel Corporativo de Reportes y Análisis de Ventas.
 * Diseño ejecutivo con tarjetas métricas, gráficas vectoriales interactivas con animación y tooltips flotantes.
 */
public class ReportesPanel extends JPanel {
    private final VentaDAO ventaDAO = new VentaDAO();
    private final VentaCreditoDAO ventaCreditoDAO = new VentaCreditoDAO();
    private final FormulacionDAO formulacionDAO = new FormulacionDAO();

    private JDateChooser dateInicio;
    private JDateChooser dateFin;
    private JButton btnGenerar;
    private JButton btnExportarPDF;

    // ---- Resumen ----
    private JLabel lblTotalVentas;
    private JLabel lblTotalGanancia;
    private JLabel lblPromedioDiario;
    private JLabel lblDias;
    private JLabel lblVentasCredito;
    private JLabel lblSubVentas, lblSubCredito, lblSubGanancia, lblSubPromedio, lblSubDias;
    private JPanel graficoBarrasPanel;
    private JPanel graficoLineaPanel;

    // Control para renderizado limpio de PDF
    private boolean exportandoPDF = false;

    // Paneles con lógica de animación expuesta
    private GraficoBarrasAnimado panelBarrasActual;
    private GraficoLineaAnimado panelLineaActual;

    // ---- Top productos ----
    private DefaultTableModel modelTopProductos;
    private JTable tablaTopProductos;

    // ---- Materiales más usados ----
    private DefaultTableModel modelTopMateriales;
    private JTable tablaTopMateriales;

    // ---- Comparativa ----
    private JCheckBox chkComparar;
    private JLabel lblRangoAnteriorInfo;
    private JLabel lblCompVentasActual, lblCompVentasAnterior, lblCompVentasDelta;
    private JLabel lblCompGananciaActual, lblCompGananciaAnterior, lblCompGananciaDelta;
    private JLabel lblCompPromedioActual, lblCompPromedioAnterior, lblCompPromedioDelta;

    // Datos del último reporte generado
    private String ultimoInicioStr, ultimoFinStr;

    // PALETA CORPORATIVA EJECUTIVA
    private static final Color VERDE_ESMERALDA = new Color(5, 150, 105);   // #059669
    private static final Color VERDE_OSCURO = new Color(4, 120, 87);       // #047857
    private static final Color TEAL_ENCABEZADO = new Color(15, 118, 110);  // #0F766E
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);       // #0F172A
    private static final Color AZUL_INDIGO = new Color(37, 99, 235);       // #2563EB
    private static final Color PURPURA = new Color(126, 34, 206);          // #7E22CE
    private static final Color AMBAR = new Color(217, 119, 6);             // #D97706
    private static final Color ROJO_CARMESI = new Color(220, 38, 38);      // #DC2626
    private static final Color FONDO = new Color(248, 250, 252);           // #F8FAFC
    private static final Color BORDE_SUAVE = new Color(226, 232, 240);     // #E2E8F0
    private static final Color TEXTO_MUTED = new Color(100, 116, 139);     // #64748B

    // Estructura para datos agrupados de los gráficos
    private static class DiaGraficoInfo {
        String fechaCompleta;
        String fechaCorta;
        double ventas;
        double ganancia;
        int transacciones;

        DiaGraficoInfo(String fechaCompleta, String fechaCorta) {
            this.fechaCompleta = fechaCompleta;
            this.fechaCorta = fechaCorta;
            this.ventas = 0.0;
            this.ganancia = 0.0;
            this.transacciones = 0;
        }
    }

    private static class ProductoStats {
        String nombre;
        int cantidadVentas;
        double ingresos;
        double ganancia;
        double porcentaje;
    }

    private static class MaterialStats {
        String nombre;
        String unidad;
        double cantidadUsada;
        double costoTotal;
        double porcentaje;
    }

    public ReportesPanel() {
        setLayout(new BorderLayout(14, 14));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        add(crearPanelTitulo(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);

        LocalDate hoy = LocalDate.now();
        dateFin.setDate(Date.from(hoy.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dateInicio.setDate(Date.from(hoy.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        generarReporte();
    }

    private JPanel crearPanelTitulo() {
        return DialogStyler.crearHeaderPagina("📊", "Reportes y Rendimiento de Ventas",
            "Métricas clave, márgenes de ganancia, materias primas consumidas y comparativas");
    }

    // ============================================================
    // ESTRUCTURA PRINCIPAL: FILTROS + PESTAÑAS
    // ============================================================
    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        panel.add(crearPanelFiltros(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("📈 Resumen Ejecutivo", crearTabResumen());
        tabs.addTab("🏆 Top Productos", crearTabTopProductos());
        tabs.addTab("🌾 Insumos Consumidos", crearTabTopMateriales());
        tabs.addTab("⚖️ Comparativa Períodos", crearTabComparativa());

        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelFiltros() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JPanel fechaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        fechaPanel.setOpaque(false);

        JLabel lblInicio = new JLabel("Fecha Inicio:");
        lblInicio.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblInicio.setForeground(SLATE_OSCURO);
        try {
            ImageIcon iconCalendar = new ImageIcon(getClass().getResource("/icons/calendar-icon.png"));
            Image imgCalendar = iconCalendar.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            lblInicio.setIcon(new ImageIcon(imgCalendar));
        } catch (Exception e) {
            lblInicio.setText("📅 Fecha Inicio:");
        }
        fechaPanel.add(lblInicio);

        dateInicio = new JDateChooser();
        dateInicio.setPreferredSize(new Dimension(135, 32));
        dateInicio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fechaPanel.add(dateInicio);

        JLabel lblFin = new JLabel("Fecha Fin:");
        lblFin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFin.setForeground(SLATE_OSCURO);
        try {
            ImageIcon iconCalendar = new ImageIcon(getClass().getResource("/icons/calendar-icon.png"));
            Image imgCalendar = iconCalendar.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            lblFin.setIcon(new ImageIcon(imgCalendar));
        } catch (Exception ex) {
            lblFin.setText("📅 Fecha Fin:");
        }
        fechaPanel.add(lblFin);

        dateFin = new JDateChooser();
        dateFin.setPreferredSize(new Dimension(135, 32));
        dateFin.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fechaPanel.add(dateFin);

        btnGenerar = crearBotonPildora("Generar Reporte", VERDE_ESMERALDA, "/icons/report.png", "📊");
        btnGenerar.addActionListener(evt -> generarReporte());
        fechaPanel.add(btnGenerar);

        btnExportarPDF = crearBotonPildora("Exportar PDF", PURPURA, "/icons/pdf-icon.png", "📄");
        btnExportarPDF.addActionListener(evt -> exportarReportePDF());
        fechaPanel.add(btnExportarPDF);

        panel.add(fechaPanel, BorderLayout.CENTER);

        // Barra inferior: Píldoras de períodos rápidos
        JPanel btnRapidos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRapidos.setOpaque(false);

        JLabel lblRapidos = new JLabel("Atajos:");
        lblRapidos.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblRapidos.setForeground(TEXTO_MUTED);
        btnRapidos.add(lblRapidos);

        String[] periodos = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Este mes"};
        for (String periodo : periodos) {
            JButton btn = new JButton(periodo);
            btn.setBackground(new Color(241, 245, 249));
            btn.setForeground(new Color(30, 41, 59));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE_SUAVE, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
            ));
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btn.setBackground(new Color(226, 232, 240));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(new Color(241, 245, 249));
                }
            });
            btn.addActionListener(evt -> setPeriodoRapido(periodo));
            btnRapidos.add(btn);
        }

        panel.add(btnRapidos, BorderLayout.SOUTH);
        return panel;
    }

    private JButton crearBotonPildora(String texto, Color fondo, String iconoPath, String fallback) {
        JButton btn = new JButton(texto);
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconoPath));
            Image img = icon.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            btn.setText(fallback + " " + texto);
        }
        btn.setBackground(fondo);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btn.setBackground(fondo.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btn.setBackground(fondo); }
        });
        return btn;
    }

    private void setPeriodoRapido(String periodo) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy;

        switch (periodo) {
            case "Hoy": inicio = hoy; break;
            case "Últimos 7 días": inicio = hoy.minusDays(6); break;
            case "Últimos 30 días": inicio = hoy.minusDays(29); break;
            case "Este mes": inicio = hoy.withDayOfMonth(1); break;
        }

        dateInicio.setDate(Date.from(inicio.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dateFin.setDate(Date.from(hoy.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        generarReporte();
    }

    // ============================================================
    // PESTAÑA: RESUMEN EJECUTIVO (TARJETAS + MODAL DE GRÁFICAS)
    // ============================================================
    private JPanel crearTabResumen() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel resumenPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        resumenPanel.setOpaque(false);

        lblTotalVentas = new JLabel("$0.00");
        lblTotalGanancia = new JLabel("$0.00");
        lblPromedioDiario = new JLabel("$0.00");
        lblDias = new JLabel("0");
        lblVentasCredito = new JLabel("$0.00");

        lblSubVentas = new JLabel(" ");
        lblSubCredito = new JLabel(" ");
        lblSubGanancia = new JLabel(" ");
        lblSubPromedio = new JLabel("Por día hábil");
        lblSubDias = new JLabel(" ");

        resumenPanel.add(crearTarjetaResumen("💰", "TOTAL VENTAS", lblTotalVentas, AZUL_INDIGO, new Color(239, 246, 255), lblSubVentas));
        resumenPanel.add(crearTarjetaResumen("🧾", "VENTAS A CRÉDITO", lblVentasCredito, PURPURA, new Color(250, 245, 255), lblSubCredito));
        resumenPanel.add(crearTarjetaResumen("📈", "GANANCIA NETA", lblTotalGanancia, VERDE_ESMERALDA, new Color(236, 253, 245), lblSubGanancia));
        resumenPanel.add(crearTarjetaResumen("📊", "PROMEDIO DIARIO", lblPromedioDiario, AMBAR, new Color(254, 243, 199), lblSubPromedio));
        resumenPanel.add(crearTarjetaResumen("📅", "DÍAS DEL PERÍODO", lblDias, SLATE_OSCURO, new Color(241, 245, 249), lblSubDias));

        graficoBarrasPanel = new JPanel(new BorderLayout());
        graficoBarrasPanel.setBackground(Color.WHITE);
        graficoBarrasPanel.setPreferredSize(new Dimension(950, 430));

        graficoLineaPanel = new JPanel(new BorderLayout());
        graficoLineaPanel.setBackground(Color.WHITE);
        graficoLineaPanel.setPreferredSize(new Dimension(950, 430));

        // Tarjeta central elegante para abrir la visualización interactiva
        JPanel bloqueBoton = new JPanel();
        bloqueBoton.setLayout(new BoxLayout(bloqueBoton, BoxLayout.Y_AXIS));
        bloqueBoton.setBackground(Color.WHITE);
        bloqueBoton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(36, 30, 36, 30)
        ));

        JLabel lblIconoGrande = new JLabel("📈", SwingConstants.CENTER);
        lblIconoGrande.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
        lblIconoGrande.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloqueBoton.add(lblIconoGrande);
        bloqueBoton.add(Box.createVerticalStrut(12));

        JLabel lblTituloGrafica = new JLabel("Análisis Visual de Ventas y Tendencia");
        lblTituloGrafica.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTituloGrafica.setForeground(SLATE_OSCURO);
        lblTituloGrafica.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloqueBoton.add(lblTituloGrafica);
        bloqueBoton.add(Box.createVerticalStrut(6));

        JLabel lblAyuda = new JLabel("Visualice la distribución de ingresos diarios con animación fluida y detalles emergentes al pasar el cursor.");
        lblAyuda.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAyuda.setForeground(TEXTO_MUTED);
        lblAyuda.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloqueBoton.add(lblAyuda);
        bloqueBoton.add(Box.createVerticalStrut(18));

        JButton btnVerGraficas = crearBotonPildora("Visualizar Gráficas Analíticas", TEAL_ENCABEZADO, "/icons/chart-icon.png", "📊");
        btnVerGraficas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnVerGraficas.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        btnVerGraficas.addActionListener(evt -> abrirVentanaGraficas());

        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        filaBoton.setOpaque(false);
        filaBoton.add(btnVerGraficas);
        filaBoton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloqueBoton.add(filaBoton);

        JPanel central = new JPanel(new BorderLayout(0, 14));
        central.setOpaque(false);
        central.add(resumenPanel, BorderLayout.NORTH);
        central.add(bloqueBoton, BorderLayout.CENTER);

        panel.add(central, BorderLayout.CENTER);
        return panel;
    }

    private void abrirVentanaGraficas() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Análisis Gráfico de Ventas", Dialog.ModalityType.MODELESS);
        dialog.setSize(1040, 590);
        dialog.setLocationRelativeTo(this);

        graficoBarrasPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        graficoLineaPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JTabbedPane tabsGraficas = new JTabbedPane();
        tabsGraficas.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabsGraficas.addTab("📊 Ventas Diarias", graficoBarrasPanel);
        tabsGraficas.addTab("📈 Tendencia y Ganancia", graficoLineaPanel);

        // Cuando el usuario cambie de pestaña dentro del diálogo, disparar la animación de entrada
        tabsGraficas.addChangeListener(e -> {
            int selected = tabsGraficas.getSelectedIndex();
            if (selected == 0 && panelBarrasActual != null) {
                panelBarrasActual.iniciarAnimacion();
            } else if (selected == 1 && panelLineaActual != null) {
                panelLineaActual.iniciarAnimacion();
            }
        });

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        contenedor.add(tabsGraficas, BorderLayout.CENTER);

        dialog.setLayout(new BorderLayout());
        dialog.add(contenedor, BorderLayout.CENTER);

        // Disparar la animación de entrada al abrir la ventana
        if (panelBarrasActual != null) {
            panelBarrasActual.iniciarAnimacion();
        }

        dialog.setVisible(true);

        graficoBarrasPanel.revalidate();
        graficoBarrasPanel.repaint();
        graficoLineaPanel.revalidate();
        graficoLineaPanel.repaint();
    }

    /** Tarjeta métrica sin bordes toscos laterales: diseño balanceado con cápsula */
    private JPanel crearTarjetaResumen(String icono, String titulo, JLabel labelValor, Color colorAcento, Color fondoIcono, JLabel labelSubtitulo) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        JPanel topFila = new JPanel(new BorderLayout(8, 0));
        topFila.setOpaque(false);

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTit.setForeground(TEXTO_MUTED);
        topFila.add(lblTit, BorderLayout.CENTER);

        // Cápsula pequeña para el icono
        JPanel capsule = new JPanel(new GridBagLayout());
        capsule.setPreferredSize(new Dimension(28, 28));
        capsule.setBackground(fondoIcono);
        capsule.setBorder(BorderFactory.createLineBorder(new Color(colorAcento.getRed(), colorAcento.getGreen(), colorAcento.getBlue(), 60), 1));
        JLabel lblIco = new JLabel(icono);
        lblIco.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        capsule.add(lblIco);
        topFila.add(capsule, BorderLayout.EAST);

        card.add(topFila, BorderLayout.NORTH);

        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 20));
        labelValor.setForeground(colorAcento);
        card.add(labelValor, BorderLayout.CENTER);

        if (labelSubtitulo != null) {
            labelSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            labelSubtitulo.setForeground(TEXTO_MUTED);
            card.add(labelSubtitulo, BorderLayout.SOUTH);
        }

        return card;
    }

    // ============================================================
    // PESTAÑA: TOP PRODUCTOS (ESTILO CORPORATIVO SLATE)
    // ============================================================
    private JPanel crearTabTopProductos() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JLabel titulo = new JLabel("🏆 Productos con mayor volumen de facturación en el período");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(SLATE_OSCURO);
        panel.add(titulo, BorderLayout.NORTH);

        modelTopProductos = new DefaultTableModel(
            new Object[]{"#", "Producto", "Ventas", "Ingresos", "Ganancia", "% del Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaTopProductos = new JTable(modelTopProductos);
        estilizarTablaReportes(tablaTopProductos);

        JScrollPane scroll = new JScrollPane(tablaTopProductos);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void actualizarTopProductos(List<Venta> ventasFiltradas, double totalVentas) {
        Map<String, ProductoStats> mapa = new LinkedHashMap<>();
        for (Venta v : ventasFiltradas) {
            ProductoStats ps = mapa.computeIfAbsent(v.getNombreProducto(), nombre -> {
                ProductoStats p = new ProductoStats();
                p.nombre = nombre;
                return p;
            });
            ps.cantidadVentas++;
            ps.ingresos += v.getPrecioVenta();
            ps.ganancia += v.getGanancia();
        }

        List<ProductoStats> lista = new ArrayList<>(mapa.values());
        for (ProductoStats ps : lista) {
            ps.porcentaje = totalVentas > 0 ? (ps.ingresos / totalVentas) * 100.0 : 0;
        }
        lista.sort((a, b) -> Double.compare(b.ingresos, a.ingresos));

        modelTopProductos.setRowCount(0);
        int rank = 1;
        for (ProductoStats ps : lista) {
            String posicion = rank == 1 ? "🥇 1" : rank == 2 ? "🥈 2" : rank == 3 ? "🥉 3" : String.valueOf(rank);
            modelTopProductos.addRow(new Object[]{
                posicion,
                ps.nombre,
                ps.cantidadVentas,
                String.format("$%.2f", ps.ingresos),
                String.format("$%.2f", ps.ganancia),
                String.format("%.1f%%", ps.porcentaje)
            });
            rank++;
        }
    }

    // ============================================================
    // PESTAÑA: MATERIALES MÁS USADOS (ESTILO CORPORATIVO SLATE)
    // ============================================================
    private JPanel crearTabTopMateriales() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JLabel titulo = new JLabel("🌾 Materias primas y microingredientes más consumidos en el período");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titulo.setForeground(SLATE_OSCURO);
        panel.add(titulo, BorderLayout.NORTH);

        modelTopMateriales = new DefaultTableModel(
            new Object[]{"#", "Material", "Cantidad Consumida", "Costo Total", "% del Costo de Insumos"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaTopMateriales = new JTable(modelTopMateriales);
        estilizarTablaReportes(tablaTopMateriales);

        JScrollPane scroll = new JScrollPane(tablaTopMateriales);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void actualizarTopMateriales(List<Venta> ventasFiltradas) {
        Map<String, MaterialStats> mapa = new LinkedHashMap<>();
        double costoTotalInsumos = 0;
        Map<Integer, Formulacion> cache = new HashMap<>();

        for (Venta v : ventasFiltradas) {
            try {
                Formulacion f = cache.get(v.getFormulacionId());
                if (f == null) {
                    f = formulacionDAO.obtenerPorId(v.getFormulacionId());
                    if (f != null) cache.put(v.getFormulacionId(), f);
                }
                if (f == null || f.getDetalles() == null) continue;

                for (DetalleFormulacion d : f.getDetalles()) {
                    boolean esLiquido = "litro".equalsIgnoreCase(d.getUnidadOriginal()) || "litros".equalsIgnoreCase(d.getUnidadOriginal());
                    double cantidad = (esLiquido && d.getCantidadOriginal() > 0) ? d.getCantidadOriginal() : d.getCantidadUsada();
                    String unidad = esLiquido ? "litro" : "lb";

                    MaterialStats ms = mapa.computeIfAbsent(d.getNombreIngrediente(), nombre -> {
                        MaterialStats m = new MaterialStats();
                        m.nombre = nombre;
                        m.unidad = unidad;
                        return m;
                    });
                    ms.cantidadUsada += cantidad;
                    ms.costoTotal += d.getSubtotal();
                    costoTotalInsumos += d.getSubtotal();
                }
            } catch (Exception ignored) {}
        }

        List<MaterialStats> lista = new ArrayList<>(mapa.values());
        for (MaterialStats ms : lista) {
            ms.porcentaje = costoTotalInsumos > 0 ? (ms.costoTotal / costoTotalInsumos) * 100.0 : 0;
        }
        lista.sort((a, b) -> Double.compare(b.cantidadUsada, a.cantidadUsada));

        modelTopMateriales.setRowCount(0);
        int rank = 1;
        for (MaterialStats ms : lista) {
            String posicion = rank == 1 ? "🥇 1" : rank == 2 ? "🥈 2" : rank == 3 ? "🥉 3" : String.valueOf(rank);
            modelTopMateriales.addRow(new Object[]{
                posicion,
                ms.nombre,
                String.format("%.2f %s", ms.cantidadUsada, ms.unidad),
                String.format("$%.2f", ms.costoTotal),
                String.format("%.1f%%", ms.porcentaje)
            });
            rank++;
        }
    }

    private void estilizarTablaReportes(JTable tabla) {
        tabla.setRowHeight(34);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabla.getTableHeader().setBackground(SLATE_OSCURO);
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tabla.setShowGrid(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(BORDE_SUAVE);
        tabla.setSelectionBackground(new Color(209, 250, 229));
        tabla.setSelectionForeground(new Color(6, 95, 70));

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                boolean esPosicion = column == 0;
                boolean esNumero = column >= 2;

                c.setHorizontalAlignment(esPosicion ? SwingConstants.CENTER : (esNumero ? SwingConstants.RIGHT : SwingConstants.LEFT));

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : FONDO);
                    c.setForeground(new Color(30, 41, 59));
                    if (column == 3 || column == 4) { // Ingreso o ganancia
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        c.setForeground(new Color(5, 150, 105));
                    }
                }
                return c;
            }
        });
    }

    // ============================================================
    // PESTAÑA: COMPARATIVA ENTRE PERÍODOS
    // ============================================================
    private JPanel crearTabComparativa() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        chkComparar = new JCheckBox("Comparar contra el período inmediatamente anterior (misma cantidad de días)");
        chkComparar.setOpaque(false);
        chkComparar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkComparar.setForeground(SLATE_OSCURO);
        chkComparar.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkComparar.addActionListener(evt -> generarReporte());
        top.add(chkComparar);

        lblRangoAnteriorInfo = new JLabel(" ");
        lblRangoAnteriorInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRangoAnteriorInfo.setForeground(TEXTO_MUTED);
        lblRangoAnteriorInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblRangoAnteriorInfo.setBorder(BorderFactory.createEmptyBorder(4, 24, 0, 0));
        top.add(lblRangoAnteriorInfo);

        JPanel comparativaPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        comparativaPanel.setOpaque(false);
        comparativaPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        lblCompVentasActual = new JLabel("$0.00");
        lblCompVentasAnterior = new JLabel("Período anterior: $0.00");
        lblCompVentasDelta = new JLabel("▲ 0.0% vs período anterior");
        comparativaPanel.add(crearTarjetaComparativa("💰", "Total Ventas",
            lblCompVentasActual, lblCompVentasAnterior, lblCompVentasDelta, AZUL_INDIGO));

        lblCompGananciaActual = new JLabel("$0.00");
        lblCompGananciaAnterior = new JLabel("Período anterior: $0.00");
        lblCompGananciaDelta = new JLabel("▲ 0.0% vs período anterior");
        comparativaPanel.add(crearTarjetaComparativa("📈", "Ganancia Neta",
            lblCompGananciaActual, lblCompGananciaAnterior, lblCompGananciaDelta, VERDE_ESMERALDA));

        lblCompPromedioActual = new JLabel("$0.00");
        lblCompPromedioAnterior = new JLabel("Período anterior: $0.00");
        lblCompPromedioDelta = new JLabel("▲ 0.0% vs período anterior");
        comparativaPanel.add(crearTarjetaComparativa("📊", "Promedio Diario",
            lblCompPromedioActual, lblCompPromedioAnterior, lblCompPromedioDelta, AMBAR));

        JPanel central = new JPanel(new BorderLayout(0, 10));
        central.setOpaque(false);
        central.add(top, BorderLayout.NORTH);
        central.add(comparativaPanel, BorderLayout.CENTER);

        panel.add(central, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTarjetaComparativa(String icono, String titulo, JLabel actual, JLabel anterior, JLabel delta, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel lblTitulo = new JLabel(icono + " " + titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitulo.setForeground(TEXTO_MUTED);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblTitulo);
        panel.add(Box.createVerticalStrut(8));

        actual.setFont(new Font("Segoe UI", Font.BOLD, 22));
        actual.setForeground(color);
        actual.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(actual);

        anterior.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        anterior.setForeground(TEXTO_MUTED);
        anterior.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(anterior);
        panel.add(Box.createVerticalStrut(10));

        delta.setFont(new Font("Segoe UI", Font.BOLD, 12));
        delta.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(delta);

        return panel;
    }

    private void actualizarComparativa(List<Venta> todasVentas, String inicioStr, String finStr) {
        LocalDate inicio = LocalDate.parse(inicioStr);
        LocalDate fin = LocalDate.parse(finStr);
        long diasPeriodo = ChronoUnit.DAYS.between(inicio, fin) + 1;

        LocalDate finAnterior = inicio.minusDays(1);
        LocalDate inicioAnterior = finAnterior.minusDays(diasPeriodo - 1);
        String inicioAntStr = inicioAnterior.toString();
        String finAntStr = finAnterior.toString();

        List<Venta> ventasActuales = filtrarPorRango(todasVentas, inicioStr, finStr);
        List<Venta> ventasAnteriores = filtrarPorRango(todasVentas, inicioAntStr, finAntStr);

        double ventasAct = ventasActuales.stream().mapToDouble(Venta::getPrecioVenta).sum();
        double ventasAnt = ventasAnteriores.stream().mapToDouble(Venta::getPrecioVenta).sum();
        double gananciaAct = ventasActuales.stream().mapToDouble(Venta::getGanancia).sum();
        double gananciaAnt = ventasAnteriores.stream().mapToDouble(Venta::getGanancia).sum();
        double promAct = diasPeriodo > 0 ? ventasAct / diasPeriodo : 0;
        double promAnt = diasPeriodo > 0 ? ventasAnt / diasPeriodo : 0;

        actualizarTarjetaComparativa(lblCompVentasActual, lblCompVentasAnterior, lblCompVentasDelta, ventasAct, ventasAnt);
        actualizarTarjetaComparativa(lblCompGananciaActual, lblCompGananciaAnterior, lblCompGananciaDelta, gananciaAct, gananciaAnt);
        actualizarTarjetaComparativa(lblCompPromedioActual, lblCompPromedioAnterior, lblCompPromedioDelta, promAct, promAnt);

        lblRangoAnteriorInfo.setText("Período anterior comparado: " + inicioAntStr + "  a  " + finAntStr
            + "  (" + diasPeriodo + " día" + (diasPeriodo == 1 ? "" : "s") + ")");
    }

    private void actualizarTarjetaComparativa(JLabel actual, JLabel anterior, JLabel delta, double valActual, double valAnterior) {
        actual.setText(String.format("$%.2f", valActual));
        anterior.setText("Período anterior: $" + String.format("%.2f", valAnterior));

        double cambio;
        if (valAnterior != 0) {
            cambio = ((valActual - valAnterior) / Math.abs(valAnterior)) * 100.0;
        } else {
            cambio = valActual > 0 ? 100.0 : 0.0;
        }
        String flecha = cambio >= 0 ? "▲ +" : "▼ -";
        Color color = cambio >= 0 ? VERDE_ESMERALDA : ROJO_CARMESI;
        delta.setText(flecha + String.format("%.1f%%", Math.abs(cambio)) + " vs período anterior");
        delta.setForeground(color);
    }

    // ============================================================
    // CÁLCULO PRINCIPAL DEL REPORTE
    // ============================================================
    private List<Venta> filtrarPorRango(List<Venta> ventas, String inicioStr, String finStr) {
        return ventas.stream()
            .filter(v -> v.getFecha().compareTo(inicioStr) >= 0 && v.getFecha().compareTo(finStr) <= 0)
            .collect(Collectors.toList());
    }

    private void generarReporte() {
        try {
            Date fechaInicio = dateInicio.getDate();
            Date fechaFin = dateFin.getDate();

            if (fechaInicio == null || fechaFin == null) {
                UITheme.showWarning(this, "Seleccione ambas fechas.");
                return;
            }

            if (fechaInicio.after(fechaFin)) {
                UITheme.showError(this, "La fecha de inicio no puede ser mayor a la fecha fin.");
                return;
            }

            List<Venta> ventas = ventaDAO.listarTodas();
            List<VentaCredito> ventasCredito = ventaCreditoDAO.listarTodos();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String inicioStr = sdf.format(fechaInicio);
            String finStr = sdf.format(fechaFin);

            List<Venta> ventasFiltradas = filtrarPorRango(ventas, inicioStr, finStr);

            double totalVentas = ventasFiltradas.stream().mapToDouble(Venta::getPrecioVenta).sum();
            double totalGanancia = ventasFiltradas.stream().mapToDouble(Venta::getGanancia).sum();

            List<VentaCredito> creditoFiltradas = ventasCredito.stream()
                .filter(v -> v.getFechaVenta() != null
                    && v.getFechaVenta().compareTo(inicioStr) >= 0
                    && v.getFechaVenta().compareTo(finStr) <= 0)
                .collect(Collectors.toList());
            double totalCredito = creditoFiltradas.stream().mapToDouble(VentaCredito::getPrecioTotal).sum();

            long dias = ChronoUnit.DAYS.between(
                LocalDate.parse(inicioStr),
                LocalDate.parse(finStr)
            ) + 1;

            double promedioDiario = dias > 0 ? totalVentas / dias : 0;
            double totalContado = totalVentas - totalCredito;

            if (lblTotalVentas != null) lblTotalVentas.setText("$" + String.format("%.2f", totalVentas));
            if (lblVentasCredito != null) lblVentasCredito.setText("$" + String.format("%.2f", totalCredito));
            if (lblTotalGanancia != null) lblTotalGanancia.setText("$" + String.format("%.2f", totalGanancia));
            if (lblPromedioDiario != null) lblPromedioDiario.setText("$" + String.format("%.2f", promedioDiario));
            if (lblDias != null) lblDias.setText(String.valueOf(dias));

            if (lblSubVentas != null) lblSubVentas.setText(String.format("Contado: $%.2f | Créd: $%.2f", totalContado, totalCredito));
            if (lblSubCredito != null) lblSubCredito.setText(creditoFiltradas.size() + " crédito" + (creditoFiltradas.size() == 1 ? "" : "s") + " en período");
            if (lblSubGanancia != null) {
                double margen = totalVentas > 0 ? (totalGanancia / totalVentas) * 100.0 : 0;
                lblSubGanancia.setText(String.format("Margen global: %.2f%%", margen));
            }
            if (lblSubDias != null) lblSubDias.setText("Rango: " + inicioStr + " al " + finStr);

            // Montar y animar los nuevos componentes gráficos
            dibujarGraficaBarras(ventasFiltradas, inicioStr, finStr);
            dibujarGraficaLinea(ventasFiltradas, inicioStr, finStr);
            actualizarTopProductos(ventasFiltradas, totalVentas);
            actualizarTopMateriales(ventasFiltradas);

            if (chkComparar != null && chkComparar.isSelected()) {
                actualizarComparativa(ventas, inicioStr, finStr);
            } else if (lblRangoAnteriorInfo != null) {
                lblRangoAnteriorInfo.setText(" ");
            }

            ultimoInicioStr = inicioStr;
            ultimoFinStr = finStr;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // ============================================================
    // GRÁFICO DE BARRAS: ANIMADO + TOOLTIP HOVER
    // ============================================================
    private void dibujarGraficaBarras(List<Venta> ventas, String inicioStr, String finStr) {
        Map<String, DiaGraficoInfo> mapaDias = new LinkedHashMap<>();
        LocalDate inicio = LocalDate.parse(inicioStr);
        LocalDate fin = LocalDate.parse(finStr);

        for (LocalDate date = inicio; !date.isAfter(fin); date = date.plusDays(1)) {
            String str = date.toString();
            String corto = str.length() >= 10 ? str.substring(5) : str;
            mapaDias.put(str, new DiaGraficoInfo(str, corto));
        }

        for (Venta v : ventas) {
            String fecha = v.getFecha();
            if (mapaDias.containsKey(fecha)) {
                DiaGraficoInfo d = mapaDias.get(fecha);
                d.ventas += v.getPrecioVenta();
                d.ganancia += v.getGanancia();
                d.transacciones++;
            }
        }

        List<DiaGraficoInfo> datos = new ArrayList<>(mapaDias.values());

        graficoBarrasPanel.removeAll();
        graficoBarrasPanel.setLayout(new BorderLayout());

        panelBarrasActual = new GraficoBarrasAnimado(datos);
        graficoBarrasPanel.add(panelBarrasActual, BorderLayout.CENTER);
        graficoBarrasPanel.revalidate();
        graficoBarrasPanel.repaint();
    }

    // ============================================================
    // GRÁFICO DE LÍNEA: ANIMADO + CURVAS CON ÁREA Y TOOLTIPS
    // ============================================================
    private void dibujarGraficaLinea(List<Venta> ventas, String inicioStr, String finStr) {
        Map<String, DiaGraficoInfo> mapaDias = new LinkedHashMap<>();
        LocalDate inicio = LocalDate.parse(inicioStr);
        LocalDate fin = LocalDate.parse(finStr);

        for (LocalDate date = inicio; !date.isAfter(fin); date = date.plusDays(1)) {
            String str = date.toString();
            String corto = str.length() >= 10 ? str.substring(5) : str;
            mapaDias.put(str, new DiaGraficoInfo(str, corto));
        }

        for (Venta v : ventas) {
            String fecha = v.getFecha();
            if (mapaDias.containsKey(fecha)) {
                DiaGraficoInfo d = mapaDias.get(fecha);
                d.ventas += v.getPrecioVenta();
                d.ganancia += v.getGanancia();
                d.transacciones++;
            }
        }

        List<DiaGraficoInfo> datos = new ArrayList<>(mapaDias.values());

        graficoLineaPanel.removeAll();
        graficoLineaPanel.setLayout(new BorderLayout());

        panelLineaActual = new GraficoLineaAnimado(datos);
        graficoLineaPanel.add(panelLineaActual, BorderLayout.CENTER);
        graficoLineaPanel.revalidate();
        graficoLineaPanel.repaint();
    }

    // =========================================================================
    // COMPONENTE GRÁFICO: BARRAS CON ANIMACIÓN CÚBICA Y TOOLTIP FLOTANTE
    // =========================================================================
    private class GraficoBarrasAnimado extends JPanel {
        private final List<DiaGraficoInfo> lista;
        private float animProgress = 0f;
        private javax.swing.Timer animTimer;
        private int hoveredIndex = -1;
        private Point mousePos = null;

        public GraficoBarrasAnimado(List<DiaGraficoInfo> lista) {
            this.lista = lista;
            setBackground(Color.WHITE);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePos = e.getPoint();
                    int width = getWidth() - 70;
                    int startX = 60;
                    int startY = 45;
                    int height = getHeight() - 110;

                    int prevHover = hoveredIndex;
                    hoveredIndex = -1;

                    if (!lista.isEmpty() && width > 0) {
                        double space = (double) width / lista.size();
                        double barWidth = Math.min(space * 0.55, 42.0);

                        for (int i = 0; i < lista.size(); i++) {
                            int x = startX + (int) (i * space + (space - barWidth) / 2);
                            if (mousePos.x >= x - 6 && mousePos.x <= x + barWidth + 6 &&
                                mousePos.y >= startY - 15 && mousePos.y <= startY + height + 25) {
                                hoveredIndex = i;
                                break;
                            }
                        }
                    }

                    if (hoveredIndex != -1) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }

                    if (prevHover != hoveredIndex) {
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredIndex = -1;
                    mousePos = null;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);

            iniciarAnimacion();
        }

        public void iniciarAnimacion() {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animProgress = 0f;
            long startTime = System.currentTimeMillis();
            final int duracion = 500; // ms

            animTimer = new javax.swing.Timer(16, evt -> {
                long transcurrido = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) transcurrido / duracion);
                // Curva de desaceleración cúbica (Cubic Ease Out)
                animProgress = 1.0f - (float) Math.pow(1.0f - t, 3);
                repaint();
                if (t >= 1.0f) {
                    animProgress = 1.0f;
                    ((javax.swing.Timer) evt.getSource()).stop();
                }
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth() - 70;
            int height = getHeight() - 110;
            int startX = 60;
            int startY = 45;

            g2.setColor(SLATE_OSCURO);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("Distribución de Ventas por Día", startX, 24);

            if (lista.isEmpty() || width <= 0 || height <= 0) {
                g2.setColor(TEXTO_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.drawString("No hay datos para el período seleccionado", Math.max(20, width / 2 - 80), Math.max(30, height / 2));
                g2.dispose();
                return;
            }

            double maxValor = lista.stream().mapToDouble(d -> d.ventas).max().orElse(1.0);
            maxValor = Math.max(maxValor * 1.25, 10);

            // Cuadrícula sutil
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (int i = 0; i <= 4; i++) {
                int y = startY + height - (int) (height * i / 4.0);
                double valor = maxValor * i / 4.0;
                g2.setColor(TEXTO_MUTED);
                g2.drawString(String.format("$%.0f", valor), 8, y + 4);
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(startX, y, startX + width, y);
            }

            g2.setColor(BORDE_SUAVE);
            g2.drawLine(startX, startY + height, startX + width, startY + height);

            double barWidth = Math.min((double) width / lista.size() * 0.55, 42.0);
            double space = (double) width / lista.size();

            double sumaValores = lista.stream().mapToDouble(d -> d.ventas).sum();
            double promedio = lista.size() > 0 ? sumaValores / lista.size() : 0;

            float factorAnim = exportandoPDF ? 1.0f : animProgress;

            // Dibujar las barras con factor de animación
            for (int i = 0; i < lista.size(); i++) {
                DiaGraficoInfo data = lista.get(i);
                int barHeight = (int) (data.ventas / maxValor * height * factorAnim);
                int x = startX + (int) (i * space + (space - barWidth) / 2);
                int y = startY + height - barHeight;

                boolean isHover = (i == hoveredIndex) && !exportandoPDF;

                if (isHover) {
                    // Resplandor exterior al pasar el ratón
                    g2.setColor(new Color(37, 99, 235, 45));
                    g2.fillRoundRect(x - 4, y - 4, (int) barWidth + 8, Math.max(barHeight + 4, 4), 10, 10);

                    // Barra iluminada con azul cielo brillante
                    GradientPaint gradHover = new GradientPaint(
                        x, y, new Color(56, 189, 248),
                        x, startY + height, new Color(37, 99, 235)
                    );
                    g2.setPaint(gradHover);
                    g2.fillRoundRect(x - 1, y - 1, (int) barWidth + 2, Math.max(barHeight + 1, 2), 7, 7);

                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x - 1, y - 1, (int) barWidth + 2, Math.max(barHeight + 1, 2), 7, 7);
                } else {
                    // Barra estándar con degradado zafiro
                    GradientPaint gradBar = new GradientPaint(
                        x, y, new Color(59, 130, 246),
                        x, startY + height, new Color(29, 78, 216)
                    );
                    g2.setPaint(gradBar);
                    g2.fillRoundRect(x, y, (int) barWidth, Math.max(barHeight, 1), 6, 6);
                }

                // Etiqueta del monto arriba de la barra
                if (data.ventas > 0 && factorAnim > 0.6f) {
                    g2.setColor(isHover ? AZUL_INDIGO : SLATE_OSCURO);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, isHover ? 11 : 10));
                    String etq = String.format("$%.0f", data.ventas);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(etq, x + (int) barWidth / 2 - fm.stringWidth(etq) / 2, y - 5);
                }

                // Etiqueta de la fecha en el eje X
                g2.setColor(isHover ? SLATE_OSCURO : TEXTO_MUTED);
                g2.setFont(new Font("Segoe UI", isHover ? Font.BOLD : Font.PLAIN, 10));
                FontMetrics fmLbl = g2.getFontMetrics();
                g2.drawString(data.fechaCorta, x + (int) barWidth / 2 - fmLbl.stringWidth(data.fechaCorta) / 2, startY + height + 16);
            }

            // Línea punteada de promedio
            if (promedio > 0) {
                int yPromedio = startY + height - (int) (promedio / maxValor * height);
                g2.setColor(AMBAR);
                float[] patron = {4f, 4f};
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, patron, 0));
                g2.drawLine(startX, yPromedio, startX + width, yPromedio);
                g2.setStroke(new BasicStroke(1f));

                String etiquetaProm = "Promedio: $" + String.format("%.2f", promedio);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(etiquetaProm, startX + width - g2.getFontMetrics().stringWidth(etiquetaProm) - 4, yPromedio - 5);
            }

            // TOOLTIP FLOTANTE AL PASAR EL CURSOR
            if (hoveredIndex >= 0 && hoveredIndex < lista.size() && mousePos != null && !exportandoPDF) {
                DiaGraficoInfo d = lista.get(hoveredIndex);
                dibujarTooltipElegante(g2, mousePos.x, mousePos.y, d.fechaCompleta, d.ventas, d.ganancia, d.transacciones, getWidth(), getHeight());
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // COMPONENTE GRÁFICO: LÍNEAS CON ANIMACIÓN CÚBICA Y TOOLTIP HOVER
    // =========================================================================
    private class GraficoLineaAnimado extends JPanel {
        private final List<DiaGraficoInfo> lista;
        private float animProgress = 0f;
        private javax.swing.Timer animTimer;
        private int hoveredIndex = -1;
        private Point mousePos = null;

        public GraficoLineaAnimado(List<DiaGraficoInfo> lista) {
            this.lista = lista;
            setBackground(Color.WHITE);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePos = e.getPoint();
                    int width = getWidth() - 70;
                    int startX = 60;

                    int prevHover = hoveredIndex;
                    hoveredIndex = -1;

                    if (lista.size() >= 2 && width > 0) {
                        double stepX = (double) width / (lista.size() - 1);
                        int closestIdx = -1;
                        double minDist = 38.0;

                        for (int i = 0; i < lista.size(); i++) {
                            int x = startX + (int) (i * stepX);
                            double dist = Math.abs(mousePos.x - x);
                            if (dist < minDist) {
                                minDist = dist;
                                closestIdx = i;
                            }
                        }
                        hoveredIndex = closestIdx;
                    }

                    if (hoveredIndex != -1) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }

                    if (prevHover != hoveredIndex) {
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredIndex = -1;
                    mousePos = null;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);

            iniciarAnimacion();
        }

        public void iniciarAnimacion() {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animProgress = 0f;
            long startTime = System.currentTimeMillis();
            final int duracion = 520; // ms

            animTimer = new javax.swing.Timer(16, evt -> {
                long transcurrido = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) transcurrido / duracion);
                animProgress = 1.0f - (float) Math.pow(1.0f - t, 3);
                repaint();
                if (t >= 1.0f) {
                    animProgress = 1.0f;
                    ((javax.swing.Timer) evt.getSource()).stop();
                }
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth() - 70;
            int height = getHeight() - 130;
            int startX = 60;
            int startY = 45;

            g2.setColor(SLATE_OSCURO);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("Curva de Ingresos vs Ganancia Neta", startX, 24);

            if (lista.size() < 2 || width <= 0 || height <= 0) {
                g2.setColor(TEXTO_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.drawString("Se requieren al menos 2 días para trazar la curva de tendencia.", startX, Math.max(30, height / 2));
                g2.dispose();
                return;
            }

            double maxValor = Math.max(
                lista.stream().mapToDouble(d -> d.ventas).max().orElse(1.0),
                lista.stream().mapToDouble(d -> d.ganancia).max().orElse(1.0)
            );
            maxValor = Math.max(maxValor * 1.25, 10);

            // Cuadrícula horizontal
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (int i = 0; i <= 4; i++) {
                int y = startY + height - (int) (height * i / 4.0);
                double valor = maxValor * i / 4.0;
                g2.setColor(TEXTO_MUTED);
                g2.drawString(String.format("$%.0f", valor), 8, y + 4);
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(startX, y, startX + width, y);
            }

            g2.setColor(BORDE_SUAVE);
            g2.drawLine(startX, startY + height, startX + width, startY + height);

            double stepX = (double) width / (lista.size() - 1);
            float factorAnim = exportandoPDF ? 1.0f : animProgress;

            // Línea guía vertical al hacer hover
            if (hoveredIndex >= 0 && hoveredIndex < lista.size() && !exportandoPDF) {
                int hx = startX + (int) (hoveredIndex * stepX);
                g2.setColor(new Color(203, 213, 225));
                float[] dash = {3f, 3f};
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                g2.drawLine(hx, startY, hx, startY + height);
                g2.setStroke(new BasicStroke(1f));
            }

            // Área sombreada bajo curva Ventas
            Path2D areaVentas = new Path2D.Double();
            for (int i = 0; i < lista.size(); i++) {
                int x = startX + (int) (i * stepX);
                int y = startY + height - (int) (lista.get(i).ventas / maxValor * height * factorAnim);
                if (i == 0) areaVentas.moveTo(x, y); else areaVentas.lineTo(x, y);
            }
            areaVentas.lineTo(startX + width, startY + height);
            areaVentas.lineTo(startX, startY + height);
            areaVentas.closePath();
            g2.setPaint(new GradientPaint(0, startY, new Color(37, 99, 235, 60), 0, startY + height, new Color(37, 99, 235, 5)));
            g2.fill(areaVentas);

            // Área sombreada bajo curva Ganancia
            Path2D areaGanancia = new Path2D.Double();
            for (int i = 0; i < lista.size(); i++) {
                int x = startX + (int) (i * stepX);
                int y = startY + height - (int) (lista.get(i).ganancia / maxValor * height * factorAnim);
                if (i == 0) areaGanancia.moveTo(x, y); else areaGanancia.lineTo(x, y);
            }
            areaGanancia.lineTo(startX + width, startY + height);
            areaGanancia.lineTo(startX, startY + height);
            areaGanancia.closePath();
            g2.setPaint(new GradientPaint(0, startY, new Color(5, 150, 105, 60), 0, startY + height, new Color(5, 150, 105, 5)));
            g2.fill(areaGanancia);

            // Trazo de línea Ventas (Índigo)
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(AZUL_INDIGO);
            Path2D pathVentas = new Path2D.Double();
            for (int i = 0; i < lista.size(); i++) {
                int x = startX + (int) (i * stepX);
                int y = startY + height - (int) (lista.get(i).ventas / maxValor * height * factorAnim);
                if (i == 0) pathVentas.moveTo(x, y); else pathVentas.lineTo(x, y);
            }
            g2.draw(pathVentas);

            // Trazo de línea Ganancia (Esmeralda)
            g2.setColor(VERDE_ESMERALDA);
            Path2D pathGanancia = new Path2D.Double();
            for (int i = 0; i < lista.size(); i++) {
                int x = startX + (int) (i * stepX);
                int y = startY + height - (int) (lista.get(i).ganancia / maxValor * height * factorAnim);
                if (i == 0) pathGanancia.moveTo(x, y); else pathGanancia.lineTo(x, y);
            }
            g2.draw(pathGanancia);

            // Puntos circulares sobre las curvas
            for (int i = 0; i < lista.size(); i++) {
                int x = startX + (int) (i * stepX);
                int yV = startY + height - (int) (lista.get(i).ventas / maxValor * height * factorAnim);
                int yG = startY + height - (int) (lista.get(i).ganancia / maxValor * height * factorAnim);

                boolean isHover = (i == hoveredIndex) && !exportandoPDF;

                // Punto Ventas
                if (isHover) {
                    g2.setColor(new Color(37, 99, 235, 70));
                    g2.fillOval(x - 8, yV - 8, 16, 16);
                    g2.setColor(Color.WHITE); g2.fillOval(x - 5, yV - 5, 10, 10);
                    g2.setColor(AZUL_INDIGO); g2.setStroke(new BasicStroke(2.2f));
                    g2.drawOval(x - 5, yV - 5, 10, 10);
                } else {
                    g2.setColor(Color.WHITE); g2.fillOval(x - 4, yV - 4, 8, 8);
                    g2.setColor(AZUL_INDIGO); g2.setStroke(new BasicStroke(1.6f));
                    g2.drawOval(x - 4, yV - 4, 8, 8);
                }

                // Punto Ganancia
                if (isHover) {
                    g2.setColor(new Color(5, 150, 105, 70));
                    g2.fillOval(x - 8, yG - 8, 16, 16);
                    g2.setColor(Color.WHITE); g2.fillOval(x - 5, yG - 5, 10, 10);
                    g2.setColor(VERDE_ESMERALDA); g2.setStroke(new BasicStroke(2.2f));
                    g2.drawOval(x - 5, yG - 5, 10, 10);
                } else {
                    g2.setColor(Color.WHITE); g2.fillOval(x - 4, yG - 4, 8, 8);
                    g2.setColor(VERDE_ESMERALDA); g2.setStroke(new BasicStroke(1.6f));
                    g2.drawOval(x - 4, yG - 4, 8, 8);
                }
            }
            g2.setStroke(new BasicStroke(1f));

            // Fechas en el eje X
            g2.setColor(TEXTO_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int labelStep = Math.max(1, lista.size() / 10);
            for (int i = 0; i < lista.size(); i += labelStep) {
                int x = startX + (int) (i * stepX);
                String label = lista.get(i).fechaCorta;
                g2.drawString(label, x - 10, startY + height + 16);
            }

            // Leyenda inferior
            int legendX = startX;
            int legendY = height + startY + 34;

            g2.setColor(AZUL_INDIGO);
            g2.fillRoundRect(legendX, legendY, 12, 12, 3, 3);
            g2.setColor(SLATE_OSCURO);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("Ingresos Totales", legendX + 18, legendY + 10);

            g2.setColor(VERDE_ESMERALDA);
            g2.fillRoundRect(legendX + 140, legendY, 12, 12, 3, 3);
            g2.setColor(SLATE_OSCURO);
            g2.drawString("Ganancia Neta", legendX + 158, legendY + 10);

            // TOOLTIP FLOTANTE AL PASAR EL CURSOR
            if (hoveredIndex >= 0 && hoveredIndex < lista.size() && mousePos != null && !exportandoPDF) {
                DiaGraficoInfo d = lista.get(hoveredIndex);
                dibujarTooltipElegante(g2, mousePos.x, mousePos.y, d.fechaCompleta, d.ventas, d.ganancia, d.transacciones, getWidth(), getHeight());
            }

            g2.dispose();
        }
    }

    /**
     * Dibuja una tarjeta flotante oscura, semitransparente y con borde redondeado con el desglose del día.
     */
    private void dibujarTooltipElegante(Graphics2D g2, int mx, int my, String fecha, double ventas, double ganancia, int pedidos, int wPanel, int hPanel) {
        int w = 175;
        int h = 95;

        // Posicionar el tooltip evitando desbordar el contenedor
        int tx = mx + 14;
        int ty = my - h - 10;

        if (tx + w > wPanel - 15) {
            tx = mx - w - 14;
        }
        if (ty < 10) {
            ty = my + 18;
        }

        // Sombra suave del tooltip
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillRoundRect(tx + 2, ty + 2, w, h, 12, 12);

        // Fondo oscuro estilo Slate ejecutiva
        g2.setColor(new Color(15, 23, 42, 235));
        g2.fillRoundRect(tx, ty, w, h, 10, 10);

        // Borde delicado
        g2.setColor(new Color(255, 255, 255, 45));
        g2.drawRoundRect(tx, ty, w, h, 10, 10);

        // Título: Fecha
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.drawString("📅 " + fecha, tx + 10, ty + 18);

        // Línea divisoria
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawLine(tx + 8, ty + 25, tx + w - 8, ty + 25);

        // Fila 1: Ventas
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(147, 197, 253)); // Azul claro
        g2.drawString("Ventas:", tx + 10, ty + 42);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(Color.WHITE);
        String sVentas = String.format("$%.2f", ventas);
        g2.drawString(sVentas, tx + w - g2.getFontMetrics().stringWidth(sVentas) - 10, ty + 42);

        // Fila 2: Ganancia
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(110, 231, 183)); // Verde claro
        g2.drawString("Ganancia:", tx + 10, ty + 60);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(new Color(52, 211, 153));
        String sGanancia = String.format("$%.2f", ganancia);
        g2.drawString(sGanancia, tx + w - g2.getFontMetrics().stringWidth(sGanancia) - 10, ty + 60);

        // Fila 3: Margen / Pedidos
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(203, 213, 225));
        double margen = ventas > 0 ? (ganancia / ventas) * 100.0 : 0.0;
        String detalle = String.format("Margen: %.1f%%  •  %d ped.", margen, pedidos);
        g2.drawString(detalle, tx + 10, ty + 80);
    }

    // ============================================================
    // EXPORTAR REPORTE A PDF
    // ============================================================
    private void exportarReportePDF() {
        if (ultimoInicioStr == null || ultimoFinStr == null) {
            UITheme.showWarning(this, "Genere un reporte primero.");
            return;
        }
        try {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Reporte_Ventas_" + ultimoInicioStr + "_a_" + ultimoFinStr + ".pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".pdf")) ruta += ".pdf";

                // Ocultar temporalmente tooltips y renderizar animación al 100%
                exportandoPDF = true;
                BufferedImage imgBarras = capturarPanel(graficoBarrasPanel);
                BufferedImage imgLinea = capturarPanel(graficoLineaPanel);
                exportandoPDF = false;

                List<Object[]> filasTop = new ArrayList<>();
                for (int i = 0; i < modelTopProductos.getRowCount(); i++) {
                    Object[] fila = new Object[modelTopProductos.getColumnCount()];
                    for (int c = 0; c < fila.length; c++) fila[c] = modelTopProductos.getValueAt(i, c);
                    filasTop.add(fila);
                }

                List<Object[]> filasMateriales = new ArrayList<>();
                for (int i = 0; i < modelTopMateriales.getRowCount(); i++) {
                    Object[] fila = new Object[modelTopMateriales.getColumnCount()];
                    for (int c = 0; c < fila.length; c++) fila[c] = modelTopMateriales.getValueAt(i, c);
                    filasMateriales.add(fila);
                }

                List<String> lineasComparativa = null;
                if (chkComparar != null && chkComparar.isSelected()) {
                    lineasComparativa = new ArrayList<>();
                    lineasComparativa.add("Total Ventas — Actual: " + lblCompVentasActual.getText()
                        + "   |   " + lblCompVentasAnterior.getText()
                        + "   |   " + lblCompVentasDelta.getText());
                    lineasComparativa.add("Ganancia Neta — Actual: " + lblCompGananciaActual.getText()
                        + "   |   " + lblCompGananciaAnterior.getText()
                        + "   |   " + lblCompGananciaDelta.getText());
                    lineasComparativa.add("Promedio Diario — Actual: " + lblCompPromedioActual.getText()
                        + "   |   " + lblCompPromedioAnterior.getText()
                        + "   |   " + lblCompPromedioDelta.getText());
                }

                ReporteGenerator.generarReporteAnalitico(
                    ultimoInicioStr, ultimoFinStr,
                    lblTotalVentas.getText(), lblVentasCredito.getText(), lblTotalGanancia.getText(),
                    lblPromedioDiario.getText(), lblDias.getText(),
                    filasTop, filasMateriales, imgBarras, imgLinea, lineasComparativa,
                    ruta
                );

                DialogStyler.mostrarExito(this, "Reporte PDF generado exitosamente en:<br><b>" + ruta + "</b>", "Éxito");
            }
        } catch (Exception ex) {
            exportandoPDF = false;
            DialogStyler.mostrarError(this, "Error al generar PDF: " + ex.getMessage());
        }
    }

    private BufferedImage capturarPanel(JPanel panel) {
        if (panel.getWidth() <= 0 || panel.getHeight() <= 0) {
            Dimension pref = panel.getPreferredSize();
            panel.setSize(pref);
            panel.doLayout();
        }
        int w = Math.max(panel.getWidth(), 400);
        int h = Math.max(panel.getHeight(), 250);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        panel.paint(g2);
        g2.dispose();
        return img;
    }

    public void actualizarReporte() {
        generarReporte();
    }
}