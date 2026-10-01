package com.feedformulator.ui;

import com.feedformulator.dao.ContabilidadDAO;
import com.feedformulator.dao.VentaDAO;
import com.feedformulator.model.MovimientoContable;
import com.feedformulator.model.Venta;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Libro de contabilidad manual, INDEPENDIENTE del apartado de Ventas.
 * Pensado como un "borrador cómodo" para sacar cuentas propias: cuánto se
 * reinvirtió, cuánto se metió de dinero propio, gastos extra, compras de
 * equipo, etc. — comparado contra la Ganancia General real de Ventas
 * (mostrada solo como referencia). Nada de lo que se registre aquí lee ni
 * modifica los valores ya establecidos en Ventas o Reportes.
 */
public class ContabilidadPanel extends JPanel {
    private final ContabilidadDAO dao = new ContabilidadDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private DefaultTableModel tableModel;
    private JTable tabla;
    private TableRowSorter<DefaultTableModel> sorter;

    private JComboBox<String> cmbFiltroTipo;
    private JTextField txtBuscar;
    private JTextArea txtNotas;

    private JLabel lblGananciaGeneral;
    private JLabel lblTotalEntradas;
    private JLabel lblTotalSalidas;
    private JLabel lblSaldoControl;
    private JLabel lblSaldoEstimado;
    private JLabel lblPorCobrarPendiente;

    private static final Color COLOR_ENTRADA = new Color(46, 204, 113);
    private static final Color COLOR_SALIDA = new Color(231, 76, 60);
    private static final Color COLOR_REFERENCIA = new Color(52, 73, 94);
    private static final Color COLOR_CXC = new Color(230, 126, 34);
    private static final Color FONDO = new Color(245, 247, 250);

    // Categorías sugeridas (el usuario puede escribir cualquier otra)
    private static final String[] CATEGORIAS_SUGERIDAS = {
        "Reinversión en insumos", "Aporte propio", "Compra de equipo/maquinaria",
        "Gasto operativo", "Gasto personal", "Transporte", "Mantenimiento",
        "Cobro de deuda", "Préstamo a terceros", "Otro"
    };

    public ContabilidadPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(crearTitulo(), BorderLayout.NORTH);
        add(crearCentral(), BorderLayout.CENTER);

        cargarDatos();
    }

    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("🧮", "Contabilidad Personal",
            "Tu borrador de cuentas — reinversión, gastos, aportes propios... comparado con la ganancia real de Ventas");
    }

    private JPanel crearCentral() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel aviso = DialogStyler.crearBannerInfo("info",
            "ℹ️ Este es tu espacio libre para sacar cuentas. La tarjeta \"Ganancia General\" es solo una " +
            "REFERENCIA en vivo de lo que ya calculó Ventas — nada de lo que anotes aquí la modifica. " +
            "Usa categorías libres (escribe lo que quieras: \"aporte propio\", \"compra de maquinaria\", etc.) " +
            "y anota tus cuentas o notas sueltas en el recuadro de abajo, como una libreta.");

        JPanel tarjetas = new JPanel(new GridLayout(2, 3, 12, 10));
        tarjetas.setOpaque(false);
        tarjetas.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        lblGananciaGeneral = new JLabel("$0.00");
        lblTotalEntradas = new JLabel("$0.00");
        lblTotalSalidas = new JLabel("$0.00");
        lblSaldoControl = new JLabel("$0.00");
        lblSaldoEstimado = new JLabel("$0.00");
        lblPorCobrarPendiente = new JLabel("$0.00");

        tarjetas.add(crearTarjeta("📈", "Ganancia General (Ventas) — referencia", lblGananciaGeneral, COLOR_REFERENCIA));
        tarjetas.add(crearTarjeta("➕", "Total Entradas (aquí)", lblTotalEntradas, COLOR_ENTRADA));
        tarjetas.add(crearTarjeta("➖", "Total Salidas (aquí)", lblTotalSalidas, COLOR_SALIDA));
        tarjetas.add(crearTarjeta("⚖️", "Saldo de este Control", lblSaldoControl, new Color(41, 128, 185)));
        tarjetas.add(crearTarjeta("💰", "Saldo Estimado (Ganancia + este control)", lblSaldoEstimado, new Color(142, 68, 173)));
        tarjetas.add(crearTarjeta("🧾", "Por Cobrar (pendiente)", lblPorCobrarPendiente, COLOR_CXC));

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(aviso, BorderLayout.NORTH);
        norte.add(tarjetas, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, crearPanelTabla(), crearPanelNotas());
        split.setResizeWeight(0.68);
        split.setBorder(null);
        split.setOpaque(false);

        panel.add(norte, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        panel.add(crearPanelBotones(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel crearTarjeta(String icono, String titulo, JLabel labelValor, Color color) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 235), 1),
                BorderFactory.createMatteBorder(0, 4, 0, 0, color)),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel lblTitulo = new JLabel("<html>" + icono + " " + titulo + "</html>");
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitulo.setForeground(new Color(120, 130, 140));
        panel.add(lblTitulo, BorderLayout.NORTH);

        labelValor.setFont(new Font("Segoe UI", Font.BOLD, 19));
        labelValor.setForeground(color);
        panel.add(labelValor, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filtros.setOpaque(false);

        filtros.add(new JLabel("Filtrar por tipo:"));
        cmbFiltroTipo = new JComboBox<>(new String[]{"Todos", "Entradas", "Salidas", "Por Cobrar"});
        cmbFiltroTipo.addActionListener(e -> aplicarFiltro());
        filtros.add(cmbFiltroTipo);

        filtros.add(new JLabel("   Buscar:"));
        txtBuscar = DialogStyler.crearCampo(18);
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
        });
        filtros.add(txtBuscar);

        String[] cols = {"ID", "Fecha", "Tipo", "Categoría", "Descripción", "Monto", "Estado"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(tableModel);
        tabla.setRowHeight(30);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(new Color(52, 73, 94));
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.setSelectionBackground(new Color(220, 235, 250));
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.getColumnModel().getColumn(0).setMaxWidth(50);

        sorter = new TableRowSorter<>(tableModel);
        tabla.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelNotas() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel filaTitulo = new JPanel(new BorderLayout());
        filaTitulo.setOpaque(false);
        JLabel lblTitulo = new JLabel("📝 Notas / Borrador — espacio libre para sacar cuentas a mano");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        filaTitulo.add(lblTitulo, BorderLayout.WEST);

        JButton btnGuardarNotas = DialogStyler.crearBotonSecundario("💾 Guardar Notas");
        btnGuardarNotas.addActionListener(e -> guardarNotas());
        filaTitulo.add(btnGuardarNotas, BorderLayout.EAST);

        txtNotas = new JTextArea();
        txtNotas.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtNotas.setLineWrap(true);
        txtNotas.setWrapStyleWord(true);
        txtNotas.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scrollNotas = new JScrollPane(txtNotas);
        scrollNotas.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));

        panel.add(filaTitulo, BorderLayout.NORTH);
        panel.add(scrollNotas, BorderLayout.CENTER);
        return panel;
    }

    private void guardarNotas() {
        try {
            dao.guardarNotas(txtNotas.getText());
            DialogStyler.mostrarExito(this, "Notas guardadas.", "Listo");
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al guardar notas: " + ex.getMessage());
        }
    }

    private void aplicarFiltro() {
        if (sorter == null) return;
        String texto = txtBuscar.getText().trim();
        String tipoSel = (String) cmbFiltroTipo.getSelectedItem();

        java.util.List<javax.swing.RowFilter<Object, Object>> filtros = new java.util.ArrayList<>();
        if (texto != null && !texto.isEmpty()) {
            filtros.add(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(texto)));
        }
        if (tipoSel != null && !"Todos".equals(tipoSel)) {
            filtros.add(javax.swing.RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(tipoSel.replaceAll("s$", "")) + ".*", 2));
        }
        sorter.setRowFilter(filtros.isEmpty() ? null : javax.swing.RowFilter.andFilter(filtros));
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setOpaque(false);

        JButton btnNuevo = DialogStyler.crearBotonPrimario("➕ Nuevo Movimiento");
        btnNuevo.addActionListener(e -> mostrarDialogoNuevoMovimiento());

        JButton btnCobrado = DialogStyler.crearBotonSecundario("✅ Marcar como Cobrado");
        btnCobrado.addActionListener(e -> marcarComoCobrado());

        JButton btnEliminar = DialogStyler.crearBotonPeligro("🗑️ Eliminar");
        btnEliminar.addActionListener(e -> eliminarSeleccionado());

        JButton btnActualizar = DialogStyler.crearBotonSecundario("🔄 Actualizar");
        btnActualizar.addActionListener(e -> cargarDatos());

        panel.add(btnNuevo);
        panel.add(btnCobrado);
        panel.add(btnEliminar);
        panel.add(btnActualizar);
        return panel;
    }

    // ============================================================
    //  NUEVO MOVIMIENTO — categoría libre, solo Entrada/Salida
    // ============================================================
    private void mostrarDialogoNuevoMovimiento() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Nuevo Movimiento Contable", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        DialogStyler.aplicar(dialog, "Nuevo Movimiento Contable", "🧮");

        JPanel form = DialogStyler.crearPanelFormulario();
        int fila = 0;

        JRadioButton rbEntrada = new JRadioButton("➕ Entrada (suma al saldo)", true);
        JRadioButton rbSalida = new JRadioButton("➖ Salida (resta del saldo)", false);
        ButtonGroup grupoTipo = new ButtonGroup();
        grupoTipo.add(rbEntrada);
        grupoTipo.add(rbSalida);
        rbEntrada.setOpaque(false);
        rbSalida.setOpaque(false);
        rbEntrada.setFont(DialogStyler.FUENTE_CAMPO);
        rbSalida.setFont(DialogStyler.FUENTE_CAMPO);

        JCheckBox chkPorCobrar = new JCheckBox("Es una cuenta por cobrar (queda \"Pendiente\" hasta marcarla como cobrada)");
        chkPorCobrar.setOpaque(false);
        chkPorCobrar.setFont(DialogStyler.FUENTE_INFO);
        chkPorCobrar.addActionListener(e -> {
            if (chkPorCobrar.isSelected()) {
                rbEntrada.setSelected(true);
                rbEntrada.setEnabled(false);
                rbSalida.setEnabled(false);
            } else {
                rbEntrada.setEnabled(true);
                rbSalida.setEnabled(true);
            }
        });

        JComboBox<String> cmbCategoria = new JComboBox<>(CATEGORIAS_SUGERIDAS);
        cmbCategoria.setEditable(true); // libre: el usuario puede escribir lo que quiera
        cmbCategoria.setFont(DialogStyler.FUENTE_CAMPO);
        cmbCategoria.setSelectedIndex(-1);

        JTextField txtDescripcion = DialogStyler.crearCampo(20);
        JTextField txtMonto = DialogStyler.crearCampoNumerico(15);
        JTextField txtNotasMov = DialogStyler.crearCampo(20);

        JDateChooser dateFecha = new JDateChooser();
        dateFecha.setDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dateFecha.setPreferredSize(new Dimension(160, 30));

        JPanel panelTipo = new JPanel();
        panelTipo.setLayout(new BoxLayout(panelTipo, BoxLayout.Y_AXIS));
        panelTipo.setOpaque(false);
        panelTipo.add(rbEntrada);
        panelTipo.add(rbSalida);

        form.add(DialogStyler.crearLabel("Tipo:"), DialogStyler.crearGBC(0, fila));
        form.add(panelTipo, DialogStyler.crearGBC(1, fila++));

        GridBagConstraints gbcCxC = DialogStyler.crearGBC(0, fila++);
        gbcCxC.gridwidth = 2;
        form.add(chkPorCobrar, gbcCxC);

        form.add(DialogStyler.crearLabel("Categoría (libre):"), DialogStyler.crearGBC(0, fila));
        form.add(cmbCategoria, DialogStyler.crearGBC(1, fila++));

        form.add(DialogStyler.crearLabel("Descripción:"), DialogStyler.crearGBC(0, fila));
        form.add(txtDescripcion, DialogStyler.crearGBC(1, fila++));

        form.add(DialogStyler.crearLabel("Monto ($):"), DialogStyler.crearGBC(0, fila));
        form.add(txtMonto, DialogStyler.crearGBC(1, fila++));

        form.add(DialogStyler.crearLabel("Fecha:"), DialogStyler.crearGBC(0, fila));
        form.add(dateFecha, DialogStyler.crearGBC(1, fila++));

        form.add(DialogStyler.crearLabel("Notas (opcional):"), DialogStyler.crearGBC(0, fila));
        form.add(txtNotasMov, DialogStyler.crearGBC(1, fila++));

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                Object catObj = cmbCategoria.getEditor().getItem();
                String categoria = catObj != null ? catObj.toString().trim() : "";
                String descripcion = txtDescripcion.getText().trim();
                String montoStr = txtMonto.getText().trim().replace(",", ".");
                Date fecha = dateFecha.getDate();

                if (categoria.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "Ingrese o elija una categoría (ej: Aporte propio, Compra de equipo).");
                    return;
                }
                if (montoStr.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "Ingrese el monto.");
                    return;
                }
                double monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    DialogStyler.mostrarError(dialog, "El monto debe ser mayor a cero.");
                    return;
                }
                if (fecha == null) {
                    DialogStyler.mostrarError(dialog, "Seleccione una fecha.");
                    return;
                }

                String tipoInterno;
                String estadoInicial;
                if (chkPorCobrar.isSelected()) {
                    tipoInterno = "CUENTA_POR_COBRAR";
                    estadoInicial = "PENDIENTE";
                } else {
                    tipoInterno = rbEntrada.isSelected() ? "ENTRADA" : "SALIDA";
                    estadoInicial = "ACTIVO";
                }

                MovimientoContable m = new MovimientoContable();
                m.setTipo(tipoInterno);
                m.setCategoria(categoria);
                m.setDescripcion(descripcion);
                m.setMonto(monto);
                m.setFecha(new java.text.SimpleDateFormat("yyyy-MM-dd").format(fecha));
                m.setEstado(estadoInicial);
                m.setNotas(txtNotasMov.getText().trim());

                dao.guardar(m);
                dialog.dispose();
                cargarDatos();
                DialogStyler.mostrarExito(this, "Movimiento registrado correctamente.", "Guardado");

            } catch (NumberFormatException ex) {
                DialogStyler.mostrarError(dialog, "El monto debe ser un número válido.");
            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error al guardar: " + ex.getMessage());
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
    //  MARCAR CUENTA POR COBRAR COMO COBRADA
    // ============================================================
    private void marcarComoCobrado() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un movimiento de la tabla.", "Aviso");
            return;
        }
        row = tabla.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(row, 0);
        String tipo = (String) tableModel.getValueAt(row, 2);
        String estado = (String) tableModel.getValueAt(row, 6);

        if (!"Por Cobrar".equals(tipo)) {
            DialogStyler.mostrarAdvertencia(this, "Solo las 'Cuentas por Cobrar' se pueden marcar como cobradas.", "Aviso");
            return;
        }
        if ("COBRADO".equals(estado) || (estado != null && estado.contains("Cobrado"))) {
            DialogStyler.mostrarAdvertencia(this, "Este movimiento ya está marcado como cobrado.", "Aviso");
            return;
        }

        if (!DialogStyler.confirmar(this, "¿Confirmar que ya recibiste este pago?", "Marcar como cobrado")) return;

        try {
            dao.marcarComoCobrado(id);
            cargarDatos();
            DialogStyler.mostrarExito(this, "Movimiento marcado como cobrado.", "Actualizado");
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    // ============================================================
    //  ELIMINAR
    // ============================================================
    private void eliminarSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un movimiento de la tabla.", "Aviso");
            return;
        }
        row = tabla.convertRowIndexToModel(row);
        int id = (int) tableModel.getValueAt(row, 0);

        if (!DialogStyler.confirmar(this, "¿Eliminar este movimiento? Esta acción no se puede deshacer.", "Eliminar movimiento")) return;

        try {
            dao.eliminar(id);
            cargarDatos();
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }

    // ============================================================
    //  CLASIFICACIÓN (compatible con datos antiguos: REINVERSION/GASTO/
    //  INGRESO ya guardados con el sistema de categorías fijas anterior)
    // ============================================================
    private boolean esEntrada(MovimientoContable m) {
        switch (m.getTipo()) {
            case "ENTRADA":
            case "INGRESO": // legado
                return true;
            case "CUENTA_POR_COBRAR":
                return "COBRADO".equals(m.getEstado());
            default: // SALIDA, REINVERSION, GASTO (legado)
                return false;
        }
    }

    private boolean cuentaParaSaldo(MovimientoContable m) {
        if ("CUENTA_POR_COBRAR".equals(m.getTipo())) {
            return "COBRADO".equals(m.getEstado());
        }
        return true;
    }

    private String tipoMostrar(MovimientoContable m) {
        switch (m.getTipo()) {
            case "ENTRADA": return "Entrada";
            case "SALIDA": return "Salida";
            case "INGRESO": return "Entrada"; // legado
            case "REINVERSION": return "Salida"; // legado
            case "GASTO": return "Salida"; // legado
            case "CUENTA_POR_COBRAR": return "Por Cobrar";
            default: return m.getTipo();
        }
    }

    // ============================================================
    //  CARGA Y RESUMEN
    // ============================================================
    public void cargarDatos() {
        try {
            tableModel.setRowCount(0);
            List<MovimientoContable> lista = dao.listarTodos();

            double totalEntradas = 0, totalSalidas = 0, totalCxCPendiente = 0;

            for (MovimientoContable m : lista) {
                String tipoMostrar = tipoMostrar(m);

                if (cuentaParaSaldo(m)) {
                    if (esEntrada(m)) totalEntradas += m.getMonto();
                    else totalSalidas += m.getMonto();
                } else {
                    totalCxCPendiente += m.getMonto();
                }

                String categoriaMostrar = (m.getCategoria() == null || m.getCategoria().isEmpty())
                    ? (m.getTipo().equals("REINVERSION") ? "Reinversión (legado)"
                       : m.getTipo().equals("GASTO") ? "Gasto (legado)"
                       : m.getTipo().equals("INGRESO") ? "Ingreso Extra (legado)" : "—")
                    : m.getCategoria();

                String estadoMostrar = "CUENTA_POR_COBRAR".equals(m.getTipo())
                    ? ("COBRADO".equals(m.getEstado()) ? "✅ Cobrado" : "⏳ Pendiente")
                    : "—";

                String montoMostrar = (esEntrada(m) ? "+$" : "−$") + String.format("%.2f", m.getMonto());

                tableModel.addRow(new Object[]{
                    m.getId(),
                    m.getFecha(),
                    tipoMostrar,
                    categoriaMostrar,
                    m.getDescripcion(),
                    montoMostrar,
                    estadoMostrar
                });
            }

            double saldoControl = totalEntradas - totalSalidas;

            double gananciaGeneral = 0;
            try {
                List<Venta> ventas = ventaDAO.listarTodas();
                for (Venta v : ventas) gananciaGeneral += v.getGanancia();
            } catch (Exception ex) {
                // Si Ventas no está disponible por algún motivo, se muestra 0
                // como referencia, sin romper el resto de Contabilidad.
            }

            lblGananciaGeneral.setText(String.format("$%.2f", gananciaGeneral));
            lblTotalEntradas.setText(String.format("$%.2f", totalEntradas));
            lblTotalSalidas.setText(String.format("$%.2f", totalSalidas));
            lblSaldoControl.setText(String.format("$%.2f", saldoControl));
            lblSaldoControl.setForeground(saldoControl >= 0 ? new Color(41, 128, 185) : COLOR_SALIDA);
            lblSaldoEstimado.setText(String.format("$%.2f", gananciaGeneral + saldoControl));
            lblPorCobrarPendiente.setText(String.format("$%.2f", totalCxCPendiente));

            try {
                txtNotas.setText(dao.obtenerNotas());
            } catch (Exception ex) {
                // Si aún no hay notas guardadas, se deja vacío sin problema
            }

        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al cargar contabilidad: " + ex.getMessage());
        }
    }
}
