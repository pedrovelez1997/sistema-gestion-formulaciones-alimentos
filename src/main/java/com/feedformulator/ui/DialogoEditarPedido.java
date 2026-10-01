package com.feedformulator.ui;

import com.feedformulator.dao.DatabaseManager;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.dao.PedidoDAO;
import com.feedformulator.model.DetallePedido;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.Pedido;
import com.feedformulator.util.LoggerUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DialogoEditarPedido extends JDialog {

    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();

    private final Pedido pedidoOriginal;
    private DefaultTableModel modelDetalles;
    private JTable tablaDetalles;
    private JLabel lblTotal;
    private JLabel lblProductos;
    private JLabel lblEstadoRecepcion;
    private JLabel lblEstadoPago;

    private List<DetallePedido> detallesOriginales;
    private List<DetallePedido> detallesEditados;

    private static final Color COLOR_VERDE = new Color(34, 139, 34);
    private static final Color COLOR_ROJO = new Color(180, 40, 40);
    private static final Color COLOR_NARANJA = new Color(230, 126, 34);
    private static final Color COLOR_AZUL = new Color(41, 128, 185);

    public DialogoEditarPedido(JFrame parent, Pedido pedido) {
        super(parent, "Editar Pedido #" + pedido.getId(), ModalityType.APPLICATION_MODAL);
        this.pedidoOriginal = pedido;
        this.detallesOriginales = new ArrayList<>(pedido.getDetalles());
        this.detallesEditados = new ArrayList<>();

        setSize(950, 700);
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(850, 600));

        iniciarComponentes();
        cargarDetalles();
    }

    private void iniciarComponentes() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 245, 248));

        // ===== PANEL SUPERIOR: INFORMACIÓN DEL PEDIDO =====
        JPanel panelInfo = new JPanel(new GridBagLayout());
        panelInfo.setBackground(Color.WHITE);
        panelInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 10, 3, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panelInfo.add(new JLabel("📋 Pedido #" + pedidoOriginal.getId()), gbc);

        gbc.gridx = 1;
        JLabel lblNombre = new JLabel("Nombre: " + (pedidoOriginal.getNotas() != null ? pedidoOriginal.getNotas() : "Sin nombre"));
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panelInfo.add(lblNombre, gbc);

        gbc.gridx = 2;
        panelInfo.add(new JLabel("Proveedor: " + pedidoOriginal.getProveedor().getNombre()), gbc);

        gbc.gridx = 3;
        panelInfo.add(new JLabel("Fecha: " + pedidoOriginal.getFechaPedido()), gbc);

        gbc.gridx = 4;
        lblEstadoPago = new JLabel("Pago: " + pedidoOriginal.getEstadoPago());
        lblEstadoPago.setForeground(getColorEstado(pedidoOriginal.getEstadoPago()));
        lblEstadoPago.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelInfo.add(lblEstadoPago, gbc);

        gbc.gridx = 5;
        lblEstadoRecepcion = new JLabel("Recepción: " + pedidoOriginal.getEstadoRecepcion());
        lblEstadoRecepcion.setForeground(getColorEstadoRecepcion(pedidoOriginal.getEstadoRecepcion()));
        lblEstadoRecepcion.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panelInfo.add(lblEstadoRecepcion, gbc);

        add(panelInfo, BorderLayout.NORTH);

        // ===== PANEL CENTRAL: TABLA DE DETALLES =====
        String[] columnas = {"ID", "Producto", "Unidad", "Cantidad", "Precio Unit.", "Subtotal", "Sacos", "Peso/Saco", "Sueltos"};
        modelDetalles = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaDetalles = new JTable(modelDetalles);
        tablaDetalles.setRowHeight(30);
        tablaDetalles.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaDetalles.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaDetalles.getTableHeader().setBackground(COLOR_AZUL);
        tablaDetalles.getTableHeader().setForeground(Color.WHITE);
        tablaDetalles.getTableHeader().setReorderingAllowed(false);

        tablaDetalles.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaDetalles.getColumnModel().getColumn(1).setPreferredWidth(150);
        tablaDetalles.getColumnModel().getColumn(2).setMaxWidth(70);
        tablaDetalles.getColumnModel().getColumn(3).setPreferredWidth(100);
        tablaDetalles.getColumnModel().getColumn(4).setPreferredWidth(100);
        tablaDetalles.getColumnModel().getColumn(5).setPreferredWidth(100);
        tablaDetalles.getColumnModel().getColumn(6).setMaxWidth(70);
        tablaDetalles.getColumnModel().getColumn(7).setMaxWidth(100);
        tablaDetalles.getColumnModel().getColumn(8).setMaxWidth(100);

        tablaDetalles.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 5 && value != null) {
                    String str = value.toString();
                    if (str.contains("-") || str.startsWith("-$")) {
                        c.setForeground(COLOR_ROJO);
                    } else {
                        c.setForeground(COLOR_VERDE);
                    }
                }
                if (column == 0 && value != null) {
                    c.setForeground(new Color(100, 100, 100));
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                }
                return c;
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaDetalles);
        scrollTabla.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        add(scrollTabla, BorderLayout.CENTER);

        tablaDetalles.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editarFilaSeleccionada();
                }
            }
        });

        // ===== PANEL INFERIOR: RESUMEN Y BOTONES =====
        JPanel panelInferior = new JPanel(new BorderLayout(10, 10));
        panelInferior.setBackground(Color.WHITE);
        panelInferior.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel panelResumen = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        panelResumen.setBackground(Color.WHITE);

        lblTotal = new JLabel("💰 Total: $0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotal.setForeground(new Color(20, 100, 20));
        panelResumen.add(lblTotal);

        lblProductos = new JLabel("📦 Productos: 0");
        lblProductos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panelResumen.add(lblProductos);

        panelInferior.add(panelResumen, BorderLayout.WEST);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panelBotones.setBackground(Color.WHITE);

        JButton btnEditarFila = crearBoton("✏️ Editar Fila", COLOR_AZUL);
        JButton btnAgregar = crearBoton("➕ Agregar Producto", new Color(0, 150, 100));
        JButton btnEliminar = crearBoton("🗑 Eliminar Producto", COLOR_ROJO);
        JButton btnGuardar = crearBoton("💾 Guardar Cambios", new Color(34, 139, 34));
        JButton btnCancelar = crearBoton("❌ Cancelar", new Color(150, 150, 150));

        btnEditarFila.addActionListener(e -> editarFilaSeleccionada());
        btnAgregar.addActionListener(e -> agregarProducto());
        btnEliminar.addActionListener(e -> eliminarProducto());
        btnGuardar.addActionListener(e -> guardarCambios());
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnEditarFila);
        panelBotones.add(btnAgregar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        panelInferior.add(panelBotones, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);

        actualizarTotal();
    }

    private JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return btn;
    }

    private Color getColorEstado(String estado) {
        if ("CANCELADO".equals(estado)) return COLOR_VERDE;
        if ("ABONADO".equals(estado)) return COLOR_NARANJA;
        return COLOR_ROJO;
    }

    private Color getColorEstadoRecepcion(String estado) {
        if ("RECIBIDA".equals(estado)) return COLOR_VERDE;
        if ("PARCIAL".equals(estado)) return COLOR_NARANJA;
        return COLOR_ROJO;
    }

    private void cargarDetalles() {
        modelDetalles.setRowCount(0);
        detallesEditados.clear();
        for (DetallePedido d : detallesOriginales) {
            agregarFila(d);
            detallesEditados.add(clonarDetalle(d));
        }
        actualizarTotal();
    }

    private void agregarFila(DetallePedido d) {
        modelDetalles.addRow(new Object[]{
            d.getId(),
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

    private DetallePedido clonarDetalle(DetallePedido original) {
        DetallePedido copia = new DetallePedido();
        copia.setId(original.getId());
        copia.setPedidoId(original.getPedidoId());
        copia.setMateriaPrimaId(original.getMateriaPrimaId());
        copia.setNombreProducto(original.getNombreProducto());
        copia.setUnidad(original.getUnidad());
        copia.setCantidad(original.getCantidad());
        copia.setPrecioUnitario(original.getPrecioUnitario());
        copia.setSubtotal(original.getSubtotal());
        copia.setNumeroSacos(original.getNumeroSacos());
        copia.setPesoSaco(original.getPesoSaco());
        copia.setLibrasSueltas(original.getLibrasSueltas());
        return copia;
    }

    private void actualizarTotal() {
        double total = 0;
        int filas = modelDetalles.getRowCount();
        for (int i = 0; i < filas; i++) {
            try {
                Object val = modelDetalles.getValueAt(i, 5);
                if (val != null) {
                    String str = val.toString().replace("$", "").replace(",", ".");
                    total += Double.parseDouble(str);
                }
            } catch (NumberFormatException e) {
                // Ignorar
            }
        }
        lblTotal.setText("💰 Total: $" + String.format("%.2f", total));
        lblProductos.setText("📦 Productos: " + filas);
    }

    // ============================================================
    //  EDITAR FILA SELECCIONADA
    // ============================================================
    private void editarFilaSeleccionada() {
    int row = tablaDetalles.getSelectedRow();
    if (row < 0) {
        DialogStyler.mostrarAdvertencia(this, "Seleccione una fila para editar.", "Aviso");
        return;
    }

    try {
        String nombreProducto = (String) modelDetalles.getValueAt(row, 1);
        String unidad = (String) modelDetalles.getValueAt(row, 2);
        double cantidadActual = Double.parseDouble(modelDetalles.getValueAt(row, 3).toString().replace(",", "."));
        double precioUnitarioActual = Double.parseDouble(modelDetalles.getValueAt(row, 4).toString().replace("$", "").replace(",", "."));
        int sacosActual = Integer.parseInt(modelDetalles.getValueAt(row, 6).toString());
        double pesoSacoActual = Double.parseDouble(modelDetalles.getValueAt(row, 7).toString().replace(",", "."));
        double sueltosActual = Double.parseDouble(modelDetalles.getValueAt(row, 8).toString().replace(",", "."));

        // ============================================================
        //  CALCULAR EL PRECIO POR SACO (lo que el usuario ingresó originalmente)
        // ============================================================
        double precioPorSacoActual = 0;
        if (sacosActual > 0) {
            double subtotalActual = Double.parseDouble(modelDetalles.getValueAt(row, 5).toString().replace("$", "").replace(",", "."));
            precioPorSacoActual = subtotalActual / sacosActual;
        } else if (sueltosActual > 0 && pesoSacoActual > 0) {
            precioPorSacoActual = precioUnitarioActual * pesoSacoActual;
        }

        // ============================================================
        //  CREAR DIÁLOGO DE EDICIÓN
        // ============================================================
        JDialog dialog = new JDialog(this, "Editar Producto: " + nombreProducto, ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 500);
        dialog.setLocationRelativeTo(this);

        DialogStyler.aplicar(dialog, "Editar Producto: " + nombreProducto, "✏️");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

        // ============================================================
        //  CAMPOS DEL FORMULARIO
        // ============================================================
        
        JTextField txtProducto = DialogStyler.crearCampo(18);
        txtProducto.setText(nombreProducto);
        txtProducto.setEditable(false);
        txtProducto.setBackground(new Color(245, 245, 245));
        
        JTextField txtUnidad = DialogStyler.crearCampo(10);
        txtUnidad.setText(unidad);
        txtUnidad.setEditable(false);
        txtUnidad.setBackground(new Color(245, 245, 245));

        JTextField txtSacos = DialogStyler.crearCampoNumerico(10);
        txtSacos.setText(String.valueOf(sacosActual));

        JTextField txtPesoSaco = DialogStyler.crearCampoNumerico(10);
        txtPesoSaco.setText(String.format("%.2f", pesoSacoActual));

        JTextField txtSueltos = DialogStyler.crearCampoNumerico(10);
        txtSueltos.setText(String.format("%.2f", sueltosActual));

        // ============================================================
        //  PRECIO POR SACO (EDITABLE - LO QUE EL USUARIO INGRESÓ)
        // ============================================================
        JTextField txtPrecioSaco = DialogStyler.crearCampoNumerico(12);
        txtPrecioSaco.setText(String.format("%.2f", precioPorSacoActual));

        // ============================================================
        //  PRECIO POR UNIDAD (CALCULADO AUTOMÁTICAMENTE - SOLO LECTURA)
        // ============================================================
        JTextField txtPrecioUnitario = DialogStyler.crearCampoNumerico(12);
        txtPrecioUnitario.setText(String.format("%.4f", precioUnitarioActual));
        txtPrecioUnitario.setEditable(false);
        txtPrecioUnitario.setBackground(new Color(240, 248, 240));
        txtPrecioUnitario.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtPrecioUnitario.setForeground(new Color(5, 150, 105));

        JLabel lblPreview = new JLabel("Subtotal: $" + String.format("%.2f", cantidadActual * precioUnitarioActual));
        lblPreview.setFont(DialogStyler.FUENTE_INFO);
        lblPreview.setForeground(DialogStyler.ACENTO);

        // ============================================================
        //  FUNCIÓN DE RECÁLCULO AUTOMÁTICO
        // ============================================================
        Runnable recalcular = () -> {
            try {
                int sacos = txtSacos.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtSacos.getText().trim());
                double pesoSaco = txtPesoSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPesoSaco.getText().trim().replace(",", "."));
                double sueltos = txtSueltos.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtSueltos.getText().trim().replace(",", "."));
                double precioSaco = txtPrecioSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPrecioSaco.getText().trim().replace(",", "."));

                double cantidadTotal = (sacos * pesoSaco) + sueltos;
                
                double precioPorUnidad = 0;
                double subtotal = 0;
                
                if (sacos > 0 && pesoSaco > 0) {
                    precioPorUnidad = precioSaco / pesoSaco;
                    subtotal = sacos * precioSaco;
                } else if (sacos > 0 && pesoSaco == 0) {
                    precioPorUnidad = precioSaco / cantidadTotal;
                    subtotal = precioSaco;
                } else if (sueltos > 0 && pesoSaco > 0 && precioSaco > 0) {
                    precioPorUnidad = precioSaco / pesoSaco;
                    subtotal = sueltos * precioPorUnidad;
                } else if (sueltos > 0 && precioSaco > 0) {
                    precioPorUnidad = precioSaco;
                    subtotal = sueltos * precioPorUnidad;
                }

                txtPrecioUnitario.setText(String.format("%.4f", precioPorUnidad));
                lblPreview.setText("Subtotal: $" + String.format("%.2f", subtotal));

            } catch (NumberFormatException ex) {
                lblPreview.setText("Ingrese valores válidos");
                lblPreview.setForeground(COLOR_ROJO);  // <--- CORREGIDO: COLOR_ROJO en lugar de ROJO
            }
        };

        // Listeners para recálculo automático
        javax.swing.event.DocumentListener recalcListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalcular.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalcular.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalcular.run(); }
        };
        txtSacos.getDocument().addDocumentListener(recalcListener);
        txtPesoSaco.getDocument().addDocumentListener(recalcListener);
        txtSueltos.getDocument().addDocumentListener(recalcListener);
        txtPrecioSaco.getDocument().addDocumentListener(recalcListener);

        // ============================================================
        //  LAYOUT DEL FORMULARIO
        // ============================================================
        int fila = 0;

        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Producto:"), gbc);
        gbc.gridx = 1;
        form.add(txtProducto, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Unidad:"), gbc);
        gbc.gridx = 1;
        form.add(txtUnidad, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Inventario del pedido"), gbc);
        gbc.gridwidth = 1;

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Numero de sacos:"), gbc);
        gbc.gridx = 1;
        form.add(txtSacos, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Peso por saco (" + unidad + "):"), gbc);
        gbc.gridx = 1;
        form.add(txtPesoSaco, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel(unidad + " sueltas:"), gbc);
        gbc.gridx = 1;
        form.add(txtSueltos, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        form.add(DialogStyler.crearSeparadorSeccion("Precio"), gbc);
        gbc.gridwidth = 1;

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Precio por saco ($):"), gbc);
        gbc.gridx = 1;
        form.add(txtPrecioSaco, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Precio por unidad ($):"), gbc);
        gbc.gridx = 1;
        form.add(txtPrecioUnitario, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        form.add(lblPreview, gbc);
        gbc.gridwidth = 1;

        // ============================================================
        //  BOTONES
        // ============================================================
        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Cambios");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(evt -> {
            try {
                int sacos = txtSacos.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtSacos.getText().trim());
                double pesoSaco = txtPesoSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPesoSaco.getText().trim().replace(",", "."));
                double sueltos = txtSueltos.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtSueltos.getText().trim().replace(",", "."));
                double precioSaco = txtPrecioSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPrecioSaco.getText().trim().replace(",", "."));

                if (sacos == 0 && sueltos == 0) {
                    DialogStyler.mostrarError(dialog, "Debe tener al menos un saco o cantidad suelta.");
                    return;
                }

                double cantidadCalculada = (sacos * pesoSaco) + sueltos;

                if (cantidadCalculada <= 0) {
                    DialogStyler.mostrarError(dialog, "La cantidad total debe ser mayor a 0. Verifique sacos, peso por saco y cantidad suelta.");
                    return;
                }
                if (precioSaco <= 0) {
                    DialogStyler.mostrarError(dialog, "El precio por saco debe ser mayor a 0.");
                    return;
                }

                double precioPorUnidad = pesoSaco > 0 ? precioSaco / pesoSaco : 0;
                double subtotal = (sacos * precioSaco) + (sueltos * precioPorUnidad);

                modelDetalles.setValueAt(String.format("%.2f", cantidadCalculada), row, 3);
                modelDetalles.setValueAt(String.format("$%.4f", precioPorUnidad), row, 4);
                modelDetalles.setValueAt(String.format("$%.2f", subtotal), row, 5);
                modelDetalles.setValueAt(sacos, row, 6);
                modelDetalles.setValueAt(String.format("%.2f", pesoSaco), row, 7);
                modelDetalles.setValueAt(String.format("%.2f", sueltos), row, 8);

                if (row < detallesEditados.size()) {
                    DetallePedido d = detallesEditados.get(row);
                    d.setCantidad(cantidadCalculada);
                    d.setPrecioUnitario(precioPorUnidad);
                    d.setSubtotal(subtotal);
                    d.setNumeroSacos(sacos);
                    d.setPesoSaco(pesoSaco);
                    d.setLibrasSueltas(sueltos);
                }

                dialog.dispose();
                actualizarTotal();
                
                DialogStyler.mostrarExito(this, 
                    "Producto actualizado correctamente.<br><br>" +
                    "Nuevo precio por saco: <b>$" + String.format("%.2f", precioSaco) + "</b><br>" +
                    "Nuevo precio por unidad: <b>$" + String.format("%.4f", precioPorUnidad) + "</b><br>" +
                    "Nuevo subtotal: <b>$" + String.format("%.2f", subtotal) + "</b>", 
                    "Éxito");

            } catch (NumberFormatException ex) {
                DialogStyler.mostrarError(dialog, "Ingrese valores numéricos válidos.<br>Use punto (.) como separador decimal.");
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

        recalcular.run();
        dialog.setVisible(true);

    } catch (Exception ex) {
        DialogStyler.mostrarError(this, "Error al editar producto: " + ex.getMessage());
        LoggerUtil.logError("Error editando producto del pedido", ex);
    }
}

    // ============================================================
    //  AGREGAR PRODUCTO
    // ============================================================
    private void agregarProducto() {
        try {
            Set<String> nombresEnPedido = detallesEditados.stream()
                .map(DetallePedido::getNombreProducto)
                .filter(n -> n != null)
                .map(n -> n.trim().toLowerCase())
                .collect(Collectors.toSet());

            List<MateriaPrima> productos = materiaDAO.listarTodos().stream()
                .filter(mp -> !nombresEnPedido.contains(mp.getNombre().trim().toLowerCase()))
                .collect(Collectors.toList());

            JDialog dialog = new JDialog(this, "Agregar Producto al Pedido", ModalityType.APPLICATION_MODAL);
            dialog.setSize(560, 620);
            dialog.setLocationRelativeTo(this);

            DialogStyler.aplicar(dialog, "Agregar Producto", "\u2795");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            String OPCION_NUEVO = "\u2795 Crear producto nuevo...";
            List<String> nombresCombo = new ArrayList<>();
            nombresCombo.add(OPCION_NUEVO);
            for (MateriaPrima mp : productos) nombresCombo.add(mp.getNombre());

            JComboBox<String> cmbProducto = new JComboBox<>(nombresCombo.toArray(new String[0]));
            cmbProducto.setFont(DialogStyler.FUENTE_CAMPO);

            boolean soloNuevoDisponible = productos.isEmpty();
            if (soloNuevoDisponible) {
                cmbProducto.setEnabled(false);
            }

            JTextField txtNombreNuevo = DialogStyler.crearCampo(18);
            JComboBox<String> cmbUnidad = DialogStyler.crearCombo(new String[]{"lb", "litro"});

            JTextField txtSacos = DialogStyler.crearCampoNumerico(10);
            JTextField txtPesoSaco = DialogStyler.crearCampoNumerico(10);
            JTextField txtSueltos = DialogStyler.crearCampoNumerico(10);
            JTextField txtPrecioSaco = DialogStyler.crearCampoNumerico(10);

            JLabel lblCantidadCalculada = new JLabel("0.00");
            lblCantidadCalculada.setFont(DialogStyler.FUENTE_LABEL);

            JLabel lblSubtotalCalculado = new JLabel("$0.00");
            lblSubtotalCalculado.setFont(DialogStyler.FUENTE_LABEL);
            lblSubtotalCalculado.setForeground(DialogStyler.ACENTO);

            Runnable recalcular = () -> {
                try {
                    int sacos = txtSacos.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtSacos.getText().trim());
                    double peso = txtPesoSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPesoSaco.getText().trim().replace(",", "."));
                    double sueltos = txtSueltos.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtSueltos.getText().trim().replace(",", "."));
                    double precioSaco = txtPrecioSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPrecioSaco.getText().trim().replace(",", "."));

                    double cantidadTotal = (sacos * peso) + sueltos;
                    double precioPorUnidad = peso > 0 ? precioSaco / peso : 0;
                    double subtotal = (sacos * precioSaco) + (sueltos * precioPorUnidad);

                    lblCantidadCalculada.setText(String.format("%.2f", cantidadTotal));
                    lblSubtotalCalculado.setText(String.format("$%.2f", subtotal));
                } catch (NumberFormatException ex) {
                    lblCantidadCalculada.setText("\u2014");
                    lblSubtotalCalculado.setText("\u2014");
                }
            };

            java.awt.event.KeyAdapter recalcularListener = new java.awt.event.KeyAdapter() {
                @Override public void keyReleased(java.awt.event.KeyEvent e) { recalcular.run(); }
            };
            txtSacos.addKeyListener(recalcularListener);
            txtPesoSaco.addKeyListener(recalcularListener);
            txtSueltos.addKeyListener(recalcularListener);
            txtPrecioSaco.addKeyListener(recalcularListener);

            Runnable actualizarSegunSeleccion = () -> {
                String seleccion = (String) cmbProducto.getSelectedItem();
                boolean esNuevo = OPCION_NUEVO.equals(seleccion);

                txtNombreNuevo.setEnabled(esNuevo);
                cmbUnidad.setEnabled(esNuevo);

                if (esNuevo) {
                    txtNombreNuevo.setText("");
                    cmbUnidad.setSelectedIndex(0);
                } else {
                    MateriaPrima mp = productos.stream()
                        .filter(p -> p.getNombre().equals(seleccion))
                        .findFirst().orElse(null);
                    if (mp != null) {
                        txtNombreNuevo.setText(mp.getNombre());
                        cmbUnidad.setSelectedItem(mp.getUnidad());
                        if (mp.getPesoSaco() != null && mp.getPesoSaco() > 0) {
                            txtPesoSaco.setText(String.format("%.2f", mp.getPesoSaco()));
                        }
                    }
                }
                recalcular.run();
            };
            cmbProducto.addActionListener(e -> actualizarSegunSeleccion.run());
            actualizarSegunSeleccion.run();

            int fila = 0;

            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(DialogStyler.crearSeparadorSeccion("Datos del producto"), gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Producto:"), gbc);
            gbc.gridx = 1;
            form.add(cmbProducto, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Nombre del producto:"), gbc);
            gbc.gridx = 1;
            form.add(txtNombreNuevo, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Unidad:"), gbc);
            gbc.gridx = 1;
            form.add(cmbUnidad, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(DialogStyler.crearSeparadorSeccion("Inventario de este pedido"), gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Numero de sacos/envases:"), gbc);
            gbc.gridx = 1;
            form.add(txtSacos, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Peso por saco (libras):"), gbc);
            gbc.gridx = 1;
            form.add(txtPesoSaco, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Libras sueltas:"), gbc);
            gbc.gridx = 1;
            form.add(txtSueltos, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Cantidad total:"), gbc);
            gbc.gridx = 1;
            form.add(lblCantidadCalculada, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
            form.add(DialogStyler.crearSeparadorSeccion("Precio"), gbc);
            gbc.gridwidth = 1;

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Precio por saco ($):"), gbc);
            gbc.gridx = 1;
            form.add(txtPrecioSaco, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Subtotal del pedido:"), gbc);
            gbc.gridx = 1;
            form.add(lblSubtotalCalculado, gbc);

            JButton btnAgregar = DialogStyler.crearBotonPrimario("\u2795 Agregar");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnAgregar.addActionListener(evt -> {
                try {
                    String seleccion = (String) cmbProducto.getSelectedItem();
                    if (seleccion == null) {
                        DialogStyler.mostrarError(dialog, "Seleccione un producto.");
                        return;
                    }

                    boolean esNuevo = OPCION_NUEVO.equals(seleccion);
                    String nombreProducto = txtNombreNuevo.getText().trim();
                    String unidadProducto = (String) cmbUnidad.getSelectedItem();
                    int materiaPrimaId;

                    if (nombreProducto.isEmpty()) {
                        DialogStyler.mostrarError(dialog, "Escriba o seleccione el nombre del producto.");
                        txtNombreNuevo.requestFocus();
                        return;
                    }

                    if (esNuevo) {
                        materiaPrimaId = 0;
                    } else {
                        MateriaPrima mp = productos.stream()
                            .filter(p -> p.getNombre().equals(seleccion))
                            .findFirst().orElse(null);
                        materiaPrimaId = (mp != null) ? mp.getId() : 0;
                    }

                    int sacos = txtSacos.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtSacos.getText().trim());
                    double pesoSaco = txtPesoSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPesoSaco.getText().trim().replace(",", "."));
                    double sueltos = txtSueltos.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtSueltos.getText().trim().replace(",", "."));
                    double precioSaco = txtPrecioSaco.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtPrecioSaco.getText().trim().replace(",", "."));

                    double cantidad = (sacos * pesoSaco) + sueltos;

                    if (cantidad <= 0) {
                        DialogStyler.mostrarError(dialog, "Ingrese sacos/peso o libras sueltas para calcular la cantidad.");
                        return;
                    }
                    if (precioSaco <= 0) {
                        DialogStyler.mostrarError(dialog, "El precio por saco debe ser mayor a 0.");
                        return;
                    }

                    double precioPorUnidad = pesoSaco > 0 ? precioSaco / pesoSaco : (cantidad > 0 ? precioSaco / cantidad : 0);
                    double subtotal = (sacos * precioSaco) + (sueltos * precioPorUnidad);

                    DetallePedido nuevoDetalle = new DetallePedido();
                    nuevoDetalle.setMateriaPrimaId(materiaPrimaId);
                    nuevoDetalle.setNombreProducto(nombreProducto);
                    nuevoDetalle.setUnidad(unidadProducto);
                    nuevoDetalle.setCantidad(cantidad);
                    nuevoDetalle.setPrecioUnitario(precioPorUnidad);
                    nuevoDetalle.setSubtotal(subtotal);
                    nuevoDetalle.setNumeroSacos(sacos > 0 ? sacos : null);
                    nuevoDetalle.setPesoSaco(pesoSaco > 0 ? pesoSaco : null);
                    nuevoDetalle.setLibrasSueltas(sueltos > 0 ? sueltos : null);

                    detallesEditados.add(nuevoDetalle);
                    modelDetalles.addRow(new Object[]{
                        "NUEVO",
                        nombreProducto,
                        unidadProducto,
                        String.format("%.2f", cantidad),
                        String.format("$%.4f", precioPorUnidad),
                        String.format("$%.2f", subtotal),
                        sacos,
                        String.format("%.2f", pesoSaco),
                        String.format("%.2f", sueltos)
                    });

                    dialog.dispose();
                    actualizarTotal();
                    DialogStyler.mostrarExito(this, "Producto agregado al pedido.", "Exito");

                } catch (NumberFormatException ex) {
                    DialogStyler.mostrarError(dialog, "Ingrese valores numericos validos.");
                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnAgregar), BorderLayout.SOUTH);
            dialog.setVisible(true);

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            LoggerUtil.logError("Error agregando producto al pedido", ex);
        }
    }

    // ============================================================
    //  ELIMINAR PRODUCTO - CORREGIDO
    // ============================================================
    private void eliminarProducto() {
    int row = tablaDetalles.getSelectedRow();
    if (row < 0) {
        DialogStyler.mostrarAdvertencia(this, "Seleccione un producto para eliminar.", "Aviso");
        return;
    }

    // Obtener datos de la fila
    String nombre = (String) modelDetalles.getValueAt(row, 1);
    String unidad = (String) modelDetalles.getValueAt(row, 2);
    double cantidad = Double.parseDouble(modelDetalles.getValueAt(row, 3).toString().replace(",", "."));
    double subtotal = Double.parseDouble(modelDetalles.getValueAt(row, 5).toString().replace("$", "").replace(",", "."));
    int sacos = Integer.parseInt(modelDetalles.getValueAt(row, 6).toString());
    double pesoSaco = Double.parseDouble(modelDetalles.getValueAt(row, 7).toString().replace(",", "."));
    double sueltos = Double.parseDouble(modelDetalles.getValueAt(row, 8).toString().replace(",", "."));

    Object idObj = modelDetalles.getValueAt(row, 0);
    boolean esNuevo = "NUEVO".equals(idObj.toString());
    int detalleId = esNuevo ? 0 : (int) idObj;

    int materiaPrimaId = 0;
    try {
        MateriaPrima mp = materiaDAO.buscarPorNombre(nombre);
        if (mp != null) {
            materiaPrimaId = mp.getId();
        }
    } catch (Exception ex) {
        // Si no se encuentra, continuar
    }

    // ============================================================
    //  VERIFICAR SI EL PEDIDO YA FUE RECIBIDO
    // ============================================================
    boolean yaRecibido = "RECIBIDA".equals(pedidoOriginal.getEstadoRecepcion());

    String confirmMsg = "⚠️ ¿ELIMINAR PRODUCTO DEL PEDIDO?\n\n" +
            "Producto: <b>" + nombre + "</b>\n" +
            "Cantidad: <b>" + String.format("%.2f", cantidad) + "</b> " + unidad + "\n" +
            "Subtotal: <b>$" + String.format("%.2f", subtotal) + "</b>\n\n";

    if (yaRecibido && !esNuevo) {
        confirmMsg += "⚠️ Este pedido YA FUE RECIBIDO.\n" +
                "Al eliminar este producto, se DEVOLVERÁ el stock al inventario.\n\n" +
                "Los movimientos de inventario se revertirán automáticamente.\n\n";
    }

    confirmMsg += "¿Está seguro de eliminar este producto?";

    if (!DialogStyler.confirmar(this, confirmMsg, "Eliminar producto")) return;

    // ============================================================
    //  SI EL PEDIDO YA FUE RECIBIDO, REVERTIR EL STOCK
    // ============================================================
    if (yaRecibido && !esNuevo && materiaPrimaId > 0) {
        try {
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                // 1. Obtener el producto actual
                String sqlMp = "SELECT * FROM materias_primas WHERE id = ?";
                ps = conn.prepareStatement(sqlMp);
                ps.setInt(1, materiaPrimaId);
                rs = ps.executeQuery();

                if (rs.next()) {
                    double stockActual = rs.getDouble("cantidad_disponible");
                    double stockTotal = rs.getDouble("cantidad_total");
                    double precioTotal = rs.getDouble("precio_total");
                    int sacosActual = rs.getInt("numero_sacos");
                    double librasSueltasActual = rs.getDouble("libras_sueltas");
                    double precioPorUnidad = rs.getDouble("precio_por_unidad");
                    rs.close();
                    ps.close();

                    // 2. Calcular nuevos valores
                    double nuevoStock = Math.max(0, stockActual - cantidad);
                    double nuevoStockTotal = Math.max(0, stockTotal - cantidad);
                    double nuevoPrecioTotal = Math.max(0, precioTotal - subtotal);
                    int nuevosSacos = Math.max(0, sacosActual - sacos);
                    double nuevasLibrasSueltas = Math.max(0, librasSueltasActual - sueltos);
                    double nuevoPrecioPorUnidad = nuevoStockTotal > 0 ? nuevoPrecioTotal / nuevoStockTotal : 0;

                    // 3. Actualizar el producto
                    String sqlUpdate = "UPDATE materias_primas SET " +
                            "cantidad_disponible = ?, cantidad_total = ?, " +
                            "precio_total = ?, precio_por_unidad = ?, " +
                            "numero_sacos = ?, libras_sueltas = ? " +
                            "WHERE id = ?";

                    ps = conn.prepareStatement(sqlUpdate);
                    ps.setDouble(1, nuevoStock);
                    ps.setDouble(2, nuevoStockTotal);
                    ps.setDouble(3, nuevoPrecioTotal);
                    ps.setDouble(4, nuevoPrecioPorUnidad);
                    ps.setInt(5, nuevosSacos);
                    ps.setDouble(6, nuevasLibrasSueltas);
                    ps.setInt(7, materiaPrimaId);
                    ps.executeUpdate();
                    ps.close();

                    // 4. CORREGIDO: Eliminar el movimiento de ingreso asociado a este detalle
                    //    SQLite NO permite ORDER BY en DELETE. Usamos una subconsulta para obtener el ID
                    String sqlGetMovId = "SELECT id FROM movimientos_inventario " +
                            "WHERE pedido_id = ? AND nombre_producto = ? AND tipo_movimiento = 'INGRESO' " +
                            "ORDER BY id DESC LIMIT 1";
                    ps = conn.prepareStatement(sqlGetMovId);
                    ps.setInt(1, pedidoOriginal.getId());
                    ps.setString(2, nombre);
                    rs = ps.executeQuery();
                    
                    int movimientoId = 0;
                    if (rs.next()) {
                        movimientoId = rs.getInt("id");
                    }
                    rs.close();
                    ps.close();

                    if (movimientoId > 0) {
                        String sqlDeleteMov = "DELETE FROM movimientos_inventario WHERE id = ?";
                        ps = conn.prepareStatement(sqlDeleteMov);
                        ps.setInt(1, movimientoId);
                        ps.executeUpdate();
                        ps.close();
                        System.out.println("✅ Movimiento #" + movimientoId + " eliminado");
                    }

                    // 5. Registrar un movimiento de SALIDA para reflejar la eliminación
                    if (cantidad > 0) {
                        String sqlMovSalida = "INSERT INTO movimientos_inventario (" +
                                "materia_prima_id, nombre_producto, unidad, tipo_movimiento, " +
                                "cantidad, precio_por_unidad, subtotal, fecha, hora, referencia, pedido_id) " +
                                "VALUES (?, ?, ?, 'SALIDA', ?, ?, ?, date('now'), time('now'), ?, ?)";

                        ps = conn.prepareStatement(sqlMovSalida);
                        ps.setInt(1, materiaPrimaId);
                        ps.setString(2, nombre);
                        ps.setString(3, unidad);
                        ps.setDouble(4, cantidad);
                        ps.setDouble(5, precioPorUnidad);
                        ps.setDouble(6, cantidad * precioPorUnidad);
                        ps.setString(7, "Eliminación de producto en pedido #" + pedidoOriginal.getId());
                        ps.setInt(8, pedidoOriginal.getId());
                        ps.executeUpdate();
                        ps.close();
                    }

                    // 6. CORREGIDO: Eliminar el ingreso del historial asociado a este detalle
                    String sqlGetHistId = "SELECT id FROM ingresos_historial " +
                            "WHERE producto_id = ? AND pedido_id = ? " +
                            "ORDER BY id DESC LIMIT 1";
                    ps = conn.prepareStatement(sqlGetHistId);
                    ps.setInt(1, materiaPrimaId);
                    ps.setInt(2, pedidoOriginal.getId());
                    rs = ps.executeQuery();
                    
                    int historialId = 0;
                    if (rs.next()) {
                        historialId = rs.getInt("id");
                    }
                    rs.close();
                    ps.close();

                    if (historialId > 0) {
                        String sqlDeleteHist = "DELETE FROM ingresos_historial WHERE id = ?";
                        ps = conn.prepareStatement(sqlDeleteHist);
                        ps.setInt(1, historialId);
                        ps.executeUpdate();
                        ps.close();
                        System.out.println("✅ Historial #" + historialId + " eliminado");
                    }

                    // 7. Si el producto quedó sin stock, eliminar todas las referencias
                    if (nuevoStock <= 0 && nuevoStockTotal <= 0) {
                        // Desactivar foreign keys temporalmente
                        try (java.sql.Statement stmtFk = conn.createStatement()) {
                            stmtFk.execute("PRAGMA foreign_keys = OFF");
                        }

                        try {
                            // Eliminar formulacion_detalle
                            String sqlDeleteForm = "DELETE FROM formulacion_detalle WHERE materia_prima_id = ?";
                            ps = conn.prepareStatement(sqlDeleteForm);
                            ps.setInt(1, materiaPrimaId);
                            ps.executeUpdate();
                            ps.close();

                            // Eliminar compras_proveedores
                            String sqlDeleteComp = "DELETE FROM compras_proveedores WHERE materia_prima_id = ?";
                            ps = conn.prepareStatement(sqlDeleteComp);
                            ps.setInt(1, materiaPrimaId);
                            ps.executeUpdate();
                            ps.close();

                            // Eliminar historial_precios
                            String sqlDeleteHistPrecios = "DELETE FROM historial_precios WHERE producto_id = ?";
                            ps = conn.prepareStatement(sqlDeleteHistPrecios);
                            ps.setInt(1, materiaPrimaId);
                            ps.executeUpdate();
                            ps.close();

                            // Eliminar detalle_pedido (por si quedan referencias)
                            String sqlDeleteDetallePedido = "DELETE FROM detalle_pedido WHERE materia_prima_id = ?";
                            ps = conn.prepareStatement(sqlDeleteDetallePedido);
                            ps.setInt(1, materiaPrimaId);
                            ps.executeUpdate();
                            ps.close();

                            // Eliminar el producto
                            String sqlDeleteMp = "DELETE FROM materias_primas WHERE id = ?";
                            ps = conn.prepareStatement(sqlDeleteMp);
                            ps.setInt(1, materiaPrimaId);
                            ps.executeUpdate();
                            ps.close();

                            System.out.println("🗑️ Producto eliminado completamente: " + nombre);
                        } finally {
                            try (java.sql.Statement stmtFkRestore = conn.createStatement()) {
                                stmtFkRestore.execute("PRAGMA foreign_keys = ON");
                            }
                        }
                    }

                    conn.commit();
                    System.out.println("✅ Stock revertido para: " + nombre + " (Pedido #" + pedidoOriginal.getId() + ")");

                } else {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                }

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                }
                throw e;
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
                }
                if (ps != null) try { ps.close(); } catch (SQLException e) {}
                if (rs != null) try { rs.close(); } catch (SQLException e) {}
            }

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al revertir stock: " + ex.getMessage());
            LoggerUtil.logError("Error revirtiendo stock al eliminar producto", ex);
            return;
        }
    }

    // ============================================================
    //  ELIMINAR EL DETALLE DEL PEDIDO EN LA BASE DE DATOS
    // ============================================================
    if (!esNuevo && detalleId > 0) {
        try {
            Connection conn = null;
            PreparedStatement ps = null;

            try {
                conn = DatabaseManager.getConnection();
                String sqlDelete = "DELETE FROM detalle_pedido WHERE id = ? AND pedido_id = ?";
                ps = conn.prepareStatement(sqlDelete);
                ps.setInt(1, detalleId);
                ps.setInt(2, pedidoOriginal.getId());
                ps.executeUpdate();
                System.out.println("✅ Detalle #" + detalleId + " eliminado de detalle_pedido");
            } finally {
                if (conn != null) try { conn.close(); } catch (SQLException ex) {}
                if (ps != null) try { ps.close(); } catch (SQLException ex) {}
            }

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar detalle: " + ex.getMessage());
            LoggerUtil.logError("Error eliminando detalle de pedido", ex);
            return;
        }
    }

    // ============================================================
    //  ELIMINAR DE LA TABLA LOCAL
    // ============================================================
    if (row < detallesEditados.size()) {
        detallesEditados.remove(row);
    }
    modelDetalles.removeRow(row);
    actualizarTotal();

    DialogStyler.mostrarExito(this,
            "✅ Producto eliminado del pedido.\n\n" +
            "Producto: <b>" + nombre + "</b>\n" +
            "Cantidad: <b>" + String.format("%.2f", cantidad) + "</b> " + unidad + "\n" +
            (yaRecibido ? "\n📦 Stock revertido al inventario correctamente." : ""),
            "Producto eliminado");
}

    // ============================================================
    //  GUARDAR CAMBIOS
    // ============================================================
    private void guardarCambios() {
        try {
            if (modelDetalles.getRowCount() == 0) {
                boolean confirmaEliminarPedido = DialogStyler.confirmar(this,
                    "Al quitar este producto, el pedido se queda sin ningún artículo.<br><br>" +
                    "¿Deseas eliminar el <b>pedido completo #" + pedidoOriginal.getId() + "</b> ahora mismo?<br>" +
                    "(Si el pedido ya fue recibido, el stock correspondiente se revertirá en el inventario).",
                    "Pedido sin productos"
                );

                if (!confirmaEliminarPedido) {
                    return;
                }

                pedidoDAO.eliminar(pedidoOriginal.getId());
                dispose();

                DialogStyler.mostrarExito(this,
                    "Pedido #" + pedidoOriginal.getId() + " eliminado completamente.",
                    "Pedido eliminado"
                );

                if (getParent() != null) {
                    Component c = getParent();
                    while (c != null && !(c instanceof ProveedoresPanel)) {
                        c = c.getParent();
                    }
                    if (c != null) {
                        ((ProveedoresPanel) c).cargarDatos();
                    }
                }
                return;
            }

            boolean yaRecibido = "RECIBIDA".equals(pedidoOriginal.getEstadoRecepcion());

            String confirmMsg = "⚠️ ¿Guardar los cambios?\n\n";
            if (yaRecibido) {
                confirmMsg += "Este pedido YA FUE RECIBIDO.\n" +
                              "Los cambios afectarán el inventario:\n" +
                              "• Productos eliminados → se restarán del stock\n" +
                              "• Productos agregados → se sumarán al stock\n" +
                              "• Cantidades modificadas → se ajustará el stock\n\n";
            }
            confirmMsg += "¿Está seguro de guardar los cambios?";

            if (!DialogStyler.confirmar(this, confirmMsg, "Guardar cambios")) return;

            List<DetallePedido> detallesActualizados = new ArrayList<>();
            for (int i = 0; i < modelDetalles.getRowCount(); i++) {
                DetallePedido d = new DetallePedido();
                Object idObj = modelDetalles.getValueAt(i, 0);
                if (idObj instanceof Integer) {
                    d.setId((Integer) idObj);
                } else {
                    try {
                        d.setId(Integer.parseInt(idObj.toString()));
                    } catch (NumberFormatException ex) {
                        d.setId(0);
                    }
                }
                d.setNombreProducto((String) modelDetalles.getValueAt(i, 1));
                d.setUnidad((String) modelDetalles.getValueAt(i, 2));
                d.setCantidad(Double.parseDouble(modelDetalles.getValueAt(i, 3).toString().replace(",", ".")));
                d.setPrecioUnitario(Double.parseDouble(modelDetalles.getValueAt(i, 4).toString().replace("$", "").replace(",", ".")));
                d.setSubtotal(Double.parseDouble(modelDetalles.getValueAt(i, 5).toString().replace("$", "").replace(",", ".")));
                d.setNumeroSacos(getIntValue(i, 6));
                d.setPesoSaco(getDoubleValue(i, 7));
                d.setLibrasSueltas(getDoubleValue(i, 8));

                try {
                    MateriaPrima mp = materiaDAO.buscarPorNombre(d.getNombreProducto());
                    if (mp != null) {
                        d.setMateriaPrimaId(mp.getId());
                    }
                } catch (SQLException ex) {
                    if (i < detallesOriginales.size()) {
                        d.setMateriaPrimaId(detallesOriginales.get(i).getMateriaPrimaId());
                    }
                }

                detallesActualizados.add(d);
            }

            pedidoDAO.actualizarPedidoConDetalles(pedidoOriginal.getId(), detallesActualizados, yaRecibido);

            dispose();

            DialogStyler.mostrarExito(this,
                "✅ Pedido #" + pedidoOriginal.getId() + " actualizado correctamente.\n\n" +
                "Productos: " + detallesActualizados.size() + "\n" +
                "Total: $" + String.format("%.2f", calcularTotal(detallesActualizados)) + "\n\n" +
                (yaRecibido ? "📦 El inventario ha sido actualizado." : "⚠️ Recuerde recibir el pedido para actualizar el inventario."),
                "Pedido actualizado"
            );

            if (getParent() != null) {
                Component c = getParent();
                while (c != null && !(c instanceof ProveedoresPanel)) {
                    c = c.getParent();
                }
                if (c != null) {
                    ((ProveedoresPanel) c).cargarDatos();
                }
            }

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al guardar cambios: " + ex.getMessage());
            LoggerUtil.logError("Error guardando cambios en pedido", ex);
        }
    }

    private int getIntValue(int row, int column) {
        try {
            Object val = modelDetalles.getValueAt(row, column);
            if (val == null) return 0;
            if (val instanceof Integer) return (Integer) val;
            return Integer.parseInt(val.toString().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getDoubleValue(int row, int column) {
        try {
            Object val = modelDetalles.getValueAt(row, column);
            if (val == null) return 0;
            if (val instanceof Double) return (Double) val;
            return Double.parseDouble(val.toString().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double calcularTotal(List<DetallePedido> detalles) {
        double total = 0;
        for (DetallePedido d : detalles) {
            total += d.getSubtotal();
        }
        return total;
    }
}