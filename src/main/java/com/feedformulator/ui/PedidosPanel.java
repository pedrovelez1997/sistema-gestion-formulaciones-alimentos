package com.feedformulator.ui;

import com.feedformulator.dao.PedidoDAO;
import com.feedformulator.dao.ProveedorDAO;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.model.DetallePedido;
import com.feedformulator.model.Pedido;
import com.feedformulator.model.Proveedor;
import com.feedformulator.model.MateriaPrima;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class PedidosPanel extends JPanel {
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    
    private DefaultTableModel tableModelPedidos;
    private DefaultTableModel tableModelDetalles;
    private JTable tablaPedidos;
    private JTable tablaDetalles;
    private JTextField txtBuscarPedido;
    private TableRowSorter<DefaultTableModel> sorterPedidos;
    private JLabel lblTotalPedidos;
    private JLabel lblTotalPendientePago;
    private JLabel lblTotalPendienteRecepcion;
    
    private static final Color COLOR_PRIMARIO = new Color(41, 128, 185);
    private static final Color COLOR_VERDE = new Color(34, 139, 34);
    private static final Color COLOR_ROJO = new Color(180, 40, 40);
    private static final Color COLOR_NARANJA = new Color(230, 126, 34);
    private static final Color COLOR_RECIBIR = new Color(0, 150, 200);
    private static final Color FONDO = new Color(240, 245, 248);

    public PedidosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        add(crearTitulo(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelResumen(), BorderLayout.SOUTH);
        
        cargarDatos();
    }
    
    private JPanel crearTitulo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(FONDO);
        
        try {
            ImageIcon iconPedidos = new ImageIcon(getClass().getResource("/icons/pedidos.png"));
            Image imgPedidos = iconPedidos.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
            JLabel icono = new JLabel(new ImageIcon(imgPedidos));
            p.add(icono);
        } catch (Exception e) {
            JLabel icono = new JLabel("📋");
            icono.setFont(new Font("Segoe UI", Font.PLAIN, 24));
            p.add(icono);
        }
        
        JLabel lbl = new JLabel("  Gestión de Pedidos a Proveedores");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(new Color(20, 100, 20));
        p.add(lbl);
        return p;
    }
    
    private JSplitPane crearPanelCentral() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setBackground(FONDO);
        split.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        // Panel superior: Lista de pedidos
        JPanel panelPedidos = new JPanel(new BorderLayout(5, 5));
        panelPedidos.setBackground(FONDO);
        
        JPanel headerPedidos = new JPanel(new BorderLayout());
        headerPedidos.setBackground(FONDO);
        
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tituloPanel.setBackground(FONDO);
        JLabel lblPedidos = new JLabel("📋  Pedidos Registrados");
        lblPedidos.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tituloPanel.add(lblPedidos);
        headerPedidos.add(tituloPanel, BorderLayout.WEST);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPanel.setBackground(FONDO);
        
        // Buscador
        try {
            ImageIcon iconSearch = new ImageIcon(getClass().getResource("/icons/magnifying-glass.png"));
            Image imgSearch = iconSearch.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JLabel lblBuscar = new JLabel(new ImageIcon(imgSearch));
            lblBuscar.setToolTipText("Buscar");
            btnPanel.add(lblBuscar);
        } catch (Exception e) {
            JLabel lblBuscar = new JLabel("🔍");
            lblBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnPanel.add(lblBuscar);
        }
        
        txtBuscarPedido = new JTextField(20);
        txtBuscarPedido.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarPedido.setToolTipText("Buscar por ID, proveedor o estado");
        txtBuscarPedido.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarPedidos();
            }
        });
        btnPanel.add(txtBuscarPedido);
        
        // Botones
        JButton btnNuevo = new JButton("➕ Nuevo Pedido");
        btnNuevo.setBackground(COLOR_PRIMARIO);
        btnNuevo.setForeground(Color.WHITE);
        btnNuevo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnNuevo.setFocusPainted(false);
        btnNuevo.setBorderPainted(false);
        btnNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnNuevo.addActionListener(e -> mostrarDialogoNuevoPedido());
        btnPanel.add(btnNuevo);
        
        JButton btnRecibir = new JButton("📥 Recibir Pedido");
        btnRecibir.setBackground(COLOR_RECIBIR);
        btnRecibir.setForeground(Color.WHITE);
        btnRecibir.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRecibir.setFocusPainted(false);
        btnRecibir.setBorderPainted(false);
        btnRecibir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRecibir.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnRecibir.addActionListener(e -> recibirPedidoSeleccionado());
        btnPanel.add(btnRecibir);
        
        JButton btnPagar = new JButton("💰 Pagar Pedido");
        btnPagar.setBackground(COLOR_VERDE);
        btnPagar.setForeground(Color.WHITE);
        btnPagar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPagar.setFocusPainted(false);
        btnPagar.setBorderPainted(false);
        btnPagar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPagar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnPagar.addActionListener(e -> pagarPedidoSeleccionado());
        btnPanel.add(btnPagar);
        
        JButton btnEliminar = new JButton("🗑 Eliminar Pedido");
        btnEliminar.setBackground(COLOR_ROJO);
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnEliminar.setFocusPainted(false);
        btnEliminar.setBorderPainted(false);
        btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEliminar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnEliminar.addActionListener(e -> eliminarPedidoSeleccionado());
        btnPanel.add(btnEliminar);
        
        JButton btnRefresh = new JButton("🔄");
        btnRefresh.setBackground(new Color(100, 100, 100));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        btnRefresh.addActionListener(e -> cargarDatos());
        btnPanel.add(btnRefresh);
        
        headerPedidos.add(btnPanel, BorderLayout.EAST);
        panelPedidos.add(headerPedidos, BorderLayout.NORTH);
        
        String[] colsPedidos = {"ID", "Proveedor", "Productos", "Total", "Abonado", "Pendiente", "Pago", "Recepción", "Fecha"};
        tableModelPedidos = new DefaultTableModel(colsPedidos, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPedidos = new JTable(tableModelPedidos);
        sorterPedidos = new TableRowSorter<>(tableModelPedidos);
        tablaPedidos.setRowSorter(sorterPedidos);
        estilizarTabla(tablaPedidos, COLOR_PRIMARIO);
        
        // Renderer para colorear estados
        tablaPedidos.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 6 && value != null) {
                    String estado = value.toString();
                    if ("CANCELADO".equals(estado)) {
                        c.setForeground(COLOR_VERDE);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("ABONADO".equals(estado)) {
                        c.setForeground(COLOR_NARANJA);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        c.setForeground(COLOR_ROJO);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    }
                }
                if (column == 7 && value != null) {
                    String estado = value.toString();
                    if ("RECIBIDA".equals(estado)) {
                        c.setForeground(COLOR_VERDE);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("PARCIAL".equals(estado)) {
                        c.setForeground(COLOR_NARANJA);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        c.setForeground(COLOR_ROJO);
                        c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    }
                }
                return c;
            }
        });
        
        panelPedidos.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        
        // Panel inferior: Detalle del pedido seleccionado
        JPanel panelDetalles = new JPanel(new BorderLayout(5, 5));
        panelDetalles.setBackground(FONDO);
        
        JPanel headerDetalles = new JPanel(new BorderLayout());
        headerDetalles.setBackground(FONDO);
        
        JLabel lblDetalles = new JLabel("📦  Productos del Pedido");
        lblDetalles.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerDetalles.add(lblDetalles, BorderLayout.WEST);
        
        panelDetalles.add(headerDetalles, BorderLayout.NORTH);
        
        String[] colsDetalles = {"Producto", "Unidad", "Cantidad", "Precio Unit.", "Subtotal", "Sacos", "Peso/Saco", "Sueltos"};
        tableModelDetalles = new DefaultTableModel(colsDetalles, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDetalles = new JTable(tableModelDetalles);
        estilizarTabla(tablaDetalles, COLOR_NARANJA);
        panelDetalles.add(new JScrollPane(tablaDetalles), BorderLayout.CENTER);
        
        // Evento de selección de pedido para cargar detalles
        tablaPedidos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarDetallesPedido();
            }
        });
        
        split.setTopComponent(panelPedidos);
        split.setBottomComponent(panelDetalles);
        
        return split;
    }
    
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 20, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        lblTotalPedidos = new JLabel("📋 Total Pedidos: 0");
        lblTotalPedidos.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        lblTotalPendientePago = new JLabel("💰 Pendiente de Pago: $0.00");
        lblTotalPendientePago.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalPendientePago.setForeground(COLOR_ROJO);
        
        lblTotalPendienteRecepcion = new JLabel("📦 Pendiente de Recibir: 0");
        lblTotalPendienteRecepcion.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalPendienteRecepcion.setForeground(COLOR_NARANJA);
        
        panel.add(lblTotalPedidos);
        panel.add(lblTotalPendientePago);
        panel.add(lblTotalPendienteRecepcion);
        
        return panel;
    }
    
    private void estilizarTabla(JTable t, Color headerColor) {
        t.setRowHeight(28);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(headerColor);
        t.getTableHeader().setForeground(Color.WHITE);
        t.setSelectionBackground(new Color(200, 220, 240));
        t.setShowGrid(true);
        t.setGridColor(new Color(220, 225, 230));
    }
    
    private void filtrarPedidos() {
        String filtro = txtBuscarPedido.getText().trim().toLowerCase();
        if (filtro.isEmpty()) {
            sorterPedidos.setRowFilter(null);
        } else {
            sorterPedidos.setRowFilter(RowFilter.regexFilter("(?i)" + filtro, 0, 1, 6, 7));
        }
    }
    
    public void cargarDatos() {
        try {
            tableModelPedidos.setRowCount(0);
            List<Pedido> pedidos = pedidoDAO.listarTodos();
            
            int pendientesPago = 0;
            int pendientesRecepcion = 0;
            double totalPendientePago = 0;
            
            for (Pedido p : pedidos) {
                String proveedorNombre = p.getProveedor() != null ? p.getProveedor().getNombre() : "N/A";
                int numProductos = p.getDetalles().size();
                
                tableModelPedidos.addRow(new Object[]{
                    p.getId(),
                    proveedorNombre,
                    numProductos + " productos",
                    String.format("$%.2f", p.getTotalPedido()),
                    String.format("$%.2f", p.getAbonado()),
                    String.format("$%.2f", p.getSaldoPendiente()),
                    p.getEstadoPago(),
                    p.getEstadoRecepcion(),
                    p.getFechaPedido()
                });
                
                if (!"CANCELADO".equals(p.getEstadoPago())) {
                    pendientesPago++;
                    totalPendientePago += p.getSaldoPendiente();
                }
                if (!"RECIBIDA".equals(p.getEstadoRecepcion())) {
                    pendientesRecepcion++;
                }
            }
            
            lblTotalPedidos.setText("📋 Total Pedidos: " + pedidos.size());
            lblTotalPendientePago.setText("💰 Pendiente de Pago: $" + String.format("%.2f", totalPendientePago));
            lblTotalPendienteRecepcion.setText("📦 Pendiente de Recibir: " + pendientesRecepcion);
            
            txtBuscarPedido.setText("");
            filtrarPedidos();
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar pedidos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cargarDetallesPedido() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) {
            tableModelDetalles.setRowCount(0);
            return;
        }
        
        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            
            Pedido p = pedidoDAO.buscarPorId(pedidoId);
            if (p == null) {
                tableModelDetalles.setRowCount(0);
                return;
            }
            
            tableModelDetalles.setRowCount(0);
            for (DetallePedido d : p.getDetalles()) {
                tableModelDetalles.addRow(new Object[]{
                    d.getNombreProducto(),
                    d.getUnidad(),
                    String.format("%.2f", d.getCantidad()),
                    String.format("$%.4f", d.getPrecioUnitario()),
                    String.format("$%.2f", d.getSubtotal()),
                    d.getNumeroSacos() != null ? d.getNumeroSacos() : 0,
                    d.getPesoSaco() != null ? String.format("%.2f", d.getPesoSaco()) : "0.00",
                    d.getLibrasSueltas() != null ? String.format("%.2f", d.getLibrasSueltas()) : "0.00"
                });
            }
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar detalles: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ============================================================
    //  NUEVO PEDIDO
    // ============================================================
    private void mostrarDialogoNuevoPedido() {
        try {
            List<Proveedor> proveedores = proveedorDAO.listarTodos();
            if (proveedores.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, "No hay proveedores registrados.\nPrimero registre un proveedor.", "Aviso");
                return;
            }
            
            List<MateriaPrima> productos = materiaDAO.listarTodos();
            if (productos.isEmpty()) {
                DialogStyler.mostrarAdvertencia(this, "No hay productos en el inventario.\nPrimero agregue productos.", "Aviso");
                return;
            }
            
            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Nuevo Pedido",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(700, 650);
            dialog.setLocationRelativeTo(this);

            DialogStyler.aplicar(dialog, "Nuevo Pedido a Proveedor", "📋");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();
            gbc.insets = new Insets(4, 8, 4, 8);

            int fila = 0;

            // ===== PROVEEDOR =====
            JComboBox<Proveedor> cmbProveedor = new JComboBox<>(proveedores.toArray(new Proveedor[0]));
            cmbProveedor.setFont(DialogStyler.FUENTE_CAMPO);
            
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Proveedor:*"), gbc);
            gbc.gridx = 1;
            form.add(cmbProveedor, gbc);
            fila++;

            // ===== TABLA DE PRODUCTOS =====
            String[] colsProductos = {"Producto", "Unidad", "Cantidad", "Precio Unit.", "Subtotal", "Sacos", "Peso/Saco", "Sueltos"};
            DefaultTableModel modelProductos = new DefaultTableModel(colsProductos, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable tablaProductos = new JTable(modelProductos);
            tablaProductos.setRowHeight(26);
            tablaProductos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
            
            JScrollPane scrollProductos = new JScrollPane(tablaProductos);
            scrollProductos.setPreferredSize(new Dimension(640, 180));
            
            gbc.gridx = 0; gbc.gridy = fila;
            gbc.gridwidth = 2;
            form.add(scrollProductos, gbc);
            gbc.gridwidth = 1;
            fila++;

            // ===== PANEL DE AGREGAR PRODUCTO =====
            JPanel panelAgregar = new JPanel(new GridBagLayout());
            panelAgregar.setBackground(DialogStyler.FONDO_FORM);
            GridBagConstraints gbcAgregar = new GridBagConstraints();
            gbcAgregar.insets = new Insets(3, 5, 3, 5);
            gbcAgregar.anchor = GridBagConstraints.WEST;
            gbcAgregar.fill = GridBagConstraints.HORIZONTAL;

            JComboBox<MateriaPrima> cmbProducto = new JComboBox<>(productos.toArray(new MateriaPrima[0]));
            cmbProducto.setFont(DialogStyler.FUENTE_CAMPO);
            cmbProducto.setPreferredSize(new Dimension(150, 28));
            
            // ===== BOTÓN "AGREGAR NUEVO PRODUCTO" =====
            JButton btnNuevoProducto = new JButton("➕ Nuevo");
            btnNuevoProducto.setBackground(COLOR_PRIMARIO);
            btnNuevoProducto.setForeground(Color.WHITE);
            btnNuevoProducto.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btnNuevoProducto.setFocusPainted(false);
            btnNuevoProducto.setBorderPainted(false);
            btnNuevoProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnNuevoProducto.addActionListener(e -> {
                MateriaPrima nuevo = mostrarDialogoCrearProductoDesdePedido(dialog);
                if (nuevo != null) {
                    cmbProducto.addItem(nuevo);
                    cmbProducto.setSelectedItem(nuevo);
                }
            });

            JTextField txtCantidad = new JTextField(8);
            txtCantidad.setFont(DialogStyler.FUENTE_CAMPO);
            txtCantidad.setText("0");
            txtCantidad.setPreferredSize(new Dimension(80, 28));
            
            JTextField txtPrecio = new JTextField(8);
            txtPrecio.setFont(DialogStyler.FUENTE_CAMPO);
            txtPrecio.setText("0");
            txtPrecio.setPreferredSize(new Dimension(80, 28));
            
            JTextField txtSacos = new JTextField(6);
            txtSacos.setFont(DialogStyler.FUENTE_CAMPO);
            txtSacos.setText("0");
            txtSacos.setPreferredSize(new Dimension(60, 28));
            
            JTextField txtPesoSaco = new JTextField(6);
            txtPesoSaco.setFont(DialogStyler.FUENTE_CAMPO);
            txtPesoSaco.setText("0");
            txtPesoSaco.setPreferredSize(new Dimension(60, 28));
            
            JTextField txtSueltos = new JTextField(6);
            txtSueltos.setFont(DialogStyler.FUENTE_CAMPO);
            txtSueltos.setText("0");
            txtSueltos.setPreferredSize(new Dimension(60, 28));

            JButton btnAgregarProducto = new JButton("➕ Agregar");
            btnAgregarProducto.setBackground(COLOR_VERDE);
            btnAgregarProducto.setForeground(Color.WHITE);
            btnAgregarProducto.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btnAgregarProducto.setFocusPainted(false);
            btnAgregarProducto.setBorderPainted(false);
            btnAgregarProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAgregarProducto.addActionListener(e -> {
                try {
                    MateriaPrima mp = (MateriaPrima) cmbProducto.getSelectedItem();
                    if (mp == null) {
                        DialogStyler.mostrarError(dialog, "Seleccione un producto.");
                        return;
                    }
                    double cantidad = Double.parseDouble(txtCantidad.getText().trim().replace(",", "."));
                    double precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
                    int sacos = Integer.parseInt(txtSacos.getText().trim());
                    double pesoSaco = Double.parseDouble(txtPesoSaco.getText().trim().replace(",", "."));
                    double sueltos = Double.parseDouble(txtSueltos.getText().trim().replace(",", "."));
                    
                    if (cantidad <= 0) {
                        DialogStyler.mostrarError(dialog, "La cantidad debe ser mayor a 0.");
                        return;
                    }
                    if (precio <= 0) {
                        DialogStyler.mostrarError(dialog, "El precio debe ser mayor a 0.");
                        return;
                    }
                    
                    double subtotal = cantidad * precio;
                    
                    modelProductos.addRow(new Object[]{
                        mp.getNombre(),
                        mp.getUnidad(),
                        String.format("%.2f", cantidad),
                        String.format("$%.4f", precio),
                        String.format("$%.2f", subtotal),
                        sacos,
                        String.format("%.2f", pesoSaco),
                        String.format("%.2f", sueltos)
                    });
                    
                    txtCantidad.setText("0");
                    txtPrecio.setText("0");
                    txtSacos.setText("0");
                    txtPesoSaco.setText("0");
                    txtSueltos.setText("0");
                    
                } catch (NumberFormatException ex) {
                    DialogStyler.mostrarError(dialog, "Ingrese valores numéricos válidos.");
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                }
            });

            JButton btnQuitarProducto = new JButton("🗑 Quitar");
            btnQuitarProducto.setBackground(COLOR_ROJO);
            btnQuitarProducto.setForeground(Color.WHITE);
            btnQuitarProducto.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btnQuitarProducto.setFocusPainted(false);
            btnQuitarProducto.setBorderPainted(false);
            btnQuitarProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnQuitarProducto.addActionListener(e -> {
                int row = tablaProductos.getSelectedRow();
                if (row < 0) {
                    DialogStyler.mostrarAdvertencia(dialog, "Seleccione un producto para quitar.", "Aviso");
                    return;
                }
                modelProductos.removeRow(row);
            });

            // Layout del panel de agregar
            gbcAgregar.gridx = 0; gbcAgregar.gridy = 0;
            panelAgregar.add(new JLabel("Producto:"), gbcAgregar);
            gbcAgregar.gridx = 1;
            panelAgregar.add(cmbProducto, gbcAgregar);
            gbcAgregar.gridx = 2;
            panelAgregar.add(btnNuevoProducto, gbcAgregar);
            
            gbcAgregar.gridx = 3; gbcAgregar.gridy = 0;
            panelAgregar.add(new JLabel("Cant:"), gbcAgregar);
            gbcAgregar.gridx = 4;
            panelAgregar.add(txtCantidad, gbcAgregar);
            
            gbcAgregar.gridx = 5;
            panelAgregar.add(new JLabel("Precio:"), gbcAgregar);
            gbcAgregar.gridx = 6;
            panelAgregar.add(txtPrecio, gbcAgregar);
            
            gbcAgregar.gridx = 7;
            panelAgregar.add(new JLabel("Sacos:"), gbcAgregar);
            gbcAgregar.gridx = 8;
            panelAgregar.add(txtSacos, gbcAgregar);
            
            gbcAgregar.gridx = 9;
            panelAgregar.add(new JLabel("Peso:"), gbcAgregar);
            gbcAgregar.gridx = 10;
            panelAgregar.add(txtPesoSaco, gbcAgregar);
            
            gbcAgregar.gridx = 11;
            panelAgregar.add(new JLabel("Sueltos:"), gbcAgregar);
            gbcAgregar.gridx = 12;
            panelAgregar.add(txtSueltos, gbcAgregar);
            
            gbcAgregar.gridx = 13;
            panelAgregar.add(btnAgregarProducto, gbcAgregar);
            
            gbcAgregar.gridx = 14;
            panelAgregar.add(btnQuitarProducto, gbcAgregar);

            gbc.gridx = 0; gbc.gridy = fila;
            gbc.gridwidth = 2;
            form.add(panelAgregar, gbc);
            gbc.gridwidth = 1;
            fila++;

            // ===== CHECKBOX =====
            JCheckBox chkAgregarInventario = new JCheckBox("✅ Agregar productos al inventario al recibir el pedido", true);
            chkAgregarInventario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            chkAgregarInventario.setBackground(DialogStyler.FONDO_FORM);

            gbc.gridx = 0; gbc.gridy = fila;
            gbc.gridwidth = 2;
            form.add(chkAgregarInventario, gbc);
            gbc.gridwidth = 1;
            fila++;

            // ============================================================
            //  BOTONES
            // ============================================================
            JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Pedido");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(e -> {
                try {
                    if (modelProductos.getRowCount() == 0) {
                        DialogStyler.mostrarError(dialog, "Agregue al menos un producto al pedido.");
                        return;
                    }
                    
                    Proveedor proveedor = (Proveedor) cmbProveedor.getSelectedItem();
                    if (proveedor == null) {
                        DialogStyler.mostrarError(dialog, "Seleccione un proveedor.");
                        return;
                    }
                    
                    Pedido pedido = new Pedido();
                    pedido.setProveedorId(proveedor.getId());
                    pedido.setEstadoRecepcion(chkAgregarInventario.isSelected() ? "PENDIENTE" : "PENDIENTE");
                    pedido.setEstadoPago("PENDIENTE");
                    pedido.setNotas("");
                    
                    for (int i = 0; i < modelProductos.getRowCount(); i++) {
                        String nombre = (String) modelProductos.getValueAt(i, 0);
                        String unidad = (String) modelProductos.getValueAt(i, 1);
                        double cantidad = Double.parseDouble(((String) modelProductos.getValueAt(i, 2)).replace(",", "."));
                        double precio = Double.parseDouble(((String) modelProductos.getValueAt(i, 3)).replace("$", "").replace(",", "."));
                        double subtotal = Double.parseDouble(((String) modelProductos.getValueAt(i, 4)).replace("$", "").replace(",", "."));
                        int sacos = Integer.parseInt((String) modelProductos.getValueAt(i, 5));
                        double pesoSaco = Double.parseDouble(((String) modelProductos.getValueAt(i, 6)).replace(",", "."));
                        double sueltos = Double.parseDouble(((String) modelProductos.getValueAt(i, 7)).replace(",", "."));
                        
                        // Buscar materia prima por nombre
                        MateriaPrima mp = materiaDAO.buscarPorNombre(nombre);
                        if (mp == null) {
                            DialogStyler.mostrarError(dialog, "Producto '" + nombre + "' no encontrado en el inventario.");
                            return;
                        }
                        
                        DetallePedido detalle = new DetallePedido();
                        detalle.setMateriaPrimaId(mp.getId());
                        detalle.setNombreProducto(nombre);
                        detalle.setUnidad(unidad);
                        detalle.setCantidad(cantidad);
                        detalle.setPrecioUnitario(precio);
                        detalle.setSubtotal(subtotal);
                        detalle.setNumeroSacos(sacos > 0 ? sacos : null);
                        detalle.setPesoSaco(pesoSaco > 0 ? pesoSaco : null);
                        detalle.setLibrasSueltas(sueltos > 0 ? sueltos : null);
                        pedido.addDetalle(detalle);
                    }
                    
                    pedidoDAO.guardar(pedido);
                    
                    // Si el checkbox está marcado, recibir automáticamente
                    if (chkAgregarInventario.isSelected()) {
                        pedidoDAO.recibirPedido(pedido.getId());
                    }
                    
                    dialog.dispose();
                    cargarDatos();
                    
                    Window window = SwingUtilities.getWindowAncestor(PedidosPanel.this);
                    if (window instanceof MainFrame) {
                        ((MainFrame) window).refrescarInventario();
                        ((MainFrame) window).refrescarDashboard();
                    }
                    
                    DialogStyler.mostrarExito(this,
                        "✅ Pedido #" + pedido.getId() + " guardado correctamente.\n" +
                        "Productos: " + modelProductos.getRowCount() + "\n" +
                        "Total: $" + String.format("%.2f", pedido.getTotalPedido()) +
                        (chkAgregarInventario.isSelected() ? "\n\n📦 Productos agregados al inventario automáticamente." : ""),
                        "Pedido Guardado"
                    );
                    
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error al guardar pedido: " + ex.getMessage());
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
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // ============================================================
    //  CREAR PRODUCTO DESDE PEDIDO
    // ============================================================
    private MateriaPrima mostrarDialogoCrearProductoDesdePedido(JDialog parent) {
        JDialog dialog = new JDialog(
            parent,
            "Agregar Nuevo Producto",
            Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(parent);

        DialogStyler.aplicar(dialog, "Agregar Nuevo Producto", "➕");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

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

        JPanel bannerPrecio = DialogStyler.crearBannerInfo("info", 
            "Ingrese peso y precio para calcular el costo por unidad."
        );

        Runnable actualizarPreview = () -> {
            try {
                String pesoStr = txtPesoEnvase.getText().trim().replace(",", ".");
                String precStr = txtPrecioEnvase.getText().trim().replace(",", ".");
                String sueltStr = txtCantidadSueltas.getText().trim().replace(",", ".");

                if (!pesoStr.isEmpty() && !precStr.isEmpty()) {
                    double peso = Double.parseDouble(pesoStr);
                    double precio = Double.parseDouble(precStr);
                    if (peso > 0) {
                        double precioU = precio / peso;
                        String msg = String.format("Precio por unidad: <b>$%.4f</b>", precioU);
                        if (!sueltStr.isEmpty()) {
                            double sueltos = Double.parseDouble(sueltStr);
                            if (sueltos > 0) {
                                msg += String.format(" &nbsp;|&nbsp; Costo sueltos: <b>$%.2f</b>", sueltos * precioU);
                            }
                        }
                        ((JLabel) bannerPrecio.getComponent(0)).setText("<html>✅ " + msg + "</html>");
                    } else {
                        ((JLabel) bannerPrecio.getComponent(0)).setText("<html>⚠️ El peso debe ser mayor a 0</html>");
                    }
                } else {
                    ((JLabel) bannerPrecio.getComponent(0)).setText("<html>ℹ️ Ingrese peso y precio para calcular el costo por unidad.</html>");
                }
                bannerPrecio.repaint();
            } catch (NumberFormatException ex) {
                ((JLabel) bannerPrecio.getComponent(0)).setText("<html>⚠️ Ingrese valores numéricos válidos</html>");
                bannerPrecio.repaint();
            }
        };

        cmbUnidad.addActionListener(e -> {
            boolean esLitro = "litro".equals(cmbUnidad.getSelectedItem());
            lblPesoEnvase.setText(esLitro ? "Litros por envase:" : "Peso por saco (libras):");
            lblCantidadSueltas.setText(esLitro ? "Litros sueltos:" : "Libras sueltas:");
            lblPrecioEnvase.setText(esLitro ? "Precio por envase ($):" : "Precio por saco ($):");
            actualizarPreview.run();
        });

        javax.swing.event.DocumentListener previewListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarPreview.run(); }
        };
        txtPesoEnvase.getDocument().addDocumentListener(previewListener);
        txtPrecioEnvase.getDocument().addDocumentListener(previewListener);
        txtCantidadSueltas.getDocument().addDocumentListener(previewListener);

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Datos del producto"), gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(DialogStyler.crearLabel("Nombre del producto:"), gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(DialogStyler.crearLabel("Unidad:"), gbc);
        gbc.gridx = 1;
        form.add(cmbUnidad, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Inventario inicial"), gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(DialogStyler.crearLabel("Número de sacos/envases:"), gbc);
        gbc.gridx = 1;
        form.add(txtNumSacos, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(lblPesoEnvase, gbc);
        gbc.gridx = 1;
        form.add(txtPesoEnvase, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(lblCantidadSueltas, gbc);
        gbc.gridx = 1;
        form.add(txtCantidadSueltas, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Precio"), gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(lblPrecioEnvase, gbc);
        gbc.gridx = 1;
        form.add(txtPrecioEnvase, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        form.add(bannerPrecio, gbc);
        gbc.gridwidth = 1;

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Producto");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");
        
        final MateriaPrima[] productoGuardado = {null};

        btnGuardar.addActionListener(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                DialogStyler.mostrarError(dialog, "El nombre del producto no puede estar vacío.");
                txtNombre.requestFocus();
                return;
            }
            try {
                String unidad = (String) cmbUnidad.getSelectedItem();
                int numEnvases = Integer.parseInt(txtNumSacos.getText().trim());
                double pesoEnvase = Double.parseDouble(txtPesoEnvase.getText().trim().replace(",", "."));
                double cantidadSuelta = Double.parseDouble(txtCantidadSueltas.getText().trim().replace(",", "."));
                double precioEnvase = Double.parseDouble(txtPrecioEnvase.getText().trim().replace(",", "."));

                if (numEnvases == 0 && cantidadSuelta == 0) {
                    DialogStyler.mostrarError(dialog, "Debe agregar al menos un envase/saco o cantidad suelta.");
                    return;
                }

                double cantidadTotal = (numEnvases * pesoEnvase) + cantidadSuelta;
                double precioPorUnidad = pesoEnvase > 0 ? precioEnvase / pesoEnvase : 0;
                double precioTotal = (numEnvases * precioEnvase) + (cantidadSuelta * precioPorUnidad);

                String resumen =
                    "<b>Producto:</b> " + nombre + "<br>" +
                    "<b>Unidad:</b> " + unidad + "<br>" +
                    "<b>Cantidad total:</b> " + String.format("%.2f", cantidadTotal) + " " + unidad + "<br>" +
                    "<b>Precio por unidad:</b> $" + String.format("%.4f", precioPorUnidad) + "<br>" +
                    "<b>Precio total:</b> $" + String.format("%.2f", precioTotal);

                int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "<html><body style='font-family:Segoe UI;font-size:13px;padding:8px;'>" + resumen + "</body></html>",
                    "Confirmar Guardado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) return;

                btnGuardar.setEnabled(false);
                btnGuardar.setText("Guardando…");

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

                materiaDAO.guardarOActualizar(mp);
                productoGuardado[0] = mp;
                
                dialog.dispose();

            } catch (NumberFormatException ex) {
                DialogStyler.mostrarError(dialog, "Ingrese valores numéricos válidos.<br>Use punto (.) como separador decimal.");
            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error al guardar: " + ex.getMessage());
                ex.printStackTrace();
                btnGuardar.setEnabled(true);
                btnGuardar.setText("💾 Guardar Producto");
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) { txtNombre.requestFocusInWindow(); }
        });

        dialog.setVisible(true);

        return productoGuardado[0];
    }

    // ============================================================
    //  RECIBIR PEDIDO
    // ============================================================
    private void recibirPedidoSeleccionado() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un pedido para recibir.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            String estadoRecepcion = (String) tableModelPedidos.getValueAt(modelRow, 7);
            
            if ("RECIBIDA".equals(estadoRecepcion)) {
                DialogStyler.mostrarAdvertencia(this, "Este pedido ya fue recibido.", "Aviso");
                return;
            }
            
            Pedido p = pedidoDAO.buscarPorId(pedidoId);
            if (p == null) {
                DialogStyler.mostrarError(this, "Pedido no encontrado.");
                return;
            }
            
            StringBuilder productos = new StringBuilder();
            for (DetallePedido d : p.getDetalles()) {
                productos.append("• ").append(d.getNombreProducto())
                         .append(": ").append(String.format("%.2f", d.getCantidad()))
                         .append(" ").append(d.getUnidad()).append("\n");
            }
            
            String mensaje = "📥 ¿RECIBIR PEDIDO #" + pedidoId + "?\n\n" +
                "Proveedor: " + p.getProveedor().getNombre() + "\n" +
                "Productos a recibir:\n" +
                productos.toString() +
                "\nTotal: $" + String.format("%.2f", p.getTotalPedido()) + "\n\n" +
                "Los productos se agregarán al inventario.\n" +
                "¿Está seguro?";
            
            if (!DialogStyler.confirmar(this, mensaje, "Recibir pedido")) return;
            
            pedidoDAO.recibirPedido(pedidoId);
            cargarDatos();
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarInventario();
                ((MainFrame) window).refrescarDashboard();
            }
            
            DialogStyler.mostrarExito(this, 
                "✅ Pedido #" + pedidoId + " recibido correctamente.\nProductos agregados al inventario.", 
                "Éxito");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al recibir pedido: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ============================================================
    //  PAGAR PEDIDO
    // ============================================================
    private void pagarPedidoSeleccionado() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un pedido para pagar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            String estadoPago = (String) tableModelPedidos.getValueAt(modelRow, 6);
            double pendiente = Double.parseDouble(((String) tableModelPedidos.getValueAt(modelRow, 5)).replace("$", "").replace(",", "."));
            
            if ("CANCELADO".equals(estadoPago)) {
                DialogStyler.mostrarAdvertencia(this, "Este pedido ya está pagado.", "Aviso");
                return;
            }
            
            if (pendiente <= 0) {
                DialogStyler.mostrarAdvertencia(this, "Este pedido no tiene saldo pendiente.", "Aviso");
                return;
            }
            
            String input = JOptionPane.showInputDialog(this,
                "💰 PAGAR PEDIDO #" + pedidoId + "\n\n" +
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
            
            double monto = Double.parseDouble(inputClean);
            if (monto <= 0) {
                DialogStyler.mostrarError(this, "El monto debe ser mayor a 0.");
                return;
            }
            
            if (monto > pendiente) {
                String mensaje = "⚠️ El monto a pagar ($" + String.format("%.2f", monto) + ") es mayor al saldo pendiente ($" + 
                    String.format("%.2f", pendiente) + ").\n\n" +
                    "¿Desea cancelar la deuda con este pago?";
                if (!DialogStyler.confirmar(this, mensaje, "Confirmar pago")) return;
                monto = pendiente;
            }
            
            pedidoDAO.registrarAbono(pedidoId, monto);
            cargarDatos();
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
            }
            
            DialogStyler.mostrarExito(this,
                "✅ Pago registrado correctamente.\n\n" +
                "Monto pagado: $" + String.format("%.2f", monto) + "\n" +
                "Saldo restante: $" + String.format("%.2f", pendiente - monto),
                "Pago registrado"
            );
            
        } catch (NumberFormatException ex) {
            DialogStyler.mostrarError(this, "Ingrese un valor numérico válido.");
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al registrar pago: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ============================================================
    //  ELIMINAR PEDIDO (CON DEVOLUCIÓN DE STOCK)
    // ============================================================
    private void eliminarPedidoSeleccionado() {
        int row = tablaPedidos.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un pedido para eliminar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tablaPedidos.convertRowIndexToModel(row);
            int pedidoId = (int) tableModelPedidos.getValueAt(modelRow, 0);
            String proveedor = (String) tableModelPedidos.getValueAt(modelRow, 1);
            String estadoPago = (String) tableModelPedidos.getValueAt(modelRow, 6);
            String estadoRecepcion = (String) tableModelPedidos.getValueAt(modelRow, 7);
            double total = Double.parseDouble(((String) tableModelPedidos.getValueAt(modelRow, 3)).replace("$", "").replace(",", "."));
            
            // Verificar si tiene pagos registrados
            if (!"PENDIENTE".equals(estadoPago)) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR: Este pedido tiene pagos registrados.\n" +
                    "Primero debe revertir los pagos en la sección de abonos.");
                return;
            }
            
            Pedido p = pedidoDAO.buscarPorId(pedidoId);
            if (p == null) {
                DialogStyler.mostrarError(this, "Pedido no encontrado.");
                return;
            }
            
            StringBuilder productosMsg = new StringBuilder();
            if ("RECIBIDA".equals(estadoRecepcion)) {
                productosMsg.append("⚠️ Este pedido YA FUE RECIBIDO\n\n");
                productosMsg.append("Los siguientes productos serán ELIMINADOS DEL INVENTARIO:\n\n");
                for (DetallePedido d : p.getDetalles()) {
                    productosMsg.append("• ")
                        .append(d.getNombreProducto())
                        .append(": ")
                        .append(String.format("%.2f", d.getCantidad()))
                        .append(" ").append(d.getUnidad())
                        .append(" (subtotal: $").append(String.format("%.2f", d.getSubtotal())).append(")\n");
                }
            } else {
                productosMsg.append("📦 Los productos NO han sido recibidos.\n");
                productosMsg.append("Solo se eliminará el pedido, sin afectar el inventario.\n\n");
                for (DetallePedido d : p.getDetalles()) {
                    productosMsg.append("• ")
                        .append(d.getNombreProducto())
                        .append(": ").append(String.format("%.2f", d.getCantidad()))
                        .append(" ").append(d.getUnidad()).append("\n");
                }
            }
            
            String mensaje = "⚠️ ¿ELIMINAR PEDIDO #" + pedidoId + "?\n\n" +
                "Proveedor: " + proveedor + "\n" +
                "Total: $" + String.format("%.2f", total) + "\n" +
                "Estado: " + estadoPago + " | " + estadoRecepcion + "\n\n" +
                productosMsg.toString() +
                "\nEsta acción NO se puede deshacer.\n\n" +
                "¿Está seguro?";
            
            if (!DialogStyler.confirmar(this, mensaje, "Eliminar pedido")) return;
            
            // Si el pedido fue recibido, eliminar los productos del inventario
            if ("RECIBIDA".equals(estadoRecepcion)) {
                for (DetallePedido d : p.getDetalles()) {
                    MateriaPrima mp = materiaDAO.buscarPorId(d.getMateriaPrimaId());
                    if (mp != null) {
                        double nuevoDisponible = mp.getCantidadDisponible() - d.getCantidad();
                        if (nuevoDisponible < 0) nuevoDisponible = 0;
                        materiaDAO.actualizarDisponible(mp.getId(), nuevoDisponible, 
                            "Eliminación de pedido #" + pedidoId + " - " + d.getNombreProducto());
                        System.out.println("📦 Devolviendo stock: " + d.getNombreProducto() + 
                            " | " + String.format("%.2f", d.getCantidad()) + " " + d.getUnidad());
                    }
                }
                DialogStyler.mostrarExito(this, 
                    "✅ Stock devuelto al inventario correctamente.", 
                    "Stock devuelto");
            }
            
            pedidoDAO.eliminar(pedidoId);
            cargarDatos();
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarInventario();
                ((MainFrame) window).refrescarDashboard();
            }
            
            DialogStyler.mostrarExito(this, 
                "✅ Pedido #" + pedidoId + " eliminado correctamente.", 
                "Pedido eliminado");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar pedido: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}