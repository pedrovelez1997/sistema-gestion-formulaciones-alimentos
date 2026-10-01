package com.feedformulator.ui;

import com.feedformulator.util.SessionManager;
import com.feedformulator.dao.FormulacionDAO;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.model.DetalleFormulacion;
import com.feedformulator.model.Formulacion;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.report.ReporteGenerator;
import com.feedformulator.util.DensidadManager;

import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel de Formulación e Inventario de Dietas Balanceadas
 * Rediseñado con Tarjetas de Alta Densidad, Métricas KPI y Selector de Vista (Tarjetas/Tabla).
 */
public class FormulacionPanel extends JPanel {
    private final FormulacionDAO formulacionDAO = new FormulacionDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    private StockPanel stockPanel;

    // Componentes del Formulario Izquierdo
    private DefaultTableModel modelIngredientes;
    private JTable tablaIngredientes;
    private JLabel lblTotalLibras, lblTotalCosto, lblCostoPromedio;
    private JTextField txtNombreFormula;

    // Componentes del Historial / Inventario Derecho
    private DefaultTableModel modelHistorial;
    private JTable tablaHistorial;
    private JTextField txtBuscarHistorial;
    private TableRowSorter<DefaultTableModel> sorterHistorial;
    
    // Vista de Tarjetas y KPIs
    private JPanel pnlContenedorTarjetas;
    private JScrollPane scrollTarjetas;
    private CardLayout cardLayoutHistorial;
    private JPanel pnlCardsOrTable;
    private JLabel lblKpiTotalLotes, lblKpiDisponibles, lblKpiVendidos, lblKpiInversion;
    private String filtroEstado = "TODOS"; // "TODOS", "DISPONIBLE", "VENDIDO"
    private boolean modoTarjetas = true;
    private JButton btnSwitchVista;
    private JButton btnFiltroTodos, btnFiltroDisp, btnFiltroVend;

    // Lista en memoria de todas las formulaciones para renderizado de tarjetas
    private List<Formulacion> listaFormulacionesCache = new ArrayList<>();
    private List<DetalleFormulacion> ingredientesActuales = new ArrayList<>();

    // Paleta de Colores Corporativa
    private static final Color VERDE = new Color(5, 150, 105);         // Esmeralda #059669
    private static final Color VERDE_OSCURO = new Color(4, 120, 87);   // Esmeralda oscuro #047857
    private static final Color FONDO = new Color(248, 250, 252);       // Slate-50 #F8FAFC
    private static final Color ROJO = new Color(220, 38, 38);          // Rojo carmesí #DC2626
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);   // #0F172A
    private static final Color SECUNDARIO_FONDO = new Color(241, 245, 249); // #F1F5F9
    private static final Color SECUNDARIO_TEXTO = new Color(51, 65, 85);    // #334155
    private static final Color AZUL_CORP = new Color(2, 132, 199);     // #0284C7
    private static final Color BORDE_SUAVE = new Color(226, 232, 240); // #E2E8F0
    private static final Color TEXTO_MUTED = new Color(100, 116, 139); // #64748B

    private static final String CONTRASENA = "12151215";

    public FormulacionPanel(StockPanel stockPanel) {
        this.stockPanel = stockPanel;
        setLayout(new BorderLayout(10, 10));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(crearTitulo(), BorderLayout.NORTH);
        add(crearContenido(), BorderLayout.CENTER);
        cargarHistorial();
    }

    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("🧪", "Nueva Formulación & Control de Lotes", "Combina materias primas, calcula costos y audita lotes producidos");
    }

    private JSplitPane crearContenido() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, crearPanelIzquierdo(), crearPanelDerecho());
        split.setDividerLocation(580);
        split.setBackground(FONDO);
        split.setBorder(null);
        return split;
    }

    // ============================================================
    //  PANEL IZQUIERDO: FORMULADOR DE SACO
    // ============================================================
    private JPanel crearPanelIzquierdo() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(FONDO);

        JPanel pNombre = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pNombre.setBackground(FONDO);
        JLabel lbl = new JLabel("Nombre del producto:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(SLATE_OSCURO);
        txtNombreFormula = new JTextField(22);
        txtNombreFormula.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pNombre.add(lbl);
        pNombre.add(txtNombreFormula);
        p.add(pNombre, BorderLayout.NORTH);

        String[] cols = {"Ingrediente", "Unidad", "Cantidad", "$/Unidad", "Subtotal"};
        modelIngredientes = new DefaultTableModel(cols, 0) {
            @Override 
            public boolean isCellEditable(int r, int c) { 
                return false; 
            }
        };
        tablaIngredientes = new JTable(modelIngredientes);
        estilizarTabla(tablaIngredientes);
        p.add(new JScrollPane(tablaIngredientes), BorderLayout.CENTER);

        JPanel pBottom = new JPanel(new BorderLayout(0, 8));
        pBottom.setBackground(FONDO);

        // Tarjeta de Totales
        JPanel pTotales = new JPanel(new GridLayout(1, 3, 0, 0));
        pTotales.setBackground(Color.WHITE);
        pTotales.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel colPeso = new JPanel(new BorderLayout());
        colPeso.setOpaque(false);
        JLabel lblEtqPeso = new JLabel("PESO TOTAL SACO");
        lblEtqPeso.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblEtqPeso.setForeground(TEXTO_MUTED);
        colPeso.add(lblEtqPeso, BorderLayout.NORTH);
        lblTotalLibras = new JLabel("0.00 lb");
        lblTotalLibras.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTotalLibras.setForeground(SLATE_OSCURO);
        colPeso.add(lblTotalLibras, BorderLayout.CENTER);

        JPanel colPromedio = new JPanel(new BorderLayout());
        colPromedio.setOpaque(false);
        colPromedio.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        JLabel lblEtqPromedio = new JLabel("COSTO PROMEDIO");
        lblEtqPromedio.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblEtqPromedio.setForeground(TEXTO_MUTED);
        colPromedio.add(lblEtqPromedio, BorderLayout.NORTH);
        lblCostoPromedio = new JLabel("$0.000 / lb");
        lblCostoPromedio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCostoPromedio.setForeground(SECUNDARIO_TEXTO);
        colPromedio.add(lblCostoPromedio, BorderLayout.CENTER);

        JPanel colCosto = new JPanel(new BorderLayout());
        colCosto.setOpaque(false);
        JLabel lblEtqCosto = new JLabel("COSTO TOTAL");
        lblEtqCosto.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblEtqCosto.setForeground(TEXTO_MUTED);
        colCosto.add(lblEtqCosto, BorderLayout.NORTH);
        lblTotalCosto = new JLabel("$0.00");
        lblTotalCosto.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTotalCosto.setForeground(VERDE);
        colCosto.add(lblTotalCosto, BorderLayout.CENTER);

        pTotales.add(colPeso);
        pTotales.add(colPromedio);
        pTotales.add(colCosto);

        // Botonera de Acción
        JPanel pBotones = new JPanel(new GridLayout(2, 3, 6, 6));
        pBotones.setBackground(FONDO);

        JButton btnAgregar = crearBotonEstilo("Agregar", VERDE, Color.WHITE);
        JButton btnQuitar = crearBotonEstilo("Quitar", ROJO, Color.WHITE);
        JButton btnEditar = crearBotonEstilo("Editar", SECUNDARIO_FONDO, SECUNDARIO_TEXTO);
        JButton btnGuardar = crearBotonEstilo("Guardar Lote", VERDE, Color.WHITE);
        JButton btnLimpiar = crearBotonEstilo("Limpiar", SECUNDARIO_FONDO, SECUNDARIO_TEXTO);
        JButton btnAjustar = crearBotonEstilo("Ajustar lb", SECUNDARIO_FONDO, SECUNDARIO_TEXTO);

        btnAgregar.addActionListener(evt -> mostrarDialogoAgregarIngrediente());
        btnQuitar.addActionListener(evt -> quitarIngrediente());
        btnEditar.addActionListener(evt -> editarIngrediente());
        btnGuardar.addActionListener(evt -> guardarFormulacion());
        btnLimpiar.addActionListener(evt -> limpiarFormulario());
        btnAjustar.addActionListener(evt -> ajustarFormulacion());

        pBotones.add(btnAgregar);
        pBotones.add(btnQuitar);
        pBotones.add(btnEditar);
        pBotones.add(btnGuardar);
        pBotones.add(btnLimpiar);
        pBotones.add(btnAjustar);

        pBottom.add(pTotales, BorderLayout.NORTH);
        pBottom.add(pBotones, BorderLayout.SOUTH);
        p.add(pBottom, BorderLayout.SOUTH);
        return p;
    }

    // ============================================================
    //  PANEL DERECHO: INVENTARIO DE FORMULACIONES (TARJETAS + TABLA)
    // ============================================================
    private JPanel crearPanelDerecho() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(FONDO);

        // 1. ZONA SUPERIOR: KPI STATS CARDS + BUSCADOR Y FILTROS
        JPanel pnlNorteCompleto = new JPanel(new BorderLayout(0, 8));
        pnlNorteCompleto.setBackground(FONDO);

        // Fila 1: Mini Tarjetas KPI
        JPanel pnlKPIs = new JPanel(new GridLayout(1, 4, 8, 0));
        pnlKPIs.setBackground(FONDO);

        lblKpiTotalLotes = new JLabel("0", SwingConstants.CENTER);
        lblKpiDisponibles = new JLabel("0.00 lb", SwingConstants.CENTER);
        lblKpiVendidos = new JLabel("0", SwingConstants.CENTER);
        lblKpiInversion = new JLabel("$0.00", SwingConstants.CENTER);

        pnlKPIs.add(crearMiniCardKPI("TOTAL LOTES", lblKpiTotalLotes, SLATE_OSCURO));
        pnlKPIs.add(crearMiniCardKPI("DISPONIBLE BODEGA", lblKpiDisponibles, VERDE));
        pnlKPIs.add(crearMiniCardKPI("VENDIDOS", lblKpiVendidos, AZUL_CORP));
        pnlKPIs.add(crearMiniCardKPI("INVERSIÓN TOTAL", lblKpiInversion, SLATE_OSCURO));
        pnlNorteCompleto.add(pnlKPIs, BorderLayout.NORTH);

        // Fila 2: Cabecera con Buscador, Filtros y Switch de Vista
        JPanel pnlFiltrosBarra = new JPanel(new BorderLayout(6, 6));
        pnlFiltrosBarra.setBackground(FONDO);
        pnlFiltrosBarra.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

        // Izquierda: Buscador
        JPanel pnlBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlBusqueda.setBackground(FONDO);
        JLabel lblLupa = new JLabel("🔍");
        lblLupa.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarHistorial = new JTextField(14);
        txtBuscarHistorial.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarHistorial.setToolTipText("Filtrar por nombre o fecha");
        txtBuscarHistorial.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroGeneral(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroGeneral(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroGeneral(); }
        });
        pnlBusqueda.add(lblLupa);
        pnlBusqueda.add(txtBuscarHistorial);

        // Centro/Derecha: Píldoras de Filtro (Todos / Con Stock / Vendidos)
        JPanel pnlPildoras = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlPildoras.setBackground(FONDO);

        btnFiltroTodos = crearBotonPildora("Todos", true);
        btnFiltroDisp = crearBotonPildora("🟢 Stock", false);
        btnFiltroVend = crearBotonPildora("⚪ Vendidos", false);

        btnFiltroTodos.addActionListener(e -> setFiltroEstado("TODOS"));
        btnFiltroDisp.addActionListener(e -> setFiltroEstado("DISPONIBLE"));
        btnFiltroVend.addActionListener(e -> setFiltroEstado("VENDIDO"));

        btnSwitchVista = new JButton("📋 Ver Tabla");
        btnSwitchVista.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSwitchVista.setBackground(Color.WHITE);
        btnSwitchVista.setForeground(SLATE_OSCURO);
        btnSwitchVista.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btnSwitchVista.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSwitchVista.setFocusPainted(false);
        btnSwitchVista.addActionListener(e -> alternarModoVista());

        pnlPildoras.add(btnFiltroTodos);
        pnlPildoras.add(btnFiltroDisp);
        pnlPildoras.add(btnFiltroVend);
        pnlPildoras.add(btnSwitchVista);

        pnlFiltrosBarra.add(pnlBusqueda, BorderLayout.WEST);
        pnlFiltrosBarra.add(pnlPildoras, BorderLayout.EAST);
        pnlNorteCompleto.add(pnlFiltrosBarra, BorderLayout.SOUTH);

        p.add(pnlNorteCompleto, BorderLayout.NORTH);

        // 2. ZONA CENTRAL: CARDLAYOUT PARA ALTERNAR ENTRE TARJETAS Y TABLA
        cardLayoutHistorial = new CardLayout();
        pnlCardsOrTable = new JPanel(cardLayoutHistorial);
        pnlCardsOrTable.setBackground(FONDO);

        // VISTA A: TARJETAS MODERNAS
        pnlContenedorTarjetas = new JPanel(new GridLayout(0, 2, 8, 8));
        pnlContenedorTarjetas.setBackground(FONDO);
        pnlContenedorTarjetas.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        scrollTarjetas = new JScrollPane(pnlContenedorTarjetas);
        scrollTarjetas.setBorder(null);
        scrollTarjetas.setBackground(FONDO);
        scrollTarjetas.getViewport().setBackground(FONDO);
        scrollTarjetas.getVerticalScrollBar().setUnitIncrement(16);

        // VISTA B: TABLA TRADICIONAL
        String[] cols = {"ID", "Nombre", "Libras", "Costo", "Fecha", "Hora", "Estado"};
        modelHistorial = new DefaultTableModel(cols, 0) {
            @Override 
            public boolean isCellEditable(int r, int c) { 
                return false; 
            }
        };
        tablaHistorial = new JTable(modelHistorial);
        sorterHistorial = new TableRowSorter<>(modelHistorial);
        tablaHistorial.setRowSorter(sorterHistorial);
        estilizarTabla(tablaHistorial);
        tablaHistorial.getColumnModel().getColumn(0).setMaxWidth(35);
        tablaHistorial.getColumnModel().getColumn(6).setMaxWidth(90);

        tablaHistorial.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    mostrarDetalleLoteSeleccionado();
                }
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaHistorial);
        scrollTabla.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));

        pnlCardsOrTable.add(scrollTarjetas, "TARJETAS");
        pnlCardsOrTable.add(scrollTabla, "TABLA");
        p.add(pnlCardsOrTable, BorderLayout.CENTER);

        // 3. ZONA INFERIOR: BOTONERA DE GESTIÓN
        JPanel pBotones = new JPanel(new GridLayout(1, 5, 6, 6));
        pBotones.setBackground(FONDO);

        JButton btnDetalle = crearBotonEstilo("👁 Ver Detalle", Color.WHITE, SLATE_OSCURO);
        JButton btnDuplicar = crearBotonEstilo("📋 Duplicar", SECUNDARIO_FONDO, SECUNDARIO_TEXTO);
        JButton btnPDF = crearBotonEstilo("📄 PDF", AZUL_CORP, Color.WHITE);
        JButton btnRenombrar = crearBotonEstilo("✏️ Renombrar", SECUNDARIO_FONDO, SECUNDARIO_TEXTO);
        JButton btnEliminar = crearBotonEstilo("🗑 Eliminar", ROJO, Color.WHITE);

        btnDetalle.addActionListener(evt -> mostrarDetalleLoteSeleccionado());
        btnDuplicar.addActionListener(evt -> duplicarFormulacionSeleccionada());
        btnPDF.addActionListener(evt -> exportarPDFSeleccionado());
        btnRenombrar.addActionListener(evt -> renombrarFormulacionSeleccionada());
        btnEliminar.addActionListener(evt -> eliminarFormulacionSeleccionada());

        pBotones.add(btnDetalle);
        pBotones.add(btnDuplicar);
        pBotones.add(btnPDF);
        pBotones.add(btnRenombrar);
        pBotones.add(btnEliminar);

        p.add(pBotones, BorderLayout.SOUTH);
        return p;
    }

    private JPanel crearMiniCardKPI(String titulo, JLabel lblValor, Color colorAcento) {
        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblTit.setForeground(TEXTO_MUTED);

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblValor.setForeground(colorAcento);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private JButton crearBotonPildora(String texto, boolean activo) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
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
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(SECUNDARIO_TEXTO);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE_SUAVE, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
        }
    }

    private void setFiltroEstado(String estado) {
        this.filtroEstado = estado;
        actualizarEstiloPildora(btnFiltroTodos, "TODOS".equals(estado));
        actualizarEstiloPildora(btnFiltroDisp, "DISPONIBLE".equals(estado));
        actualizarEstiloPildora(btnFiltroVend, "VENDIDO".equals(estado));
        aplicarFiltroGeneral();
    }

    private void alternarModoVista() {
        modoTarjetas = !modoTarjetas;
        if (modoTarjetas) {
            cardLayoutHistorial.show(pnlCardsOrTable, "TARJETAS");
            btnSwitchVista.setText("📋 Ver Tabla");
        } else {
            cardLayoutHistorial.show(pnlCardsOrTable, "TABLA");
            btnSwitchVista.setText("📇 Ver Tarjetas");
        }
    }

    private JButton crearBotonEstilo(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        return btn;
    }

    // ============================================================
    //  FILTRADO GENERAL Y CARGA DE DATOS
    // ============================================================
    private void aplicarFiltroGeneral() {
        String query = txtBuscarHistorial != null ? txtBuscarHistorial.getText().trim().toLowerCase() : "";

        // 1. Filtrar en la tabla
        if (sorterHistorial != null) {
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            if (!query.isEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)" + query, 1, 4));
            }
            if ("DISPONIBLE".equals(filtroEstado)) {
                filters.add(RowFilter.regexFilter("(?i)disponible", 6));
            } else if ("VENDIDO".equals(filtroEstado)) {
                filters.add(RowFilter.regexFilter("(?i)vendido", 6));
            }
            if (filters.isEmpty()) {
                sorterHistorial.setRowFilter(null);
            } else {
                sorterHistorial.setRowFilter(RowFilter.andFilter(filters));
            }
        }

        // 2. Filtrar en las Tarjetas
        pnlContenedorTarjetas.removeAll();
        for (Formulacion f : listaFormulacionesCache) {
            String estado = f.getEstado() != null ? f.getEstado() : "Disponible";
            boolean matchQuery = query.isEmpty() || 
                f.getNombre().toLowerCase().contains(query) || 
                String.valueOf(f.getId()).contains(query) ||
                (f.getFecha() != null && f.getFecha().contains(query));

            boolean matchEstado = "TODOS".equals(filtroEstado) || 
                ("DISPONIBLE".equals(filtroEstado) && "Disponible".equalsIgnoreCase(estado)) ||
                ("VENDIDO".equals(filtroEstado) && "Vendido".equalsIgnoreCase(estado));

            if (matchQuery && matchEstado) {
                FormulacionCardItem card = new FormulacionCardItem(f);
                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        seleccionarFormulacionPorId(f.getId());
                        if (e.getClickCount() == 2) {
                            abrirDialogoAuditoriaFormulacion(f);
                        }
                    }
                });
                pnlContenedorTarjetas.add(card);
            }
        }
        pnlContenedorTarjetas.revalidate();
        pnlContenedorTarjetas.repaint();
    }

    private void seleccionarFormulacionPorId(int id) {
        for (int i = 0; i < tablaHistorial.getRowCount(); i++) {
            int modelRow = tablaHistorial.convertRowIndexToModel(i);
            int rowId = (int) modelHistorial.getValueAt(modelRow, 0);
            if (rowId == id) {
                tablaHistorial.setRowSelectionInterval(i, i);
                tablaHistorial.scrollRectToVisible(tablaHistorial.getCellRect(i, 0, true));
                break;
            }
        }
    }

    public void cargarHistorial() {
        try {
            listaFormulacionesCache = formulacionDAO.listarTodas();
            modelHistorial.setRowCount(0);

            double totalLibrasDisponibles = 0;
            int totalVendidos = 0;
            double totalInversion = 0;

            for (Formulacion f : listaFormulacionesCache) {
                String estado = f.getEstado() != null ? f.getEstado() : "Disponible";
                modelHistorial.addRow(new Object[]{
                    f.getId(), 
                    f.getNombre(),
                    String.format("%.2f lb", f.getTotalLibras()),
                    String.format("$%.2f", f.getCostoTotal()),
                    f.getFecha(), 
                    f.getHora(),
                    estado
                });

                totalInversion += f.getCostoTotal();
                if ("Vendido".equalsIgnoreCase(estado)) {
                    totalVendidos++;
                } else {
                    totalLibrasDisponibles += f.getTotalLibras();
                }
            }

            // Actualizar KPIs
            lblKpiTotalLotes.setText(String.valueOf(listaFormulacionesCache.size()));
            lblKpiDisponibles.setText(String.format("%.1f lb", totalLibrasDisponibles));
            lblKpiVendidos.setText(String.valueOf(totalVendidos));
            lblKpiInversion.setText(String.format("$%.2f", totalInversion));

            aplicarFiltroGeneral();

        } catch (Exception ex) { 
            DialogStyler.mostrarError(this, "Error al cargar historial: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ============================================================
    //  TARJETA VISUAL DE FORMULACIÓN (FormulacionCardItem)
    // ============================================================
    private class FormulacionCardItem extends JPanel {
        private final Formulacion formulacion;
        private boolean isHovered = false;

        public FormulacionCardItem(Formulacion f) {
            this.formulacion = f;
            setOpaque(false);
            setPreferredSize(new Dimension(240, 185));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int yOff = isHovered ? -2 : 0;

            boolean esDisponible = !"Vendido".equalsIgnoreCase(formulacion.getEstado());

            // Fondo Blanco con esquinas redondeadas
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, yOff, w - 1, h - 1, 14, 14);

            // Borde
            Color colBorde = isHovered ? (esDisponible ? VERDE : SLATE_OSCURO) : BORDE_SUAVE;
            g2.setColor(colBorde);
            g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1.0f));
            g2.drawRoundRect(0, yOff, w - 1, h - 1, 14, 14);

            // Franja de color superior
            g2.setColor(esDisponible ? VERDE : new Color(148, 163, 184));
            g2.fillRoundRect(0, yOff, w - 1, 6, 14, 14);
            g2.fillRect(0, yOff + 3, w - 1, 3);

            // Píldora de Estado (DISPONIBLE / VENDIDO)
            String txtEstado = esDisponible ? "● DISPONIBLE" : "VENDIDO";
            Color bgPildora = esDisponible ? new Color(209, 250, 229) : new Color(241, 245, 249);
            Color fgPildora = esDisponible ? new Color(6, 95, 70) : TEXTO_MUTED;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            int pWidth = fm.stringWidth(txtEstado) + 12;
            int pX = w - pWidth - 10;
            int pY = 14 + yOff;

            g2.setColor(bgPildora);
            g2.fillRoundRect(pX, pY, pWidth, 16, 8, 8);
            g2.setColor(fgPildora);
            g2.drawString(txtEstado, pX + 6, pY + 12);

            // Lote ID Badge
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(TEXTO_MUTED);
            g2.drawString("LOTE #" + formulacion.getId(), 12, 26 + yOff);

            // Título de la Fórmula
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(SLATE_OSCURO);
            String nombreTruncado = formulacion.getNombre();
            if (g2.getFontMetrics().stringWidth(nombreTruncado) > w - 24) {
                nombreTruncado = nombreTruncado.substring(0, Math.min(nombreTruncado.length(), 22)) + "...";
            }
            g2.drawString(nombreTruncado, 12, 44 + yOff);

            // Bloque Central: Stock en Bodega
            int bY = 54 + yOff;
            g2.setColor(new Color(248, 250, 252));
            g2.fillRoundRect(10, bY, w - 75, 48, 10, 10);
            g2.setColor(BORDE_SUAVE);
            g2.drawRoundRect(10, bY, w - 75, 48, 10, 10);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
            g2.setColor(TEXTO_MUTED);
            g2.drawString(esDisponible ? "STOCK EN BODEGA" : "TOTAL FABRICADO", 18, bY + 14);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.setColor(esDisponible ? VERDE_OSCURO : TEXTO_MUTED);
            g2.drawString(String.format("%.2f lb", formulacion.getTotalLibras()), 18, bY + 34);

            // Aro Donut de Rotación
            int donutX = w - 58;
            int donutY = bY + 2;
            int donutSize = 44;
            g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.setColor(new Color(241, 245, 249));
            g2.drawArc(donutX, donutY, donutSize, donutSize, 0, 360);

            g2.setColor(esDisponible ? VERDE : AZUL_CORP);
            int arcAngle = esDisponible ? 0 : -360;
            g2.drawArc(donutX, donutY, donutSize, donutSize, 90, arcAngle);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(SLATE_OSCURO);
            String pctStr = esDisponible ? "100%" : "0%";
            FontMetrics fmArc = g2.getFontMetrics();
            g2.drawString(pctStr, donutX + (donutSize - fmArc.stringWidth(pctStr)) / 2, donutY + 26);

            // Métricas Inferiores
            int mY = 118 + yOff;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(TEXTO_MUTED);
            g2.drawString("Costo lote:", 12, mY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(SLATE_OSCURO);
            g2.drawString(String.format("$%.2f", formulacion.getCostoTotal()), w - 65, mY);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(TEXTO_MUTED);
            g2.drawString("Fecha prep:", 12, mY + 16);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(SLATE_OSCURO);
            String fechaStr = formulacion.getFecha() != null ? formulacion.getFecha() : "-";
            g2.drawString(fechaStr, w - 85, mY + 16);

            // Botón sutil en pie de tarjeta
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(10, 146 + yOff, w - 20, 24, 6, 6);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(SECUNDARIO_TEXTO);
            String txtAccion = "👁 Doble clic para auditar receta";
            FontMetrics fmAcc = g2.getFontMetrics();
            g2.drawString(txtAccion, (w - fmAcc.stringWidth(txtAccion)) / 2, 162 + yOff);

            g2.dispose();
        }
    }

    // ============================================================
    //  MODAL DE AUDITORÍA Y DETALLE DE FORMULACIÓN
    // ============================================================
    private void mostrarDetalleLoteSeleccionado() {
        int row = tablaHistorial.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione una formulación del historial para ver el detalle.", "Aviso");
            return;
        }
        row = tablaHistorial.convertRowIndexToModel(row);
        int id = (int) modelHistorial.getValueAt(row, 0);

        try {
            Formulacion f = formulacionDAO.obtenerPorId(id);
            if (f != null) {
                abrirDialogoAuditoriaFormulacion(f);
            }
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al obtener formulación: " + ex.getMessage());
        }
    }

    private void abrirDialogoAuditoriaFormulacion(Formulacion f) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Auditoría de Lote - " + f.getNombre(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(580, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(FONDO);

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(SLATE_OSCURO);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblTit = new JLabel("Lote #" + f.getId() + " - " + f.getNombre());
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTit.setForeground(Color.WHITE);

        String estado = f.getEstado() != null ? f.getEstado() : "Disponible";
        JLabel lblEst = new JLabel("ESTADO: " + estado.toUpperCase());
        lblEst.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblEst.setForeground("Vendido".equalsIgnoreCase(estado) ? new Color(148, 163, 184) : new Color(52, 211, 153));

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblEst, BorderLayout.SOUTH);
        dialog.add(pnlHeader, BorderLayout.NORTH);

        // Centro: Tabla de Ingredientes y KPIs
        JPanel pnlCentro = new JPanel(new BorderLayout(8, 8));
        pnlCentro.setBackground(FONDO);
        pnlCentro.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        String[] cols = {"Materia Prima", "Cantidad (lb)", "Precio Unit.", "Subtotal"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        estilizarTabla(table);

        for (DetalleFormulacion d : f.getDetalles()) {
            model.addRow(new Object[]{
                d.getNombreIngrediente(),
                String.format("%.2f lb", d.getCantidadUsada()),
                String.format("$%.4f", d.getPrecioUnitario()),
                String.format("$%.2f", d.getSubtotal())
            });
        }

        pnlCentro.add(new JScrollPane(table), BorderLayout.CENTER);

        // Resumen
        JPanel pnlResumen = new JPanel(new GridLayout(1, 3, 10, 0));
        pnlResumen.setBackground(Color.WHITE);
        pnlResumen.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel l1 = new JLabel("<html><small style='color:#64748B'>PESO TOTAL</small><br><b>" + String.format("%.2f lb", f.getTotalLibras()) + "</b></html>");
        JLabel l2 = new JLabel("<html><small style='color:#64748B'>COSTO TOTAL</small><br><b style='color:#059669'>$" + String.format("%.2f", f.getCostoTotal()) + "</b></html>");
        double cXlb = f.getTotalLibras() > 0 ? f.getCostoTotal() / f.getTotalLibras() : 0;
        JLabel l3 = new JLabel("<html><small style='color:#64748B'>COSTO / LB</small><br><b>$" + String.format("%.4f", cXlb) + "</b></html>");

        pnlResumen.add(l1);
        pnlResumen.add(l2);
        pnlResumen.add(l3);
        pnlCentro.add(pnlResumen, BorderLayout.SOUTH);

        dialog.add(pnlCentro, BorderLayout.CENTER);

        // Footer
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFooter.setBackground(FONDO);
        JButton btnCerrar = crearBotonEstilo("Cerrar", SECUNDARIO_FONDO, SECUNDARIO_TEXTO);
        btnCerrar.addActionListener(e -> dialog.dispose());
        pnlFooter.add(btnCerrar);
        dialog.add(pnlFooter, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ============================================================
    //  MÉTODOS DE NEGOCIO (PRESERVADOS ÍNTEGRAMENTE)
    // ============================================================

    private void editarIngrediente() {
        int row = tablaIngredientes.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un ingrediente de la tabla para editar.", "Aviso");
            return;
        }
        
        try {
            DetalleFormulacion detalle = ingredientesActuales.get(row);
            String nombreOriginal = detalle.getNombreIngrediente();
            double cantidadOriginal = detalle.getCantidadUsada();
            String unidadOriginal = detalle.getUnidad();
            
            String mensajeAdicional = "";
            double litrosActuales = 0;
            if ("litro".equals(unidadOriginal) || "litros".equals(unidadOriginal)) {
                litrosActuales = detalle.getCantidadOriginal();
                mensajeAdicional = "\nCantidad original en litros: " + String.format("%.2f", litrosActuales) + " L\n" +
                                   "Equivalente en libras: " + String.format("%.2f", cantidadOriginal) + " lb\n";
            }
            
            String input = JOptionPane.showInputDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:14px;padding:8px;'>" +
                "✏️ EDITAR INGREDIENTE<br><br>" +
                "Ingrediente: <b>" + nombreOriginal + "</b><br>" +
                "Cantidad actual: <b>" + String.format("%.2f", cantidadOriginal) + "</b> " + unidadOriginal +
                mensajeAdicional + "<br><br>" +
                "Ingrese la nueva cantidad:" +
                "</body></html>",
                "Editar ingrediente",
                JOptionPane.QUESTION_MESSAGE);
            
            if (input == null) return;
            
            double nuevaCantidad;
            try {
                nuevaCantidad = Double.parseDouble(input.trim().replace(",", "."));
                if (nuevaCantidad <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                DialogStyler.mostrarError(this, "Ingrese un número válido mayor a 0.");
                return;
            }
            
            MateriaPrima mp = materiaDAO.buscarPorId(detalle.getMateriaPrimaId());
            if (mp != null) {
                boolean esLiquido = "litro".equals(mp.getUnidad()) || "litros".equals(mp.getUnidad());
                
                if (esLiquido) {
                    if (nuevaCantidad > mp.getCantidadDisponible()) {
                        DialogStyler.mostrarError(this,
                            "Stock insuficiente.<br><br>" +
                            "Stock disponible: <b>" + String.format("%.2f", mp.getCantidadDisponible()) + "</b> litros<br>" +
                            "Cantidad solicitada: <b>" + String.format("%.2f", nuevaCantidad) + "</b> litros"
                        );
                        return;
                    }
                    
                    double nuevaCantidadOriginal = nuevaCantidad;
                    double nuevaCantidadLibras = DensidadManager.convertirLitrosALibras(nuevaCantidadOriginal, detalle.getNombreIngrediente());
                    
                    detalle.setCantidadOriginal(nuevaCantidadOriginal);
                    detalle.setCantidadUsada(nuevaCantidadLibras);
                    detalle.setSubtotal(nuevaCantidadOriginal * detalle.getPrecioUnitario());
                    
                    String nombreMostrar = detalle.getNombreIngrediente() + " (" + String.format("%.2f", nuevaCantidadOriginal) + " L)";
                    modelIngredientes.setValueAt(nombreMostrar, row, 0);
                    modelIngredientes.setValueAt("lb", row, 1);
                    modelIngredientes.setValueAt(String.format("%.2f", nuevaCantidadLibras), row, 2);
                    modelIngredientes.setValueAt(String.format("$%.2f", detalle.getSubtotal()), row, 4);
                    
                } else {
                    if (nuevaCantidad > mp.getCantidadDisponible()) {
                        DialogStyler.mostrarError(this,
                            "Stock insuficiente.<br><br>" +
                            "Stock disponible: <b>" + String.format("%.2f", mp.getCantidadDisponible()) + "</b> " + mp.getUnidad() + "<br>" +
                            "Cantidad solicitada: <b>" + String.format("%.2f", nuevaCantidad) + "</b> " + mp.getUnidad()
                        );
                        return;
                    }
                    
                    detalle.setCantidadUsada(nuevaCantidad);
                    detalle.setSubtotal(nuevaCantidad * detalle.getPrecioUnitario());
                    
                    modelIngredientes.setValueAt(String.format("%.2f", nuevaCantidad), row, 2);
                    modelIngredientes.setValueAt(String.format("$%.2f", detalle.getSubtotal()), row, 4);
                }
            }
            
            actualizarTotales();
            DialogStyler.mostrarExito(this, "Ingrediente actualizado correctamente.", "Éxito");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al editar ingrediente: " + ex.getMessage());
        }
    }

    private void ajustarFormulacion() {
        try {
            if (ingredientesActuales.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, "Primero agregue los ingredientes de la fórmula base.", "Sin ingredientes");
                return;
            }

            double totalActual = ingredientesActuales.stream()
                .mapToDouble(DetalleFormulacion::getCantidadUsada)
                .sum();

            String input = JOptionPane.showInputDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:14px;padding:8px;'>" +
                "📐 AJUSTAR FORMULACIÓN<br><br>" +
                "Cantidad actual: <b>" + String.format("%.2f", totalActual) + "</b> lb<br><br>" +
                "Ingrese la nueva cantidad deseada (libras):" +
                "</body></html>",
                "Ajustar formulación",
                JOptionPane.QUESTION_MESSAGE);

            if (input == null) return;

            double nuevaCantidad;
            try {
                nuevaCantidad = Double.parseDouble(input.trim().replace(",", "."));
                if (nuevaCantidad <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                DialogStyler.mostrarError(this, "Ingrese un número válido mayor a 0.");
                return;
            }

            double factor = nuevaCantidad / totalActual;

            StringBuilder preview = new StringBuilder();
            preview.append("📊 VISTA PREVIA DEL AJUSTE<br><br>");
            preview.append("Factor de ajuste: <b>").append(String.format("%.4f", factor)).append("</b><br>");
            preview.append("Cantidad actual: ").append(String.format("%.2f", totalActual)).append(" lb<br>");
            preview.append("Nueva cantidad: <b>").append(String.format("%.2f", nuevaCantidad)).append("</b> lb<br><br>");
            preview.append("📋 INGREDIENTES AJUSTADOS:<br>");
            preview.append("─────────────────────────────<br>");

            for (DetalleFormulacion det : ingredientesActuales) {
                double nuevaCantidadIngrediente = det.getCantidadUsada() * factor;
                preview.append("• ")
                    .append(det.getNombreIngrediente())
                    .append(": ")
                    .append(String.format("%.2f", det.getCantidadUsada()))
                    .append(" → <b>")
                    .append(String.format("%.2f", nuevaCantidadIngrediente))
                    .append("</b> ").append(det.getUnidad())
                    .append("<br>");
            }

            if (!DialogStyler.confirmar(this, preview.toString(), "Confirmar ajuste")) return;

            for (DetalleFormulacion det : ingredientesActuales) {
                double nuevaCant = det.getCantidadUsada() * factor;
                det.setCantidadUsada(nuevaCant);
                
                if ("litro".equals(det.getUnidadOriginal()) || "litros".equals(det.getUnidadOriginal())) {
                    double nuevaCantOriginal = det.getCantidadOriginal() * factor;
                    det.setCantidadOriginal(nuevaCantOriginal);
                    det.setSubtotal(nuevaCantOriginal * det.getPrecioUnitario());
                } else {
                    det.setSubtotal(nuevaCant * det.getPrecioUnitario());
                }
            }

            modelIngredientes.setRowCount(0);
            for (DetalleFormulacion det : ingredientesActuales) {
                String nombreMostrar = det.getNombreIngrediente();
                if ("litro".equals(det.getUnidadOriginal()) || "litros".equals(det.getUnidadOriginal())) {
                    nombreMostrar = det.getNombreIngrediente() + " (" + String.format("%.2f", det.getCantidadOriginal()) + " L)";
                }
                modelIngredientes.addRow(new Object[]{
                    nombreMostrar,
                    det.getUnidad(),
                    String.format("%.2f", det.getCantidadUsada()),
                    String.format("$%.4f", det.getPrecioUnitario()),
                    String.format("$%.2f", det.getSubtotal())
                });
            }

            actualizarTotales();

            String nombreActual = txtNombreFormula.getText().trim();
            if (!nombreActual.isEmpty()) {
                String nuevoNombre = JOptionPane.showInputDialog(this,
                    "Ingrese un nombre para la fórmula ajustada:",
                    nombreActual + " (" + String.format("%.0f", nuevaCantidad) + " lb)");
                if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
                    txtNombreFormula.setText(nuevoNombre.trim());
                }
            }

            DialogStyler.mostrarExito(this, "Formulación ajustada correctamente a " + String.format("%.2f", nuevaCantidad) + " lb", "Éxito");

        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al ajustar: " + e.getMessage());
        }
    }

    private void duplicarFormulacionSeleccionada() {
        if (!esAdministrador()) {
            DialogStyler.mostrarAdvertencia(this,
                "🔒 ACCESO RESTRINGIDO<br><br>Duplicar formulaciones solo está disponible para <b>Administradores</b>.",
                "Permiso denegado");
            return;
        }
        
        int row = tablaHistorial.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione una formulación para duplicar.", "Aviso");
            return;
        }
        row = tablaHistorial.convertRowIndexToModel(row);
        
        try {
            int id = (int) modelHistorial.getValueAt(row, 0);
            String nombreOriginal = (String) modelHistorial.getValueAt(row, 1);
            
            Formulacion f = formulacionDAO.obtenerPorId(id);
            if (f == null) {
                DialogStyler.mostrarError(this, "No se encontró la formulación.");
                return;
            }
            
            boolean stockSuficiente = true;
            StringBuilder mensajeStock = new StringBuilder();
            mensajeStock.append("📦 VERIFICANDO STOCK DISPONIBLE:<br><br>");
            
            for (DetalleFormulacion det : f.getDetalles()) {
                MateriaPrima mp = materiaDAO.buscarPorId(det.getMateriaPrimaId());
                if (mp == null) {
                    stockSuficiente = false;
                    mensajeStock.append("❌ ").append(det.getNombreIngrediente()).append(" - ¡NO ENCONTRADO!<br>");
                } else {
                    boolean esLiquido = "litro".equals(mp.getUnidad()) || "litros".equals(mp.getUnidad());
                    double cantidadNecesaria = esLiquido 
                        ? det.getCantidadUsada() / DensidadManager.getDensidad(det.getNombreIngrediente())
                        : det.getCantidadUsada();
                    String unidadMostrar = esLiquido ? "litro" : mp.getUnidad();
                    
                    if (mp.getCantidadDisponible() < cantidadNecesaria) {
                        stockSuficiente = false;
                        mensajeStock.append("❌ ").append(det.getNombreIngrediente())
                            .append(" - Faltan: ")
                            .append(String.format("%.2f", cantidadNecesaria - mp.getCantidadDisponible()))
                            .append(" ").append(unidadMostrar).append("<br>");
                    } else {
                        mensajeStock.append("✅ ").append(det.getNombreIngrediente()).append(" - Disponible<br>");
                    }
                }
            }
            
            if (!stockSuficiente) {
                mensajeStock.append("<br>⚠️ No se puede duplicar la fórmula porque falta stock.");
                DialogStyler.mostrarError(this, mensajeStock.toString());
                return;
            }
            
            String nuevoNombre = JOptionPane.showInputDialog(this,
                "Ingrese el nombre para la nueva formulación:", nombreOriginal);
            
            if (nuevoNombre == null) return;
            if (nuevoNombre.trim().isEmpty()) nuevoNombre = nombreOriginal + " (copia)";
            
            limpiarFormulario();
            txtNombreFormula.setText(nuevoNombre.trim());
            
            for (DetalleFormulacion det : f.getDetalles()) {
                MateriaPrima mp = materiaDAO.buscarPorId(det.getMateriaPrimaId());
                if (mp != null) {
                    boolean esLiquido = "litro".equals(mp.getUnidad()) || "litros".equals(mp.getUnidad());
                    DetalleFormulacion nuevoDet;
                    
                    if (esLiquido) {
                        double pesoLibras = det.getCantidadUsada();
                        double densidad = DensidadManager.getDensidad(det.getNombreIngrediente());
                        double litrosCalculados = Math.round((pesoLibras / densidad) * 100.0) / 100.0;
                        nuevoDet = new DetalleFormulacion(mp.getId(), mp.getNombre(), mp.getUnidad(), pesoLibras, mp.getPrecioPorUnidad(), litrosCalculados, "litro");
                    } else {
                        nuevoDet = new DetalleFormulacion(mp.getId(), mp.getNombre(), mp.getUnidad(), det.getCantidadUsada(), mp.getPrecioPorUnidad());
                    }
                    
                    ingredientesActuales.add(nuevoDet);
                    
                    String nombreMostrar = esLiquido && nuevoDet.getCantidadOriginal() > 0 
                        ? mp.getNombre() + " (" + String.format("%.2f", nuevoDet.getCantidadOriginal()) + " L)"
                        : mp.getNombre();
                    
                    modelIngredientes.addRow(new Object[]{
                        nombreMostrar,
                        "lb",
                        String.format("%.2f", nuevoDet.getCantidadUsada()),
                        String.format("$%.4f", mp.getPrecioPorUnidad()),
                        String.format("$%.2f", nuevoDet.getSubtotal())
                    });
                }
            }
            
            actualizarTotales();
            DialogStyler.mostrarExito(this, "Formulación duplicada en el editor. Puede ajustarla y guardarla.", "Duplicado exitoso");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al duplicar: " + ex.getMessage());
        }
    }

    private void eliminarFormulacionSeleccionada() {
        if (!esAdministrador()) {
            DialogStyler.mostrarAdvertencia(this,
                "🔒 ACCESO RESTRINGIDO<br><br>Eliminar formulaciones solo está disponible para <b>Administradores</b>.",
                "Permiso denegado");
            return;
        }
        
        int row = tablaHistorial.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione una formulación del historial para eliminar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tablaHistorial.convertRowIndexToModel(row);
            int id = (int) modelHistorial.getValueAt(modelRow, 0);
            String nombre = (String) modelHistorial.getValueAt(modelRow, 1);
            String estado = (String) modelHistorial.getValueAt(modelRow, 6);
            
            if ("Vendido".equalsIgnoreCase(estado)) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR UNA FORMULACIÓN VENDIDA<br><br>" +
                    "La formulación <b>" + nombre + "</b> ya fue vendida.<br>" +
                    "Elimine primero la venta asociada si desea anularla.");
                return;
            }
            
            Formulacion f = formulacionDAO.obtenerPorId(id);
            if (f == null) {
                DialogStyler.mostrarError(this, "No se encontró la formulación.");
                return;
            }
            
            StringBuilder ingredientesMsg = new StringBuilder("📦 INGREDIENTES A DEVOLVER AL INVENTARIO:<br><br>");
            for (DetalleFormulacion det : f.getDetalles()) {
                ingredientesMsg.append("• ").append(det.getNombreIngrediente())
                    .append(": <b>+").append(String.format("%.2f", det.getCantidadUsada()))
                    .append("</b> ").append(det.getUnidad()).append("<br>");
            }
            
            String mensaje = "⚠️ ¿Eliminar la formulación <b>" + nombre + "</b>?<br><br>" +
                ingredientesMsg.toString() + "<br>Los ingredientes serán devueltos automáticamente al stock.";
            
            if (!DialogStyler.confirmar(this, mensaje, "Confirmar eliminación")) return;
            
            formulacionDAO.eliminar(id);
            cargarHistorial();
            
            if (stockPanel != null) {
                stockPanel.cargarDatos();
            }
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
            }
            
            DialogStyler.mostrarExito(this, "Formulación eliminada y stock restituido.", "Éxito");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar: " + ex.getMessage());
        }
    }

    private void mostrarDialogoAgregarIngrediente() {
        try {
            List<MateriaPrima> disponibles = materiaDAO.listarConStock();
            if (disponibles.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, 
                    "No hay materias primas con stock disponible.<br>Agregue materiales en la pestaña Inventario.", 
                    "Sin stock");
                return;
            }
            
            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Agregar Ingrediente",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(550, 480);
            dialog.setLocationRelativeTo(this);

            DialogStyler.aplicar(dialog, "Agregar Ingrediente", "➕");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            JComboBox<MateriaPrima> cmbMaterias = new JComboBox<>(disponibles.toArray(new MateriaPrima[0]));
            cmbMaterias.setFont(DialogStyler.FUENTE_CAMPO);
            
            JTextField txtCantidad = DialogStyler.crearCampoNumerico(12);

            JLabel lblInfo = new JLabel(" ");
            lblInfo.setFont(DialogStyler.FUENTE_INFO);
            lblInfo.setForeground(DialogStyler.TEXTO_SECUNDARIO);
            
            JLabel lblPesoEquivalente = new JLabel(" ");
            lblPesoEquivalente.setFont(DialogStyler.FUENTE_INFO);
            lblPesoEquivalente.setForeground(DialogStyler.ACENTO);

            cmbMaterias.addActionListener(e -> {
                MateriaPrima sel = (MateriaPrima) cmbMaterias.getSelectedItem();
                if (sel != null) {
                    lblInfo.setText("Disponible: " + String.format("%.2f", sel.getCantidadDisponible()) + " " + sel.getUnidad() + 
                        "  |  Precio: $" + String.format("%.4f", sel.getPrecioPorUnidad()) + "/" + sel.getUnidad());
                    
                    if ("litro".equals(sel.getUnidad()) || "litros".equals(sel.getUnidad())) {
                        double densidad = DensidadManager.getDensidad(sel.getNombre());
                        lblPesoEquivalente.setText("💡 1 litro = " + String.format("%.2f", densidad) + " libras");
                    } else {
                        lblPesoEquivalente.setText("");
                    }
                }
            });
            
            txtCantidad.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarPreviewPeso(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarPreviewPeso(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarPreviewPeso(); }
                
                private void actualizarPreviewPeso() {
                    try {
                        MateriaPrima sel = (MateriaPrima) cmbMaterias.getSelectedItem();
                        if (sel != null && "litro".equals(sel.getUnidad())) {
                            String texto = txtCantidad.getText().trim().replace(",", ".");
                            if (!texto.isEmpty()) {
                                double litros = Double.parseDouble(texto);
                                double libras = DensidadManager.convertirLitrosALibras(litros, sel.getNombre());
                                lblPesoEquivalente.setText("⚖️ " + String.format("%.2f", litros) + 
                                    " litros = <b>" + String.format("%.2f", libras) + "</b> libras");
                            }
                        }
                    } catch (NumberFormatException ex) {
                        // ignorar
                    }
                }
            });
            
            if (!disponibles.isEmpty()) {
                MateriaPrima sel = disponibles.get(0);
                lblInfo.setText("Disponible: " + String.format("%.2f", sel.getCantidadDisponible()) + " " + sel.getUnidad() + 
                    "  |  Precio: $" + String.format("%.4f", sel.getPrecioPorUnidad()) + "/" + sel.getUnidad());
                
                if ("litro".equals(sel.getUnidad()) || "litros".equals(sel.getUnidad())) {
                    double densidad = DensidadManager.getDensidad(sel.getNombre());
                    lblPesoEquivalente.setText("💡 1 litro = " + String.format("%.2f", densidad) + " libras");
                }
            }

            int fila = 0;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Ingrediente:"), gbc);
            gbc.gridx = 1;
            form.add(cmbMaterias, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Cantidad a usar:"), gbc);
            gbc.gridx = 1;
            form.add(txtCantidad, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(lblInfo, gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(lblPesoEquivalente, gbc);
            gbc.gridwidth = 1;

            JButton btnAdd = DialogStyler.crearBotonPrimario("➕ Agregar");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnAdd.addActionListener(e -> {
                try {
                    MateriaPrima mp = (MateriaPrima) cmbMaterias.getSelectedItem();
                    String cantStr = txtCantidad.getText().trim().replace(",", ".");
                    if (cantStr.isEmpty() || !cantStr.matches("[0-9.]*")) {
                        DialogStyler.mostrarError(dialog, "Ingrese una cantidad válida.");
                        return;
                    }
                    
                    double cant = Double.parseDouble(cantStr);
                    if (cant <= 0) {
                        DialogStyler.mostrarError(dialog, "La cantidad debe ser mayor a 0.");
                        return;
                    }
                    
                    double stockDisponible = mp.getCantidadDisponible();
                    boolean esLiquido = "litro".equals(mp.getUnidad()) || "litros".equals(mp.getUnidad());
                    
                    if (cant > stockDisponible) {
                        DialogStyler.mostrarError(dialog, 
                            "Stock insuficiente.<br>Disponible: <b>" + String.format("%.2f", stockDisponible) + "</b> " + (esLiquido ? "litros" : mp.getUnidad()));
                        return;
                    }

                    DetalleFormulacion det;
                    if (esLiquido) {
                        double pesoLibras = DensidadManager.convertirLitrosALibras(cant, mp.getNombre());
                        det = new DetalleFormulacion(mp.getId(), mp.getNombre(), mp.getUnidad(), pesoLibras, mp.getPrecioPorUnidad(), cant, mp.getUnidad());
                    } else {
                        det = new DetalleFormulacion(mp.getId(), mp.getNombre(), mp.getUnidad(), cant, mp.getPrecioPorUnidad());
                    }
                    
                    ingredientesActuales.add(det);
                    
                    String nombreMostrar = esLiquido 
                        ? mp.getNombre() + " (" + String.format("%.2f", det.getCantidadOriginal()) + " L)"
                        : mp.getNombre();

                    modelIngredientes.addRow(new Object[]{
                        nombreMostrar,
                        "lb",
                        String.format("%.2f", det.getCantidadUsada()),
                        String.format("$%.4f", mp.getPrecioPorUnidad()),
                        String.format("$%.2f", det.getSubtotal())
                    });
                    
                    actualizarTotales();
                    dialog.dispose();
                    
                } catch (NumberFormatException ex) {
                    DialogStyler.mostrarError(dialog, "Ingrese un número válido.");
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnAdd), BorderLayout.SOUTH);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private void quitarIngrediente() {
        int row = tablaIngredientes.getSelectedRow();
        if (row < 0) { 
            DialogStyler.mostrarAdvertencia(this, "Seleccione un ingrediente.", "Aviso");
            return; 
        }
        ingredientesActuales.remove(row);
        modelIngredientes.removeRow(row);
        actualizarTotales();
    }

    private void actualizarTotales() {
        double totalLibras = ingredientesActuales.stream().mapToDouble(DetalleFormulacion::getCantidadUsada).sum();
        double totalCosto = ingredientesActuales.stream().mapToDouble(DetalleFormulacion::getSubtotal).sum();
        
        lblTotalLibras.setText(String.format("%.2f lb", totalLibras));
        lblTotalCosto.setText(String.format("$%.2f", totalCosto));
        double costoPorLibra = totalLibras > 0 ? totalCosto / totalLibras : 0;
        lblCostoPromedio.setText(String.format("$%.3f / lb", costoPorLibra));
    }

    private void guardarFormulacion() {
        try {
            String nombre = txtNombreFormula.getText().trim();
            if (nombre.isEmpty()) { 
                DialogStyler.mostrarError(this, "Ingrese el nombre del producto.");
                return; 
            }
            if (ingredientesActuales.isEmpty()) { 
                DialogStyler.mostrarError(this, "Agregue al menos un ingrediente.");
                return; 
            }

            double totalLibras = ingredientesActuales.stream().mapToDouble(DetalleFormulacion::getCantidadUsada).sum();
            double totalCosto = ingredientesActuales.stream().mapToDouble(DetalleFormulacion::getSubtotal).sum();

            Formulacion f = new Formulacion();
            f.setNombre(nombre);
            f.setTotalLibras(totalLibras);
            f.setCostoTotal(totalCosto);
            f.setDetalles(new ArrayList<>(ingredientesActuales));

            try {
                formulacionDAO.guardar(f);
            } catch (SQLException ex) {
                String mensaje = ex.getMessage();
                if (mensaje != null && mensaje.contains("<br>")) {
                    JOptionPane.showMessageDialog(this, 
                        "<html><body style='font-family:Segoe UI;font-size:13px;padding:10px;'>" + mensaje + "</body></html>",
                        "Stock Insuficiente",
                        JOptionPane.ERROR_MESSAGE);
                } else {
                    DialogStyler.mostrarError(this, "Error al guardar: " + mensaje);
                }
                return;
            }

            DialogStyler.mostrarExito(this,
                "✅ Formulación guardada exitosamente.<br><br>" +
                "Producto: <b>" + nombre + "</b><br>" +
                "Libras preparadas: <b>" + String.format("%.2f", totalLibras) + "</b> lb<br>" +
                "Costo total: <b>$" + String.format("%.2f", totalCosto) + "</b>",
                "Guardado"
            );

            limpiarFormulario();
            cargarHistorial();
            if (stockPanel != null) {
                stockPanel.cargarDatos();
            }
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
            }
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al guardar: " + ex.getMessage());
        }
    }

    private void limpiarFormulario() {
        txtNombreFormula.setText("");
        ingredientesActuales.clear();
        modelIngredientes.setRowCount(0);
        lblTotalLibras.setText("0.00 lb");
        lblTotalCosto.setText("$0.00");
        lblCostoPromedio.setText("$0.000 / lb");
    }

    private void exportarPDFSeleccionado() {
        if (!verificarPermisoAdmin()) return;
        
        int row = tablaHistorial.getSelectedRow();
        if (row < 0) { 
            DialogStyler.mostrarAdvertencia(this, "Seleccione una formulación del historial.", "Aviso");
            return; 
        }
        row = tablaHistorial.convertRowIndexToModel(row);
        try {
            int id = (int) modelHistorial.getValueAt(row, 0);
            Formulacion f = formulacionDAO.obtenerPorId(id);
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Formulacion_" + f.getNombre().replace(" ", "_") + ".pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".pdf")) ruta += ".pdf";
                ReporteGenerator.generarReporteFormulacion(f, ruta);
                DialogStyler.mostrarExito(this, "PDF generado:<br><b>" + ruta + "</b>", "Éxito");
            }
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al generar PDF: " + ex.getMessage());
        }
    }

    private void renombrarFormulacionSeleccionada() {
        if (!verificarPermisoAdmin()) return;

        int row = tablaHistorial.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione una formulación para renombrar.", "Aviso");
            return;
        }
        row = tablaHistorial.convertRowIndexToModel(row);

        try {
            int id = (int) modelHistorial.getValueAt(row, 0);
            String nombreActual = (String) modelHistorial.getValueAt(row, 1);

            String nuevoNombre = JOptionPane.showInputDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:13px;padding:6px;'>" +
                "✏️ RENOMBRAR FORMULACIÓN<br><br>" +
                "Nombre actual: <b>" + nombreActual + "</b><br><br>" +
                "Nuevo nombre:" +
                "</body></html>",
                "Renombrar formulación",
                JOptionPane.QUESTION_MESSAGE);

            if (nuevoNombre == null) return;
            nuevoNombre = nuevoNombre.trim();

            if (nuevoNombre.isEmpty()) {
                DialogStyler.mostrarError(this, "El nombre no puede quedar vacío.");
                return;
            }
            if (nuevoNombre.equals(nombreActual)) return;

            formulacionDAO.renombrar(id, nuevoNombre);
            cargarHistorial();
            DialogStyler.mostrarExito(this, "Formulación renombrada correctamente a:<br><b>" + nuevoNombre + "</b>", "Éxito");

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al renombrar: " + ex.getMessage());
        }
    }

    private void estilizarTabla(JTable t) {
        t.setRowHeight(34);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(SLATE_OSCURO);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.setSelectionBackground(new Color(209, 250, 229));
        t.setSelectionForeground(new Color(6, 95, 70));
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDE_SUAVE);
        t.setIntercellSpacing(new Dimension(0, 1));

        t.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                String nombreCol = table.getColumnName(column);
                boolean esNumerica = nombreCol.equals("Cantidad") || nombreCol.equals("$/Unidad")
                    || nombreCol.equals("Subtotal") || nombreCol.equals("Libras")
                    || nombreCol.equals("Costo") || nombreCol.equals("ID")
                    || nombreCol.equals("Cantidad (lb)") || nombreCol.equals("Precio Unit.");
                boolean esEstado = nombreCol.equals("Estado");

                if (esEstado) {
                    JLabel pastilla = new JLabel(value != null ? value.toString() : "");
                    pastilla.setHorizontalAlignment(SwingConstants.CENTER);
                    pastilla.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    pastilla.setOpaque(true);
                    pastilla.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                    String estado = value != null ? value.toString().toUpperCase() : "";
                    if (estado.contains("VENDID")) {
                        pastilla.setBackground(new Color(241, 245, 249));
                        pastilla.setForeground(TEXTO_MUTED);
                    } else {
                        pastilla.setBackground(new Color(209, 250, 229));
                        pastilla.setForeground(new Color(6, 95, 70));
                    }
                    JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                    wrapper.setBackground(isSelected ? new Color(209, 250, 229) : (row % 2 == 0 ? Color.WHITE : FONDO));
                    wrapper.add(pastilla);
                    return wrapper;
                }

                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                c.setHorizontalAlignment(esNumerica ? SwingConstants.RIGHT : SwingConstants.LEFT);
                c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : FONDO);
                    c.setForeground(new Color(30, 41, 59));
                }
                return c;
            }
        });
    }

    private boolean esAdministrador() {
        return SessionManager.isAdmin();
    }

    private boolean verificarPermisoAdmin() {
        if (!esAdministrador()) {
            DialogStyler.mostrarAdvertencia(this,
                "🔒 ACCESO RESTRINGIDO<br><br>Esta función solo está disponible para <b>Administradores</b>.<br>Inicie sesión como Administrador para acceder.",
                "Permiso denegado");
            return false;
        }
        return true;
    }
}