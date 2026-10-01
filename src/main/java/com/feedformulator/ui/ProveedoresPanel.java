package com.feedformulator.ui;

import com.feedformulator.dao.ProveedorDAO;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.dao.PedidoDAO;
import com.feedformulator.dao.DatabaseManager;
import com.feedformulator.model.Proveedor;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.Pedido;
import com.feedformulator.model.DetallePedido;
import com.feedformulator.model.PagoPedido;
import com.feedformulator.util.LoggerUtil;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProveedoresPanel extends JPanel {
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    
    private DefaultTableModel tableModelProveedores;
    private DefaultTableModel tableModelPedidos;
    private DefaultTableModel tableModelPagos;
    private JTable tablaProveedores;
    private JTable tablaPedidos;
    private JTable tablaPagos;
    private JLabel lblTotalProveedores;
    private JLabel lblTotalPendiente;
    private JLabel lblTotalPagado;
    private JTabbedPane tabbedPane;
    private JTextField txtBuscarProveedor;
    private TableRowSorter<DefaultTableModel> sorterProveedores;
    private String codigoProveedorSeleccionado = null;
    
    // Paleta de Colores
    private static final Color COLOR_PRIMARIO = new Color(5, 150, 105);     // Esmeralda #059669
    private static final Color COLOR_VERDE = new Color(5, 150, 105);        // #059669
    private static final Color COLOR_VERDE_OSCURO = new Color(4, 120, 87);  // #047857
    private static final Color COLOR_VERDE_CLARO = new Color(209, 250, 229);
    private static final Color COLOR_ROJO = new Color(220, 38, 38);         // Carmesí #DC2626
    private static final Color COLOR_NARANJA = new Color(217, 119, 6);      // Ámbar #D97706
    private static final Color COLOR_RECIBIR = new Color(0, 150, 200);
    private static final Color FONDO = new Color(248, 250, 252);            // #F8FAFC
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);        // #0F172A
    private static final Color BORDE_SUAVE = new Color(226, 232, 240);
    private static final Color TEAL_ENCABEZADO = new Color(15, 118, 110);  // #0F766E

    public ProveedoresPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        add(crearTitulo(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelResumen(), BorderLayout.SOUTH);
        
        cargarDatos();
    }
    
    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("🏢", "Gestión de Proveedores y Pedidos", "Proveedores registrados y pedidos de compra");
    }
    
    private JSplitPane crearPanelCentral() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.35);
        split.setBackground(FONDO);
        split.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // Panel superior: Lista de proveedores
        JPanel panelProveedores = new JPanel(new BorderLayout(5, 5));
        panelProveedores.setBackground(FONDO);
        
        JPanel headerProveedores = new JPanel(new BorderLayout());
        headerProveedores.setBackground(FONDO);
        
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tituloPanel.setBackground(FONDO);
        
        JLabel lblProveedores = new JLabel(" Proveedores Registrados");
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/clipboard-icon.png"));
            Image img = icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            lblProveedores.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            lblProveedores.setText("📋  Proveedores Registrados");
        }
        lblProveedores.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tituloPanel.add(lblProveedores);
        headerProveedores.add(tituloPanel, BorderLayout.WEST);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPanel.setBackground(FONDO);
        
        // Buscador
        try {
            ImageIcon iconSearch = new ImageIcon(getClass().getResource("/icons/magnifying-glass.png"));
            Image imgSearch = iconSearch.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JLabel lblBuscar = new JLabel(new ImageIcon(imgSearch));
            lblBuscar.setToolTipText("Buscar");
            btnPanel.add(lblBuscar);
        } catch (Exception exSearch) {
            JLabel lblBuscar = new JLabel("🔍");
            lblBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnPanel.add(lblBuscar);
        }
        
        txtBuscarProveedor = new JTextField(20);
        txtBuscarProveedor.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarProveedor.setToolTipText("Buscar por nombre, código, RUC o contacto");
        txtBuscarProveedor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarProveedores();
            }
        });
        btnPanel.add(txtBuscarProveedor);
        
        // Botones
        JButton btnNuevo = crearBotonAccion("Nuevo", "/icons/add.png", "➕", COLOR_PRIMARIO);
        JButton btnEditar = crearBotonAccion("Editar", "/icons/edit.png", "✏️", COLOR_NARANJA);
        JButton btnEliminar = crearBotonAccion("Eliminar", "/icons/delete.png", "🗑", COLOR_ROJO);
        JButton btnRefrescar = crearBotonAccion("Refrescar", "/icons/refresh.png", "🔄", new Color(100, 116, 139));
        
        btnNuevo.addActionListener(e -> mostrarDialogoNuevoProveedor());
        btnEditar.addActionListener(e -> editarProveedor());
        btnEliminar.addActionListener(e -> eliminarProveedor());
        btnRefrescar.addActionListener(e -> cargarDatos());
        
        btnPanel.add(btnNuevo);
        btnPanel.add(btnEditar);
        btnPanel.add(btnEliminar);
        btnPanel.add(btnRefrescar);
        
        headerProveedores.add(btnPanel, BorderLayout.EAST);
        panelProveedores.add(headerProveedores, BorderLayout.NORTH);
        
        String[] colsProveedores = {"Código", "Nombre", "Teléfono", "Contacto", "RUC", "Tipo", "Estado"};
        tableModelProveedores = new DefaultTableModel(colsProveedores, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaProveedores = new JTable(tableModelProveedores);
        sorterProveedores = new TableRowSorter<>(tableModelProveedores);
        tablaProveedores.setRowSorter(sorterProveedores);
        estilizarTabla(tablaProveedores, SLATE_OSCURO);
        panelProveedores.add(new JScrollPane(tablaProveedores), BorderLayout.CENTER);
        
        // Panel inferior: Pedidos del proveedor
        JPanel panelPedidos = new JPanel(new BorderLayout(5, 5));
        panelPedidos.setBackground(FONDO);
        
        JPanel headerPedidos = new JPanel(new BorderLayout());
        headerPedidos.setBackground(FONDO);
        
        JPanel tituloPedidosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tituloPedidosPanel.setBackground(FONDO);
        JLabel lblPedidos = new JLabel(" Pedidos del Proveedor");
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/package-icon.png"));
            Image img = icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            lblPedidos.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            lblPedidos.setText("📦  Pedidos del Proveedor");
        }
        lblPedidos.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tituloPedidosPanel.add(lblPedidos);
        headerPedidos.add(tituloPedidosPanel, BorderLayout.WEST);
        
        JPanel btnPedidoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPedidoPanel.setBackground(FONDO);
        
        JButton btnNuevoPedido = crearBotonAccion("Nuevo Pedido", "/icons/add.png", "📦", COLOR_VERDE);
        JButton btnPagar = crearBotonAccion("Pagar Pedido", "/icons/pay-supplier.png", "💰", COLOR_VERDE);
        JButton btnEliminarPago = crearBotonAccion("Eliminar Pago", "/icons/delete.png", "🗑", COLOR_ROJO);
        JButton btnEliminarPedido = crearBotonAccion("Eliminar Pedido", "/icons/delete.png", "🗑", COLOR_ROJO);
        
        btnNuevoPedido.addActionListener(e -> mostrarDialogoNuevoPedido());
        btnPagar.addActionListener(e -> pagarPedidoSeleccionado());
        btnEliminarPago.addActionListener(e -> eliminarPagoPedido());
        btnEliminarPedido.addActionListener(e -> eliminarPedidoSeleccionado());
        
        btnPedidoPanel.add(btnNuevoPedido);
        btnPedidoPanel.add(btnPagar);
        btnPedidoPanel.add(btnEliminarPago);
        btnPedidoPanel.add(btnEliminarPedido);
        
        headerPedidos.add(btnPedidoPanel, BorderLayout.EAST);
        panelPedidos.add(headerPedidos, BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Tabla de Pedidos
        String[] colsPedidos = {"ID", "Pedido", "Productos", "Total", "Abonado", "Pendiente", "Estado", "Recepción", "Fecha"};
        tableModelPedidos = new DefaultTableModel(colsPedidos, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPedidos = new JTable(tableModelPedidos);
        estilizarTabla(tablaPedidos, SLATE_OSCURO);
        
        // Doble clic para ver detalles
        tablaPedidos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tablaPedidos.getSelectedRow();
                    if (row >= 0) {
                        mostrarDetallesPedido(row);
                    }
                }
            }
        });
        
        JScrollPane scrollPedidos = new JScrollPane(tablaPedidos);
        tabbedPane.addTab("Pedidos", scrollPedidos);
        
        // Tabla de Pagos
        String[] colsPagos = {"ID", "Monto", "Forma Pago", "Fecha", "Hora"};
        tableModelPagos = new DefaultTableModel(colsPagos, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPagos = new JTable(tableModelPagos);
        estilizarTabla(tablaPagos, SLATE_OSCURO);
        JScrollPane scrollPagos = new JScrollPane(tablaPagos);
        tabbedPane.addTab("Pagos", scrollPagos);
        
        panelPedidos.add(tabbedPane, BorderLayout.CENTER);
        
        // Eventos de selección
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarPedidosProveedor();
                tableModelPagos.setRowCount(0);
            }
        });
        
        tablaPedidos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarPagosPedido();
            }
        });
        
        split.setTopComponent(panelProveedores);
        split.setBottomComponent(panelPedidos);
        
        return split;
    }

    private JButton crearBotonAccion(String texto, String iconoPath, String emojiFallback, Color colorFondo) {
        JButton btn = new JButton(texto);
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(iconoPath));
            Image img = icon.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            btn.setText(emojiFallback + " " + texto);
        }
        btn.setBackground(colorFondo);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return btn;
    }
    
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        lblTotalProveedores = new JLabel("0");
        lblTotalPendiente = new JLabel("$0.00");
        lblTotalPagado = new JLabel("$0.00");

        panel.add(crearChipResumenProv("🏢", "PROVEEDORES", lblTotalProveedores, Color.WHITE, new Color(15, 23, 42), BORDE_SUAVE));
        panel.add(crearChipResumenProv("⚠️", "TOTAL PENDIENTE", lblTotalPendiente, new Color(255, 228, 230), new Color(190, 18, 60), new Color(253, 200, 207)));
        panel.add(crearChipResumenProv("💳", "TOTAL PAGADO", lblTotalPagado, new Color(209, 250, 229), new Color(6, 95, 70), new Color(167, 243, 208)));

        return panel;
    }

    private JPanel crearChipResumenProv(String icono, String titulo, JLabel valor, Color fondo, Color colorTexto, Color colorBorde) {
        JPanel chip = new JPanel(new BorderLayout(10, 0));
        chip.setBackground(fondo);
        chip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorBorde, 1),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        chip.add(lblIcono, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitulo.setForeground(colorTexto.equals(new Color(15, 23, 42)) ? new Color(100, 116, 139) : colorTexto);
        textos.add(lblTitulo);

        valor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valor.setForeground(colorTexto);
        textos.add(valor);

        chip.add(textos, BorderLayout.CENTER);
        return chip;
    }
    
    private void estilizarTabla(JTable t, Color headerColor) {
        t.setRowHeight(34);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.getTableHeader().setBackground(headerColor);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.setSelectionBackground(new Color(209, 250, 229));
        t.setSelectionForeground(new Color(6, 95, 70));
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDE_SUAVE);
        t.setIntercellSpacing(new Dimension(0, 1));

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                String nombreCol = table.getColumnName(column);
                boolean esNumerica = nombreCol.equals("Total") || nombreCol.equals("Abonado")
                    || nombreCol.equals("Pendiente") || nombreCol.equals("Monto") || nombreCol.equals("ID");
                boolean esBadge = nombreCol.equals("Estado") || nombreCol.equals("Tipo") || nombreCol.equals("Recepción");

                if (esBadge) {
                    JLabel pastilla = new JLabel(value != null ? value.toString() : "");
                    pastilla.setHorizontalAlignment(SwingConstants.CENTER);
                    pastilla.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    pastilla.setOpaque(true);
                    pastilla.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
                    String txt = value != null ? value.toString().toUpperCase() : "";

                    Color fondoBadge, colorTexto;
                    if (txt.contains("MATERIA PRIMA")) {
                        fondoBadge = new Color(254, 243, 199); colorTexto = new Color(146, 64, 14);
                    } else if (txt.contains("SERVICIO")) {
                        fondoBadge = new Color(237, 233, 254); colorTexto = new Color(91, 33, 182);
                    } else if (txt.contains("INSUMO")) {
                        fondoBadge = new Color(209, 250, 229); colorTexto = new Color(6, 95, 70);
                    } else if (txt.contains("EQUIPO")) {
                        fondoBadge = new Color(219, 234, 254); colorTexto = new Color(30, 64, 175);
                    } else if (txt.contains("PAGADO") || txt.contains("ACTIVO") || txt.contains("RECIBID") || txt.contains("CANCELADO")) {
                        fondoBadge = new Color(209, 250, 229); colorTexto = new Color(6, 95, 70);
                    } else if (txt.contains("PARCIAL") || txt.contains("ABONADO")) {
                        fondoBadge = new Color(219, 234, 254); colorTexto = new Color(30, 64, 175);
                    } else if (txt.contains("PENDIENTE")) {
                        fondoBadge = new Color(254, 243, 199); colorTexto = new Color(146, 64, 14);
                    } else {
                        fondoBadge = new Color(241, 245, 249); colorTexto = new Color(51, 65, 85);
                    }
                    pastilla.setBackground(fondoBadge);
                    pastilla.setForeground(colorTexto);

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
                    if ("Pendiente".equals(nombreCol) && value != null) {
                        try {
                            double v = Double.parseDouble(value.toString().replace("$", "").replace(",", "."));
                            if (v > 0) c.setForeground(COLOR_ROJO);
                        } catch (Exception ignored) {}
                    }
                    if ("Abonado".equals(nombreCol)) {
                        c.setForeground(COLOR_VERDE);
                    }
                }
                return c;
            }
        });
    }
    
    private void filtrarProveedores() {
        String texto = txtBuscarProveedor.getText().trim().toLowerCase();
        if (texto.isEmpty()) {
            sorterProveedores.setRowFilter(null);
        } else {
            sorterProveedores.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 1, 0, 3, 4));
        }
    }

    private void restaurarSeleccion(int pestañaActiva) {
        if (codigoProveedorSeleccionado != null) {
            for (int i = 0; i < tablaProveedores.getRowCount(); i++) {
                try {
                    int modelRow = tablaProveedores.convertRowIndexToModel(i);
                    String codigo = (String) tableModelProveedores.getValueAt(modelRow, 0);
                    if (codigo.equals(codigoProveedorSeleccionado)) {
                        tablaProveedores.setRowSelectionInterval(i, i);
                        break;
                    }
                } catch (Exception ex) {}
            }
        }
        
        if (pestañaActiva >= 0 && pestañaActiva < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(pestañaActiva);
        }
    }

    public void cargarDatos() {
        int rowProveedor = tablaProveedores.getSelectedRow();
        if (rowProveedor >= 0) {
            try {
                int modelRow = tablaProveedores.convertRowIndexToModel(rowProveedor);
                codigoProveedorSeleccionado = (String) tableModelProveedores.getValueAt(modelRow, 0);
            } catch (Exception ignored) {}
        }
        
        int pestañaActiva = tabbedPane.getSelectedIndex();
        
        cargarProveedores();
        actualizarResumen();
        txtBuscarProveedor.setText("");
        filtrarProveedores();
        
        restaurarSeleccion(pestañaActiva);
    }
    
    private void cargarProveedores() {
        try {
            tableModelProveedores.setRowCount(0);
            List<Proveedor> proveedores = proveedorDAO.listarTodos();
            
            for (Proveedor p : proveedores) {
                tableModelProveedores.addRow(new Object[]{
                    p.getCodigo(),
                    p.getNombre(),
                    p.getTelefono() != null ? p.getTelefono() : "",
                    p.getContacto() != null ? p.getContacto() : "",
                    p.getRucCedula() != null ? p.getRucCedula() : "",
                    p.getTipoProveedor(),
                    p.getEstado()
                });
            }
            
            lblTotalProveedores.setText(String.valueOf(proveedores.size()));
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar proveedores: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cargarPedidosProveedor() {
        int row = tablaProveedores.getSelectedRow();
        if (row < 0) {
            tableModelPedidos.setRowCount(0);
            return;
        }

        try {
            String codigo = (String) tableModelProveedores.getValueAt(row, 0);
            Proveedor proveedor = proveedorDAO.buscarPorCodigo(codigo);

            if (proveedor == null) {
                tableModelPedidos.setRowCount(0);
                return;
            }

            tableModelPedidos.setRowCount(0);
            List<Pedido> pedidos = pedidoDAO.listarPorProveedor(proveedor.getId());

            for (Pedido p : pedidos) {
                int numProductos = p.getDetalles().size();
                String nombrePedido = p.getNotas() != null ? p.getNotas() : "Pedido #" + p.getId();
                
                tableModelPedidos.addRow(new Object[]{
                    p.getId(),
                    nombrePedido,
                    numProductos + " productos",
                    String.format("$%.2f", p.getTotalPedido()),
                    String.format("$%.2f", p.getAbonado()),
                    String.format("$%.2f", p.getSaldoPendiente()),
                    p.getEstadoPago(),
                    p.getEstadoRecepcion(),
                    p.getFechaPedido()
                });
            }

        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar pedidos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cargarPagosPedido() {
        int rowPedido = tablaPedidos.getSelectedRow();
        if (rowPedido < 0) {
            tableModelPagos.setRowCount(0);
            return;
        }

        try {
            int pedidoId = (int) tableModelPedidos.getValueAt(rowPedido, 0);
            tableModelPagos.setRowCount(0);
            
            Pedido p = pedidoDAO.buscarPorId(pedidoId);
            if (p == null) {
                tableModelPagos.addRow(new Object[]{"-", "Sin pagos", "-", "-", "-"});
                return;
            }
            
            List<PagoPedido> pagos = p.getPagos();

            if (pagos.isEmpty()) {
                tableModelPagos.addRow(new Object[]{"-", "Sin pagos", "-", "-", "-"});
                return;
            }

            for (PagoPedido pago : pagos) {
                tableModelPagos.addRow(new Object[]{
                    pago.getId(),
                    String.format("$%.2f", pago.getMonto()),
                    pago.getFormaPago(),
                    pago.getFechaPago(),
                    pago.getHoraPago()
                });
            }

        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar pagos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void actualizarResumen() {
        try {
            List<Pedido> pedidos = pedidoDAO.listarTodos();
            double totalPendiente = 0;
            double totalPagado = 0;
            
            for (Pedido p : pedidos) {
                if (!"CANCELADO".equals(p.getEstadoPago())) {
                    totalPendiente += p.getSaldoPendiente();
                }
                totalPagado += p.getAbonado();
            }
            
            lblTotalPendiente.setText("$" + String.format("%.2f", totalPendiente));
            lblTotalPagado.setText("$" + String.format("%.2f", totalPagado));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ============================================================
    //  NUEVO PEDIDO A PROVEEDOR (DISEÑO REDISEÑADO ERGONÓMICO)
    // ============================================================
    private void mostrarDialogoNuevoPedido() {
        try {
            List<Proveedor> proveedores = proveedorDAO.listarTodos();
            if (proveedores.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, "No hay proveedores registrados.\nPrimero registre un proveedor.", "Aviso");
                return;
            }
            
            int row = tablaProveedores.getSelectedRow();
            Proveedor proveedorSeleccionado = null;
            if (row >= 0) {
                String codigo = (String) tableModelProveedores.getValueAt(row, 0);
                proveedorSeleccionado = proveedorDAO.buscarPorCodigo(codigo);
            }
            
            List<MateriaPrima> productos = materiaDAO.listarTodos();
            
            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Nuevo Pedido a Proveedor",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(980, 680);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(0, 0));
            dialog.getContentPane().setBackground(FONDO);

            // 1. ENCABEZADO MODERNO CON GRADIENTE SLATE / TEAL
            JPanel header = new JPanel(new BorderLayout(12, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0, 0, TEAL_ENCABEZADO, getWidth(), 0, SLATE_OSCURO);
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

            JPanel textosHeader = new JPanel();
            textosHeader.setLayout(new BoxLayout(textosHeader, BoxLayout.Y_AXIS));
            textosHeader.setOpaque(false);

            JLabel lblTituloH = new JLabel("Nuevo Pedido a Proveedor");
            lblTituloH.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblTituloH.setForeground(Color.WHITE);
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource("/icons/clipboard-icon.png"));
                Image img = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                lblTituloH.setIcon(new ImageIcon(img));
            } catch (Exception ignored) {
                lblTituloH.setText("📋 Nuevo Pedido a Proveedor");
            }

            JLabel lblSubtituloH = new JLabel("Orden de compra de materias primas y recepción de envases");
            lblSubtituloH.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblSubtituloH.setForeground(new Color(204, 251, 241));

            textosHeader.add(lblTituloH);
            textosHeader.add(Box.createVerticalStrut(2));
            textosHeader.add(lblSubtituloH);
            header.add(textosHeader, BorderLayout.WEST);
            dialog.add(header, BorderLayout.NORTH);

            // 2. CUERPO PRINCIPAL
            JPanel panelCentral = new JPanel(new BorderLayout(0, 10));
            panelCentral.setOpaque(false);
            panelCentral.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));

            // FILA SUPERIOR: PROVEEDOR Y NOMBRE DE PEDIDO
            JPanel pnlDatosGenerales = new JPanel(new GridBagLayout());
            pnlDatosGenerales.setBackground(Color.WHITE);
            pnlDatosGenerales.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE_SUAVE, 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
            ));

            GridBagConstraints gbcGen = new GridBagConstraints();
            gbcGen.insets = new Insets(4, 6, 4, 10);
            gbcGen.fill = GridBagConstraints.HORIZONTAL;

            JLabel lblProv = new JLabel("Proveedor:*");
            lblProv.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblProv.setForeground(SLATE_OSCURO);

            JComboBox<Proveedor> cmbProveedorLocal = new JComboBox<>(proveedores.toArray(new Proveedor[0]));
            cmbProveedorLocal.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cmbProveedorLocal.setPreferredSize(new Dimension(320, 32));
            if (proveedorSeleccionado != null) {
                cmbProveedorLocal.setSelectedItem(proveedorSeleccionado);
            }

            JLabel lblCod = new JLabel("Nombre / Código del Pedido:*");
            lblCod.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblCod.setForeground(SLATE_OSCURO);

            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            JTextField txtNombrePedido = new JTextField("PEDIDO-" + fechaActual + "-001");
            txtNombrePedido.setFont(new Font("Segoe UI", Font.BOLD, 12));
            txtNombrePedido.setPreferredSize(new Dimension(240, 32));

            gbcGen.gridx = 0; gbcGen.gridy = 0; gbcGen.weightx = 0; pnlDatosGenerales.add(lblProv, gbcGen);
            gbcGen.gridx = 1; gbcGen.weightx = 0.5; pnlDatosGenerales.add(cmbProveedorLocal, gbcGen);
            gbcGen.gridx = 2; gbcGen.weightx = 0; pnlDatosGenerales.add(lblCod, gbcGen);
            gbcGen.gridx = 3; gbcGen.weightx = 0.5; pnlDatosGenerales.add(txtNombrePedido, gbcGen);

            panelCentral.add(pnlDatosGenerales, BorderLayout.NORTH);

            // CENTRO: TABLA DE PRODUCTOS
            JPanel pnlCentro = new JPanel(new BorderLayout(0, 8));
            pnlCentro.setOpaque(false);

            JLabel lblSeccion = new JLabel("📦 PRODUCTOS DEL PEDIDO");
            lblSeccion.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSeccion.setForeground(TEAL_ENCABEZADO);
            pnlCentro.add(lblSeccion, BorderLayout.NORTH);

            String[] colsProductos = {"Producto", "Unidad", "Cantidad (LB/Lt)", "Precio Unit.", "Subtotal", "Envases/Sacos", "Peso/Envase", "Sueltos"};
            DefaultTableModel modelProductos = new DefaultTableModel(colsProductos, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable tablaProductos = new JTable(modelProductos);
            tablaProductos.setRowHeight(32);
            tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            tablaProductos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
            tablaProductos.getTableHeader().setBackground(SLATE_OSCURO);
            tablaProductos.getTableHeader().setForeground(Color.WHITE);
            tablaProductos.getTableHeader().setPreferredSize(new Dimension(0, 34));
            tablaProductos.setShowGrid(false);
            tablaProductos.setShowHorizontalLines(true);
            tablaProductos.setGridColor(BORDE_SUAVE);

            tablaProductos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    boolean esNumero = column >= 2 && column <= 7;
                    c.setHorizontalAlignment(esNumero ? SwingConstants.RIGHT : (column == 1 ? SwingConstants.CENTER : SwingConstants.LEFT));
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : FONDO);
                        c.setForeground(new Color(30, 41, 59));
                        if (column == 4) { // Subtotal
                            c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            c.setForeground(new Color(4, 120, 87));
                        }
                    }
                    return c;
                }
            });

            JScrollPane scrollProductos = new JScrollPane(tablaProductos);
            scrollProductos.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
            scrollProductos.setPreferredSize(new Dimension(940, 180));
            pnlCentro.add(scrollProductos, BorderLayout.CENTER);

            // ============================================================
            // FORMULARIO DE ENTRADA: 2 FILAS ERGONÓMICAS (NO APLASTADO)
            // ============================================================
            JPanel pnlAgregar = new JPanel(new GridLayout(2, 1, 0, 8));
            pnlAgregar.setBackground(new Color(240, 253, 250)); // Teal-50
            pnlAgregar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(153, 246, 228), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));

            // FILA 1: Producto + Botón Nuevo Producto + Unidad
            JPanel fila1 = new JPanel(new BorderLayout(10, 0));
            fila1.setOpaque(false);

            JPanel pnlProd = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            pnlProd.setOpaque(false);
            JLabel lblProd = new JLabel("Producto:*");
            lblProd.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblProd.setForeground(SLATE_OSCURO);

            JComboBox<Object> cmbProducto = new JComboBox<>();
            cmbProducto.addItem("➕ Agregar nuevo producto...");
            if (!productos.isEmpty()) {
                for (MateriaPrima mp : productos) {
                    cmbProducto.addItem(mp);
                }
            }
            cmbProducto.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cmbProducto.setPreferredSize(new Dimension(280, 30));

            JButton btnNuevoProducto = new JButton("+ Nuevo");
            btnNuevoProducto.setBackground(COLOR_PRIMARIO);
            btnNuevoProducto.setForeground(Color.WHITE);
            btnNuevoProducto.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnNuevoProducto.setFocusPainted(false);
            btnNuevoProducto.setBorderPainted(false);
            btnNuevoProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnNuevoProducto.setPreferredSize(new Dimension(80, 30));

            pnlProd.add(lblProd);
            pnlProd.add(cmbProducto);
            pnlProd.add(btnNuevoProducto);
            fila1.add(pnlProd, BorderLayout.WEST);

            JLabel lblPesoDefinidoPedido = new JLabel(" ");
            lblPesoDefinidoPedido.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblPesoDefinidoPedido.setForeground(new Color(180, 83, 9));
            fila1.add(lblPesoDefinidoPedido, BorderLayout.CENTER);

            pnlAgregar.add(fila1);

            // FILA 2: Cantidades, Pesos, Precios y Acciones
            JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            fila2.setOpaque(false);

            JLabel lblCantidadEnvases = new JLabel("Cant. Sacos:");
            lblCantidadEnvases.setFont(new Font("Segoe UI", Font.BOLD, 11));
            JTextField txtCantidadEnvases = new JTextField("0");
            txtCantidadEnvases.setFont(new Font("Segoe UI", Font.BOLD, 11));
            txtCantidadEnvases.setHorizontalAlignment(SwingConstants.CENTER);
            txtCantidadEnvases.setPreferredSize(new Dimension(65, 28));

            JLabel lblPrecioEnvase = new JLabel("Precio/Saco ($):");
            lblPrecioEnvase.setFont(new Font("Segoe UI", Font.BOLD, 11));
            JTextField txtPrecioEnvase = new JTextField("0");
            txtPrecioEnvase.setFont(new Font("Segoe UI", Font.BOLD, 11));
            txtPrecioEnvase.setHorizontalAlignment(SwingConstants.RIGHT);
            txtPrecioEnvase.setPreferredSize(new Dimension(75, 28));

            JLabel lblPesoEnvase = new JLabel("Libras/Saco:");
            lblPesoEnvase.setFont(new Font("Segoe UI", Font.BOLD, 11));
            JComboBox<String> cmbPesoEnvasePedido = new JComboBox<>(new String[]{"0"});
            cmbPesoEnvasePedido.setFont(new Font("Segoe UI", Font.BOLD, 11));
            cmbPesoEnvasePedido.setPreferredSize(new Dimension(80, 28));

            JLabel lblSueltos = new JLabel("Sueltos:");
            lblSueltos.setFont(new Font("Segoe UI", Font.BOLD, 11));
            JTextField txtSueltos = new JTextField("0");
            txtSueltos.setFont(new Font("Segoe UI", Font.BOLD, 11));
            txtSueltos.setHorizontalAlignment(SwingConstants.CENTER);
            txtSueltos.setPreferredSize(new Dimension(65, 28));

            JButton btnAgregarProducto = new JButton("+ Agregar");
            btnAgregarProducto.setBackground(COLOR_VERDE);
            btnAgregarProducto.setForeground(Color.WHITE);
            btnAgregarProducto.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnAgregarProducto.setFocusPainted(false);
            btnAgregarProducto.setBorderPainted(false);
            btnAgregarProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAgregarProducto.setPreferredSize(new Dimension(100, 28));

            JButton btnQuitarProducto = new JButton("Quitar Fila");
            btnQuitarProducto.setBackground(COLOR_ROJO);
            btnQuitarProducto.setForeground(Color.WHITE);
            btnQuitarProducto.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnQuitarProducto.setFocusPainted(false);
            btnQuitarProducto.setBorderPainted(false);
            btnQuitarProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnQuitarProducto.setPreferredSize(new Dimension(95, 28));

            fila2.add(lblCantidadEnvases);
            fila2.add(txtCantidadEnvases);
            fila2.add(lblPrecioEnvase);
            fila2.add(txtPrecioEnvase);
            fila2.add(lblPesoEnvase);
            fila2.add(cmbPesoEnvasePedido);
            fila2.add(lblSueltos);
            fila2.add(txtSueltos);
            fila2.add(Box.createHorizontalStrut(8));
            fila2.add(btnAgregarProducto);
            fila2.add(btnQuitarProducto);

            pnlAgregar.add(fila2);
            pnlCentro.add(pnlAgregar, BorderLayout.SOUTH);
            panelCentral.add(pnlCentro, BorderLayout.CENTER);

            // ============================================================
            // RESUMEN INFERIOR (3 CHIPS) + CHECKBOX DE ENTRADA A STOCK
            // ============================================================
            JPanel pnlSur = new JPanel(new BorderLayout(0, 8));
            pnlSur.setOpaque(false);

            JPanel panelResumen = new JPanel(new GridLayout(1, 3, 14, 0));
            panelResumen.setBackground(new Color(236, 253, 245));
            panelResumen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(167, 243, 208), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));

            JLabel lblTotalProductos = new JLabel("Productos: 0", SwingConstants.LEFT);
            lblTotalProductos.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblTotalProductos.setForeground(new Color(6, 95, 70));

            JLabel lblTotalCantidad = new JLabel("Total unidades: 0.00 lb", SwingConstants.CENTER);
            lblTotalCantidad.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblTotalCantidad.setForeground(new Color(15, 118, 110));

            JLabel lblTotalPedido = new JLabel("Total Pedido: $0.00", SwingConstants.RIGHT);
            lblTotalPedido.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblTotalPedido.setForeground(new Color(4, 120, 87));

            panelResumen.add(lblTotalProductos);
            panelResumen.add(lblTotalCantidad);
            panelResumen.add(lblTotalPedido);
            pnlSur.add(panelResumen, BorderLayout.NORTH);

            JCheckBox chkAgregarInventario = new JCheckBox("Agregar productos al inventario automáticamente al recibir el pedido", true);
            chkAgregarInventario.setFont(new Font("Segoe UI", Font.BOLD, 12));
            chkAgregarInventario.setForeground(SLATE_OSCURO);
            chkAgregarInventario.setOpaque(false);
            pnlSur.add(chkAgregarInventario, BorderLayout.SOUTH);

            panelCentral.add(pnlSur, BorderLayout.SOUTH);
            dialog.add(panelCentral, BorderLayout.CENTER);

            // EVENTOS DE COMBOBOX DE PRODUCTO
            cmbProducto.addActionListener(e -> {
                Object seleccion = cmbProducto.getSelectedItem();
                if (seleccion instanceof MateriaPrima) {
                    MateriaPrima mp = (MateriaPrima) seleccion;
                    String unidad = mp.getUnidad();
                    if ("litro".equalsIgnoreCase(unidad) || "litros".equalsIgnoreCase(unidad)) {
                        lblCantidadEnvases.setText("Cant. Bidones:");
                        lblPrecioEnvase.setText("Precio/Bidón ($):");
                        lblPesoEnvase.setText("Litros/Bidón:");
                        lblSueltos.setText("Litros Sueltos:");
                    } else {
                        lblCantidadEnvases.setText("Cant. Sacos:");
                        lblPrecioEnvase.setText("Precio/Saco ($):");
                        lblPesoEnvase.setText("Libras/Saco:");
                        lblSueltos.setText("Libras Sueltas:");
                    }
                    
                    double pesoDefinido = mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;
                    cmbPesoEnvasePedido.removeAllItems();
                    if (pesoDefinido > 0) {
                        cmbPesoEnvasePedido.addItem(String.format("%.2f", pesoDefinido));
                        lblPesoDefinidoPedido.setText("📌 Peso por envase: " + String.format("%.2f", pesoDefinido) + " " + unidad);
                    } else {
                        cmbPesoEnvasePedido.addItem("0");
                        lblPesoDefinidoPedido.setText("⚠️ Producto sin peso fijo por envase.");
                    }
                    cmbPesoEnvasePedido.setSelectedIndex(0);
                } else if (seleccion instanceof String) {
                    lblPesoDefinidoPedido.setText("📌 Agregando nuevo producto...");
                    cmbPesoEnvasePedido.removeAllItems();
                    cmbPesoEnvasePedido.addItem("0");
                    cmbPesoEnvasePedido.setSelectedIndex(0);
                }
            });

            btnNuevoProducto.addActionListener(e -> {
                MateriaPrima nuevo = mostrarDialogoCrearProductoDesdePedido(dialog, modelProductos);
                if (nuevo != null) {
                    cmbProducto.addItem(nuevo);
                    cmbProducto.setSelectedItem(nuevo);
                }
            });

            // RECALCULO DE TOTALES AL AGREGAR O QUITAR
            modelProductos.addTableModelListener(e -> {
                int filas = modelProductos.getRowCount();
                double totalUnidades = 0;
                double totalPedido = 0;

                for (int i = 0; i < filas; i++) {
                    try {
                        totalUnidades += Double.parseDouble(((String) modelProductos.getValueAt(i, 2)).replace(" lb", "").replace(" lt", "").replace(",", "."));
                        totalPedido += Double.parseDouble(((String) modelProductos.getValueAt(i, 4)).replace("$", "").replace(",", "."));
                    } catch (Exception ignored) {}
                }

                lblTotalProductos.setText("Productos: " + filas);
                lblTotalCantidad.setText("Total cantidad: " + String.format("%.2f", totalUnidades));
                lblTotalPedido.setText("Total Pedido: $" + String.format("%.2f", totalPedido));
            });

            btnAgregarProducto.addActionListener(e -> {
                try {
                    Object seleccion = cmbProducto.getSelectedItem();
                    if (seleccion == null) {
                        DialogStyler.mostrarError(dialog, "Seleccione un producto.");
                        return;
                    }
                    if (seleccion instanceof String) {
                        MateriaPrima nuevo = mostrarDialogoCrearProductoDesdePedido(dialog, modelProductos);
                        if (nuevo != null) {
                            cmbProducto.addItem(nuevo);
                            cmbProducto.setSelectedItem(nuevo);
                        }
                        return;
                    }
                    MateriaPrima mp = (MateriaPrima) seleccion;

                    int cantidadEnvases = Integer.parseInt(txtCantidadEnvases.getText().trim());
                    double precioEnvase = Double.parseDouble(txtPrecioEnvase.getText().trim().replace(",", "."));
                    
                    String pesoSeleccionadoStr = (String) cmbPesoEnvasePedido.getSelectedItem();
                    double pesoEnvase = (pesoSeleccionadoStr != null && !pesoSeleccionadoStr.equals("0")) 
                        ? Double.parseDouble(pesoSeleccionadoStr.replace(",", ".")) : 0;
                    
                    double sueltos = Double.parseDouble(txtSueltos.getText().trim().replace(",", "."));

                    if (cantidadEnvases == 0 && sueltos == 0) {
                        DialogStyler.mostrarError(dialog, "Debe ingresar al menos una unidad (envases o sueltos).");
                        return;
                    }

                    double cantidadTotal = (cantidadEnvases * pesoEnvase) + sueltos;
                    double precioPorUnidad = pesoEnvase > 0 ? (precioEnvase / pesoEnvase) : (sueltos > 0 ? precioEnvase / sueltos : 0);
                    double subtotal = cantidadEnvases > 0 ? (cantidadEnvases * precioEnvase) : (sueltos * precioPorUnidad);

                    String tipoEnvase = "litro".equalsIgnoreCase(mp.getUnidad()) ? "Bidón" : "Saco";

                    modelProductos.addRow(new Object[]{
                        mp.getNombre(),
                        mp.getUnidad(),
                        String.format("%.2f", cantidadTotal),
                        String.format("$%.4f", precioPorUnidad),
                        String.format("$%.2f", subtotal),
                        cantidadEnvases + " " + tipoEnvase + "(s)",
                        String.format("%.2f", pesoEnvase),
                        String.format("%.2f", sueltos)
                    });

                    txtCantidadEnvases.setText("0");
                    txtPrecioEnvase.setText("0");
                    txtSueltos.setText("0");

                } catch (NumberFormatException ex) {
                    DialogStyler.mostrarError(dialog, "Ingrese valores numéricos válidos.");
                }
            });

            btnQuitarProducto.addActionListener(e -> {
                int rowProd = tablaProductos.getSelectedRow();
                if (rowProd < 0) {
                    DialogStyler.mostrarAdvertencia(dialog, "Seleccione un producto para quitar.", "Aviso");
                    return;
                }
                modelProductos.removeRow(rowProd);
            });

            // BOTONES DE PIE
            JPanel pnlBotonesPie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            pnlBotonesPie.setBackground(Color.WHITE);
            pnlBotonesPie.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDE_SUAVE));

            JButton btnCancelar = new JButton("Cancelar");
            btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnCancelar.setBackground(new Color(241, 245, 249));
            btnCancelar.setForeground(SLATE_OSCURO);
            btnCancelar.setFocusPainted(false);
            btnCancelar.setBorderPainted(false);
            btnCancelar.setPreferredSize(new Dimension(110, 36));
            btnCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnCancelar.addActionListener(e -> dialog.dispose());

            JButton btnGuardarFinal = new JButton("Guardar Pedido");
            btnGuardarFinal.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnGuardarFinal.setBackground(COLOR_VERDE);
            btnGuardarFinal.setForeground(Color.WHITE);
            btnGuardarFinal.setFocusPainted(false);
            btnGuardarFinal.setBorderPainted(false);
            btnGuardarFinal.setPreferredSize(new Dimension(150, 36));
            btnGuardarFinal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnGuardarFinal.addActionListener(e -> {
                if (modelProductos.getRowCount() == 0) {
                    DialogStyler.mostrarError(dialog, "Agregue al menos un producto al pedido.");
                    return;
                }

                final Proveedor proveedor = (Proveedor) cmbProveedorLocal.getSelectedItem();
                if (proveedor == null) {
                    DialogStyler.mostrarError(dialog, "Seleccione un proveedor.");
                    return;
                }

                String nombreFinal = txtNombrePedido.getText().trim();
                if (nombreFinal.isEmpty()) {
                    nombreFinal = "PEDIDO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001";
                }
                
                final String nombreFinalPed = nombreFinal;
                final boolean recibirAutomatico = chkAgregarInventario.isSelected();

                btnGuardarFinal.setEnabled(false);
                btnGuardarFinal.setText("⏳ Procesando...");
                dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    private String errorMsg;
                    private int pedidoIdGenerado = 0;
                    private double totalPedido = 0;

                    @Override
                    protected Void doInBackground() {
                        try {
                            Pedido pedido = new Pedido();
                            pedido.setProveedorId(proveedor.getId());
                            pedido.setEstadoPago("PENDIENTE");
                            pedido.setEstadoRecepcion(recibirAutomatico ? "RECIBIDA" : "PENDIENTE");
                            pedido.setNotas(nombreFinalPed);

                            for (int i = 0; i < modelProductos.getRowCount(); i++) {
                                String nombre = (String) modelProductos.getValueAt(i, 0);
                                String unidad = (String) modelProductos.getValueAt(i, 1);
                                double cantidadTotal = Double.parseDouble(((String) modelProductos.getValueAt(i, 2)).replace(",", "."));
                                double precioUnitario = Double.parseDouble(((String) modelProductos.getValueAt(i, 3)).replace("$", "").replace(",", "."));
                                double subtotal = Double.parseDouble(((String) modelProductos.getValueAt(i, 4)).replace("$", "").replace(",", "."));
                                
                                String envaseStr = (String) modelProductos.getValueAt(i, 5);
                                int envases = 0;
                                if (envaseStr != null && !envaseStr.isEmpty()) {
                                    try {
                                        envases = Integer.parseInt(envaseStr.split(" ")[0]);
                                    } catch (Exception ex) { envases = 0; }
                                }
                                
                                double pesoEnvase = Double.parseDouble(((String) modelProductos.getValueAt(i, 6)).replace(",", "."));
                                double sueltos = Double.parseDouble(((String) modelProductos.getValueAt(i, 7)).replace(",", "."));

                                MateriaPrima mp = materiaDAO.buscarPorNombre(nombre);
                                if (mp == null) {
                                    mp = new MateriaPrima();
                                    mp.setNombre(nombre);
                                    mp.setUnidad(unidad);
                                    mp.setCantidadTotal(0);
                                    mp.setCantidadDisponible(0);
                                    mp.setPrecioTotal(0);
                                    mp.setPrecioPorUnidad(precioUnitario);
                                    mp.setNumeroSacos(envases > 0 ? envases : 0);
                                    mp.setPesoSaco(pesoEnvase > 0 ? pesoEnvase : 0);
                                    mp.setLibrasSueltas(sueltos > 0 ? sueltos : 0);
                                    materiaDAO.guardarRapido(mp);
                                }

                                DetallePedido detalle = new DetallePedido();
                                detalle.setMateriaPrimaId(mp.getId());
                                detalle.setNombreProducto(nombre);
                                detalle.setUnidad(unidad);
                                detalle.setCantidad(cantidadTotal);
                                detalle.setPrecioUnitario(precioUnitario);
                                detalle.setSubtotal(subtotal);
                                detalle.setNumeroSacos(envases > 0 ? envases : null);
                                detalle.setPesoSaco(pesoEnvase > 0 ? pesoEnvase : null);
                                detalle.setLibrasSueltas(sueltos > 0 ? sueltos : null);
                                pedido.addDetalle(detalle);
                                
                                totalPedido += subtotal;
                            }

                            pedidoDAO.guardar(pedido);
                            pedidoIdGenerado = pedido.getId();

                        } catch (Exception ex) {
                            errorMsg = ex.getMessage();
                            LoggerUtil.logError("Error guardando pedido", ex);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        btnGuardarFinal.setEnabled(true);
                        btnGuardarFinal.setText("Guardar Pedido");
                        dialog.setCursor(Cursor.getDefaultCursor());

                        if (errorMsg != null) {
                            DialogStyler.mostrarError(dialog, "Error al guardar pedido: " + errorMsg);
                            return;
                        }

                        dialog.dispose();
                        cargarDatos();

                        Window window = SwingUtilities.getWindowAncestor(ProveedoresPanel.this);
                        if (window instanceof MainFrame) {
                            ((MainFrame) window).refrescarInventario();
                            ((MainFrame) window).refrescarDashboard();
                        }

                        String mensaje = "✅ Pedido guardado correctamente.<br><br>" +
                            "Nombre: <b>" + nombreFinalPed + "</b><br>" +
                            "Número: <b>#" + pedidoIdGenerado + "</b><br>" +
                            "Productos: <b>" + modelProductos.getRowCount() + "</b><br>" +
                            "Total: <b>$" + String.format("%.2f", totalPedido) + "</b>" +
                            (recibirAutomatico ? "<br><br>📦 Productos agregados al inventario automáticamente." : "");

                        DialogStyler.mostrarExito(ProveedoresPanel.this, mensaje, "Pedido Guardado");
                    }
                };
                worker.execute();
            });

            pnlBotonesPie.add(btnCancelar);
            pnlBotonesPie.add(btnGuardarFinal);
            dialog.add(pnlBotonesPie, BorderLayout.SOUTH);

            dialog.setVisible(true);

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // ============================================================
    //  CREAR PRODUCTO DESDE PEDIDO
    // ============================================================
    private MateriaPrima mostrarDialogoCrearProductoDesdePedido(JDialog parent, DefaultTableModel modelProductos) {
        JDialog dialog = new JDialog(parent, "Agregar Nuevo Producto", Dialog.ModalityType.APPLICATION_MODAL);
        DialogStyler.aplicar(dialog, "Agregar Nuevo Producto", "➕");

        JPanel form = DialogStyler.crearPanelFormulario();

        JTextField txtNombre = DialogStyler.crearCampo(18);
        JComboBox<String> cmbUnidad = DialogStyler.crearCombo(new String[]{"lb", "litro"});
        JTextField txtNumEnvases = DialogStyler.crearCampoNumerico(10);
        txtNumEnvases.setText("0");
        JTextField txtPesoEnvase = DialogStyler.crearCampoNumerico(10);
        txtPesoEnvase.setText("0");
        JTextField txtCantidadSueltas = DialogStyler.crearCampoNumerico(10);
        txtCantidadSueltas.setText("0");
        JTextField txtPrecioEnvase = DialogStyler.crearCampoNumerico(10);
        txtPrecioEnvase.setText("0");

        JLabel lblPesoEnvase = DialogStyler.crearLabel("Peso por envase:");
        JLabel lblCantidadSueltas = DialogStyler.crearLabel("Cantidad suelta:");
        JLabel lblPrecioEnvase = DialogStyler.crearLabel("Precio por envase ($):");

        JPanel bannerPrecio = DialogStyler.crearBannerInfo("info", "Ingrese peso y precio para calcular el costo por unidad.");

        final JTextField txtNombreFinal = txtNombre;
        final JComboBox<String> cmbUnidadFinal = cmbUnidad;
        final JTextField txtNumEnvasesFinal = txtNumEnvases;
        final JTextField txtPesoEnvaseFinal = txtPesoEnvase;
        final JTextField txtCantidadSueltasFinal = txtCantidadSueltas;
        final JTextField txtPrecioEnvaseFinal = txtPrecioEnvase;
        final DefaultTableModel modelProductosFinal = modelProductos;

        Runnable actualizarPreview = () -> {
            String pesoStr = txtPesoEnvaseFinal.getText().trim().replace(",", ".");
            String precStr = txtPrecioEnvaseFinal.getText().trim().replace(",", ".");
            
            if (pesoStr.isEmpty() || precStr.isEmpty()) {
                ((JLabel) bannerPrecio.getComponent(0)).setText("<html><body style='width:440px'>ℹ️ Ingrese peso y precio para calcular el costo por unidad.</body></html>");
                bannerPrecio.repaint();
                return;
            }
            
            try {
                double peso = Double.parseDouble(pesoStr);
                double precio = Double.parseDouble(precStr);
                
                if (peso > 0) {
                    double precioU = precio / peso;
                    String sueltStr = txtCantidadSueltasFinal.getText().trim().replace(",", ".");
                    String msg = String.format("Precio por unidad: <b>$%.4f</b>", precioU);
                    
                    if (!sueltStr.isEmpty()) {
                        double sueltos = Double.parseDouble(sueltStr);
                        if (sueltos > 0) {
                            msg += String.format(" &nbsp;|&nbsp; Costo sueltos: <b>$%.2f</b>", sueltos * precioU);
                        }
                    }
                    ((JLabel) bannerPrecio.getComponent(0)).setText("<html><body style='width:440px'>✅ " + msg + "</body></html>");
                } else {
                    ((JLabel) bannerPrecio.getComponent(0)).setText("<html><body style='width:440px'>⚠️ El peso debe ser mayor a 0</body></html>");
                }
            } catch (NumberFormatException ex) {
                ((JLabel) bannerPrecio.getComponent(0)).setText("<html><body style='width:440px'>⚠️ Ingrese valores numéricos válidos</body></html>");
            }
            bannerPrecio.repaint();
        };

        final boolean[] isUpdating = {false};
        javax.swing.event.DocumentListener previewListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                if (!isUpdating[0]) { isUpdating[0] = true; actualizarPreview.run(); isUpdating[0] = false; }
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                if (!isUpdating[0]) { isUpdating[0] = true; actualizarPreview.run(); isUpdating[0] = false; }
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                if (!isUpdating[0]) { isUpdating[0] = true; actualizarPreview.run(); isUpdating[0] = false; }
            }
        };
        
        txtPesoEnvaseFinal.getDocument().addDocumentListener(previewListener);
        txtPrecioEnvaseFinal.getDocument().addDocumentListener(previewListener);
        txtCantidadSueltasFinal.getDocument().addDocumentListener(previewListener);

        cmbUnidadFinal.addActionListener(e -> {
            boolean esLitro = "litro".equals(cmbUnidadFinal.getSelectedItem());
            lblPesoEnvase.setText(esLitro ? "Litros por envase:" : "Peso por envase (libras):");
            lblCantidadSueltas.setText(esLitro ? "Litros sueltos:" : "Libras sueltas:");
            lblPrecioEnvase.setText(esLitro ? "Precio por envase ($):" : "Precio por envase ($):");
            if (!isUpdating[0]) { isUpdating[0] = true; actualizarPreview.run(); isUpdating[0] = false; }
        });

        int r = 0;
        GridBagConstraints gbcSep0 = DialogStyler.crearGBC(0, r++);
        gbcSep0.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Datos del producto"), gbcSep0);

        form.add(DialogStyler.crearLabel("Nombre del producto:"), DialogStyler.crearGBC(0, r));
        form.add(txtNombreFinal, DialogStyler.crearGBC(1, r++));

        form.add(DialogStyler.crearLabel("Unidad:"), DialogStyler.crearGBC(0, r));
        form.add(cmbUnidadFinal, DialogStyler.crearGBC(1, r++));

        GridBagConstraints gbcSep1 = DialogStyler.crearGBC(0, r++);
        gbcSep1.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Datos del envase"), gbcSep1);

        form.add(DialogStyler.crearLabel("Número de envases:"), DialogStyler.crearGBC(0, r));
        form.add(txtNumEnvasesFinal, DialogStyler.crearGBC(1, r++));

        form.add(lblPesoEnvase, DialogStyler.crearGBC(0, r));
        form.add(txtPesoEnvaseFinal, DialogStyler.crearGBC(1, r++));

        form.add(lblCantidadSueltas, DialogStyler.crearGBC(0, r));
        form.add(txtCantidadSueltasFinal, DialogStyler.crearGBC(1, r++));

        GridBagConstraints gbcSep2 = DialogStyler.crearGBC(0, r++);
        gbcSep2.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Precio"), gbcSep2);

        form.add(lblPrecioEnvase, DialogStyler.crearGBC(0, r));
        form.add(txtPrecioEnvaseFinal, DialogStyler.crearGBC(1, r++));

        GridBagConstraints gbcBanner = DialogStyler.crearGBC(0, r++);
        gbcBanner.gridwidth = 2;
        form.add(bannerPrecio, gbcBanner);

        JButton btnGuardar = DialogStyler.crearBotonPrimario("✅ Crear Producto y Agregar");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(e -> {
            String nombre = txtNombreFinal.getText().trim();
            if (nombre.isEmpty()) {
                DialogStyler.mostrarError(dialog, "El nombre del producto no puede estar vacío.");
                txtNombreFinal.requestFocus();
                return;
            }
            
            try {
                String unidad = (String) cmbUnidadFinal.getSelectedItem();
                int numEnvases = Integer.parseInt(txtNumEnvasesFinal.getText().trim());
                double pesoEnvase = Double.parseDouble(txtPesoEnvaseFinal.getText().trim().replace(",", "."));
                double cantidadSuelta = Double.parseDouble(txtCantidadSueltasFinal.getText().trim().replace(",", "."));
                double precioEnvase = Double.parseDouble(txtPrecioEnvaseFinal.getText().trim().replace(",", "."));

                double cantidadTotal = (numEnvases * pesoEnvase) + cantidadSuelta;
                double precioPorUnidad = pesoEnvase > 0 ? precioEnvase / pesoEnvase : 0;
                double subtotal = cantidadTotal * precioPorUnidad;

                String tipoEnvase = "litro".equalsIgnoreCase(unidad) ? "Bidón" : "Saco";
                modelProductosFinal.addRow(new Object[]{
                    nombre,
                    unidad,
                    String.format("%.2f", cantidadTotal),
                    String.format("$%.4f", precioPorUnidad),
                    String.format("$%.2f", subtotal),
                    numEnvases + " " + tipoEnvase + "(s)",
                    String.format("%.2f", pesoEnvase),
                    String.format("%.2f", cantidadSuelta)
                });

                dialog.dispose();

            } catch (NumberFormatException ex) {
                DialogStyler.mostrarError(dialog, "Ingrese valores numéricos válidos.");
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(540, 580));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return null;
    }
    
    // ============================================================
    //  NUEVO PROVEEDOR
    // ============================================================
    private void mostrarDialogoNuevoProveedor() {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Nuevo Proveedor",
            Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(500, 520);
        dialog.setLocationRelativeTo(this);

        DialogStyler.aplicar(dialog, "Nuevo Proveedor", "🏢");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

        JTextField txtNombre = DialogStyler.crearCampo(20);
        JTextField txtTelefono = DialogStyler.crearCampoNumerico(20);
        JTextField txtEmail = DialogStyler.crearCampo(20);
        JTextField txtDireccion = DialogStyler.crearCampo(20);
        JTextField txtRuc = DialogStyler.crearCampo(20);
        JTextField txtContacto = DialogStyler.crearCampo(20);
        JComboBox<String> cmbTipo = DialogStyler.crearCombo(new String[]{"MATERIA_PRIMA", "SERVICIOS", "EQUIPOS"});

        int fila = 0;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Nombre:*"), gbc);
        gbc.gridx = 1; form.add(txtNombre, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Teléfono:"), gbc);
        gbc.gridx = 1; form.add(txtTelefono, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Email:"), gbc);
        gbc.gridx = 1; form.add(txtEmail, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Dirección:"), gbc);
        gbc.gridx = 1; form.add(txtDireccion, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("RUC/Cédula:"), gbc);
        gbc.gridx = 1; form.add(txtRuc, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Persona de Contacto:"), gbc);
        gbc.gridx = 1; form.add(txtContacto, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Tipo Proveedor:"), gbc);
        gbc.gridx = 1; form.add(cmbTipo, gbc);

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "El nombre del proveedor no puede estar vacío.");
                    return;
                }

                Proveedor proveedor = new Proveedor();
                proveedor.setNombre(nombre);
                proveedor.setTelefono(txtTelefono.getText().trim());
                proveedor.setEmail(txtEmail.getText().trim());
                proveedor.setDireccion(txtDireccion.getText().trim());
                proveedor.setRucCedula(txtRuc.getText().trim());
                proveedor.setContacto(txtContacto.getText().trim());
                proveedor.setTipoProveedor((String) cmbTipo.getSelectedItem());
                proveedor.setEstado("ACTIVO");

                proveedorDAO.guardar(proveedor);

                dialog.dispose();
                cargarDatos();

                DialogStyler.mostrarExito(this,
                    "Proveedor <b>" + nombre + "</b> guardado correctamente.<br>Código: <b>" + proveedor.getCodigo() + "</b>",
                    "Éxito"
                );

            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    // ============================================================
    //  EDITAR PROVEEDOR
    // ============================================================
    private void editarProveedor() {
        int row = tablaProveedores.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un proveedor para editar.", "Aviso");
            return;
        }
        
        try {
            String codigo = (String) tableModelProveedores.getValueAt(row, 0);
            Proveedor proveedor = proveedorDAO.buscarPorCodigo(codigo);
            
            if (proveedor == null) {
                DialogStyler.mostrarError(this, "Proveedor no encontrado.");
                return;
            }
            
            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Editar Proveedor",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(500, 540);
            dialog.setLocationRelativeTo(this);

            DialogStyler.aplicar(dialog, "Editar Proveedor", "✏️");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            JTextField txtNombre = DialogStyler.crearCampo(20);
            txtNombre.setText(proveedor.getNombre());
            JTextField txtTelefono = DialogStyler.crearCampoNumerico(20);
            txtTelefono.setText(proveedor.getTelefono() != null ? proveedor.getTelefono() : "");
            JTextField txtEmail = DialogStyler.crearCampo(20);
            txtEmail.setText(proveedor.getEmail() != null ? proveedor.getEmail() : "");
            JTextField txtDireccion = DialogStyler.crearCampo(20);
            txtDireccion.setText(proveedor.getDireccion() != null ? proveedor.getDireccion() : "");
            JTextField txtRuc = DialogStyler.crearCampo(20);
            txtRuc.setText(proveedor.getRucCedula() != null ? proveedor.getRucCedula() : "");
            JTextField txtContacto = DialogStyler.crearCampo(20);
            txtContacto.setText(proveedor.getContacto() != null ? proveedor.getContacto() : "");
            JComboBox<String> cmbTipo = DialogStyler.crearCombo(new String[]{"MATERIA_PRIMA", "SERVICIOS", "EQUIPOS"});
            cmbTipo.setSelectedItem(proveedor.getTipoProveedor());
            JComboBox<String> cmbEstado = DialogStyler.crearCombo(new String[]{"ACTIVO", "INACTIVO"});
            cmbEstado.setSelectedItem(proveedor.getEstado());

            int fila = 0;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Código:"), gbc);
            gbc.gridx = 1;
            JLabel lblCodigo = new JLabel(proveedor.getCodigo());
            lblCodigo.setFont(DialogStyler.FUENTE_CAMPO);
            lblCodigo.setForeground(DialogStyler.TEXTO_SECUNDARIO);
            form.add(lblCodigo, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Nombre:*"), gbc);
            gbc.gridx = 1; form.add(txtNombre, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Teléfono:"), gbc);
            gbc.gridx = 1; form.add(txtTelefono, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Email:"), gbc);
            gbc.gridx = 1; form.add(txtEmail, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Dirección:"), gbc);
            gbc.gridx = 1; form.add(txtDireccion, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("RUC/Cédula:"), gbc);
            gbc.gridx = 1; form.add(txtRuc, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Persona de Contacto:"), gbc);
            gbc.gridx = 1; form.add(txtContacto, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Tipo Proveedor:"), gbc);
            gbc.gridx = 1; form.add(cmbTipo, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Estado:"), gbc);
            gbc.gridx = 1; form.add(cmbEstado, gbc);

            JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Cambios");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(e -> {
                try {
                    String nombre = txtNombre.getText().trim();
                    if (nombre.isEmpty()) {
                        DialogStyler.mostrarError(dialog, "El nombre del proveedor no puede estar vacío.");
                        return;
                    }

                    proveedor.setNombre(nombre);
                    proveedor.setTelefono(txtTelefono.getText().trim());
                    proveedor.setEmail(txtEmail.getText().trim());
                    proveedor.setDireccion(txtDireccion.getText().trim());
                    proveedor.setRucCedula(txtRuc.getText().trim());
                    proveedor.setContacto(txtContacto.getText().trim());
                    proveedor.setTipoProveedor((String) cmbTipo.getSelectedItem());
                    proveedor.setEstado((String) cmbEstado.getSelectedItem());

                    proveedorDAO.actualizar(proveedor);

                    dialog.dispose();
                    cargarDatos();

                    DialogStyler.mostrarExito(this, "Proveedor <b>" + nombre + "</b> actualizado correctamente.", "Éxito");

                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
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
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private void eliminarProveedor() {
        int row = tablaProveedores.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un proveedor para eliminar.", "Aviso");
            return;
        }
        
        try {
            final String codigo = (String) tableModelProveedores.getValueAt(row, 0);
            final String nombre = (String) tableModelProveedores.getValueAt(row, 1);
            final Proveedor proveedor = proveedorDAO.buscarPorCodigo(codigo);
            
            if (proveedor == null) {
                DialogStyler.mostrarError(this, "Proveedor no encontrado.");
                return;
            }
            
            List<Pedido> pedidos = pedidoDAO.listarPorProveedor(proveedor.getId());
            boolean tienePendientes = false;
            boolean tieneRecibidos = false;
            StringBuilder pedidosRecibidos = new StringBuilder();
            
            for (Pedido p : pedidos) {
                if (!"CANCELADO".equals(p.getEstadoPago())) {
                    tienePendientes = true;
                    break;
                }
                if ("RECIBIDA".equals(p.getEstadoRecepcion())) {
                    tieneRecibidos = true;
                    pedidosRecibidos.append("• Pedido #").append(p.getId())
                        .append(" - ").append(p.getNotas() != null ? p.getNotas() : "Sin nombre")
                        .append(" - Total: $").append(String.format("%.2f", p.getTotalPedido()))
                        .append("\n");
                }
            }
            
            if (tienePendientes) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR\n\n" +
                    "El proveedor " + nombre + " tiene pedidos pendientes de pago.\n" +
                    "Primero debe cancelar todas las deudas.");
                return;
            }
            
            if (tieneRecibidos) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR\n\n" +
                    "El proveedor " + nombre + " tiene pedidos YA RECIBIDOS:\n\n" +
                    pedidosRecibidos.toString() +
                    "\nEstos pedidos ya afectaron el inventario y no se pueden eliminar.\n\n" +
                    "Para eliminar este proveedor, primero debe eliminar los pedidos recibidos.");
                return;
            }
            
            String mensaje = "¿Eliminar el proveedor " + nombre + "?\n\n" +
                "Esta acción eliminará todos los datos del proveedor.\n" +
                "¿Está seguro?";
            
            int confirm = JOptionPane.showConfirmDialog(this, mensaje, "Eliminar proveedor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            JOptionPane.showMessageDialog(this, "✅ Proveedor eliminado correctamente.", "Proveedor eliminado", JOptionPane.INFORMATION_MESSAGE);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private String errorMsg;
                
                @Override
                protected Void doInBackground() {
                    Connection conn = null;
                    PreparedStatement ps = null;
                    ResultSet rs = null;
                    
                    try {
                        List<Pedido> pedidosLocal = pedidoDAO.listarPorProveedor(proveedor.getId());
                        conn = DatabaseManager.getConnection();
                        conn.setAutoCommit(false);
                        
                        for (Pedido p : pedidosLocal) {
                            String sqlPagos = "DELETE FROM pagos_pedido WHERE pedido_id = ?";
                            ps = conn.prepareStatement(sqlPagos);
                            ps.setInt(1, p.getId());
                            ps.executeUpdate();
                            ps.close();
                            
                            String sqlDetalles = "DELETE FROM detalle_pedido WHERE pedido_id = ?";
                            ps = conn.prepareStatement(sqlDetalles);
                            ps.setInt(1, p.getId());
                            ps.executeUpdate();
                            ps.close();
                        }
                        
                        for (Pedido p : pedidosLocal) {
                            String sqlPedido = "DELETE FROM pedidos WHERE id = ?";
                            ps = conn.prepareStatement(sqlPedido);
                            ps.setInt(1, p.getId());
                            ps.executeUpdate();
                            ps.close();
                        }
                        
                        String sqlSelect = "SELECT id FROM compras_proveedores WHERE proveedor_id = ?";
                        ps = conn.prepareStatement(sqlSelect);
                        ps.setInt(1, proveedor.getId());
                        rs = ps.executeQuery();
                        
                        List<Integer> comprasIds = new ArrayList<>();
                        while (rs.next()) {
                            comprasIds.add(rs.getInt(1));
                        }
                        rs.close();
                        ps.close();
                        
                        for (int compraId : comprasIds) {
                            String sqlPagosCompra = "DELETE FROM pagos_proveedores WHERE compra_id = ?";
                            ps = conn.prepareStatement(sqlPagosCompra);
                            ps.setInt(1, compraId);
                            ps.executeUpdate();
                            ps.close();
                        }
                        
                        for (int compraId : comprasIds) {
                            String sqlCompra = "DELETE FROM compras_proveedores WHERE id = ?";
                            ps = conn.prepareStatement(sqlCompra);
                            ps.setInt(1, compraId);
                            ps.executeUpdate();
                            ps.close();
                        }
                        
                        String sqlProveedor = "DELETE FROM proveedores WHERE id = ?";
                        ps = conn.prepareStatement(sqlProveedor);
                        ps.setInt(1, proveedor.getId());
                        ps.executeUpdate();
                        ps.close();
                        
                        conn.commit();
                        LoggerUtil.logInfo("Proveedor eliminado: " + proveedor.getId() + " - " + nombre);
                        
                    } catch (Exception ex) {
                        if (conn != null) { try { conn.rollback(); } catch (Exception ignored) {} }
                        errorMsg = ex.getMessage();
                        LoggerUtil.logError("Error eliminando proveedor", ex);
                    } finally {
                        if (conn != null) { try { conn.setAutoCommit(true); } catch (Exception ignored) {} }
                        DatabaseManager.closeResources(conn, ps, rs);
                    }
                    return null;
                }
                
                @Override
                protected void done() {
                    if (errorMsg != null) {
                        JOptionPane.showMessageDialog(ProveedoresPanel.this, "Error al eliminar proveedor: " + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    cargarDatos();
                    Window window = SwingUtilities.getWindowAncestor(ProveedoresPanel.this);
                    if (window instanceof MainFrame) {
                        ((MainFrame) window).refrescarDashboard();
                    }
                }
            };
            worker.execute();
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar proveedor: " + ex.getMessage());
        }
    }

    private void eliminarPedidoSeleccionado() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un pedido para eliminar.", "Aviso");
            return;
        }

        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            final int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            final String nombrePedido = (String) tableModelPedidos.getValueAt(modelRow, 1);
            final String estadoPago = (String) tableModelPedidos.getValueAt(modelRow, 6);
            final String estadoRecepcion = (String) tableModelPedidos.getValueAt(modelRow, 7);
            final double total = Double.parseDouble(((String) tableModelPedidos.getValueAt(modelRow, 3)).replace("$", "").replace(",", "."));

            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            int countPagos = 0;

            try {
                conn = DatabaseManager.getConnection();
                String sqlCount = "SELECT COUNT(*) as total FROM pagos_pedido WHERE pedido_id = ?";
                ps = conn.prepareStatement(sqlCount);
                ps.setInt(1, pedidoId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    countPagos = rs.getInt("total");
                }
            } catch (Exception ignored) {
            } finally {
                DatabaseManager.closeResources(conn, ps, rs);
            }

            if (countPagos > 0) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR\n\n" +
                    "El pedido \"" + nombrePedido + "\" tiene " + countPagos + " pago(s) registrados.\n\n" +
                    "Para eliminar este pedido, primero debe eliminar todos los pagos con '🗑 Eliminar Pago'.");
                return;
            }

            String mensaje = "⚠️ ¿ELIMINAR PEDIDO?\n\n" +
                "Nombre: " + nombrePedido + "\n" +
                "Total: $" + String.format("%.2f", total) + "\n" +
                "Estado: " + estadoPago + " | " + estadoRecepcion + "\n\n";

            if ("RECIBIDA".equals(estadoRecepcion)) {
                mensaje += "⚠️ Este pedido YA FUE RECIBIDO.\nAl eliminarlo, se devolverá el stock al inventario.\n\n";
            }
            mensaje += "¿Está seguro?";

            if (!DialogStyler.confirmar(this, mensaje, "Eliminar pedido")) return;

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private String errorMsg;

                @Override
                protected Void doInBackground() {
                    try {
                        pedidoDAO.eliminar(pedidoId);
                    } catch (Exception ex) {
                        errorMsg = ex.getMessage();
                        LoggerUtil.logError("Error eliminando pedido", ex);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    if (errorMsg != null) {
                        DialogStyler.mostrarError(ProveedoresPanel.this, "Error al eliminar pedido: " + errorMsg);
                        return;
                    }

                    cargarDatos();
                    Window window = SwingUtilities.getWindowAncestor(ProveedoresPanel.this);
                    if (window instanceof MainFrame) {
                        ((MainFrame) window).refrescarInventario();
                        ((MainFrame) window).refrescarDashboard();
                    }

                    DialogStyler.mostrarExito(ProveedoresPanel.this, "✅ Pedido eliminado correctamente.", "Pedido eliminado");
                }
            };
            worker.execute();

        } catch (Exception ex) {
            setCursor(Cursor.getDefaultCursor());
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    private void eliminarPagoPedido() {
        int rowPago = tablaPagos.getSelectedRow();
        if (rowPago < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un pago para eliminar.", "Aviso");
            return;
        }

        Object idObj = tableModelPagos.getValueAt(rowPago, 0);
        if (idObj == null || "-".equals(idObj.toString())) {
            DialogStyler.mostrarAdvertencia(this, "No hay un pago válido para eliminar.", "Aviso");
            return;
        }

        try {
            final int pagoId = (int) idObj;
            final double monto = Double.parseDouble(((String) tableModelPagos.getValueAt(rowPago, 1)).replace("$", "").replace(",", "."));

            int rowPedido = tablaPedidos.getSelectedRow();
            if (rowPedido < 0) {
                DialogStyler.mostrarError(this, "Seleccione el pedido asociado.");
                return;
            }

            final int pedidoId = (int) tableModelPedidos.getValueAt(rowPedido, 0);
            final Pedido pedido = pedidoDAO.buscarPorId(pedidoId);

            if (pedido == null) {
                DialogStyler.mostrarError(this, "No se encontró el pedido asociado.");
                return;
            }

            String mensaje = "⚠️ ¿ELIMINAR PAGO?\n\n" +
                "Monto: $" + String.format("%.2f", monto) + "\n" +
                "Pedido: " + (pedido.getNotas() != null ? pedido.getNotas() : "Pedido #" + pedido.getId()) + "\n\n" +
                "Esta acción no se puede deshacer.\n¿Está seguro?";

            int confirm = JOptionPane.showConfirmDialog(this, mensaje, "Eliminar pago", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private String errorMsg;

                @Override
                protected Void doInBackground() {
                    try {
                        pedidoDAO.eliminarPagoPedido(pagoId);
                    } catch (Exception ex) {
                        errorMsg = ex.getMessage();
                        LoggerUtil.logError("Error eliminando pago", ex);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    if (errorMsg != null) {
                        DialogStyler.mostrarError(ProveedoresPanel.this, "Error al eliminar pago: " + errorMsg);
                        return;
                    }
                    cargarDatos();
                    DialogStyler.mostrarExito(ProveedoresPanel.this, "✅ Pago eliminado correctamente.\nMonto revertido: $" + String.format("%.2f", monto), "Pago eliminado");
                }
            };
            worker.execute();

        } catch (Exception ex) {
            setCursor(Cursor.getDefaultCursor());
            DialogStyler.mostrarError(this, "Error al eliminar pago: " + ex.getMessage());
        }
    }

    private void pagarPedidoSeleccionado() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un pedido para pagar.", "Aviso");
            return;
        }

        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            final int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            final String nombrePedido = (String) tableModelPedidos.getValueAt(modelRow, 1);
            final String estadoPago = (String) tableModelPedidos.getValueAt(modelRow, 6);
            final double pendiente = Double.parseDouble(((String) tableModelPedidos.getValueAt(modelRow, 5)).replace("$", "").replace(",", "."));

            if ("CANCELADO".equals(estadoPago) || pendiente <= 0) {
                DialogStyler.mostrarAdvertencia(this, "Este pedido ya está totalmente pagado.", "Aviso");
                return;
            }

            String input = JOptionPane.showInputDialog(this,
                    "PAGAR PEDIDO #" + pedidoId + "\n\n" +
                    "Nombre: " + nombrePedido + "\n" +
                    "Saldo pendiente: $" + String.format("%.2f", pendiente) + "\n\n" +
                    "Ingrese el monto a pagar:",
                    "Registrar Pago",
                    JOptionPane.QUESTION_MESSAGE);

            if (input == null) return;

            String inputClean = input.trim().replace(",", ".");
            if (!inputClean.matches("[0-9.]*")) {
                DialogStyler.mostrarError(this, "Ingrese un valor numérico válido.");
                return;
            }

            final double monto = Double.parseDouble(inputClean);
            if (monto <= 0) {
                DialogStyler.mostrarError(this, "El monto debe ser mayor a 0.");
                return;
            }

            final double montoFinal = Math.min(monto, pendiente);

            if (monto > pendiente) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "El monto a pagar ($" + String.format("%.2f", monto) +
                    ") es mayor al saldo pendiente ($" + String.format("%.2f", pendiente) + ").\n¿Desea cancelar la deuda con este pago?",
                    "Confirmar pago",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) return;
            }

            JOptionPane.showMessageDialog(this, "✅ Pago registrado correctamente.\nMonto pagado: $" + String.format("%.2f", montoFinal), "Pago registrado", JOptionPane.INFORMATION_MESSAGE);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private String errorMsg;

                @Override
                protected Void doInBackground() {
                    try {
                        pedidoDAO.registrarAbono(pedidoId, montoFinal);
                    } catch (Exception ex) {
                        errorMsg = ex.getMessage();
                        LoggerUtil.logError("Error registrando pago", ex);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (errorMsg != null) {
                        JOptionPane.showMessageDialog(ProveedoresPanel.this, "Error al registrar pago: " + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    cargarDatos();
                    Window window = SwingUtilities.getWindowAncestor(ProveedoresPanel.this);
                    if (window instanceof MainFrame) {
                        ((MainFrame) window).refrescarDashboard();
                    }
                }
            };
            worker.execute();

        } catch (NumberFormatException ex) {
            DialogStyler.mostrarError(this, "Ingrese un valor numérico válido.");
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al registrar pago: " + ex.getMessage());
        }
    }

    private void mostrarDetallesPedido(int row) {
        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            final int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            
            final Pedido p = pedidoDAO.buscarPorId(pedidoId);
            if (p == null) {
                DialogStyler.mostrarError(this, "Pedido no encontrado.");
                return;
            }
            
            DialogoEditarPedido dialog = new DialogoEditarPedido(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                p
            );
            dialog.setVisible(true);
            cargarDatos();
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al cargar detalles: " + ex.getMessage());
        }
    }
}