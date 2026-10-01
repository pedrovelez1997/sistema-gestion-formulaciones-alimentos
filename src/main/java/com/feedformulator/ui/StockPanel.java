package com.feedformulator.ui;

import com.feedformulator.dao.AuditoriaDAO;
import com.feedformulator.dao.DatabaseManager;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.dao.MovimientoInventarioDAO;
import com.feedformulator.model.IngresoHistorial;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.MovimientoInventario;
import com.feedformulator.report.ReporteGenerator;
import com.feedformulator.report.ReporteIngresosDetallado;
import com.feedformulator.report.ReporteMovimientosMejorado;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Panel de Control de Inventario (Materias Primas & Inventario de Formulaciones)
 * Altura optimizada, marcos nítidos bien definidos y medidor circular por Stock Disponible.
 */
public class StockPanel extends JPanel {
    private final MateriaPrimaDAO dao = new MateriaPrimaDAO();
    private final com.feedformulator.dao.FormulacionDAO formulacionDAO = new com.feedformulator.dao.FormulacionDAO();

    private DefaultTableModel tableModel;
    private JTable tabla; // Puente de selección en memoria

    private JPanel panelTarjetas;
    private JPanel panelTarjetasFormulaciones;
    private CardLayout cardLayoutVistas;
    private JPanel contenedorVistas;
    private JPanel panelHeaderMaterias;
    private JPanel panelHeaderFormulaciones;

    private List<MateriaPrima> productosCache = new ArrayList<>();
    private List<ResumenFormula> formulasCache = new ArrayList<>();

    // Buscadores y Filtros
    private JTextField txtBuscarMaterias;
    private JTextField txtBuscarFormulaciones;
    private String filtroEstadoFormulaciones = "TODOS"; // "TODOS", "STOCK", "AGOTADO"

    // KPIs de Formulaciones
    private JLabel lblKpiTotalRecetas;
    private JLabel lblKpiLibrasFabricadas;
    private JLabel lblKpiLibrasDisponibles;
    private JLabel lblKpiLibrasVendidas;
    private JLabel lblKpiInversionTotal;

    // Botones de filtro de formulación
    private JButton btnFiltroFormTodos;
    private JButton btnFiltroFormStock;
    private JButton btnFiltroFormAgotados;

    // Paleta de Colores
    private static final Color VERDE = new Color(5, 150, 105);         // Esmeralda #059669
    private static final Color VERDE_OSCURO = new Color(4, 120, 87);   // #047857
    private static final Color VERDE_CLARO = new Color(209, 250, 229); // #D1FAE5
    private static final Color FONDO = new Color(248, 250, 252);       // #F8FAFC
    private static final Color ROJO = new Color(220, 38, 38);          // #DC2626
    private static final Color AZUL = new Color(2, 132, 199);          // #0284C7
    private static final Color VERDE_MENTA = new Color(13, 148, 136);  // #0D9488
    private static final Color MORADO = new Color(124, 58, 237);       // #7C3AED
    private static final Color NARANJA = new Color(217, 119, 6);       // #D97706
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);   // #0F172A
    private static final Color BORDE_MARCADO = new Color(148, 163, 184); // #94A3B8 (Borde nítido para agotados)
    private static final Color BORDE_SUAVE = new Color(203, 213, 225);   // #CBD5E1
    private static final Color TEXTO_MUTED = new Color(100, 116, 139); // #64748B

    public StockPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(crearTitulo(), BorderLayout.NORTH);
        crearTabla();

        cardLayoutVistas = new CardLayout();
        contenedorVistas = new JPanel(cardLayoutVistas);
        contenedorVistas.setOpaque(false);
        contenedorVistas.add(crearVistaTarjetas(), "MATERIAS");
        contenedorVistas.add(crearVistaTarjetasFormulaciones(), "FORMULACIONES");

        add(contenedorVistas, BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
        cargarDatos();
    }

    private JPanel crearTitulo() {
        JPanel contenedor = new JPanel(new GridLayout(1, 2, 12, 0));
        contenedor.setOpaque(false);

        JPanel headerMaterias = crearTabHeaderPanel("📦", "Inventario de Materias Primas", "Control de stock, sacos y costos por unidad", true);
        JPanel headerFormulaciones = crearTabHeaderPanel("🧪", "Inventario de Formulaciones", "Fórmulas creadas, vendidas y disponibles", false);

        headerMaterias.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { seleccionarVista(true); }
        });
        headerFormulaciones.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { seleccionarVista(false); }
        });

        this.panelHeaderMaterias = headerMaterias;
        this.panelHeaderFormulaciones = headerFormulaciones;

        contenedor.add(headerMaterias);
        contenedor.add(headerFormulaciones);
        return contenedor;
    }

    private JPanel crearTabHeaderPanel(String icono, String titulo, String subtitulo, boolean activo) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean estaActivo = Boolean.TRUE.equals(getClientProperty("activo"));
                
                Color start = estaActivo ? new Color(15, 23, 42) : new Color(71, 85, 105);
                Color end = estaActivo ? new Color(6, 78, 59) : new Color(100, 116, 139);
                
                GradientPaint gp = new GradientPaint(0, 0, start, getWidth(), 0, end);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                if (estaActivo) {
                    g2.setColor(new Color(16, 185, 129));
                    g2.setStroke(new BasicStroke(2.4f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                }
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.putClientProperty("activo", activo);
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izq.setOpaque(false);

        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean estaActivo = Boolean.TRUE.equals(header.getClientProperty("activo"));
                g2.setColor(estaActivo ? new Color(16, 185, 129, 60) : new Color(255, 255, 255, 30));
                g2.fillOval(0, 0, 42, 42);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
                FontMetrics fm = g2.getFontMetrics();
                int x = (42 - fm.stringWidth(icono)) / 2;
                int y = (42 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(icono, x, y);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(42, 42); }
        };
        iconCircle.setOpaque(false);
        izq.add(iconCircle);

        JPanel tituloCol = new JPanel();
        tituloCol.setLayout(new BoxLayout(tituloCol, BoxLayout.Y_AXIS));
        tituloCol.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        tituloCol.add(lblTitulo);

        JLabel lblSubtitulo = new JLabel(subtitulo);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubtitulo.setForeground(new Color(226, 232, 240));
        tituloCol.add(lblSubtitulo);

        izq.add(tituloCol);
        header.add(izq, BorderLayout.WEST);

        MouseAdapter reenviarClic = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (java.awt.event.MouseListener ml : header.getMouseListeners()) {
                    if (ml != this) ml.mouseClicked(e);
                }
            }
        };
        agregarClickRecursivo(izq, reenviarClic);

        return header;
    }

    private void seleccionarVista(boolean materiasPrimas) {
        panelHeaderMaterias.putClientProperty("activo", materiasPrimas);
        panelHeaderFormulaciones.putClientProperty("activo", !materiasPrimas);
        panelHeaderMaterias.repaint();
        panelHeaderFormulaciones.repaint();
        cardLayoutVistas.show(contenedorVistas, materiasPrimas ? "MATERIAS" : "FORMULACIONES");
        if (!materiasPrimas) {
            cargarFormulacionesResumen();
        }
    }

    private JScrollPane crearTabla() {
        String[] cols = {"Código", "Nombre", "Unidad", "Sacos/Envases", "Peso/Envase", 
                         "Sueltos", "Total", "Disponible", "Precio Total", "$/Unidad", "Origen"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(tableModel);
        tabla.setRowHeight(28);
        return new JScrollPane(tabla);
    }

    // ============================================================
    //  VISTA 1: TARJETAS DE MATERIAS PRIMAS
    // ============================================================
    private JComponent crearVistaTarjetas() {
        JPanel vista = new JPanel(new BorderLayout(0, 8));
        vista.setBackground(FONDO);

        JPanel barraFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barraFiltros.setBackground(FONDO);
        JLabel lblLupa = new JLabel("🔍 Buscar materia prima:");
        lblLupa.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLupa.setForeground(SLATE_OSCURO);

        txtBuscarMaterias = new JTextField(22);
        txtBuscarMaterias.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarMaterias.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarTarjetasMaterias(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarTarjetasMaterias(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarTarjetasMaterias(); }
        });

        barraFiltros.add(lblLupa);
        barraFiltros.add(txtBuscarMaterias);
        vista.add(barraFiltros, BorderLayout.NORTH);

        panelTarjetas = new JPanel(new GridLayout(0, 4, 14, 14));
        panelTarjetas.setBackground(FONDO);
        panelTarjetas.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.add(panelTarjetas, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(contenedor);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(FONDO);
        vista.add(scroll, BorderLayout.CENTER);

        return vista;
    }

    private void filtrarTarjetasMaterias() {
        String q = txtBuscarMaterias.getText().trim().toLowerCase();
        panelTarjetas.removeAll();
        for (int i = 0; i < productosCache.size(); i++) {
            MateriaPrima mp = productosCache.get(i);
            boolean match = q.isEmpty() ||
                mp.getNombre().toLowerCase().contains(q) ||
                (mp.getCodigo() != null && mp.getCodigo().toLowerCase().contains(q));
            if (match) {
                panelTarjetas.add(crearTarjetaProducto(mp, i));
            }
        }
        panelTarjetas.revalidate();
        panelTarjetas.repaint();
    }

    // ============================================================
    //  VISTA 2: INVENTARIO DE FORMULACIONES
    // ============================================================
    private JComponent crearVistaTarjetasFormulaciones() {
        JPanel vista = new JPanel(new BorderLayout(0, 10));
        vista.setBackground(FONDO);

        JPanel pnlNorte = new JPanel(new BorderLayout(0, 8));
        pnlNorte.setBackground(FONDO);

        // Barra de 5 KPIs
        JPanel pnlKPIs = new JPanel(new GridLayout(1, 5, 10, 0));
        pnlKPIs.setBackground(FONDO);

        lblKpiTotalRecetas = new JLabel("0", SwingConstants.CENTER);
        lblKpiLibrasFabricadas = new JLabel("0.00 lb", SwingConstants.CENTER);
        lblKpiLibrasDisponibles = new JLabel("0.00 lb", SwingConstants.CENTER);
        lblKpiLibrasVendidas = new JLabel("0.00 lb", SwingConstants.CENTER);
        lblKpiInversionTotal = new JLabel("$0.00", SwingConstants.CENTER);

        pnlKPIs.add(crearMiniCardKPI("FÓRMULAS CATÁLOGO", lblKpiTotalRecetas, SLATE_OSCURO, "Recetas base"));
        pnlKPIs.add(crearMiniCardKPI("LIBRAS FABRICADAS", lblKpiLibrasFabricadas, SLATE_OSCURO, "Total histórico"));
        pnlKPIs.add(crearMiniCardKPI("STOCK DISPONIBLE", lblKpiLibrasDisponibles, VERDE, "Listo en bodega"));
        pnlKPIs.add(crearMiniCardKPI("LIBRAS VENDIDAS", lblKpiLibrasVendidas, AZUL, "Despachado"));
        pnlKPIs.add(crearMiniCardKPI("INVERSIÓN TOTAL", lblKpiInversionTotal, SLATE_OSCURO, "Costo acumulado"));
        pnlNorte.add(pnlKPIs, BorderLayout.NORTH);

        // Barra de Búsqueda y Filtros Rápidos
        JPanel pnlBarraFiltro = new JPanel(new BorderLayout(8, 0));
        pnlBarraFiltro.setBackground(Color.WHITE);
        pnlBarraFiltro.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JPanel pnlIzq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlIzq.setOpaque(false);
        JLabel lblLupa = new JLabel("🔍 Buscar fórmula:");
        lblLupa.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLupa.setForeground(SLATE_OSCURO);
        txtBuscarFormulaciones = new JTextField(22);
        txtBuscarFormulaciones.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarFormulaciones.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroFormulaciones(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroFormulaciones(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroFormulaciones(); }
        });
        pnlIzq.add(lblLupa);
        pnlIzq.add(txtBuscarFormulaciones);
        pnlBarraFiltro.add(pnlIzq, BorderLayout.WEST);

        JPanel pnlDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlDer.setOpaque(false);

        btnFiltroFormTodos = crearBotonPildora("Todos", true);
        btnFiltroFormStock = crearBotonPildora("🟢 Con Stock", false);
        btnFiltroFormAgotados = crearBotonPildora("⚪ Agotados", false);

        btnFiltroFormTodos.addActionListener(e -> setFiltroFormulacionEstado("TODOS"));
        btnFiltroFormStock.addActionListener(e -> setFiltroFormulacionEstado("STOCK"));
        btnFiltroFormAgotados.addActionListener(e -> setFiltroFormulacionEstado("AGOTADO"));

        pnlDer.add(btnFiltroFormTodos);
        pnlDer.add(btnFiltroFormStock);
        pnlDer.add(btnFiltroFormAgotados);
        pnlBarraFiltro.add(pnlDer, BorderLayout.EAST);

        pnlNorte.add(pnlBarraFiltro, BorderLayout.SOUTH);
        vista.add(pnlNorte, BorderLayout.NORTH);

        // Grid de Tarjetas
        panelTarjetasFormulaciones = new JPanel(new GridLayout(0, 4, 14, 14));
        panelTarjetasFormulaciones.setBackground(FONDO);
        panelTarjetasFormulaciones.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(FONDO);
        contenedor.add(panelTarjetasFormulaciones, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(contenedor);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(FONDO);
        vista.add(scroll, BorderLayout.CENTER);

        return vista;
    }

    private JPanel crearMiniCardKPI(String titulo, JLabel lblValor, Color colorValor, String subtitulo) {
        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblTit.setForeground(TEXTO_MUTED);

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblValor.setForeground(colorValor);

        JLabel lblSub = new JLabel(subtitulo, SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lblSub.setForeground(new Color(148, 163, 184));

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);
        return card;
    }

    private JButton crearBotonPildora(String texto, boolean activo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actualizarEstiloPildora(btn, activo);
        return btn;
    }

    private void actualizarEstiloPildora(JButton btn, boolean activo) {
        if (activo) {
            btn.setBackground(VERDE);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VERDE, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(SLATE_OSCURO);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE_SUAVE, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        }
    }

    private void setFiltroFormulacionEstado(String estado) {
        this.filtroEstadoFormulaciones = estado;
        actualizarEstiloPildora(btnFiltroFormTodos, "TODOS".equals(estado));
        actualizarEstiloPildora(btnFiltroFormStock, "STOCK".equals(estado));
        actualizarEstiloPildora(btnFiltroFormAgotados, "AGOTADO".equals(estado));
        aplicarFiltroFormulaciones();
    }

    private static class ResumenFormula {
        String nombre;
        int creadas = 0;
        int vendidas = 0;
        int disponibles = 0;
        double librasCreadas = 0;
        double librasVendidas = 0;
        double librasDisponibles = 0;
        double costoTotalAcumulado = 0;
        List<com.feedformulator.model.Formulacion> lotes = new ArrayList<>();
    }

    private String obtenerNombreBaseFormula(String nombreCompleto) {
        if (nombreCompleto == null) return "";
        return nombreCompleto.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
    }

    private void cargarFormulacionesResumen() {
        try {
            List<com.feedformulator.model.Formulacion> todas = formulacionDAO.listarTodasResumen();
            java.util.LinkedHashMap<String, ResumenFormula> mapa = new java.util.LinkedHashMap<>();

            double totalLbsFabricadas = 0;
            double totalLbsDisponibles = 0;
            double totalLbsVendidas = 0;
            double totalInversion = 0;

            for (com.feedformulator.model.Formulacion f : todas) {
                String nombreBase = obtenerNombreBaseFormula(f.getNombre());
                ResumenFormula r = mapa.computeIfAbsent(nombreBase, nombre -> {
                    ResumenFormula nuevo = new ResumenFormula();
                    nuevo.nombre = nombre;
                    return nuevo;
                });
                r.creadas++;
                r.librasCreadas += f.getTotalLibras();
                r.costoTotalAcumulado += f.getCostoTotal();
                r.lotes.add(f);

                totalLbsFabricadas += f.getTotalLibras();
                totalInversion += f.getCostoTotal();

                if ("Vendido".equalsIgnoreCase(f.getEstado())) {
                    r.vendidas++;
                    r.librasVendidas += f.getTotalLibras();
                    totalLbsVendidas += f.getTotalLibras();
                } else {
                    r.disponibles++;
                    r.librasDisponibles += f.getTotalLibras();
                    totalLbsDisponibles += f.getTotalLibras();
                }
            }

            formulasCache = new ArrayList<>(mapa.values());
            formulasCache.sort((a, b) -> b.creadas - a.creadas);

            lblKpiTotalRecetas.setText(String.valueOf(formulasCache.size()));
            lblKpiLibrasFabricadas.setText(String.format("%.1f lb", totalLbsFabricadas));
            lblKpiLibrasDisponibles.setText(String.format("%.1f lb", totalLbsDisponibles));
            lblKpiLibrasVendidas.setText(String.format("%.1f lb", totalLbsVendidas));
            lblKpiInversionTotal.setText(String.format("$%.2f", totalInversion));

            aplicarFiltroFormulaciones();

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al cargar inventario de formulaciones: " + ex.getMessage());
        }
    }

    private void aplicarFiltroFormulaciones() {
        String q = txtBuscarFormulaciones != null ? txtBuscarFormulaciones.getText().trim().toLowerCase() : "";
        panelTarjetasFormulaciones.removeAll();

        for (ResumenFormula r : formulasCache) {
            boolean matchQuery = q.isEmpty() || r.nombre.toLowerCase().contains(q);
            boolean matchEstado = "TODOS".equals(filtroEstadoFormulaciones) ||
                ("STOCK".equals(filtroEstadoFormulaciones) && r.librasDisponibles > 0.001) ||
                ("AGOTADO".equals(filtroEstadoFormulaciones) && r.librasDisponibles <= 0.001);

            if (matchQuery && matchEstado) {
                panelTarjetasFormulaciones.add(crearTarjetaFormulaModerna(r));
            }
        }
        panelTarjetasFormulaciones.revalidate();
        panelTarjetasFormulaciones.repaint();
    }

    // ============================================================
    //  TARJETA DE FORMULACIÓN CON MARCOS BIEN MARCADOS Y % DISPONIBLE
    // ============================================================
    private JPanel crearTarjetaFormulaModerna(ResumenFormula r) {
        // LÓGICA DE STOCK DISPONIBLE:
        // 100% cuando no se ha vendido nada y todo está disponible en bodega.
        // Se reduce conforme se vende. Si se agota todo, queda en 0.0%
        double porcentajeDisponible = r.librasCreadas > 0
            ? Math.max(0, Math.min(100, (r.librasDisponibles / r.librasCreadas) * 100.0))
            : 0;

        boolean tieneStock = r.librasDisponibles > 0.001;

        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int yOff = hovered ? -2 : 0;

                // Fondo de la tarjeta
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, yOff, w - 1, h - 1, 16, 16);

                // MARCOS MÁS MARCADOS Y VISIBLES
                if (hovered) {
                    g2.setColor(tieneStock ? new Color(4, 120, 87) : SLATE_OSCURO);
                    g2.setStroke(new BasicStroke(2.8f)); // Marco grueso al pasar cursor
                } else {
                    g2.setColor(tieneStock ? VERDE : BORDE_MARCADO);
                    g2.setStroke(new BasicStroke(tieneStock ? 2.2f : 1.8f)); // Marco firme y nítido
                }
                g2.drawRoundRect(0, yOff, w - 1, h - 1, 16, 16);

                // Franja superior identificadora si tiene stock
                if (tieneStock) {
                    g2.setColor(VERDE);
                    g2.fillRoundRect(0, yOff, w - 1, 6, 16, 16);
                    g2.fillRect(0, yOff + 3, w - 1, 3);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(245, 252));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 1. Cabecera en 2 niveles (Subtítulo + Píldora arriba, Nombre completo abajo)
        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.setOpaque(false);

        // Fila Superior de Cabecera: Cantidad de lotes + Píldora
        JPanel filaHeaderTop = new JPanel(new BorderLayout(4, 0));
        filaHeaderTop.setOpaque(false);

        JLabel lblLotes = new JLabel(r.creadas + " lote" + (r.creadas == 1 ? "" : "s") + " creado" + (r.creadas == 1 ? "" : "s"));
        lblLotes.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblLotes.setForeground(TEXTO_MUTED);
        filaHeaderTop.add(lblLotes, BorderLayout.WEST);

        JLabel lblPill = new JLabel(tieneStock ? "● EN STOCK" : "AGOTADO");
        lblPill.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblPill.setOpaque(true);
        lblPill.setBackground(tieneStock ? VERDE_CLARO : new Color(241, 245, 249));
        lblPill.setForeground(tieneStock ? new Color(6, 95, 70) : TEXTO_MUTED);
        lblPill.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(tieneStock ? new Color(110, 231, 183) : BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        filaHeaderTop.add(lblPill, BorderLayout.EAST);

        norte.add(filaHeaderTop);
        norte.add(Box.createVerticalStrut(4));

        // Fila Principal: Nombre de la fórmula con ancho completo
        JLabel lblNombre = new JLabel("<html><body style='width: 195px;'><b>" + r.nombre + "</b></body></html>");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNombre.setForeground(SLATE_OSCURO);
        lblNombre.setToolTipText(r.nombre);
        norte.add(lblNombre);

        card.add(norte, BorderLayout.NORTH);

        // 2. Centro: Bloque Bodega + Medidor Circular (% DISPONIBLE)
        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setOpaque(false);

        JPanel boxStock = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(tieneStock ? new Color(167, 243, 208) : BORDE_SUAVE);
                g2.setStroke(new BasicStroke(1.4f)); // Borde interior nítido
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        boxStock.setOpaque(false);
        boxStock.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel lblTitDisp = new JLabel(tieneStock ? "DISPONIBLE BODEGA" : "TOTAL FABRICADO");
        lblTitDisp.setFont(new Font("Segoe UI", Font.BOLD, 8));
        lblTitDisp.setForeground(tieneStock ? VERDE_OSCURO : TEXTO_MUTED);

        JLabel lblValDisp = new JLabel(String.format("%.2f lb", tieneStock ? r.librasDisponibles : r.librasCreadas));
        lblValDisp.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblValDisp.setForeground(tieneStock ? VERDE_OSCURO : TEXTO_MUTED);

        JLabel lblSacos = new JLabel("(" + r.disponibles + " saco" + (r.disponibles == 1 ? "" : "s") + " listo" + (r.disponibles == 1 ? "" : "s") + ")");
        lblSacos.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSacos.setForeground(TEXTO_MUTED);

        boxStock.add(lblTitDisp, BorderLayout.NORTH);
        boxStock.add(lblValDisp, BorderLayout.CENTER);
        boxStock.add(lblSacos, BorderLayout.SOUTH);
        centro.add(boxStock, BorderLayout.CENTER);

        // Medidor Circular mostrando % DISPONIBLE
        Color colorMedidor = tieneStock 
            ? (porcentajeDisponible >= 50 ? VERDE : (porcentajeDisponible >= 20 ? VERDE_MENTA : NARANJA)) 
            : BORDE_MARCADO;

        GaugeCircular gauge = new GaugeCircular(porcentajeDisponible, colorMedidor, tieneStock);
        gauge.setPreferredSize(new Dimension(56, 56));
        gauge.setOpaque(false);
        JPanel gaugeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        gaugeWrap.setOpaque(false);
        gaugeWrap.add(gauge);
        centro.add(gaugeWrap, BorderLayout.EAST);

        card.add(centro, BorderLayout.CENTER);

        // 3. Sur: Métricas Detalladas + Botón de Auditoría
        JPanel sur = new JPanel(new BorderLayout(0, 6));
        sur.setOpaque(false);

        JPanel pnlMetricas = new JPanel(new GridLayout(3, 1, 0, 3));
        pnlMetricas.setOpaque(false);
        pnlMetricas.add(crearLineaInfo("% Disponible (lb):", String.format("%.1f%%", porcentajeDisponible)));
        pnlMetricas.add(crearLineaInfo("Vendido:", String.format("%.2f lb (%d)", r.librasVendidas, r.vendidas)));
        pnlMetricas.add(crearLineaInfo("Total fabricado:", String.format("%.2f lb", r.librasCreadas)));
        sur.add(pnlMetricas, BorderLayout.NORTH);

        JButton btnDetalle = new JButton("Ver Detalle y Lotes");
        btnDetalle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnDetalle.setBackground(new Color(241, 245, 249));
        btnDetalle.setForeground(SLATE_OSCURO);
        btnDetalle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        btnDetalle.setFocusPainted(false);
        btnDetalle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDetalle.addActionListener(e -> mostrarDialogoDetalleFormulacion(r));
        sur.add(btnDetalle, BorderLayout.SOUTH);

        card.add(sur, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mostrarDialogoDetalleFormulacion(r);
                }
            }
        });

        return card;
    }

    // ============================================================
    //  GAUGE CIRCULAR DE DISPONIBILIDAD (100% LLENO -> 0% VACÍO)
    // ============================================================
    private class GaugeCircular extends JPanel {
        private final double porcentaje;
        private final Color color;
        private final boolean tieneStock;

        GaugeCircular(double porcentaje, Color color, boolean tieneStock) {
            this.porcentaje = porcentaje;
            this.color = color;
            this.tieneStock = tieneStock;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            // Fondo circular
            g2.setColor(new Color(255, 255, 255, 245));
            g2.fillOval(x - 2, y - 2, size + 4, size + 4);
            g2.setColor(BORDE_SUAVE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Ellipse2D.Double(x - 2, y - 2, size + 4, size + 4));

            // Pista gris clara del aro
            g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(226, 232, 240));
            g2.draw(new Ellipse2D.Double(x, y, size, size));

            // Arco de Stock Disponible: Si hay stock, dibuja el porcentaje. Si es 0%, no dibuja arco activo
            if (porcentaje > 0.001) {
                double angulo = 360.0 * (porcentaje / 100.0);
                g2.setColor(color);
                g2.draw(new Arc2D.Double(x, y, size, size, 90, -angulo, Arc2D.OPEN));
            }

            // Texto central: Porcentaje (0% si está agotado, 100% si está completo)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(tieneStock ? SLATE_OSCURO : TEXTO_MUTED);
            String texto = String.format("%.0f%%", porcentaje);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, x + (size - fm.stringWidth(texto)) / 2, y + size / 2 + 1);

            // Subtítulo pequeño debajo del porcentaje ("Disp" o "Agot")
            g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
            g2.setColor(tieneStock ? VERDE_OSCURO : TEXTO_MUTED);
            String subtexto = tieneStock ? "DISP" : "AGOT";
            FontMetrics fmSub = g2.getFontMetrics();
            g2.drawString(subtexto, x + (size - fmSub.stringWidth(subtexto)) / 2, y + size / 2 + 10);

            g2.dispose();
        }
    }

    // ============================================================
    //  MODAL DE AUDITORÍA Y DETALLE DE FORMULACIÓN
    // ============================================================
    private void mostrarDialogoDetalleFormulacion(ResumenFormula r) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Detalle y Lotes - " + r.nombre, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(640, 510);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(FONDO);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(SLATE_OSCURO);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel lblTit = new JLabel("Auditoría: " + r.nombre);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTit.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Historial de " + r.creadas + " lotes producidos en planta");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(226, 232, 240));

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        dialog.add(pnlHeader, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new BorderLayout(8, 8));
        pnlCentro.setBackground(FONDO);
        pnlCentro.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        String[] cols = {"Lote ID", "Fecha", "Hora", "Libras", "Costo Lote", "Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(SLATE_OSCURO);
        table.getTableHeader().setForeground(Color.WHITE);

        for (com.feedformulator.model.Formulacion f : r.lotes) {
            String est = f.getEstado() != null ? f.getEstado() : "Disponible";
            model.addRow(new Object[]{
                "#" + f.getId(),
                f.getFecha() != null ? f.getFecha() : "-",
                f.getHora() != null ? f.getHora() : "-",
                String.format("%.2f lb", f.getTotalLibras()),
                String.format("$%.2f", f.getCostoTotal()),
                est
            });
        }

        pnlCentro.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel pnlResumen = new JPanel(new GridLayout(1, 4, 8, 0));
        pnlResumen.setBackground(Color.WHITE);
        pnlResumen.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        double pctDisp = r.librasCreadas > 0 ? (r.librasDisponibles / r.librasCreadas) * 100.0 : 0;
        pnlResumen.add(new JLabel("<html><small style='color:#64748B'>DISPONIBLE</small><br><b style='color:#059669'>" + String.format("%.2f lb (%.1f%%)", r.librasDisponibles, pctDisp) + "</b></html>"));
        pnlResumen.add(new JLabel("<html><small style='color:#64748B'>VENDIDO</small><br><b style='color:#0284C7'>" + String.format("%.2f lb", r.librasVendidas) + "</b></html>"));
        pnlResumen.add(new JLabel("<html><small style='color:#64748B'>TOTAL CREADO</small><br><b>" + String.format("%.2f lb", r.librasCreadas) + "</b></html>"));
        pnlResumen.add(new JLabel("<html><small style='color:#64748B'>INVERSIÓN</small><br><b>$" + String.format("%.2f", r.costoTotalAcumulado) + "</b></html>"));

        pnlCentro.add(pnlResumen, BorderLayout.SOUTH);
        dialog.add(pnlCentro, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFooter.setBackground(FONDO);
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.addActionListener(e -> dialog.dispose());
        pnlFooter.add(btnCerrar);
        dialog.add(pnlFooter, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ============================================================
    //  TARJETAS DE MATERIAS PRIMAS
    // ============================================================
    private void reconstruirTarjetas(List<MateriaPrima> productos) {
        panelTarjetas.removeAll();
        for (int i = 0; i < productos.size(); i++) {
            panelTarjetas.add(crearTarjetaProducto(productos.get(i), i));
        }
        panelTarjetas.revalidate();
        panelTarjetas.repaint();
    }

    private JPanel crearTarjetaProducto(MateriaPrima mp, int rowIndex) {
        double total = mp.getCantidadTotal();
        double disponible = mp.getCantidadDisponible();
        double porcentaje = total > 0 ? Math.max(0, Math.min(100, (disponible / total) * 100.0)) : 0;
        Color colorGauge = colorParaPorcentaje(porcentaje);
        String emoji = iconoParaMaterial(mp.getNombre());
        BufferedImage imagenFondo = cargarImagenParaProducto(mp);

        TarjetaMaterial card = new TarjetaMaterial(emoji, imagenFondo);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setPreferredSize(new Dimension(230, 210));

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);

        JPanel infoNombre = new JPanel();
        infoNombre.setOpaque(false);
        infoNombre.setLayout(new BoxLayout(infoNombre, BoxLayout.Y_AXIS));
        JLabel lblNombre = new JLabel(mp.getNombre());
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNombre.setForeground(new Color(40, 50, 40));
        JLabel lblCodigo = new JLabel(mp.getCodigo() != null ? mp.getCodigo() : "---");
        lblCodigo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCodigo.setForeground(new Color(130, 140, 130));
        infoNombre.add(lblNombre);
        infoNombre.add(lblCodigo);
        norte.add(infoNombre, BorderLayout.WEST);

        JPanel miniBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        miniBotones.setOpaque(false);
        JButton btnMiniEditar = crearMiniBoton("editar", "Editar este producto");
        JButton btnMiniDetalle = crearMiniBoton("detalle", "Ver detalles completos");
        btnMiniEditar.addActionListener(e -> {
            tabla.setRowSelectionInterval(rowIndex, rowIndex);
            editarProducto();
        });
        btnMiniDetalle.addActionListener(e -> {
            tabla.setRowSelectionInterval(rowIndex, rowIndex);
            mostrarDetalleProducto(mp, rowIndex);
        });
        miniBotones.add(btnMiniEditar);
        miniBotones.add(btnMiniDetalle);
        norte.add(miniBotones, BorderLayout.EAST);

        card.add(norte, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setOpaque(false);

        JLabel lblStock = new JLabel("<html>En Stock:<br>" + String.format("%.2f", disponible) + " " + mp.getUnidad() + "</html>");
        lblStock.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStock.setForeground(new Color(30, 90, 40));
        centro.add(lblStock, BorderLayout.WEST);

        GaugeCircular gauge = new GaugeCircular(porcentaje, colorGauge, disponible > 0);
        gauge.setPreferredSize(new Dimension(52, 52));
        gauge.setOpaque(false);
        JPanel gaugeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        gaugeWrap.setOpaque(false);
        gaugeWrap.add(gauge);
        centro.add(gaugeWrap, BorderLayout.EAST);

        card.add(centro, BorderLayout.CENTER);

        JPanel sur = new JPanel(new GridLayout(3, 1, 0, 2));
        sur.setOpaque(false);
        double pesoPromedio = mp.getNumeroSacos() > 0 && mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;
        sur.add(crearLineaInfo("Costo $/Unidad:", String.format("$%.4f", mp.getPrecioPorUnidad())));
        sur.add(crearLineaInfo("Sacos/Env:", mp.getNumeroSacos() + " sacos (" + String.format("%.0f", pesoPromedio) + " " + mp.getUnidad() + ")"));
        sur.add(crearLineaInfo("Precio Total:", String.format("$%.2f", mp.getPrecioTotal())));
        card.add(sur, BorderLayout.SOUTH);

        MouseAdapter listenerClic = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tabla.setRowSelectionInterval(rowIndex, rowIndex);
                resaltarTarjetaSeleccionada(card);
                if (e.getClickCount() == 2) {
                    mostrarDetalleProducto(mp, rowIndex);
                }
            }
        };
        agregarClickRecursivo(card, listenerClic);

        return card;
    }

    private JButton crearMiniBoton(String tipo, String tooltip) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(70, 95, 70));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int w = getWidth(), h = getHeight();
                int cx = w / 2, cy = h / 2;

                if ("editar".equals(tipo)) {
                    g2.drawLine(cx - 5, cy + 5, cx + 4, cy - 4);
                    g2.drawLine(cx + 2, cy - 6, cx + 6, cy - 2);
                    int[] px = {cx - 7, cx - 5, cx - 3};
                    int[] py = {cy + 7, cy + 5, cy + 7};
                    g2.fillPolygon(px, py, 3);
                } else {
                    g2.drawRoundRect(cx - 6, cy - 8, 12, 16, 3, 3);
                    g2.drawLine(cx - 3, cy - 3, cx + 3, cy - 3);
                    g2.drawLine(cx - 3, cy, cx + 3, cy);
                    g2.drawLine(cx - 3, cy + 3, cx + 1, cy + 3);
                }
                g2.dispose();
            }
        };
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.setPreferredSize(new Dimension(26, 26));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(255, 255, 255, 210));
        btn.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 220), 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel crearLineaInfo(String etiqueta, String valor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel lblEt = new JLabel(etiqueta);
        lblEt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEt.setForeground(TEXTO_MUTED);
        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblVal.setForeground(SLATE_OSCURO);
        lblVal.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(lblEt, BorderLayout.WEST);
        p.add(lblVal, BorderLayout.EAST);
        return p;
    }

    private void agregarClickRecursivo(Component comp, MouseAdapter listener) {
        if (comp instanceof JButton) return;
        comp.addMouseListener(listener);
        if (comp instanceof Container) {
            for (Component hijo : ((Container) comp).getComponents()) {
                agregarClickRecursivo(hijo, listener);
            }
        }
    }

    private JPanel tarjetaSeleccionadaActual = null;

    private void resaltarTarjetaSeleccionada(JPanel card) {
        if (tarjetaSeleccionadaActual != null) {
            tarjetaSeleccionadaActual.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        }
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(VERDE, 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        tarjetaSeleccionadaActual = card;
    }

    private String categoriaParaMaterial(String nombreRaw) {
        String n = nombreRaw == null ? "" : nombreRaw.toLowerCase(Locale.ROOT);
        if (n.contains("maiz") || n.contains("maíz")) return "maiz";
        if (n.contains("melaza")) return "melaza";
        if (n.contains("aceite")) return "aceite";
        if (n.contains("harina") && n.contains("pescado")) return "harina_pescado";
        if (n.contains("sal")) return "sal";
        if (n.contains("calcio")) return "calcio";
        if (n.contains("soya") || n.contains("soja")) return "soya";
        if (n.contains("afrecho") || n.contains("cono") || n.contains("palmiste")) return "fibra";
        if (n.contains("vitamina")) return "vitaminas";
        if (n.contains("zol") || n.contains("premix") || n.contains("aditivo")) return "aditivos";
        return "generico";
    }

    private String iconoParaMaterial(String nombreRaw) {
        switch (categoriaParaMaterial(nombreRaw)) {
            case "maiz": return "🌽";
            case "melaza": return "🍯";
            case "aceite": return "🛢️";
            case "harina_pescado": return "🐟";
            case "sal": return "🧂";
            case "calcio": return "🦴";
            case "soya": return "🫘";
            case "fibra": return "🌾";
            case "vitaminas": return "💊";
            case "aditivos": return "🧪";
            default: return "📦";
        }
    }

    private static final Map<String, BufferedImage> CACHE_IMAGENES_MATERIAL = new HashMap<>();

    private BufferedImage cargarImagenMaterial(String nombreMaterial) {
        String categoria = categoriaParaMaterial(nombreMaterial);
        if (CACHE_IMAGENES_MATERIAL.containsKey(categoria)) {
            return CACHE_IMAGENES_MATERIAL.get(categoria);
        }
        BufferedImage img = intentarCargarImagen("/icons/materiales/" + categoria + ".jpg");
        if (img == null) img = intentarCargarImagen("/icons/materiales/" + categoria + ".png");
        CACHE_IMAGENES_MATERIAL.put(categoria, img);
        return img;
    }

    private BufferedImage intentarCargarImagen(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    private static final Map<String, BufferedImage> CACHE_IMAGENES_ARCHIVO = new HashMap<>();

    private BufferedImage cargarImagenParaProducto(MateriaPrima mp) {
        String path = mp.getImagenPath();
        if (path != null && !path.trim().isEmpty()) {
            if (CACHE_IMAGENES_ARCHIVO.containsKey(path)) {
                BufferedImage cacheada = CACHE_IMAGENES_ARCHIVO.get(path);
                if (cacheada != null) return cacheada;
            } else {
                BufferedImage img = null;
                File f = new File(path);
                if (f.exists()) {
                    try { img = ImageIO.read(f); } catch (IOException e) { img = null; }
                }
                CACHE_IMAGENES_ARCHIVO.put(path, img);
                if (img != null) return img;
            }
        }
        return cargarImagenMaterial(mp.getNombre());
    }

    private void subirImagenProducto(MateriaPrima mp, Window ventanaPadre) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen para " + mp.getNombre());
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes (jpg, jpeg, png)", "jpg", "jpeg", "png"));
        if (fc.showOpenDialog(ventanaPadre) != JFileChooser.APPROVE_OPTION) return;

        File origen = fc.getSelectedFile();
        try {
            File carpeta = new File(DatabaseManager.getCarpetaImagenesMateriales());
            if (!carpeta.exists()) carpeta.mkdirs();

            String nombreOrigen = origen.getName().toLowerCase(Locale.ROOT);
            String ext = nombreOrigen.endsWith(".png") ? "png" : "jpg";
            File destino = new File(carpeta, "producto_" + mp.getId() + "." + ext);

            java.nio.file.Files.copy(origen.toPath(), destino.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            if (mp.getImagenPath() != null) CACHE_IMAGENES_ARCHIVO.remove(mp.getImagenPath());
            CACHE_IMAGENES_ARCHIVO.remove(destino.getAbsolutePath());

            dao.actualizarImagen(mp.getId(), destino.getAbsolutePath());
            DialogStyler.mostrarExito(this, "Imagen actualizada para <b>" + mp.getNombre() + "</b>.", "Listo");
            cargarDatos();
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "No se pudo guardar la imagen: " + ex.getMessage());
        }
    }

    private void quitarImagenProducto(MateriaPrima mp) {
        if (!DialogStyler.confirmar(this, "¿Quitar la imagen de fondo de <b>" + mp.getNombre() + "</b>?", "Quitar imagen")) return;
        try {
            if (mp.getImagenPath() != null) CACHE_IMAGENES_ARCHIVO.remove(mp.getImagenPath());
            dao.actualizarImagen(mp.getId(), null);
            cargarDatos();
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private Color colorParaPorcentaje(double pct) {
        if (pct >= 60) return new Color(46, 160, 67);
        if (pct >= 30) return new Color(230, 150, 30);
        return new Color(210, 60, 50);
    }

    private class TarjetaMaterial extends JPanel {
        private final String emoji;
        private final BufferedImage imagenFondo;

        TarjetaMaterial(String emoji, BufferedImage imagenFondo) {
            this.emoji = emoji;
            this.imagenFondo = imagenFondo;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            Shape clipOriginal = g2.getClip();
            g2.clip(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 16, 16));

            if (imagenFondo != null) {
                int iw = imagenFondo.getWidth(), ih = imagenFondo.getHeight();
                double escala = Math.max((double) w / iw, (double) h / ih);
                int dw = (int) Math.ceil(iw * escala), dh = (int) Math.ceil(ih * escala);
                int dx = (w - dw) / 2, dy = (h - dh) / 2;
                g2.drawImage(imagenFondo, dx, dy, dw, dh, null);
                g2.setColor(new Color(255, 255, 255, 175));
                g2.fillRect(0, 0, w, h);
            } else {
                GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, w, h, new Color(240, 247, 240));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
                Font fEmoji = new Font("Segoe UI Emoji", Font.PLAIN, (int) (h * 1.1));
                g2.setFont(fEmoji);
                FontMetrics fm = g2.getFontMetrics();
                int ex = w - fm.stringWidth(emoji) + (int) (h * 0.28);
                int ey = h + (int) (h * 0.22);
                g2.drawString(emoji, ex, ey);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

            g2.setClip(clipOriginal);
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.dispose();

            super.paintComponent(g);
        }
    }

    private void mostrarDetalleProducto(MateriaPrima mp, int rowIndex) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Detalle de Producto", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 520);
        dialog.setLocationRelativeTo(this);
        DialogStyler.aplicar(dialog, mp.getNombre(), iconoParaMaterial(mp.getNombre()));

        JPanel form = DialogStyler.crearPanelFormulario();
        int fila = 0;
        double pesoPromedio = mp.getNumeroSacos() > 0 && mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;
        String sueltosStr = mp.getLibrasSueltas() != null && mp.getLibrasSueltas() > 0 ? String.format("%.2f", mp.getLibrasSueltas()) : "0.00";

        String[][] filas = {
            {"Código:", mp.getCodigo() != null ? mp.getCodigo() : "---"},
            {"Unidad:", mp.getUnidad()},
            {"Sacos/Envases:", String.valueOf(mp.getNumeroSacos())},
            {"Peso/Envase:", String.format("%.2f %s", pesoPromedio, mp.getUnidad())},
            {"Sueltos:", sueltosStr + " " + mp.getUnidad()},
            {"Total:", String.format("%.2f %s", mp.getCantidadTotal(), mp.getUnidad())},
            {"Disponible:", String.format("%.2f %s", mp.getCantidadDisponible(), mp.getUnidad())},
            {"Precio Total:", String.format("$%.2f", mp.getPrecioTotal())},
            {"$/Unidad:", String.format("$%.4f", mp.getPrecioPorUnidad())},
            {"Origen:", mp.getOrigen() != null ? mp.getOrigen() : "INVENTARIO"}
        };

        for (String[] f : filas) {
            form.add(DialogStyler.crearLabel(f[0]), DialogStyler.crearGBC(0, fila));
            JLabel lblValor = new JLabel(f[1]);
            lblValor.setFont(new Font("Segoe UI", Font.BOLD, 13));
            form.add(lblValor, DialogStyler.crearGBC(1, fila));
            fila++;
        }

        JButton btnEditar = DialogStyler.crearBotonPrimario("✏️ Editar");
        JButton btnAgregarStock = DialogStyler.crearBotonSecundario("📦 Agregar Stock");
        JButton btnEliminar = DialogStyler.crearBotonPeligro("🗑️ Eliminar");
        JButton btnCerrar = DialogStyler.crearBotonSecundario("Cerrar");
        JButton btnImagen = DialogStyler.crearBotonSecundario(mp.getImagenPath() != null && !mp.getImagenPath().trim().isEmpty() ? "🖼️ Cambiar Imagen" : "🖼️ Subir Imagen");
        JButton btnQuitarImagen = DialogStyler.crearBotonSecundario("Quitar Imagen");
        btnQuitarImagen.setVisible(mp.getImagenPath() != null && !mp.getImagenPath().trim().isEmpty());

        btnEditar.addActionListener(e -> {
            tabla.setRowSelectionInterval(rowIndex, rowIndex);
            dialog.dispose();
            editarProducto();
        });
        btnAgregarStock.addActionListener(e -> {
            dialog.dispose();
            mostrarDialogoAgregarStock();
        });
        btnEliminar.addActionListener(e -> {
            tabla.setRowSelectionInterval(rowIndex, rowIndex);
            dialog.dispose();
            eliminarProducto();
        });
        btnImagen.addActionListener(e -> {
            dialog.dispose();
            subirImagenProducto(mp, SwingUtilities.getWindowAncestor(this));
        });
        btnQuitarImagen.addActionListener(e -> {
            dialog.dispose();
            quitarImagenProducto(mp);
        });
        btnCerrar.addActionListener(e -> dialog.dispose());

        JPanel filaImagen = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaImagen.setOpaque(false);
        filaImagen.add(btnImagen);
        filaImagen.add(btnQuitarImagen);
        GridBagConstraints gbcImagen = DialogStyler.crearGBC(0, fila);
        gbcImagen.gridwidth = 2;
        form.add(filaImagen, gbcImagen);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCerrar, btnEliminar, btnAgregarStock, btnEditar), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel crearBotones() {
        JPanel p = new JPanel(new GridLayout(2, 4, 10, 8));
        p.setBackground(FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));

        JButton btnAgregar = crearBotonAccion("➕ Agregar Producto", VERDE);
        JButton btnAgregarStock = crearBotonAccion("📦 Agregar Stock", AZUL);
        JButton btnEditar = crearBotonAccion("✏️ Editar Producto", NARANJA);
        JButton btnEliminar = crearBotonAccion("🗑️ Eliminar", ROJO);
        JButton btnReporte = crearBotonAccion("📄 PDF Stock", new Color(70, 100, 180));
        JButton btnReporteIngresos = crearBotonAccion("📈 PDF Ingresos", VERDE_MENTA);
        JButton btnReporteMovimientos = crearBotonAccion("📋 PDF Movimientos", MORADO);
        JButton btnRefresh = crearBotonAccion("🔄 Actualizar", new Color(100, 116, 139));

        btnAgregar.addActionListener(evt -> mostrarDialogoAgregarProducto());
        btnAgregarStock.addActionListener(evt -> mostrarDialogoAgregarStock());
        btnEditar.addActionListener(evt -> editarProducto());
        btnEliminar.addActionListener(evt -> eliminarProducto());
        btnReporte.addActionListener(evt -> exportarPDFStockResumen());
        btnReporteIngresos.addActionListener(evt -> exportarPDFIngresosDetallado());
        btnReporteMovimientos.addActionListener(evt -> exportarPDFMovimientos());
        btnRefresh.addActionListener(evt -> cargarDatos());

        p.add(btnAgregar);
        p.add(btnAgregarStock);
        p.add(btnEditar);
        p.add(btnEliminar);
        p.add(btnReporte);
        p.add(btnReporteIngresos);
        p.add(btnReporteMovimientos);
        p.add(btnRefresh);

        return p;
    }

    private JButton crearBotonAccion(String texto, Color bg) {
        JButton btn = new JButton(texto);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return btn;
    }

    public void cargarDatos() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            tableModel.setRowCount(0);
            List<MateriaPrima> productos = dao.listarTodos();

            for (MateriaPrima mp : productos) {
                String sueltosStr = mp.getLibrasSueltas() != null && mp.getLibrasSueltas() > 0 ? String.format("%.2f", mp.getLibrasSueltas()) : "0.00";
                double pesoPromedio = mp.getNumeroSacos() > 0 && mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;

                tableModel.addRow(new Object[]{
                    mp.getCodigo() != null ? mp.getCodigo() : "---",
                    mp.getNombre(),
                    mp.getUnidad(),
                    mp.getNumeroSacos(),
                    String.format("%.2f", pesoPromedio),
                    sueltosStr,
                    String.format("%.2f", mp.getCantidadTotal()),
                    String.format("%.2f", mp.getCantidadDisponible()),
                    String.format("$%.2f", mp.getPrecioTotal()),
                    String.format("$%.4f", mp.getPrecioPorUnidad()),
                    mp.getOrigen() != null ? mp.getOrigen() : "INVENTARIO"
                });
            }

            productosCache = productos;
            reconstruirTarjetas(productos);
            cargarFormulacionesResumen();

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al cargar datos: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void editarProducto() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un producto para editar.", "Aviso");
            return;
        }
        try {
            String nombre = (String) tableModel.getValueAt(row, 1);
            String origen = (String) tableModel.getValueAt(row, 10);
            
            if ("PEDIDO".equals(origen)) {
                DialogStyler.mostrarError(this,
                    "❌ NO SE PUEDE EDITAR DESDE INVENTARIO<br><br>" +
                    "Este producto fue <b>ingresado a través de un PEDIDO</b>.<br><br>" +
                    "Para modificarlo, debe ir a la pestaña <b>PROVEEDORES</b>.");
                return;
            }
            
            MateriaPrima productoAcumulado = dao.buscarPorNombre(nombre);
            if (productoAcumulado == null) {
                DialogStyler.mostrarError(this, "Producto no encontrado.");
                return;
            }
            
            List<IngresoHistorial> todosLosIngresos = obtenerHistorialPorNombre(nombre);
            List<IngresoHistorial> ingresosDeInventario = new ArrayList<>();
            
            for (IngresoHistorial ing : todosLosIngresos) {
                String origenIngreso = ing.getOrigen();
                if ("INVENTARIO".equals(origenIngreso) || origenIngreso == null) {
                    ingresosDeInventario.add(ing);
                }
            }
            
            if (ingresosDeInventario.isEmpty()) {
                DialogStyler.mostrarError(this,
                    "❌ NO HAY REGISTROS DE INVENTARIO PARA EDITAR<br><br>" +
                    "Este producto solo tiene registros de <b>PEDIDOS</b>.<br>" +
                    "Para modificarlo, vaya a la pestaña <b>PROVEEDORES</b>.");
                return;
            }
            
            String[] opciones = new String[ingresosDeInventario.size()];
            for (int i = 0; i < ingresosDeInventario.size(); i++) {
                IngresoHistorial ing = ingresosDeInventario.get(i);
                double precioPorEnvase = ing.getNumeroSacos() != null && ing.getNumeroSacos() > 0 ? ing.getPrecioTotal() / ing.getNumeroSacos() : 0;
                opciones[i] = String.format("#%d - %.2f %s | Sacos: %d | $%.2f c/u | %s %s", 
                    ing.getId(), ing.getCantidad(), ing.getUnidad(), ing.getNumeroSacos() != null ? ing.getNumeroSacos() : 0, precioPorEnvase, ing.getFecha(), ing.getHora());
            }
            
            String seleccion = (String) JOptionPane.showInputDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:13px;padding:8px;'><b>Producto:</b> " + nombre + "<br>Seleccione el registro de inventario a editar:</body></html>",
                "Seleccionar registro", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
            
            if (seleccion == null) return;
            
            int index = -1;
            for (int i = 0; i < opciones.length; i++) {
                if (opciones[i].equals(seleccion)) { index = i; break; }
            }
            if (index >= 0) {
                mostrarDialogoEditarIngreso(ingresosDeInventario.get(index), productoAcumulado);
            }
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al editar: " + ex.getMessage());
        }
    }

    private void eliminarProducto() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un producto para eliminar.", "Aviso");
            return;
        }

        try {
            String nombre = (String) tableModel.getValueAt(row, 1);
            String origen = (String) tableModel.getValueAt(row, 10);

            MateriaPrima productoAcumulado = dao.buscarPorNombre(nombre);
            if (productoAcumulado == null) {
                DialogStyler.mostrarError(this, "Producto no encontrado.");
                return;
            }

            boolean tieneStockReal = productoAcumulado.getCantidadTotal() > 0.0001;

            if ("PEDIDO".equals(origen) && tieneStockReal) {
                DialogStyler.mostrarError(this,
                    "❌ NO SE PUEDE ELIMINAR DESDE INVENTARIO<br><br>" +
                    "Este producto fue <b>ingresado a través de un PEDIDO</b>.<br><br>" +
                    "Para eliminarlo, vaya a la pestaña <b>PROVEEDORES</b>.");
                return;
            }

            List<IngresoHistorial> todosLosIngresos = obtenerHistorialPorNombre(nombre);
            List<IngresoHistorial> ingresosDeInventario = new ArrayList<>();

            for (IngresoHistorial ing : todosLosIngresos) {
                String origenIngreso = ing.getOrigen();
                if ("INVENTARIO".equals(origenIngreso) || origenIngreso == null) {
                    ingresosDeInventario.add(ing);
                }
            }

            if (ingresosDeInventario.isEmpty()) {
                if (productoAcumulado.getCantidadTotal() > 0.0001) {
                    DialogStyler.mostrarError(this, "Este producto todavía tiene stock ingresado por <b>PEDIDOS</b>. Modifíquelo en Proveedores.");
                    return;
                }
                if (DialogStyler.confirmar(this, "¿Desea eliminar completamente el producto <b>" + nombre + "</b>?", "Eliminar producto")) {
                    eliminarProductoCompleto(productoAcumulado.getId(), nombre);
                }
                return;
            }

            String[] opciones = new String[ingresosDeInventario.size()];
            for (int i = 0; i < ingresosDeInventario.size(); i++) {
                IngresoHistorial ing = ingresosDeInventario.get(i);
                double precioPorEnvase = ing.getNumeroSacos() != null && ing.getNumeroSacos() > 0 ? ing.getPrecioTotal() / ing.getNumeroSacos() : 0;
                opciones[i] = String.format("#%d - %.2f %s | Sacos: %d | $%.2f c/u | %s %s",
                    ing.getId(), ing.getCantidad(), ing.getUnidad(), ing.getNumeroSacos() != null ? ing.getNumeroSacos() : 0, precioPorEnvase, ing.getFecha(), ing.getHora());
            }

            String seleccion = (String) JOptionPane.showInputDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:13px;padding:8px;'>Seleccione el registro de inventario a eliminar:</body></html>",
                "Eliminar ingreso", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

            if (seleccion == null) return;

            int index = -1;
            for (int i = 0; i < opciones.length; i++) {
                if (opciones[i].equals(seleccion)) { index = i; break; }
            }
            if (index < 0) return;

            IngresoHistorial ingSel = ingresosDeInventario.get(index);
            double cantidadDelLote = ingSel.getCantidad();
            double disponibleActual = productoAcumulado.getCantidadDisponible();

            if (cantidadDelLote > disponibleActual + 0.01) {
                DialogStyler.mostrarError(this, "No hay suficiente stock disponible para retirar esta cantidad sin quedar en negativo.");
                return;
            }

            if (!DialogStyler.confirmar(this, "¿Eliminar el ingreso #" + ingSel.getId() + " de <b>" + nombre + "</b>?", "Confirmar")) return;

            eliminarIngresoEspecifico(ingSel.getId(), productoAcumulado.getId(), nombre);

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar: " + ex.getMessage());
        }
    }

    private List<IngresoHistorial> obtenerHistorialPorNombre(String nombre) {
        List<IngresoHistorial> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            MateriaPrima mp = dao.buscarPorNombre(nombre);
            if (mp == null) return lista;

            String sql = "SELECT * FROM ingresos_historial WHERE producto_id = ? ORDER BY id ASC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, mp.getId());
            rs = ps.executeQuery();

            while (rs.next()) {
                IngresoHistorial ing = new IngresoHistorial();
                ing.setId(rs.getInt("id"));
                ing.setProductoId(rs.getInt("producto_id"));
                ing.setNombreProducto(rs.getString("nombre_producto"));
                ing.setUnidad(rs.getString("unidad"));
                ing.setCantidad(rs.getDouble("cantidad"));
                ing.setPrecioUnitario(rs.getDouble("precio_unitario"));
                ing.setPrecioTotal(rs.getDouble("precio_total"));
                ing.setNumeroSacos(rs.getInt("numero_sacos"));
                ing.setPesoSaco(rs.getDouble("peso_saco"));
                ing.setLibrasSueltas(rs.getDouble("libras_sueltas"));
                ing.setFecha(rs.getString("fecha_ingreso"));
                ing.setHora(rs.getString("hora_ingreso"));
                ing.setOrigen(rs.getString("origen"));
                lista.add(ing);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    private void eliminarIngresoEspecifico(int ingresoId, int productoId, String nombreProducto) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sqlIngreso = "SELECT * FROM ingresos_historial WHERE id = ?";
            ps = conn.prepareStatement(sqlIngreso);
            ps.setInt(1, ingresoId);
            rs = ps.executeQuery();

            if (!rs.next()) throw new SQLException("Ingreso no encontrado");

            double cantidadEliminar = rs.getDouble("cantidad");
            double precioTotalEliminar = rs.getDouble("precio_total");
            int sacosEliminar = rs.getInt("numero_sacos");
            double librasSueltasEliminar = rs.getDouble("libras_sueltas");
            rs.close();
            ps.close();

            String sqlMp = "SELECT * FROM materias_primas WHERE id = ?";
            ps = conn.prepareStatement(sqlMp);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();

            if (!rs.next()) throw new SQLException("Producto no encontrado");

            double stockActual = rs.getDouble("cantidad_disponible");
            double cantidadTotalActual = rs.getDouble("cantidad_total");
            double precioTotalActual = rs.getDouble("precio_total");
            int totalSacosActual = rs.getInt("numero_sacos");
            double librasSueltasActual = rs.getDouble("libras_sueltas");
            rs.close();
            ps.close();

            double cantidadAEliminar = Math.min(cantidadEliminar, stockActual);
            double nuevoStock = Math.max(0, stockActual - cantidadAEliminar);
            double nuevaCantidadTotal = Math.max(0, cantidadTotalActual - cantidadAEliminar);
            double nuevoPrecioTotal = Math.max(0, precioTotalActual - (precioTotalEliminar * (cantidadAEliminar / cantidadEliminar)));
            double nuevoPrecioPorUnidad = nuevaCantidadTotal > 0 ? nuevoPrecioTotal / nuevaCantidadTotal : 0;
            int nuevosSacos = Math.max(0, totalSacosActual - sacosEliminar);
            double nuevasLibrasSueltas = Math.max(0, librasSueltasActual - librasSueltasEliminar);

            boolean quedaStock = nuevaCantidadTotal > 0.0001;

            if (quedaStock) {
                String sqlUpdateMp = "UPDATE materias_primas SET cantidad_disponible = ?, cantidad_total = ?, precio_total = ?, precio_por_unidad = ?, numero_sacos = ?, libras_sueltas = ? WHERE id = ?";
                ps = conn.prepareStatement(sqlUpdateMp);
                ps.setDouble(1, nuevoStock);
                ps.setDouble(2, nuevaCantidadTotal);
                ps.setDouble(3, nuevoPrecioTotal);
                ps.setDouble(4, nuevoPrecioPorUnidad);
                ps.setInt(5, nuevosSacos);
                ps.setDouble(6, nuevasLibrasSueltas);
                ps.setInt(7, productoId);
                ps.executeUpdate();
                ps.close();

                String sqlDeleteIngreso = "DELETE FROM ingresos_historial WHERE id = ?";
                ps = conn.prepareStatement(sqlDeleteIngreso);
                ps.setInt(1, ingresoId);
                ps.executeUpdate();
                ps.close();

                conn.commit();
            } else {
                eliminarProductoCompleto(productoId, nombreProducto);
                return;
            }

            cargarDatos();
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) ((MainFrame) window).refrescarDashboard();

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) { conn.setAutoCommit(true); conn.close(); }
        }
    }

    private void eliminarProductoCompleto(int productoId, String nombre) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String[] deletes = {
                "DELETE FROM movimientos_inventario WHERE materia_prima_id = ?",
                "DELETE FROM ingresos_historial WHERE producto_id = ?",
                "DELETE FROM formulacion_detalle WHERE materia_prima_id = ?",
                "DELETE FROM compras_proveedores WHERE materia_prima_id = ?",
                "DELETE FROM historial_precios WHERE producto_id = ?",
                "DELETE FROM materias_primas WHERE id = ?"
            };

            for (String sql : deletes) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, productoId);
                ps.executeUpdate();
                ps.close();
            }

            conn.commit();
            cargarDatos();

            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) ((MainFrame) window).refrescarDashboard();

            DialogStyler.mostrarExito(this, "Producto <b>" + nombre + "</b> eliminado completamente.", "Listo");

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) {} }
            DialogStyler.mostrarError(this, "Error al eliminar producto: " + e.getMessage());
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {} }
        }
    }

    private void exportarPDFStockResumen() {
        try {
            List<MateriaPrima> lista = dao.listarTodos();
            if (lista.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, "No hay materiales para exportar.", "Aviso");
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Reporte_Stock_Resumen.pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".pdf")) ruta += ".pdf";
                ReporteGenerator.generarReporteStock(lista, ruta);
                DialogStyler.mostrarExito(this, "Reporte generado:<br><b>" + ruta + "</b>", "PDF Generado");
            }
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al generar PDF: " + ex.getMessage());
        }
    }

    private void exportarPDFIngresosDetallado() {
        try {
            List<MateriaPrima> lista = dao.listarTodos();
            if (lista.isEmpty()) return;
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Reporte_Ingresos_Detallado.pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".pdf")) ruta += ".pdf";
                ReporteIngresosDetallado.generarReporteIngresosDetallado(lista, new MovimientoInventarioDAO(), ruta);
                DialogStyler.mostrarExito(this, "Reporte generado:<br><b>" + ruta + "</b>", "PDF Generado");
            }
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private void exportarPDFMovimientos() {
        try {
            List<MateriaPrima> lista = dao.listarTodos();
            if (lista.isEmpty()) return;
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Historial_Movimientos.pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".pdf")) ruta += ".pdf";
                ReporteMovimientosMejorado.generarReporteMovimientosMejorado(lista, new MovimientoInventarioDAO(), ruta);
                DialogStyler.mostrarExito(this, "Reporte generado:<br><b>" + ruta + "</b>", "PDF Generado");
            }
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private void mostrarDialogoAgregarProducto() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Agregar Nuevo Producto", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 680);
        dialog.setLocationRelativeTo(this);
        DialogStyler.aplicar(dialog, "Agregar Nuevo Producto", "➕");

        JPanel form = DialogStyler.crearPanelFormulario();
        JTextField txtNombre = DialogStyler.crearCampo(18);
        JComboBox<String> cmbUnidad = DialogStyler.crearCombo(new String[]{"lb", "litro"});
        JTextField txtNumSacos = DialogStyler.crearCampoNumerico(10);
        txtNumSacos.setText("0");
        JTextField txtPesoEnvase = DialogStyler.crearCampoNumerico(10);
        txtPesoEnvase.setText("0");
        JTextField txtCantidadSueltas = DialogStyler.crearCampoNumerico(10);
        txtCantidadSueltas.setText("0");
        JTextField txtPrecioEnvase = DialogStyler.crearCampoNumerico(10);

        JLabel lblPesoEnvase = DialogStyler.crearLabel("Peso por saco (libras):");
        JLabel lblCantidadSueltas = DialogStyler.crearLabel("Libras sueltas:");
        JLabel lblPrecioEnvase = DialogStyler.crearLabel("Precio por saco ($):");

        JPanel bannerPrecio = DialogStyler.crearBannerInfo("info", "Ingrese peso y precio para calcular el costo por unidad.");

        Runnable actualizarPreview = () -> {
            try {
                String pesoStr = txtPesoEnvase.getText().trim().replace(",", ".");
                String precStr = txtPrecioEnvase.getText().trim().replace(",", ".");
                if (!pesoStr.isEmpty() && !precStr.isEmpty()) {
                    double peso = Double.parseDouble(pesoStr);
                    double precio = Double.parseDouble(precStr);
                    if (peso > 0) {
                        double precioU = precio / peso;
                        ((JLabel) bannerPrecio.getComponent(0)).setText("<html><body style='width:440px'>✅ Precio por unidad: <b>$" + String.format("%.4f", precioU) + "</b></body></html>");
                    }
                }
            } catch (Exception ex) {}
        };

        cmbUnidad.addActionListener(evt -> {
            boolean esLitro = "litro".equals(cmbUnidad.getSelectedItem());
            lblPesoEnvase.setText(esLitro ? "Litros por envase:" : "Peso por saco (libras):");
            lblCantidadSueltas.setText(esLitro ? "Litros sueltos:" : "Libras sueltas:");
            lblPrecioEnvase.setText(esLitro ? "Precio por envase ($):" : "Precio por saco ($):");
            actualizarPreview.run();
        });

        int row = 0;
        form.add(DialogStyler.crearLabel("Nombre del producto:"), DialogStyler.crearGBC(0, row));
        form.add(txtNombre, DialogStyler.crearGBC(1, row++));
        form.add(DialogStyler.crearLabel("Unidad:"), DialogStyler.crearGBC(0, row));
        form.add(cmbUnidad, DialogStyler.crearGBC(1, row++));
        form.add(DialogStyler.crearLabel("Número de sacos/envases:"), DialogStyler.crearGBC(0, row));
        form.add(txtNumSacos, DialogStyler.crearGBC(1, row++));
        form.add(lblPesoEnvase, DialogStyler.crearGBC(0, row));
        form.add(txtPesoEnvase, DialogStyler.crearGBC(1, row++));
        form.add(lblCantidadSueltas, DialogStyler.crearGBC(0, row));
        form.add(txtCantidadSueltas, DialogStyler.crearGBC(1, row++));
        form.add(lblPrecioEnvase, DialogStyler.crearGBC(0, row));
        form.add(txtPrecioEnvase, DialogStyler.crearGBC(1, row++));

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Producto");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(evt -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) return;
            try {
                String unidad = (String) cmbUnidad.getSelectedItem();
                int numEnvases = Integer.parseInt(txtNumSacos.getText().trim());
                double pesoEnvase = Double.parseDouble(txtPesoEnvase.getText().trim().replace(",", "."));
                double cantidadSuelta = Double.parseDouble(txtCantidadSueltas.getText().trim().replace(",", "."));
                double precioEnvase = Double.parseDouble(txtPrecioEnvase.getText().trim().replace(",", "."));

                double cantidadTotal = (numEnvases * pesoEnvase) + cantidadSuelta;
                double precioPorUnidad = pesoEnvase > 0 ? precioEnvase / pesoEnvase : 0;
                double precioTotal = (numEnvases * precioEnvase) + (cantidadSuelta * precioPorUnidad);

                MateriaPrima mp = new MateriaPrima();
                mp.setNombre(nombre);
                mp.setUnidad(unidad);
                mp.setCantidadTotal(cantidadTotal);
                mp.setCantidadDisponible(cantidadTotal);
                mp.setPrecioTotal(precioTotal);
                mp.setPrecioPorUnidad(precioPorUnidad);
                mp.setNumeroSacos(numEnvases);
                mp.setPesoSaco(pesoEnvase > 0 ? pesoEnvase : 0);
                mp.setLibrasSueltas(cantidadSuelta);

                dao.guardarOActualizar(mp);
                cargarDatos();
                dialog.dispose();

                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof MainFrame) ((MainFrame) window).refrescarDashboard();

                DialogStyler.mostrarExito(this, "Producto <b>" + nombre + "</b> guardado correctamente.", "Éxito");
            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
            }
        });

        btnCancelar.addActionListener(evt -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void mostrarDialogoAgregarStock() {
        try {
            List<MateriaPrima> productos = dao.listarTodos();
            if (productos.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, "Primero agregue un producto nuevo.", "Aviso");
                return;
            }

            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Agregar Stock", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(650, 650);
            dialog.setLocationRelativeTo(this);
            DialogStyler.aplicar(dialog, "Agregar Stock a Producto Existente", "📦");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            JComboBox<MateriaPrima> cmbProducto = new JComboBox<>(productos.toArray(new MateriaPrima[0]));
            cmbProducto.setFont(DialogStyler.FUENTE_CAMPO);
            JTextField txtBuscar = DialogStyler.crearCampo(15);
            JTextField txtNumEnvases = DialogStyler.crearCampoNumerico(10);
            txtNumEnvases.setText("0");
            JComboBox<String> cmbPesoEnvase = new JComboBox<>();
            JTextField txtCantidadSueltas = DialogStyler.crearCampoNumerico(10);
            txtCantidadSueltas.setText("0");
            JTextField txtPrecio = DialogStyler.crearCampoNumerico(10);

            JLabel lblPesoEnvase = DialogStyler.crearLabel("Peso por envase (libras):");
            JLabel lblCantidadSueltas = DialogStyler.crearLabel("Cantidad suelta (libras):");
            JLabel lblPrecio = DialogStyler.crearLabel("Precio por envase ($):");
            JLabel lblInfo = new JLabel(" ");

            cmbProducto.addActionListener(evt -> {
                MateriaPrima p = (MateriaPrima) cmbProducto.getSelectedItem();
                if (p != null) {
                    double pesoDefinido = p.getPesoSaco() != null ? p.getPesoSaco() : 0;
                    cmbPesoEnvase.removeAllItems();
                    if (pesoDefinido > 0) {
                        cmbPesoEnvase.addItem(String.format("%.2f", pesoDefinido) + " (" + p.getUnidad() + "/envase)");
                    } else {
                        cmbPesoEnvase.addItem("0 (sin peso definido)");
                    }
                    lblInfo.setText("Stock actual: " + String.format("%.2f", p.getCantidadDisponible()) + " " + p.getUnidad());
                }
            });

            if (!productos.isEmpty()) {
                MateriaPrima p = productos.get(0);
                double pesoDef = p.getPesoSaco() != null ? p.getPesoSaco() : 0;
                cmbPesoEnvase.addItem(pesoDef > 0 ? String.format("%.2f", pesoDef) + " (" + p.getUnidad() + "/envase)" : "0");
            }

            int row = 0;
            gbc.gridx = 0; gbc.gridy = row; form.add(DialogStyler.crearLabel("Producto:"), gbc);
            gbc.gridx = 1; form.add(cmbProducto, gbc);

            row++; gbc.gridx = 0; gbc.gridy = row; form.add(DialogStyler.crearLabel("Número de sacos:"), gbc);
            gbc.gridx = 1; form.add(txtNumEnvases, gbc);

            row++; gbc.gridx = 0; gbc.gridy = row; form.add(lblPesoEnvase, gbc);
            gbc.gridx = 1; form.add(cmbPesoEnvase, gbc);

            row++; gbc.gridx = 0; gbc.gridy = row; form.add(lblCantidadSueltas, gbc);
            gbc.gridx = 1; form.add(txtCantidadSueltas, gbc);

            row++; gbc.gridx = 0; gbc.gridy = row; form.add(lblPrecio, gbc);
            gbc.gridx = 1; form.add(txtPrecio, gbc);

            JButton btnGuardar = DialogStyler.crearBotonPrimario("📦 Agregar Stock");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(evt -> {
                try {
                    MateriaPrima existente = (MateriaPrima) cmbProducto.getSelectedItem();
                    int numEnvases = Integer.parseInt(txtNumEnvases.getText().trim());
                    double pesoEnvase = existente.getPesoSaco() != null ? existente.getPesoSaco() : 0;
                    double cantidadSueltas = Double.parseDouble(txtCantidadSueltas.getText().trim().replace(",", "."));
                    double precioIngresado = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));

                    double cantidadTotal = (numEnvases * pesoEnvase) + cantidadSueltas;
                    double precioPorUnidad = pesoEnvase > 0 ? precioIngresado / pesoEnvase : existente.getPrecioPorUnidad();
                    double precioTotal = (numEnvases * precioIngresado) + (cantidadSueltas * precioPorUnidad);

                    MateriaPrima nuevo = new MateriaPrima();
                    nuevo.setNombre(existente.getNombre());
                    nuevo.setUnidad(existente.getUnidad());
                    nuevo.setCantidadTotal(cantidadTotal);
                    nuevo.setCantidadDisponible(cantidadTotal);
                    nuevo.setPrecioTotal(precioTotal);
                    nuevo.setPrecioPorUnidad(precioPorUnidad);
                    nuevo.setNumeroSacos(numEnvases);
                    nuevo.setPesoSaco(pesoEnvase);
                    nuevo.setLibrasSueltas(cantidadSueltas);
                    nuevo.setOrigen("INVENTARIO");

                    dao.guardarOActualizar(nuevo);
                    cargarDatos();
                    dialog.dispose();

                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window instanceof MainFrame) ((MainFrame) window).refrescarDashboard();

                    DialogStyler.mostrarExito(this, "Stock agregado a <b>" + existente.getNombre() + "</b>.", "Listo");
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                }
            });

            btnCancelar.addActionListener(evt -> dialog.dispose());

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
            dialog.setVisible(true);

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private void mostrarDialogoEditarIngreso(IngresoHistorial ingreso, MateriaPrima productoAcumulado) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Editar Ingreso #" + ingreso.getId(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 720);
        dialog.setLocationRelativeTo(this);
        DialogStyler.aplicar(dialog, "Editar Ingreso #" + ingreso.getId(), "✏️");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

        int row = 0;
        JTextField txtNombre = DialogStyler.crearCampo(18);
        txtNombre.setText(ingreso.getNombreProducto());
        txtNombre.setEditable(false);
        form.add(DialogStyler.crearLabel("Producto:"), DialogStyler.crearGBC(0, row));
        form.add(txtNombre, DialogStyler.crearGBC(1, row++));

        JTextField txtNumSacos = DialogStyler.crearCampoNumerico(10);
        txtNumSacos.setText(String.valueOf(ingreso.getNumeroSacos() != null ? ingreso.getNumeroSacos() : 0));
        form.add(DialogStyler.crearLabel("Sacos:"), DialogStyler.crearGBC(0, row));
        form.add(txtNumSacos, DialogStyler.crearGBC(1, row++));

        JTextField txtPesoSaco = DialogStyler.crearCampoNumerico(10);
        txtPesoSaco.setText(String.format("%.2f", ingreso.getPesoSaco() != null ? ingreso.getPesoSaco() : 0));
        form.add(DialogStyler.crearLabel("Peso saco:"), DialogStyler.crearGBC(0, row));
        form.add(txtPesoSaco, DialogStyler.crearGBC(1, row++));

        JTextField txtLibrasSueltas = DialogStyler.crearCampoNumerico(10);
        txtLibrasSueltas.setText(String.format("%.2f", ingreso.getLibrasSueltas() != null ? ingreso.getLibrasSueltas() : 0));
        form.add(DialogStyler.crearLabel("Sueltas:"), DialogStyler.crearGBC(0, row));
        form.add(txtLibrasSueltas, DialogStyler.crearGBC(1, row++));

        int numSacosOriginal = ingreso.getNumeroSacos() != null ? ingreso.getNumeroSacos() : 0;
        double precioPorSaco = numSacosOriginal > 0 ? ingreso.getPrecioTotal() / numSacosOriginal : 0;
        JTextField txtPrecioSaco = DialogStyler.crearCampoNumerico(15);
        txtPrecioSaco.setText(String.format("%.2f", precioPorSaco));
        form.add(DialogStyler.crearLabel("Precio saco ($):"), DialogStyler.crearGBC(0, row));
        form.add(txtPrecioSaco, DialogStyler.crearGBC(1, row++));

        JButton btnGuardar = DialogStyler.crearBotonPrimario("Guardar Cambios");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(evt -> {
            try {
                int numSacos = Integer.parseInt(txtNumSacos.getText().trim());
                double pesoSaco = Double.parseDouble(txtPesoSaco.getText().trim().replace(",", "."));
                double librasSueltas = Double.parseDouble(txtLibrasSueltas.getText().trim().replace(",", "."));
                double precioSaco = Double.parseDouble(txtPrecioSaco.getText().trim().replace(",", "."));

                double nuevaCantidad = (numSacos * pesoSaco) + librasSueltas;
                double precioPorUnidad = pesoSaco > 0 ? precioSaco / pesoSaco : 0;
                double precioTotal = (numSacos * precioSaco) + (librasSueltas * precioPorUnidad);

                actualizarIngresoHistorial(ingreso.getId(), nuevaCantidad, precioPorUnidad, precioTotal, numSacos, pesoSaco, librasSueltas);
                recalcularStockAcumulado(productoAcumulado.getId());

                dialog.dispose();
                cargarDatos();

                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof MainFrame) ((MainFrame) window).refrescarDashboard();

                DialogStyler.mostrarExito(this, "Ingreso #" + ingreso.getId() + " actualizado.", "Listo");

            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
            }
        });

        btnCancelar.addActionListener(evt -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void actualizarIngresoHistorial(int id, double cantidad, double precioUnitario, double precioTotal, int numSacos, double pesoSaco, double librasSueltas) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            String sql = "UPDATE ingresos_historial SET cantidad = ?, precio_unitario = ?, precio_total = ?, numero_sacos = ?, peso_saco = ?, libras_sueltas = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, cantidad);
            ps.setDouble(2, precioUnitario);
            ps.setDouble(3, precioTotal);
            ps.setInt(4, numSacos);
            ps.setDouble(5, pesoSaco);
            ps.setDouble(6, librasSueltas);
            ps.setInt(7, id);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps, null);
        }
    }

    private void recalcularStockAcumulado(int productoId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            String sqlTotal = "SELECT SUM(cantidad) as total, SUM(precio_total) as total_precio, SUM(numero_sacos) as total_sacos, SUM(libras_sueltas) as total_sueltas, MAX(peso_saco) as peso_saco_ref FROM ingresos_historial WHERE producto_id = ?";
            ps = conn.prepareStatement(sqlTotal);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            double totalCantidad = 0, totalPrecio = 0, totalSueltas = 0, pesoSacoRef = 0;
            int totalSacos = 0;
            if (rs.next()) {
                totalCantidad = rs.getDouble("total");
                totalPrecio = rs.getDouble("total_precio");
                totalSacos = rs.getInt("total_sacos");
                totalSueltas = rs.getDouble("total_sueltas");
                pesoSacoRef = rs.getDouble("peso_saco_ref");
            }
            rs.close();
            ps.close();

            String sqlUsado = "SELECT SUM(cantidad_usada) as total_usado FROM formulacion_detalle WHERE materia_prima_id = ?";
            ps = conn.prepareStatement(sqlUsado);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            double totalUsado = 0;
            if (rs.next()) totalUsado = rs.getDouble("total_usado");
            rs.close();
            ps.close();

            double stockDisponible = Math.max(0, totalCantidad - totalUsado);
            double precioPorUnidad = totalCantidad > 0 ? totalPrecio / totalCantidad : 0;

            String sqlUpdate = "UPDATE materias_primas SET cantidad_total = ?, cantidad_disponible = ?, precio_total = ?, precio_por_unidad = ?, numero_sacos = ?, libras_sueltas = ? WHERE id = ?";
            ps = conn.prepareStatement(sqlUpdate);
            ps.setDouble(1, totalCantidad);
            ps.setDouble(2, stockDisponible);
            ps.setDouble(3, totalPrecio);
            ps.setDouble(4, precioPorUnidad);
            ps.setInt(5, totalSacos);
            ps.setDouble(6, totalSueltas);
            ps.setInt(7, productoId);
            ps.executeUpdate();

        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }
}