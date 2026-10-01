package com.feedformulator.ui;

import com.feedformulator.dao.ClienteDAO;
import com.feedformulator.dao.FormulacionDAO;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.dao.VentaDAO;
import com.feedformulator.dao.VentaCreditoDAO;
import com.feedformulator.model.Cliente;
import com.feedformulator.model.Formulacion;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.Venta;
import com.feedformulator.model.VentaCredito;
import com.feedformulator.report.ReporteGenerator;
import com.feedformulator.util.MathUtils;
import com.feedformulator.util.LoggerUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VentasPanel extends JPanel {
    private final VentaDAO ventaDAO = new VentaDAO();
    private final FormulacionDAO formulacionDAO = new FormulacionDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VentaCreditoDAO ventaCreditoDAO = new VentaCreditoDAO();
    
    private DefaultTableModel tableModel;
    private JTable tabla;
    private JTextField txtBuscarVentas;
    private TableRowSorter<DefaultTableModel> sorterVentas;

    private static final Color VERDE = new Color(5, 150, 105);        // Verde Esmeralda #059669
    private static final Color VERDE_OSCURO = new Color(4, 120, 87);  // Esmeralda oscuro #047857
    private static final Color FONDO = new Color(248, 250, 252);      // Slate-50 #F8FAFC
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);  // Slate-900 #0F172A
    private static final Color SLATE_MEDIO = new Color(51, 65, 85);   // Slate-700 #334155
    private static final Color ROJO = new Color(220, 38, 38);         // Rojo suave #DC2626
    private static final Color AZUL = new Color(2, 132, 199);         // Celeste #0284C7
    private static final Color GRIS_BOTON = new Color(100, 116, 139); // Slate-500 #64748B
    private static final Color BORDE_SUAVE = new Color(226, 232, 240);
    private JLabel lblKpiInversion, lblKpiVentas, lblKpiGanancia, lblKpiPorcentaje, lblKpiVolumen;
    private JComboBox<String> cmbFiltroTipoVentas;

    public VentasPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(crearTitulo(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(crearPanelKpis(), BorderLayout.NORTH);
        centro.add(crearTabla(), BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
        cargarDatos();
    }

    // ================================================================
    //  TARJETAS KPI (reemplazan el texto plano de totales de antes)
    // ================================================================
    private JPanel crearPanelKpis() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);

        lblKpiInversion = new JLabel("$0.00");
        lblKpiVentas = new JLabel("$0.00");
        lblKpiGanancia = new JLabel("$0.00");
        lblKpiPorcentaje = new JLabel("+0.0%");
        lblKpiVolumen = new JLabel("0.00 lb");

        panel.add(crearTarjetaKpi("💰", "INVERSIÓN TOTAL", lblKpiInversion, "Costo base de materias primas", SLATE_OSCURO, null));
        panel.add(crearTarjetaKpi("💵", "VENTAS TOTALES", lblKpiVentas, "Facturación bruta registrada", SLATE_OSCURO, null));
        panel.add(crearTarjetaKpi("📈", "GANANCIA NETA", lblKpiGanancia, "Margen neto sobre ventas", VERDE, lblKpiPorcentaje));
        panel.add(crearTarjetaKpi("⚖️", "VOLUMEN VENDIDO", lblKpiVolumen, "En despachos confirmados", SLATE_OSCURO, null));

        return panel;
    }

    private JPanel crearTarjetaKpi(String icono, String titulo, JLabel valor, String subtitulo, Color colorValor, JLabel badgePorcentaje) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 6));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JPanel filaSuperior = new JPanel(new BorderLayout());
        filaSuperior.setOpaque(false);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitulo.setForeground(new Color(100, 116, 139));
        filaSuperior.add(lblTitulo, BorderLayout.WEST);

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setOpaque(true);
        lblIcono.setBackground(new Color(colorValor.getRed(), colorValor.getGreen(), colorValor.getBlue(), 22));
        lblIcono.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        filaSuperior.add(lblIcono, BorderLayout.EAST);
        tarjeta.add(filaSuperior, BorderLayout.NORTH);

        JPanel filaValor = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaValor.setOpaque(false);
        valor.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valor.setForeground(colorValor.equals(VERDE) ? VERDE : new Color(15, 23, 42));
        filaValor.add(valor);
        if (badgePorcentaje != null) {
            badgePorcentaje.setOpaque(true);
            badgePorcentaje.setBackground(new Color(209, 250, 229));
            badgePorcentaje.setForeground(VERDE_OSCURO);
            badgePorcentaje.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badgePorcentaje.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            filaValor.add(badgePorcentaje);
        }
        tarjeta.add(filaValor, BorderLayout.CENTER);

        JLabel lblSubtitulo = new JLabel(subtitulo);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubtitulo.setForeground(new Color(148, 163, 184));
        tarjeta.add(lblSubtitulo, BorderLayout.SOUTH);

        return tarjeta;
    }

    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("💰", "Registro de Ventas", "Historial de ventas al contado y a crédito");
    }

    private JScrollPane crearTabla() {
        String[] cols = {"ID", "Producto", "Libras", "Inversión", "Precio Venta", "Ganancia", "Fecha", "Hora", "Cliente", "Tipo", "Pago"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override 
            public boolean isCellEditable(int r, int c) { 
                return false; 
            }
        };
        tabla = new JTable(tableModel);
        
        sorterVentas = new TableRowSorter<>(tableModel);
        tabla.setRowSorter(sorterVentas);
        
        estilizarTabla(tabla);
        
        tabla.getColumnModel().getColumn(0).setMaxWidth(40);
        tabla.getColumnModel().getColumn(8).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(9).setMaxWidth(90);
        
        JPanel headerVentas = new JPanel(new BorderLayout());
        headerVentas.setOpaque(false);
        headerVentas.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // ---- Buscador con estilo moderno redondeado ----
        JPanel buscadorWrap = new JPanel(new BorderLayout());
        buscadorWrap.setBackground(Color.WHITE);
        buscadorWrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel lblBuscar = new JLabel("🔍");
        lblBuscar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        lblBuscar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        buscadorWrap.add(lblBuscar, BorderLayout.WEST);

        txtBuscarVentas = new JTextField(22);
        txtBuscarVentas.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBuscarVentas.setBorder(BorderFactory.createEmptyBorder());
        txtBuscarVentas.setToolTipText("Buscar por ID, producto o cliente...");
        txtBuscarVentas.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarVentas(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarVentas(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarVentas(); }
        });
        buscadorWrap.add(txtBuscarVentas, BorderLayout.CENTER);
        headerVentas.add(buscadorWrap, BorderLayout.WEST);

        // ---- Filtro rápido de Tipo (Todos / Contado / Crédito) ----
        // Puramente aditivo: no cambia el comportamiento del buscador de texto
        // existente, solo agrega un filtro adicional sobre la misma tabla.
        JPanel filtrosPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtrosPanel.setOpaque(false);

        cmbFiltroTipoVentas = new JComboBox<>(new String[]{"Todos", "Solo Contado", "Solo Crédito"});
        cmbFiltroTipoVentas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFiltroTipoVentas.addActionListener(e -> filtrarVentas());
        filtrosPanel.add(cmbFiltroTipoVentas);

        headerVentas.add(filtrosPanel, BorderLayout.EAST);
        
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setOpaque(false);
        panelTabla.add(headerVentas, BorderLayout.NORTH);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panelTabla.add(scrollTabla, BorderLayout.CENTER);
        
        JScrollPane scroll = new JScrollPane(panelTabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(FONDO);
        
        return scroll;
    }

    private void estilizarTabla(JTable t) {
        t.setRowHeight(34);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(SLATE_OSCURO);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
        t.setSelectionBackground(new Color(209, 250, 229));
        t.setSelectionForeground(new Color(15, 23, 42));
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDE_SUAVE);
        t.setIntercellSpacing(new Dimension(0, 1));

        t.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                c.setHorizontalAlignment(SwingConstants.LEFT);
                c.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                // Zebra striping: filas pares blancas, impares gris muy sutil
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : FONDO);
                }

                // Columnas numéricas alineadas a la derecha
                if (column == 2 || column == 3 || column == 4) {
                    c.setHorizontalAlignment(SwingConstants.RIGHT);
                }
                // Fecha/Hora centradas
                if (column == 6 || column == 7) {
                    c.setHorizontalAlignment(SwingConstants.CENTER);
                    c.setFont(new Font("Consolas", Font.PLAIN, 12));
                }

                // Columna Ganancia: verde esmeralda, negrita, derecha
                if (column == 5 && value != null) {
                    c.setHorizontalAlignment(SwingConstants.RIGHT);
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    try {
                        String valorTxt = value.toString().replace("$", "").replace(",", ".");
                        double ganancia = Double.parseDouble(valorTxt);
                        if (!isSelected) c.setForeground(ganancia < 0 ? ROJO : VERDE);
                    } catch (Exception e) { /* ignorar formato inesperado */ }
                } else if (!isSelected) {
                    c.setForeground(new Color(30, 41, 59));
                }

                // Columna Cliente: consumidor final en gris
                if (column == 8 && value != null && "Consumidor Final".equals(value.toString()) && !isSelected) {
                    c.setForeground(new Color(148, 163, 184));
                }

                return c;
            }
        });

        // ---- Columna "Tipo": pastilla/badge redondeado ----
        t.getColumnModel().getColumn(9).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "");
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                label.setOpaque(false);
                label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

                String tipo = value != null ? value.toString().toUpperCase() : "";
                Color fondoPastilla, colorTexto;
                if (tipo.contains("CRÉDITO") || tipo.contains("CREDITO")) {
                    fondoPastilla = new Color(254, 243, 199);
                    colorTexto = new Color(146, 64, 14);
                } else {
                    fondoPastilla = new Color(209, 250, 229);
                    colorTexto = new Color(6, 95, 70);
                }
                label.setForeground(colorTexto);

                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                wrapper.setBackground(isSelected ? new Color(209, 250, 229) : (row % 2 == 0 ? Color.WHITE : FONDO));
                label.setBackground(fondoPastilla);
                label.setOpaque(true);
                wrapper.add(label);
                return wrapper;
            }
        });
    }

    private void filtrarVentas() {
        String filtroTexto = txtBuscarVentas.getText().trim();
        String filtroTipo = cmbFiltroTipoVentas != null ? (String) cmbFiltroTipoVentas.getSelectedItem() : "Todos";

        java.util.List<RowFilter<Object, Object>> filtros = new java.util.ArrayList<>();
        if (!filtroTexto.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + filtroTexto, 0, 1, 6, 8));
        }
        if (filtroTipo != null && !"Todos".equals(filtroTipo)) {
            String tipoBuscado = filtroTipo.contains("Contado") ? "CONTADO" : "CRÉDITO";
            filtros.add(RowFilter.regexFilter("(?i)" + tipoBuscado, 9));
        }

        sorterVentas.setRowFilter(filtros.isEmpty() ? null : RowFilter.andFilter(filtros));
    }

    private JPanel crearBotones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p.setOpaque(false);

        try {
            ImageIcon iconAdd = new ImageIcon(getClass().getResource("/icons/add.png"));
            ImageIcon iconEdit = new ImageIcon(getClass().getResource("/icons/edit.png"));
            ImageIcon iconDelete = new ImageIcon(getClass().getResource("/icons/delete.png"));
            ImageIcon iconPdf = new ImageIcon(getClass().getResource("/icons/pdf.png"));
            ImageIcon iconRefresh = new ImageIcon(getClass().getResource("/icons/refresh.png"));

            Image imgAdd = iconAdd.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            Image imgEdit = iconEdit.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            Image imgDelete = iconDelete.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            Image imgPdf = iconPdf.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            Image imgRefresh = iconRefresh.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);

            JButton btnNueva = new JButton("Nueva Venta", new ImageIcon(imgAdd));
            JButton btnEditar = new JButton("Editar Venta", new ImageIcon(imgEdit));
            JButton btnEliminar = new JButton("Eliminar Venta", new ImageIcon(imgDelete));
            JButton btnPDF = new JButton("Exportar PDF", new ImageIcon(imgPdf));
            JButton btnRefresh = new JButton("Actualizar", new ImageIcon(imgRefresh));

            btnNueva.setBackground(VERDE);
            btnEditar.setBackground(SLATE_MEDIO);
            btnEliminar.setBackground(ROJO);
            btnPDF.setBackground(AZUL);
            btnRefresh.setBackground(GRIS_BOTON);

            aplicarEstiloBoton(btnNueva);
            aplicarEstiloBoton(btnEditar);
            aplicarEstiloBoton(btnEliminar);
            aplicarEstiloBoton(btnPDF);
            aplicarEstiloBoton(btnRefresh);

            btnNueva.addActionListener(evt -> mostrarDialogoVenta());
            btnEditar.addActionListener(evt -> editarVenta());
            btnEliminar.addActionListener(evt -> eliminarVenta());
            btnPDF.addActionListener(evt -> exportarPDF());
            btnRefresh.addActionListener(evt -> cargarDatos());

            p.add(btnNueva);
            p.add(btnEditar);
            p.add(btnEliminar);
            p.add(btnPDF);
            p.add(btnRefresh);
        } catch (Exception ex) {
            JButton btnNueva = new JButton("➕ Nueva Venta");
            JButton btnEditar = new JButton("✏️ Editar Venta");
            JButton btnEliminar = new JButton("🗑 Eliminar Venta");
            JButton btnPDF = new JButton("📄 Exportar PDF");
            JButton btnRefresh = new JButton("🔄 Actualizar");

            btnNueva.setBackground(VERDE);
            btnEditar.setBackground(SLATE_MEDIO);
            btnEliminar.setBackground(ROJO);
            btnPDF.setBackground(AZUL);
            btnRefresh.setBackground(GRIS_BOTON);

            aplicarEstiloBoton(btnNueva);
            aplicarEstiloBoton(btnEditar);
            aplicarEstiloBoton(btnEliminar);
            aplicarEstiloBoton(btnPDF);
            aplicarEstiloBoton(btnRefresh);

            btnNueva.addActionListener(evt -> mostrarDialogoVenta());
            btnEditar.addActionListener(evt -> editarVenta());
            btnEliminar.addActionListener(evt -> eliminarVenta());
            btnPDF.addActionListener(evt -> exportarPDF());
            btnRefresh.addActionListener(evt -> cargarDatos());

            p.add(btnNueva);
            p.add(btnEditar);
            p.add(btnEliminar);
            p.add(btnPDF);
            p.add(btnRefresh);
        }

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottom.add(p, BorderLayout.CENTER);

        actualizarResumen();
        return bottom;
    }

    private void aplicarEstiloBoton(JButton btn) {
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
    }

    private void actualizarResumen() {
        try {
            List<Venta> ventas = ventaDAO.listarTodas();
            double inv = MathUtils.redondear(ventas.stream().mapToDouble(Venta::getCostoInversion).sum());
            double ven = MathUtils.redondear(ventas.stream().mapToDouble(Venta::getPrecioVenta).sum());
            double gan = MathUtils.redondear(ventas.stream().mapToDouble(Venta::getGanancia).sum());
            double libras = MathUtils.redondear(ventas.stream().mapToDouble(Venta::getLibrasVendidas).sum());
            double margen = ven > 0 ? (gan / ven) * 100.0 : 0;

            if (lblKpiInversion != null) {
                lblKpiInversion.setText(String.format("$%.2f", inv));
                lblKpiVentas.setText(String.format("$%.2f", ven));
                lblKpiGanancia.setText(String.format("$%.2f", gan));
                lblKpiGanancia.setForeground(gan >= 0 ? VERDE : ROJO);
                lblKpiPorcentaje.setText(String.format("%s%.1f%%", margen >= 0 ? "+" : "", margen));
                lblKpiVolumen.setText(String.format("%.2f lb", libras));
            }
        } catch (Exception ignored) {}
    }

    // ============================================================
    //  MOSTRAR DIÁLOGO DE VENTA - CON FECHA LÍMITE
    // ============================================================
    /** Envuelve un JRadioButton en una tarjeta clicable con resaltado verde
     *  cuando está seleccionada, imitando el estilo tipo "pill card" del mockup. */
    private JPanel crearTarjetaOpcion(JRadioButton rb, String titulo, String subtitulo) {
        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        rb.setText(""); // El título se muestra en lblTitulo; evita duplicar el texto
        rb.setOpaque(false);
        JPanel filaTitulo = new JPanel(new BorderLayout(6, 0));
        filaTitulo.setOpaque(false);
        filaTitulo.add(rb, BorderLayout.WEST);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filaTitulo.add(lblTitulo, BorderLayout.CENTER);
        card.add(filaTitulo, BorderLayout.NORTH);

        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSub.setForeground(new Color(148, 163, 184));
        lblSub.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        card.add(lblSub, BorderLayout.SOUTH);

        Runnable actualizarEstiloCard = () -> {
            if (!rb.isEnabled()) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(254, 202, 202), 2),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                card.setBackground(new Color(254, 242, 242));
                lblTitulo.setForeground(new Color(190, 150, 150));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else if (rb.isSelected()) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(5, 150, 105), 2),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                card.setBackground(new Color(240, 253, 244));
                lblTitulo.setForeground(new Color(4, 120, 87));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 2),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                card.setBackground(Color.WHITE);
                lblTitulo.setForeground(new Color(30, 41, 59));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        };
        rb.addItemListener(e -> actualizarEstiloCard.run());
        rb.addPropertyChangeListener("enabled", e -> actualizarEstiloCard.run());
        actualizarEstiloCard.run();

        java.awt.event.MouseAdapter clic = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (rb.isEnabled()) rb.doClick();
            }
        };
        card.addMouseListener(clic);
        filaTitulo.addMouseListener(clic);
        lblTitulo.addMouseListener(clic);
        lblSub.addMouseListener(clic);

        return card;
    }

    private void mostrarDialogoVenta() {
        try {
            List<Formulacion> formulaciones = formulacionDAO.listarDisponibles();
            if (formulaciones.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, 
                    "No hay formulaciones disponibles para vender.<br>" +
                    "Todas las formulaciones ya han sido vendidas o no hay formulaciones registradas.", 
                    "Sin datos");
                return;
            }

            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Registrar Venta",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            Dimension tamanoIdeal = new Dimension(700, 600);
            dialog.setPreferredSize(tamanoIdeal);
            dialog.setMinimumSize(new Dimension(680, 560));

            DialogStyler.aplicar(dialog, "Registrar Venta", "💰");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();
            gbc.insets = new Insets(3, 8, 3, 8);

            JComboBox<Formulacion> cmbFormulacion = new JComboBox<>(formulaciones.toArray(new Formulacion[0]));
            cmbFormulacion.setFont(DialogStyler.FUENTE_CAMPO);
            cmbFormulacion.setPrototypeDisplayValue(null);
            cmbFormulacion.setPreferredSize(new Dimension(300, 30));
            cmbFormulacion.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                              boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof Formulacion) {
                        Formulacion f = (Formulacion) value;
                        value = f.getNombre() + " (" + String.format("%.2f", f.getTotalLibras()) + " lb) - $" + String.format("%.2f", f.getCostoTotal());
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            
            JTextField txtPrecioVenta = DialogStyler.crearCampoNumerico(12);
            
            JTextField txtAbonoInicial = DialogStyler.crearCampoNumerico(12);
            txtAbonoInicial.setText("0.00");
            txtAbonoInicial.setEnabled(false);
            txtAbonoInicial.setBackground(new Color(240, 240, 240));
            
            // ===== CAMPO DE FECHA LÍMITE (NUEVO) =====
            JLabel lblFechaLimite = DialogStyler.crearLabel("Fecha límite de pago:");
            JTextField txtFechaLimite = DialogStyler.crearCampo(12);
            txtFechaLimite.setToolTipText("Formato: YYYY-MM-DD (Ej: 2026-12-31)");
            txtFechaLimite.setEnabled(false);
            txtFechaLimite.setBackground(new Color(240, 240, 240));
            
            // Mostrar fecha sugerida (30 días por defecto)
            LocalDate fechaSugerida = LocalDate.now().plusDays(30);
            txtFechaLimite.setText(fechaSugerida.toString());
            
            JButton btnCalendario = new JButton("📅");
            btnCalendario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btnCalendario.setBackground(DialogStyler.FONDO_FORM);
            btnCalendario.setFocusPainted(false);
            btnCalendario.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnCalendario.setEnabled(false);
            btnCalendario.addActionListener(e -> {
                String input = JOptionPane.showInputDialog(dialog,
                    "Ingrese la fecha límite (YYYY-MM-DD):\n" +
                    "Ejemplo: 2026-12-31",
                    txtFechaLimite.getText()
                );
                if (input != null && !input.trim().isEmpty()) {
                    try {
                        LocalDate.parse(input.trim());
                        txtFechaLimite.setText(input.trim());
                    } catch (Exception ex) {
                        DialogStyler.mostrarError(dialog, "Formato de fecha inválido. Use YYYY-MM-DD");
                    }
                }
            });
            
            JPanel panelFechaLimite = new JPanel(new BorderLayout(5, 0));
            panelFechaLimite.setBackground(DialogStyler.FONDO_FORM);
            panelFechaLimite.add(txtFechaLimite, BorderLayout.CENTER);
            panelFechaLimite.add(btnCalendario, BorderLayout.EAST);
            
            JLabel lblDetalle = new JLabel(" ");
            lblDetalle.setFont(DialogStyler.FUENTE_INFO);
            lblDetalle.setForeground(DialogStyler.TEXTO_SECUNDARIO);

            // ---- Tarjeta de detalle del lote (peso, costo base, $/lb) ----
            JPanel tarjetaDetalleLote = new JPanel();
            tarjetaDetalleLote.setLayout(new BoxLayout(tarjetaDetalleLote, BoxLayout.Y_AXIS));
            tarjetaDetalleLote.setBackground(new Color(240, 253, 244));
            tarjetaDetalleLote.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel lblDetalleLote = new JLabel(" ");
            lblDetalleLote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDetalleLote.setForeground(new Color(21, 128, 61));
            tarjetaDetalleLote.add(lblDetalleLote);

            // ---- Botones de margen rápido (+20%, +25%, +30%, +35%) ----
            JPanel panelMargenRapido = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            panelMargenRapido.setBackground(DialogStyler.FONDO_FORM);
            JLabel lblMargenRapido = new JLabel("Margen rápido:");
            lblMargenRapido.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblMargenRapido.setForeground(DialogStyler.TEXTO_SECUNDARIO);
            panelMargenRapido.add(lblMargenRapido);
            int[] margenesRapidos = {20, 25, 30, 35};
            java.util.List<JButton> botonesMargen = new java.util.ArrayList<>();
            for (int margen : margenesRapidos) {
                JButton btnMargen = new JButton("+" + margen + "%");
                btnMargen.setFont(new Font("Segoe UI", Font.BOLD, 10));
                btnMargen.setFocusPainted(false);
                btnMargen.setBackground(Color.WHITE);
                btnMargen.setForeground(new Color(71, 85, 105));
                btnMargen.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
                btnMargen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                botonesMargen.add(btnMargen);
                panelMargenRapido.add(btnMargen);
            }
            
            JLabel lblPreview = new JLabel("Ingrese un precio para ver la ganancia");
            lblPreview.setFont(DialogStyler.FUENTE_BOTON);
            lblPreview.setForeground(DialogStyler.TEXTO_SECUNDARIO);

            // ---- Tarjeta de ganancia dinámica (reemplaza el texto plano) ----
            JPanel tarjetaGanancia = new JPanel(new BorderLayout(10, 0));
            tarjetaGanancia.setBackground(new Color(240, 253, 244));
            tarjetaGanancia.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel lblIconoGanancia = new JLabel("▲");
            lblIconoGanancia.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblIconoGanancia.setForeground(new Color(4, 120, 87));
            tarjetaGanancia.add(lblIconoGanancia, BorderLayout.WEST);
            JLabel lblMargenBadge = new JLabel(" ");
            lblMargenBadge.setOpaque(true);
            lblMargenBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblMargenBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            JPanel filaMargen = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            filaMargen.setOpaque(false);
            filaMargen.add(lblMargenBadge);
            tarjetaGanancia.add(filaMargen, BorderLayout.EAST);
            lblPreview.setOpaque(false);
            tarjetaGanancia.add(lblPreview, BorderLayout.CENTER);

            JRadioButton rbContado = new JRadioButton("Venta al Contado", true);
            JRadioButton rbCredito = new JRadioButton("Venta a Crédito", false);
            ButtonGroup groupTipoVenta = new ButtonGroup();
            groupTipoVenta.add(rbContado);
            groupTipoVenta.add(rbCredito);
            rbContado.setFont(new Font("Segoe UI", Font.BOLD, 12));
            rbCredito.setFont(new Font("Segoe UI", Font.BOLD, 12));
            rbContado.setBackground(DialogStyler.FONDO_FORM);
            rbCredito.setBackground(DialogStyler.FONDO_FORM);

            JRadioButton rbEfectivo = new JRadioButton("Efectivo", true);
            JRadioButton rbTransferencia = new JRadioButton("Transferencia", false);
            ButtonGroup groupFormaPago = new ButtonGroup();
            groupFormaPago.add(rbEfectivo);
            groupFormaPago.add(rbTransferencia);
            rbEfectivo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            rbTransferencia.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            rbEfectivo.setBackground(DialogStyler.FONDO_FORM);
            rbTransferencia.setBackground(DialogStyler.FONDO_FORM);
            
            rbCredito.addActionListener(e -> {
                if (rbCredito.isSelected()) {
                    txtAbonoInicial.setEnabled(true);
                    txtAbonoInicial.setBackground(Color.WHITE);
                    txtAbonoInicial.setText("0.00");
                    txtFechaLimite.setEnabled(true);
                    txtFechaLimite.setBackground(Color.WHITE);
                    btnCalendario.setEnabled(true);
                    txtFechaLimite.setText(LocalDate.now().plusDays(30).toString());
                } else {
                    txtAbonoInicial.setEnabled(false);
                    txtAbonoInicial.setBackground(new Color(240, 240, 240));
                    txtAbonoInicial.setText("0.00");
                    txtFechaLimite.setEnabled(false);
                    txtFechaLimite.setBackground(new Color(240, 240, 240));
                    btnCalendario.setEnabled(false);
                    txtFechaLimite.setText("");
                }
            });
            
            rbContado.addActionListener(e -> {
                txtAbonoInicial.setEnabled(false);
                txtAbonoInicial.setBackground(new Color(240, 240, 240));
                txtAbonoInicial.setText("0.00");
                txtFechaLimite.setEnabled(false);
                txtFechaLimite.setBackground(new Color(240, 240, 240));
                btnCalendario.setEnabled(false);
                txtFechaLimite.setText("");
            });

            JRadioButton rbConsumidorFinal = new JRadioButton("Consumidor Final", true);
            JRadioButton rbClienteDatos = new JRadioButton("Cliente con Datos", false);
            ButtonGroup groupTipoCliente = new ButtonGroup();
            groupTipoCliente.add(rbConsumidorFinal);
            groupTipoCliente.add(rbClienteDatos);
            rbConsumidorFinal.setFont(new Font("Segoe UI", Font.BOLD, 12));
            rbClienteDatos.setFont(new Font("Segoe UI", Font.BOLD, 12));
            rbConsumidorFinal.setBackground(DialogStyler.FONDO_FORM);
            rbClienteDatos.setBackground(DialogStyler.FONDO_FORM);

            JPanel panelDatosCliente = new JPanel(new GridBagLayout());
            panelDatosCliente.setBackground(DialogStyler.FONDO_FORM);
            panelDatosCliente.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DialogStyler.BORDE), 
                "Datos del Cliente", 
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                DialogStyler.FUENTE_LABEL,
                DialogStyler.TEXTO_LABEL
            ));
            
            GridBagConstraints gbcCliente = new GridBagConstraints();
            gbcCliente.insets = new Insets(3, 5, 3, 5);
            gbcCliente.anchor = GridBagConstraints.WEST;
            gbcCliente.fill = GridBagConstraints.HORIZONTAL;
            
            JTextField txtBuscarCliente = DialogStyler.crearCampo(15);
            txtBuscarCliente.setToolTipText("Escriba el nombre o código del cliente");
            
            JComboBox<Cliente> cmbCliente = new JComboBox<>();
            cmbCliente.setFont(DialogStyler.FUENTE_CAMPO);
            
            JButton btnNuevoCliente = DialogStyler.crearBotonPrimario("➕ Nuevo");
            btnNuevoCliente.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnNuevoCliente.addActionListener(e -> {
                Cliente nuevo = mostrarDialogoCrearClienteDesdeVenta();
                if (nuevo != null) {
                    cargarClientes(cmbCliente);
                    cmbCliente.setSelectedItem(nuevo);
                }
            });
            
            JTextField txtNombreCliente = DialogStyler.crearCampo(15);
            txtNombreCliente.setEditable(false);
            JTextField txtTelefonoCliente = DialogStyler.crearCampo(15);
            txtTelefonoCliente.setEditable(false);
            JTextField txtEmailCliente = DialogStyler.crearCampo(15);
            txtEmailCliente.setEditable(false);
            JTextField txtDireccionCliente = DialogStyler.crearCampo(15);
            txtDireccionCliente.setEditable(false);
            JTextField txtRucCliente = DialogStyler.crearCampo(15);
            txtRucCliente.setEditable(false);
            
            int cfila = 0;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("Buscar:"), gbcCliente);
            gbcCliente.gridx = 1; gbcCliente.gridwidth = 2;
            panelDatosCliente.add(txtBuscarCliente, gbcCliente);
            gbcCliente.gridwidth = 1;
            
            cfila++;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("Cliente:"), gbcCliente);
            gbcCliente.gridx = 1;
            panelDatosCliente.add(cmbCliente, gbcCliente);
            gbcCliente.gridx = 2;
            panelDatosCliente.add(btnNuevoCliente, gbcCliente);
            
            cfila++;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("Nombre:"), gbcCliente);
            gbcCliente.gridx = 1; gbcCliente.gridwidth = 2;
            panelDatosCliente.add(txtNombreCliente, gbcCliente);
            gbcCliente.gridwidth = 1;
            
            cfila++;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("Teléfono:"), gbcCliente);
            gbcCliente.gridx = 1; gbcCliente.gridwidth = 2;
            panelDatosCliente.add(txtTelefonoCliente, gbcCliente);
            gbcCliente.gridwidth = 1;
            
            cfila++;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("Email:"), gbcCliente);
            gbcCliente.gridx = 1; gbcCliente.gridwidth = 2;
            panelDatosCliente.add(txtEmailCliente, gbcCliente);
            gbcCliente.gridwidth = 1;
            
            cfila++;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("Dirección:"), gbcCliente);
            gbcCliente.gridx = 1; gbcCliente.gridwidth = 2;
            panelDatosCliente.add(txtDireccionCliente, gbcCliente);
            gbcCliente.gridwidth = 1;
            
            cfila++;
            gbcCliente.gridx = 0; gbcCliente.gridy = cfila;
            panelDatosCliente.add(new JLabel("RUC/Cédula:"), gbcCliente);
            gbcCliente.gridx = 1; gbcCliente.gridwidth = 2;
            panelDatosCliente.add(txtRucCliente, gbcCliente);
            gbcCliente.gridwidth = 1;

            rbConsumidorFinal.addActionListener(e -> {
                boolean seleccionado = rbConsumidorFinal.isSelected();
                panelDatosCliente.setEnabled(!seleccionado);
                panelDatosCliente.setVisible(!seleccionado);
                for (Component c : panelDatosCliente.getComponents()) {
                    c.setEnabled(!seleccionado);
                }
                if (seleccionado) {
                    rbCredito.setSelected(false);
                    rbContado.setSelected(true);
                    rbCredito.setEnabled(false);
                } else {
                    rbCredito.setEnabled(true);
                }
                dialog.revalidate(); dialog.repaint();
            });
            
            rbClienteDatos.addActionListener(e -> {
                boolean seleccionado = rbClienteDatos.isSelected();
                panelDatosCliente.setEnabled(seleccionado);
                panelDatosCliente.setVisible(seleccionado);
                for (Component c : panelDatosCliente.getComponents()) {
                    c.setEnabled(seleccionado);
                }
                if (seleccionado) {
                    rbCredito.setEnabled(true);
                }
                dialog.revalidate(); dialog.repaint();
            });
            
            cmbCliente.addActionListener(e -> {
                Cliente c = (Cliente) cmbCliente.getSelectedItem();
                if (c != null) {
                    txtNombreCliente.setText(c.getNombre());
                    txtTelefonoCliente.setText(c.getTelefono() != null ? c.getTelefono() : "");
                    txtEmailCliente.setText(c.getEmail() != null ? c.getEmail() : "");
                    txtDireccionCliente.setText(c.getDireccion() != null ? c.getDireccion() : "");
                    txtRucCliente.setText(c.getRucCedula() != null ? c.getRucCedula() : "");
                }
            });
            
            txtBuscarCliente.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarClientes(txtBuscarCliente, cmbCliente); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarClientes(txtBuscarCliente, cmbCliente); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarClientes(txtBuscarCliente, cmbCliente); }
            });

            Runnable actualizarPreview = () -> {
                try {
                    Formulacion sel = (Formulacion) cmbFormulacion.getSelectedItem();
                    if (sel != null) {
                        lblDetalle.setText("📦 " + sel.getNombre() + " | " + String.format("%.2f", sel.getTotalLibras()) + " lb | Costo: $" + String.format("%.2f", sel.getCostoTotal()));
                        double costoPorLibra = sel.getTotalLibras() > 0 ? sel.getCostoTotal() / sel.getTotalLibras() : 0;
                        lblDetalleLote.setText(String.format(
                            "<html>Peso: <b>%.2f lb</b> &nbsp;|&nbsp; Costo Base: <b>$%.2f</b> &nbsp;|&nbsp; ($%.3f/lb)</html>",
                            sel.getTotalLibras(), sel.getCostoTotal(), costoPorLibra));

                        String precioStr = txtPrecioVenta.getText().trim().replace(",", ".");
                        if (!precioStr.isEmpty()) {
                            double precio = Double.parseDouble(precioStr);
                            double ganancia = MathUtils.redondear(precio - sel.getCostoTotal());
                            double margenPct = precio > 0 ? (ganancia / precio) * 100.0 : 0;
                            String estado = ganancia >= 0 ? "GANANCIA NETA ESTIMADA" : "⚠️ PRECIO POR DEBAJO DEL COSTO";
                            lblPreview.setText("<html><b>" + estado + "</b><br>" +
                                (ganancia >= 0 ? "+$" : "-$") + String.format("%.2f", Math.abs(ganancia)) + "</html>");
                            lblPreview.setForeground(ganancia >= 0 ? DialogStyler.ACENTO : DialogStyler.PELIGRO);
                            lblMargenBadge.setText(String.format("%.1f%% Margen", margenPct));
                            lblMargenBadge.setBackground(ganancia >= 0 ? new Color(209, 250, 229) : new Color(254, 226, 226));
                            lblMargenBadge.setForeground(ganancia >= 0 ? new Color(6, 95, 70) : new Color(153, 27, 27));
                            lblIconoGanancia.setText(ganancia >= 0 ? "▲" : "!");
                            lblIconoGanancia.setForeground(ganancia >= 0 ? new Color(4, 120, 87) : new Color(153, 27, 27));
                        } else {
                            lblPreview.setText("Ingrese un precio para ver la ganancia");
                            lblPreview.setForeground(DialogStyler.TEXTO_SECUNDARIO);
                            lblMargenBadge.setText(" ");
                        }
                    }
                } catch (NumberFormatException ignored) {
                    lblPreview.setText("❌ Precio inválido");
                    lblPreview.setForeground(DialogStyler.PELIGRO);
                }
            };

            for (int idx = 0; idx < margenesRapidos.length; idx++) {
                int margen = margenesRapidos[idx];
                botonesMargen.get(idx).addActionListener(evtMargen -> {
                    Formulacion sel = (Formulacion) cmbFormulacion.getSelectedItem();
                    if (sel != null) {
                        double nuevoPrecio = sel.getCostoTotal() * (1 + margen / 100.0);
                        txtPrecioVenta.setText(String.format("%.2f", nuevoPrecio));
                    }
                });
            }

            cmbFormulacion.addActionListener(evt -> actualizarPreview.run());
            txtPrecioVenta.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview.run(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview.run(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview.run(); }
            });

            int fila = 0;

            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(DialogStyler.crearLabel("Formulación disponible / Lote preparado:"), gbc);
            fila++;
            gbc.gridy = fila;
            form.add(cmbFormulacion, gbc);

            fila++;
            gbc.gridy = fila;
            form.add(tarjetaDetalleLote, gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Precio de venta total ($):"), gbc);
            gbc.gridx = 1;
            form.add(txtPrecioVenta, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(panelMargenRapido, gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Abono inicial ($):"), gbc);
            gbc.gridx = 1;
            JPanel panelAbono = new JPanel(new BorderLayout());
            panelAbono.setBackground(DialogStyler.FONDO_FORM);
            panelAbono.add(txtAbonoInicial, BorderLayout.CENTER);
            JLabel lblAbonoInfo = new JLabel(" (Solo para crédito)");
            lblAbonoInfo.setFont(DialogStyler.FUENTE_INFO);
            lblAbonoInfo.setForeground(DialogStyler.TEXTO_SECUNDARIO);
            panelAbono.add(lblAbonoInfo, BorderLayout.EAST);
            form.add(panelAbono, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(lblFechaLimite, gbc);
            gbc.gridx = 1;
            form.add(panelFechaLimite, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(tarjetaGanancia, gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Tipo de venta:"), gbc);
            gbc.gridx = 1;
            form.add(DialogStyler.crearLabel("Forma de pago:"), gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            JPanel panelTipoVenta = new JPanel(new GridLayout(1, 2, 6, 0));
            panelTipoVenta.setBackground(DialogStyler.FONDO_FORM);
            JPanel tarjetaContado = crearTarjetaOpcion(rbContado, "Venta al Contado", "Cobro total inmediato");
            JPanel tarjetaCredito = crearTarjetaOpcion(rbCredito, "Venta a Crédito", "Cuenta por cobrar");
            panelTipoVenta.add(tarjetaContado);
            panelTipoVenta.add(tarjetaCredito);
            gbc.gridx = 0;
            form.add(panelTipoVenta, gbc);

            JPanel panelFormaPago = new JPanel(new GridLayout(1, 2, 6, 0));
            panelFormaPago.setBackground(DialogStyler.FONDO_FORM);
            JPanel tarjetaEfectivo = crearTarjetaOpcion(rbEfectivo, "Efectivo", "Caja chica");
            JPanel tarjetaTransferencia = crearTarjetaOpcion(rbTransferencia, "Transferencia", "Bancaria");
            panelFormaPago.add(tarjetaEfectivo);
            panelFormaPago.add(tarjetaTransferencia);
            gbc.gridx = 1;
            form.add(panelFormaPago, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Tipo de cliente:"), gbc);
            gbc.gridx = 1;
            JPanel panelTipoCliente = new JPanel(new GridLayout(1, 2, 6, 0));
            panelTipoCliente.setBackground(DialogStyler.FONDO_FORM);
            panelTipoCliente.add(crearTarjetaOpcion(rbConsumidorFinal, "Consumidor Final", "Despacho exprés"));
            panelTipoCliente.add(crearTarjetaOpcion(rbClienteDatos, "Cliente con Datos", "Registrar comprador"));
            form.add(panelTipoCliente, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            panelDatosCliente.setVisible(false);
            panelDatosCliente.setEnabled(false);
            form.add(panelDatosCliente, gbc);
            gbc.gridwidth = 1;

            JButton btnGuardar = DialogStyler.crearBotonPrimario("Registrar Venta");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(evt -> {
                try {
                    Formulacion formulacion = (Formulacion) cmbFormulacion.getSelectedItem();
                    if (formulacion == null) {
                        DialogStyler.mostrarError(dialog, "Seleccione una formulación válida.");
                        return;
                    }

                    if (!"Disponible".equals(formulacion.getEstado())) {
                        DialogStyler.mostrarError(dialog, 
                            "❌ Esta formulación NO está disponible para la venta.<br><br>" +
                            "Estado actual: <b>" + formulacion.getEstado() + "</b><br>" +
                            "Producto: <b>" + formulacion.getNombre() + "</b><br><br>" +
                            "Solo las formulaciones con estado <b>'Disponible'</b> pueden ser vendidas.");
                        return;
                    }

                    // NOTA: NO se valida stock de materia prima aquí.
                    // La materia prima ya fue descontada al CREAR la formulación
                    // (ver FormulacionDAO.guardar). Vender un saco ya fabricado es
                    // un producto terminado: no consume materia prima de nuevo, y
                    // por lo tanto no debe volver a chequear inventario de insumos.

                    String precioStr = txtPrecioVenta.getText().trim().replace(",", ".");
                    if (precioStr.isEmpty()) {
                        DialogStyler.mostrarError(dialog, "Ingrese el precio de venta.");
                        return;
                    }
                    double precioVenta = Double.parseDouble(precioStr);
                    if (precioVenta <= 0) {
                        DialogStyler.mostrarError(dialog, "El precio de venta debe ser mayor a 0.");
                        return;
                    }

                    boolean esConsumidorFinal = rbConsumidorFinal.isSelected();
                    Cliente cliente = null;
                    if (!esConsumidorFinal) {
                        cliente = (Cliente) cmbCliente.getSelectedItem();
                        if (cliente == null) {
                            DialogStyler.mostrarError(dialog, "Seleccione un cliente válido.");
                            return;
                        }
                    }

                    boolean esCredito = rbCredito.isSelected();
                    double abonoInicial = 0;
                    LocalDate fechaLimite = null;
                    
                    if (esCredito) {
                        if (esConsumidorFinal) {
                            DialogStyler.mostrarError(dialog, "No se puede vender a crédito a un consumidor final.<br>Seleccione 'Cliente con Datos'.");
                            return;
                        }
                        
                        String abonoStr = txtAbonoInicial.getText().trim().replace(",", ".");
                        if (abonoStr.isEmpty()) {
                            abonoStr = "0";
                        }
                        abonoInicial = Double.parseDouble(abonoStr);
                        if (abonoInicial < 0) {
                            DialogStyler.mostrarError(dialog, "El abono no puede ser negativo.");
                            return;
                        }
                        if (abonoInicial > precioVenta) {
                            DialogStyler.mostrarError(dialog, "El abono no puede ser mayor al precio de venta.");
                            return;
                        }
                        
                        // ===== VALIDAR FECHA LÍMITE =====
                        String fechaLimiteStr = txtFechaLimite.getText().trim();
                        if (fechaLimiteStr.isEmpty()) {
                            DialogStyler.mostrarError(dialog, "Ingrese la fecha límite de pago.");
                            return;
                        }
                        try {
                            fechaLimite = LocalDate.parse(fechaLimiteStr);
                            if (fechaLimite.isBefore(LocalDate.now())) {
                                DialogStyler.mostrarError(dialog, "La fecha límite no puede ser anterior a hoy.");
                                return;
                            }
                            // Verificar que no sea demasiado lejana (máximo 1 año)
                            if (fechaLimite.isAfter(LocalDate.now().plusYears(1))) {
                                DialogStyler.mostrarAdvertencia(dialog, 
                                    "La fecha límite es muy lejana (más de 1 año).<br>" +
                                    "¿Desea continuar?", "Aviso");
                            }
                        } catch (Exception ex) {
                            DialogStyler.mostrarError(dialog, "Formato de fecha inválido. Use YYYY-MM-DD");
                            return;
                        }
                    }

                    double ganancia = MathUtils.redondear(precioVenta - formulacion.getCostoTotal());

                    if (ganancia < 0) {
                        String confirmMsg = "⚠️ Esta venta generará una PÉRDIDA de $" + String.format("%.2f", Math.abs(ganancia)) + ".<br><br>¿Desea continuar?";
                        if (!DialogStyler.confirmar(dialog, confirmMsg, "Confirmar pérdida")) {
                            return;
                        }
                    }

                    // Registrar Venta
                    Venta venta = new Venta();
                    venta.setFormulacionId(formulacion.getId());
                    venta.setNombreProducto(formulacion.getNombre());
                    venta.setLibrasVendidas(formulacion.getTotalLibras());
                    venta.setCostoInversion(formulacion.getCostoTotal());
                    venta.setPrecioVenta(precioVenta);
                    venta.setGanancia(ganancia);
                    venta.setFormaPago(rbTransferencia.isSelected() ? "Transferencia" : "Efectivo");
                    // El cliente se guarda siempre que se haya seleccionado uno,
                    // sea venta al contado o a crédito (antes solo se guardaba
                    // para crédito, y en contado se perdía por completo).
                    venta.setClienteId(cliente != null ? cliente.getId() : 0);
                    ventaDAO.guardar(venta);

                    formulacionDAO.marcarComoVendida(formulacion.getId());

                    // Registrar Crédito si corresponde
                    if (esCredito) {
                        VentaCredito vc = new VentaCredito();
                        vc.setVentaId(venta.getId());
                        vc.setClienteId(cliente.getId());
                        vc.setFormulacionId(formulacion.getId());
                        vc.setNombreProducto(formulacion.getNombre());
                        vc.setTotalLibras(formulacion.getTotalLibras());
                        vc.setPrecioTotal(precioVenta);
                        vc.setAbonado(abonoInicial);
                        vc.setSaldoPendiente(MathUtils.redondear(precioVenta - abonoInicial));
                        vc.setEstado(vc.getSaldoPendiente() <= 0 ? "CANCELADO" : "PENDIENTE");
                        vc.setGananciaDiferida(ganancia);
                        vc.setGananciaRealizada(0);
                        vc.setFechaLimitePago(fechaLimite != null ? fechaLimite.toString() : null);
                        vc.setNotas(null);
                        ventaCreditoDAO.guardar(vc);

                        if (abonoInicial > 0) {
                            try {
                                ventaCreditoDAO.registrarAbonoInicial(vc.getId(), cliente.getId(), abonoInicial);
                            } catch (Exception ex) {
                                System.err.println("Error al registrar abono inicial: " + ex.getMessage());
                                LoggerUtil.logError("Error registrando abono inicial", ex);
                            }
                        }
                    }

                    dialog.dispose();
                    cargarDatos();

                    Window window = SwingUtilities.getWindowAncestor(VentasPanel.this);
                    if (window instanceof MainFrame) {
                        ((MainFrame) window).refrescarDashboard();
                        ((MainFrame) window).refrescarFormulaciones();
                        ((MainFrame) window).refrescarClientes();
                    }

                    String tipoVenta = esCredito ? "a CRÉDITO" : "al CONTADO";
                    String mensajeExito = "✅ Venta registrada correctamente.<br><br>" +
                            "Producto: <b>" + formulacion.getNombre() + "</b><br>" +
                            "Precio: <b>$" + String.format("%.2f", precioVenta) + "</b><br>" +
                            "Ganancia: <b>$" + String.format("%.2f", ganancia) + "</b><br>" +
                            "Tipo: <b>" + tipoVenta + "</b><br>" +
                            "Cliente: <b>" + (cliente != null ? cliente.getNombre() : "Consumidor Final") + "</b>";
                    if (esCredito) {
                        mensajeExito += "<br>Abono inicial: <b>$" + String.format("%.2f", abonoInicial) + "</b>";
                        mensajeExito += "<br>Saldo pendiente: <b>$" + String.format("%.2f", precioVenta - abonoInicial) + "</b>";
                        mensajeExito += "<br>Ganancia diferida: <b>$" + String.format("%.2f", ganancia) + "</b>";
                        mensajeExito += "<br>Fecha límite: <b>" + (fechaLimite != null ? fechaLimite.toString() : "No definida") + "</b>";
                    }
                    DialogStyler.mostrarExito(VentasPanel.this, mensajeExito, "Venta registrada");

                } catch (NumberFormatException ex) {
                    DialogStyler.mostrarError(dialog, "Ingrese valores numéricos válidos.<br>Use punto (.) como separador decimal.");
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error al registrar venta: " + ex.getMessage());
                    LoggerUtil.logError("Error registrando venta", ex);
                    ex.printStackTrace();
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            cargarClientes(cmbCliente);
            
            rbConsumidorFinal.setSelected(true);
            rbContado.setSelected(true);
            rbCredito.setEnabled(false);
            txtAbonoInicial.setEnabled(false);
            txtAbonoInicial.setBackground(new Color(240, 240, 240));
            txtFechaLimite.setEnabled(false);
            txtFechaLimite.setBackground(new Color(240, 240, 240));
            btnCalendario.setEnabled(false);
            panelDatosCliente.setVisible(false);
            panelDatosCliente.setEnabled(false);

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);
            scroll.setPreferredSize(new Dimension(580, 480));

            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(true);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            LoggerUtil.logError("Error en mostrarDialogoVenta", ex);
            ex.printStackTrace();
        }
    }

    // ============================================================
    //  MÉTODOS AUXILIARES
    // ============================================================

    private void cargarClientes(JComboBox<Cliente> cmbCliente) {
        try {
            cmbCliente.removeAllItems();
            List<Cliente> clientes = clienteDAO.listarTodos();
            for (Cliente c : clientes) {
                cmbCliente.addItem(c);
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error cargando clientes", e);
            e.printStackTrace();
        }
    }

    private void filtrarClientes(JTextField txtBuscar, JComboBox<Cliente> cmbCliente) {
        try {
            String filtro = txtBuscar.getText().trim().toUpperCase();
            cmbCliente.removeAllItems();
            List<Cliente> clientes = clienteDAO.listarTodos();
            
            if (filtro.isEmpty()) {
                for (Cliente c : clientes) {
                    cmbCliente.addItem(c);
                }
            } else {
                for (Cliente c : clientes) {
                    if (c.getNombre().toUpperCase().contains(filtro) || 
                        (c.getCodigo() != null && c.getCodigo().toUpperCase().contains(filtro))) {
                        cmbCliente.addItem(c);
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.logError("Error filtrando clientes", e);
            e.printStackTrace();
        }
    }

    private Cliente mostrarDialogoCrearClienteDesdeVenta() {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Nuevo Cliente",
            Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(500, 520);
        dialog.setLocationRelativeTo(this);

        DialogStyler.aplicar(dialog, "Nuevo Cliente", "👤");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

        JTextField txtNombre = DialogStyler.crearCampo(20);
        JTextField txtTelefono = DialogStyler.crearCampoNumerico(20);
        JTextField txtEmail = DialogStyler.crearCampo(20);
        JTextField txtDireccion = DialogStyler.crearCampo(20);
        JTextField txtRuc = DialogStyler.crearCampo(20);

        int fila = 0;

        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Teléfono:"), gbc);
        gbc.gridx = 1;
        form.add(txtTelefono, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Email:"), gbc);
        gbc.gridx = 1;
        form.add(txtEmail, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        form.add(txtDireccion, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("RUC/Cédula:"), gbc);
        gbc.gridx = 1;
        form.add(txtRuc, gbc);

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        final Cliente[] clienteCreado = {null};

        btnGuardar.addActionListener(e -> {
            try {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "Ingrese el nombre del cliente.");
                    return;
                }

                Cliente cliente = new Cliente();
                cliente.setNombre(nombre);
                cliente.setTelefono(txtTelefono.getText().trim());
                cliente.setEmail(txtEmail.getText().trim());
                cliente.setDireccion(txtDireccion.getText().trim());
                cliente.setRucCedula(txtRuc.getText().trim());
                cliente.setEstado("ACTIVO");

                clienteDAO.guardar(cliente);
                clienteCreado[0] = cliente;

                dialog.dispose();
                
                DialogStyler.mostrarExito(this, "Cliente <b>" + nombre + "</b> guardado correctamente.<br>Código: <b>" + cliente.getCodigo() + "</b>", "Éxito");

            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                LoggerUtil.logError("Error creando cliente desde venta", ex);
                ex.printStackTrace();
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
        dialog.setVisible(true);

        return clienteCreado[0];
    }

    // ============================================================
    //  ELIMINAR VENTA
    // ============================================================
    private void eliminarVenta() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione una venta para eliminar.", "Aviso");
            return;
        }

        try {
            int modelRow = tabla.convertRowIndexToModel(row);
            int ventaId = (int) tableModel.getValueAt(modelRow, 0);
            String producto = (String) tableModel.getValueAt(modelRow, 1);
            double precioVenta = Double.parseDouble(((String) tableModel.getValueAt(modelRow, 4)).replace("$", "").replace(",", "."));

            List<Venta> todas = ventaDAO.listarTodas();
            Venta ventaSeleccionada = null;
            for (Venta v : todas) {
                if (v.getId() == ventaId) {
                    ventaSeleccionada = v;
                    break;
                }
            }

            if (ventaSeleccionada == null) {
                DialogStyler.mostrarError(this, "No se encontró la venta.");
                return;
            }

            VentaCredito vc = ventaCreditoDAO.buscarPorVentaId(ventaId);
            if (vc != null && !"CANCELADO".equals(vc.getEstado()) && vc.getSaldoPendiente() > 0) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR<br><br>" +
                    "Esta venta tiene un crédito pendiente.<br>" +
                    "Primero debe cancelar la deuda en la pestaña 'Clientes'.<br>" +
                    "Saldo pendiente: <b>$" + String.format("%.2f", vc.getSaldoPendiente()) + "</b>"
                );
                return;
            }

            // NOTA: NO se devuelve materia prima al inventario aquí.
            // La materia prima ya se descontó UNA sola vez al crear la formulación
            // (el saco/producto terminado). Vender o des-vender un saco ya fabricado
            // no consume ni libera materia prima; el saco sigue existiendo físicamente.
            // La materia prima solo debe devolverse si se elimina la formulación en sí
            // (ver FormulacionDAO.eliminar), no al eliminar la venta.
            String mensaje = "⚠️ ¿Eliminar la venta de <b>" + producto + "</b>?<br><br>" +
                "Precio de venta: <b>$" + String.format("%.2f", precioVenta) + "</b><br><br>" +
                "Esta acción:<br>" +
                "1. Eliminará el registro de la venta<br>" +
                "2. La formulación volverá a estar DISPONIBLE para venderse de nuevo<br>" +
                "<br>ℹ️ El inventario de materias primas NO se modifica: ya fue descontado " +
                "cuando se creó esta formulación.<br><br>" +
                "¿Continuar?";

            if (!DialogStyler.confirmar(this, mensaje, "Eliminar venta")) return;

            if (vc != null) {
                ventaCreditoDAO.eliminar(vc.getId());
            }

            formulacionDAO.marcarComoDisponible(ventaSeleccionada.getFormulacionId());
            
            ventaDAO.eliminar(ventaId);

            cargarDatos();

            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
                ((MainFrame) window).refrescarFormulaciones();
                ((MainFrame) window).refrescarClientes();
                ((MainFrame) window).refrescarInventario();
            }

            DialogStyler.mostrarExito(this,
                "✅ Venta eliminada correctamente.<br><br>" +
                "La formulación <b>" + producto + "</b> ahora está DISPONIBLE nuevamente.<br>" +
                "El inventario de materias primas no fue modificado.",
                "Venta eliminada"
            );

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar venta: " + ex.getMessage());
            LoggerUtil.logError("Error eliminando venta", ex);
            ex.printStackTrace();
        }
    }

    // ============================================================
    //  EDITAR VENTA
    // ============================================================
    private void editarVenta() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione una venta para editar.", "Aviso");
            return;
        }

        try {
            int modelRow = tabla.convertRowIndexToModel(row);
            int ventaId = (int) tableModel.getValueAt(modelRow, 0);
            String producto = (String) tableModel.getValueAt(modelRow, 1);
            double precioActual = Double.parseDouble(((String) tableModel.getValueAt(modelRow, 4)).replace("$", "").replace(",", "."));

            VentaCredito vc = ventaCreditoDAO.buscarPorVentaId(ventaId);
            if (vc != null && !"CANCELADO".equals(vc.getEstado())) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE EDITAR<br><br>" +
                    "Esta venta tiene un crédito pendiente.<br>" +
                    "Solo se puede editar el precio de ventas al contado o créditos cancelados."
                );
                return;
            }

            List<Venta> todas = ventaDAO.listarTodas();
            Venta ventaSeleccionada = null;
            for (Venta v : todas) {
                if (v.getId() == ventaId) {
                    ventaSeleccionada = v;
                    break;
                }
            }

            if (ventaSeleccionada == null) {
                DialogStyler.mostrarError(this, "No se encontró la venta.");
                return;
            }

            final Venta ventaFinal = ventaSeleccionada;
            final double costoInversion = ventaSeleccionada.getCostoInversion();

            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Editar Venta",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(450, 350);
            dialog.setLocationRelativeTo(this);

            DialogStyler.aplicar(dialog, "Editar Venta", "✏️");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            JLabel lblInfo = new JLabel("Producto: " + producto);
            lblInfo.setFont(DialogStyler.FUENTE_LABEL);
            
            JLabel lblInversion = new JLabel("Inversión: $" + String.format("%.2f", costoInversion));
            lblInversion.setFont(DialogStyler.FUENTE_LABEL);

            JLabel lblPrecioActual = new JLabel("$" + String.format("%.2f", precioActual));
            lblPrecioActual.setFont(DialogStyler.FUENTE_BOTON);
            lblPrecioActual.setForeground(new Color(230, 126, 34));

            JTextField txtNuevoPrecio = DialogStyler.crearCampoNumerico(12);
            txtNuevoPrecio.setText(String.format("%.2f", precioActual));

            JLabel lblPreview = new JLabel("");
            lblPreview.setFont(DialogStyler.FUENTE_BOTON);

            txtNuevoPrecio.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview(); }
                private void actualizarPreview() {
                    try {
                        String texto = txtNuevoPrecio.getText().trim().replace(",", ".");
                        if (!texto.isEmpty()) {
                            double nuevo = Double.parseDouble(texto);
                            double ganancia = MathUtils.redondear(nuevo - costoInversion);
                            String estado = ganancia >= 0 ? "🟢 GANANCIA" : "🔴 PÉRDIDA";
                            lblPreview.setText(estado + ": $" + String.format("%.2f", ganancia));
                            lblPreview.setForeground(ganancia >= 0 ? DialogStyler.ACENTO : DialogStyler.PELIGRO);
                        } else {
                            lblPreview.setText("");
                        }
                    } catch (NumberFormatException ex) {
                        lblPreview.setText("❌ Precio inválido");
                        lblPreview.setForeground(DialogStyler.PELIGRO);
                    }
                }
            });

            int fila = 0;

            gbc.gridx = 0; gbc.gridy = fila;
            form.add(lblInfo, gbc);
            gbc.gridx = 1;
            form.add(lblInversion, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Precio de venta actual:"), gbc);
            gbc.gridx = 1;
            form.add(lblPrecioActual, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Nuevo precio ($):"), gbc);
            gbc.gridx = 1;
            form.add(txtNuevoPrecio, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(lblPreview, gbc);
            gbc.gridwidth = 1;

            JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Cambios");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(evt -> {
                try {
                    String texto = txtNuevoPrecio.getText().trim().replace(",", ".");
                    if (texto.isEmpty()) {
                        DialogStyler.mostrarError(dialog, "Ingrese un precio válido.");
                        return;
                    }
                    double nuevoPrecio = Double.parseDouble(texto);
                    if (nuevoPrecio <= 0) {
                        DialogStyler.mostrarError(dialog, "El precio debe ser mayor a 0.");
                        return;
                    }

                    double nuevaGanancia = MathUtils.redondear(nuevoPrecio - costoInversion);
                    ventaFinal.setPrecioVenta(nuevoPrecio);
                    ventaFinal.setGanancia(nuevaGanancia);
                    ventaDAO.actualizar(ventaFinal);

                    VentaCredito vcExistente = ventaCreditoDAO.buscarPorVentaId(ventaId);
                    if (vcExistente != null && "CANCELADO".equals(vcExistente.getEstado())) {
                        vcExistente.setPrecioTotal(nuevoPrecio);
                        vcExistente.setSaldoPendiente(0);
                        vcExistente.setGananciaDiferida(nuevaGanancia);
                        ventaCreditoDAO.actualizar(vcExistente);
                    }

                    dialog.dispose();
                    cargarDatos();

                    Window window = SwingUtilities.getWindowAncestor(VentasPanel.this);
                    if (window instanceof MainFrame) {
                        ((MainFrame) window).refrescarDashboard();
                        ((MainFrame) window).refrescarClientes();
                    }

                    DialogStyler.mostrarExito(this,
                        "Venta actualizada correctamente.<br><br>" +
                        "Producto: <b>" + producto + "</b><br>" +
                        "Precio anterior: $" + String.format("%.2f", precioActual) + "<br>" +
                        "Nuevo precio: <b>$" + String.format("%.2f", nuevoPrecio) + "</b><br>" +
                        "Nueva ganancia: <b>$" + String.format("%.2f", ventaFinal.getGanancia()) + "</b>",
                        "Venta actualizada"
                    );

                } catch (NumberFormatException ex) {
                    DialogStyler.mostrarError(dialog, "Ingrese un número válido.<br>Use punto (.) como separador decimal.");
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                    LoggerUtil.logError("Error editando venta", ex);
                    ex.printStackTrace();
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
            dialog.setVisible(true);

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al editar venta: " + ex.getMessage());
            LoggerUtil.logError("Error editando venta", ex);
            ex.printStackTrace();
        }
    }

    // ============================================================
    //  EXPORTAR PDF
    // ============================================================
    private void exportarPDF() {
        try {
            List<Venta> ventas = ventaDAO.listarTodas();
            if (ventas.isEmpty()) { 
                DialogStyler.mostrarAdvertencia(this, "No hay ventas para exportar.", "Aviso");
                return; 
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Reporte_Ventas.pdf"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if (!ruta.endsWith(".pdf")) ruta += ".pdf";
                ReporteGenerator.generarReporteVentas(ventas, ruta);
                DialogStyler.mostrarExito(this, "PDF generado:<br><b>" + ruta + "</b>", "Éxito");
            }
        } catch (Exception ex) { 
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            LoggerUtil.logError("Error exportando PDF de ventas", ex);
        }
    }

    // ============================================================
    //  CARGAR DATOS
    // ============================================================
    public void cargarDatos() {
        try {
            tableModel.setRowCount(0);
            List<Venta> ventas = ventaDAO.listarTodas();
            for (Venta v : ventas) {
                String tipo = "CONTADO";
                String nombreCliente = "";
                String rucCliente = "";

                // El cliente ahora se guarda directamente en la venta (tanto al
                // contado como a crédito). Se intenta primero desde ahí.
                try {
                    if (v.getClienteId() > 0) {
                        Cliente cliente = clienteDAO.buscarPorId(v.getClienteId());
                        if (cliente != null) {
                            nombreCliente = cliente.getNombre();
                            rucCliente = cliente.getRucCedula() != null ? cliente.getRucCedula() : "";
                        }
                    }
                } catch (Exception e) {
                    // Cliente no encontrado (pudo haber sido eliminado)
                }

                try {
                    VentaCredito vc = ventaCreditoDAO.buscarPorVentaId(v.getId());
                    if (vc != null) {
                        // Respaldo: ventas antiguas (antes de que "ventas" tuviera
                        // cliente_id) solo tenían el cliente guardado aquí.
                        if (nombreCliente.isEmpty()) {
                            Cliente cliente = clienteDAO.buscarPorId(vc.getClienteId());
                            if (cliente != null) {
                                nombreCliente = cliente.getNombre();
                                rucCliente = cliente.getRucCedula() != null ? cliente.getRucCedula() : "";
                            }
                        }
                        
                        if ("CANCELADO".equals(vc.getEstado())) {
                            tipo = "CONTADO";
                        } else {
                            tipo = "CRÉDITO";
                        }
                    }
                } catch (Exception e) {
                    // Si no tiene crédito, es venta al contado
                }
                
                tableModel.addRow(new Object[]{
                    v.getId(), 
                    v.getNombreProducto(),
                    String.format("%.2f lb", v.getLibrasVendidas()),
                    String.format("$%.2f", v.getCostoInversion()),
                    String.format("$%.2f", v.getPrecioVenta()),
                    String.format("$%.2f", v.getGanancia()),
                    v.getFecha(), 
                    v.getHora(),
                    nombreCliente.isEmpty() ? "Consumidor Final" : nombreCliente + (rucCliente.isEmpty() ? "" : " (" + rucCliente + ")"),
                    tipo,
                    v.getFormaPago() != null ? v.getFormaPago() : "Efectivo"
                });
            }
            actualizarResumen();
            
            if (txtBuscarVentas != null) {
                txtBuscarVentas.setText("");
            }
            if (sorterVentas != null) {
                sorterVentas.setRowFilter(null);
            }
        } catch (Exception ex) { 
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            LoggerUtil.logError("Error cargando datos de ventas", ex);
            ex.printStackTrace();
        }
    }
}