package com.feedformulator.ui;

import com.feedformulator.dao.MovimientoInventarioDAO;
import com.feedformulator.dao.MateriaPrimaDAO;
import com.feedformulator.dao.DatabaseManager;
import com.feedformulator.model.MovimientoInventario;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.util.DensidadManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovimientosPanel extends JPanel {
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    private DefaultTableModel tableModel;
    private JTable tabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtBuscar;

    private static final Color VERDE = new Color(5, 150, 105);        // Esmeralda #059669
    private static final Color VERDE_STOCK = new Color(22, 163, 74);  // #16A34A
    private static final Color ROJO = new Color(220, 38, 38);
    private static final Color FONDO = new Color(248, 250, 252);      // #F8FAFC
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);  // #0F172A
    private static final Color BORDE_SUAVE = new Color(226, 232, 240);
    private static final Color GRIS_BOTON = new Color(100, 116, 139);

    private JLabel lblKpiProductos, lblKpiInvertido, lblKpiGastado, lblKpiBalance;
    private Map<String, String> codigoPorNombre = new HashMap<>();

    public MovimientosPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(crearTitulo(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(crearLeyenda(), BorderLayout.NORTH);
        centro.add(crearTabla(), BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);
        add(crearPanelSur(), BorderLayout.SOUTH);
        cargarDatos();
    }

    private JPanel crearTitulo() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel header = DialogStyler.crearHeaderPagina("📋", "Resumen de Movimientos por Producto",
            "Compras, usos y valor de cada materia prima");
        wrapper.add(header, BorderLayout.CENTER);
        return wrapper;
    }

    // ================================================================
    //  CAJA DE GUÍA (tarjeta con borde redondeado, 2 columnas)
    // ================================================================
    private JPanel crearLeyenda() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel lblTituloGuia = new JLabel("ⓘ  GUÍA DE INTERPRETACIÓN DE COLUMNAS");
        lblTituloGuia.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTituloGuia.setForeground(new Color(51, 65, 85));
        lblTituloGuia.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        p.add(lblTituloGuia, BorderLayout.NORTH);

        JPanel cols = new JPanel(new GridLayout(1, 2, 24, 0));
        cols.setOpaque(false);

        JPanel colIzq = new JPanel();
        colIzq.setLayout(new BoxLayout(colIzq, BoxLayout.Y_AXIS));
        colIzq.setOpaque(false);
        agregarPuntoGuia(colIzq, "Comprado:", "cantidad total que has ingresado de ese producto (desde Inventario o Pedidos).");
        agregarPuntoGuia(colIzq, "Stock actual:", "lo que debería quedar ahora mismo (Comprado − Usado).");
        agregarPuntoGuia(colIzq, "Dinero usado:", "el valor en $ de lo que ya se consumió en fórmulas (a precio de compra).");

        JPanel colDer = new JPanel();
        colDer.setLayout(new BoxLayout(colDer, BoxLayout.Y_AXIS));
        colDer.setOpaque(false);
        agregarPuntoGuia(colDer, "Usado en fórmulas:", "cantidad que ya se descontó al preparar formulaciones.");
        agregarPuntoGuia(colDer, "Dinero invertido:", "lo que has pagado en total por las compras de ese producto.");
        agregarPuntoGuia(colDer, "🖱️ Doble clic:", "en una fila para ver el detalle de cada movimiento individual.");

        cols.add(colIzq);
        cols.add(colDer);
        p.add(cols, BorderLayout.CENTER);
        return p;
    }

    private void agregarPuntoGuia(JPanel contenedor, String etiqueta, String texto) {
        JLabel lbl = new JLabel("<html><b style='color:#0F172A'>• " + etiqueta + "</b> " +
            "<span style='color:#475569'>" + texto + "</span></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        contenedor.add(lbl);
    }

    // ================================================================
    //  TABLA
    // ================================================================
    private JScrollPane crearTabla() {
        String[] cols = {"Producto", "Unidad", "Comprado", "Usado en fórmulas", "Stock actual", "Dinero invertido", "Dinero usado"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override 
            public boolean isCellEditable(int r, int c) { 
                return false; 
            }
        };
        tabla = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        tabla.setRowSorter(sorter);

        estilizarTabla(tabla);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(1).setMaxWidth(70);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(120);

        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tabla.getSelectedRow();
                    if (row >= 0) {
                        row = tabla.convertRowIndexToModel(row);
                        String nombreProducto = (String) tableModel.getValueAt(row, 0);
                        String unidadProducto = (String) tableModel.getValueAt(row, 1);
                        mostrarDetalleProducto(nombreProducto, unidadProducto);
                    }
                }
            }
        });

        // ---- Encabezado de la tabla: título + contador + buscador ----
        JPanel headerTabla = new JPanel(new BorderLayout());
        headerTabla.setOpaque(false);
        headerTabla.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel lblTituloTabla = new JLabel("⚖️ Balance de Materias Primas");
        lblTituloTabla.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerTabla.add(lblTituloTabla, BorderLayout.WEST);

        JPanel buscadorWrap = new JPanel(new BorderLayout());
        buscadorWrap.setBackground(Color.WHITE);
        buscadorWrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        JLabel lblLupa = new JLabel("🔍");
        lblLupa.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        lblLupa.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        buscadorWrap.add(lblLupa, BorderLayout.WEST);

        txtBuscar = new JTextField(20);
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.setBorder(BorderFactory.createEmptyBorder());
        txtBuscar.setToolTipText("Buscar por nombre, unidad o código...");
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });
        buscadorWrap.add(txtBuscar, BorderLayout.CENTER);
        headerTabla.add(buscadorWrap, BorderLayout.EAST);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setOpaque(false);
        panelTabla.add(headerTabla, BorderLayout.NORTH);
        JScrollPane scrollInterno = new JScrollPane(tabla);
        scrollInterno.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panelTabla.add(scrollInterno, BorderLayout.CENTER);

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
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String nombreCol = table.getColumnName(column);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                c.setHorizontalAlignment(SwingConstants.LEFT);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : FONDO);
                    c.setForeground(new Color(30, 41, 59));
                }

                if (nombreCol.equals("Producto") && value != null) {
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    String codigo = codigoPorNombre.get(value.toString());
                    if (codigo != null && !isSelected) {
                        c.setText("<html>" + value + "  <span style='color:#94A3B8;font-weight:normal;font-size:9px'>(" + codigo + ")</span></html>");
                    }
                } else if (nombreCol.equals("Comprado") || nombreCol.equals("Usado en fórmulas")) {
                    c.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (nombreCol.equals("Stock actual")) {
                    c.setHorizontalAlignment(SwingConstants.RIGHT);
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if (!isSelected) c.setForeground(VERDE_STOCK);
                } else if (nombreCol.equals("Dinero invertido") || nombreCol.equals("Dinero usado")) {
                    c.setHorizontalAlignment(SwingConstants.RIGHT);
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return c;
            }
        });
    }

    private void filtrar() {
        String texto = txtBuscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 0, 1));
        }
    }

    /**
     * Muestra el detalle movimiento por movimiento (no solo el resumen) para
     * un producto específico: fecha, tipo, cantidad y valor en $ de cada uno.
     */
    private void mostrarDetalleProducto(String nombreProducto, String unidad) {
        try {
            List<MovimientoInventario> todos = movimientoDAO.listarTodos();

            JDialog dialog = new JDialog(
                (Window) SwingUtilities.getWindowAncestor(this),
                "Detalle de movimientos: " + nombreProducto,
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(700, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            String[] colsDetalle = {"Fecha", "Hora", "Tipo", "Cantidad (" + unidad + ")", "Precio/unidad", "Valor ($)", "Referencia"};
            DefaultTableModel modeloDetalle = new DefaultTableModel(colsDetalle, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };

            for (MovimientoInventario m : todos) {
                if (!nombreProducto.equals(m.getNombreProducto())) continue;
                boolean esIngreso = "INGRESO".equals(m.getTipoMovimiento());
                modeloDetalle.addRow(new Object[]{
                    m.getFecha(),
                    m.getHora(),
                    esIngreso ? "🟢 Compra" : "🔴 Uso en fórmula",
                    String.format("%.2f", m.getCantidad()),
                    String.format("$%.4f", m.getPrecioPorUnidad()),
                    String.format("$%.2f", m.getSubtotal()),
                    m.getReferencia() != null ? m.getReferencia() : "-"
                });
            }

            if (modeloDetalle.getRowCount() == 0) {
                modeloDetalle.addRow(new Object[]{"-", "-", "Sin movimientos registrados", "-", "-", "-", "-"});
            }

            JTable tablaDetalle = new JTable(modeloDetalle);
            estilizarTabla(tablaDetalle);
            tablaDetalle.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

            JLabel lblTitulo = new JLabel("  Movimientos de: " + nombreProducto);
            lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblTitulo.setForeground(SLATE_OSCURO);
            lblTitulo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton btnCerrar = new JButton("Cerrar");
            btnCerrar.addActionListener(ev -> dialog.dispose());
            JPanel pSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            pSur.add(btnCerrar);

            dialog.add(lblTitulo, BorderLayout.NORTH);
            dialog.add(new JScrollPane(tablaDetalle), BorderLayout.CENTER);
            dialog.add(pSur, BorderLayout.SOUTH);
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar detalle: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    //  BARRA INFERIOR: botón Actualizar + tarjetas KPI
    // ================================================================
    private JPanel crearPanelSur() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel filaSuperior = new JPanel(new BorderLayout());
        filaSuperior.setOpaque(false);

        JButton btnRefresh;
        JButton btnExportarCsv;
        try {
            ImageIcon iconRefresh = new ImageIcon(getClass().getResource("/icons/refresh.png"));
            Image imgRefresh = iconRefresh.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btnRefresh = new JButton("Actualizar", new ImageIcon(imgRefresh));
        } catch (Exception e) {
            btnRefresh = new JButton("🔄 Actualizar");
        }
        btnRefresh.setBackground(VERDE);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        btnRefresh.addActionListener(evt -> cargarDatos());

        btnExportarCsv = new JButton("⬇️ Exportar CSV");
        btnExportarCsv.setBackground(GRIS_BOTON);
        btnExportarCsv.setForeground(Color.WHITE);
        btnExportarCsv.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportarCsv.setFocusPainted(false);
        btnExportarCsv.setBorderPainted(false);
        btnExportarCsv.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportarCsv.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        btnExportarCsv.addActionListener(evt -> exportarCSV());

        JPanel pBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pBotones.setOpaque(false);
        pBotones.add(btnRefresh);
        pBotones.add(btnExportarCsv);
        filaSuperior.add(pBotones, BorderLayout.WEST);

        JPanel kpis = new JPanel(new GridLayout(1, 3, 10, 0));
        kpis.setOpaque(false);

        lblKpiProductos = new JLabel("0 productos activos");
        lblKpiInvertido = new JLabel("Total Invertido: $0.00");
        lblKpiGastado = new JLabel("Total Gastado: $0.00");
        lblKpiBalance = new JLabel("Balance: $0.00");

        kpis.add(crearChipKpi("📦", lblKpiProductos, Color.WHITE, new Color(15, 23, 42), BORDE_SUAVE));
        kpis.add(crearChipKpi("📉", lblKpiInvertido, new Color(219, 234, 254), new Color(30, 64, 175), new Color(191, 219, 254)));
        kpis.add(crearChipKpi("📈", lblKpiGastado, new Color(254, 243, 199), new Color(146, 64, 14), new Color(253, 224, 138)));

        JPanel filaKpis = new JPanel(new BorderLayout(10, 0));
        filaKpis.setOpaque(false);
        filaKpis.add(kpis, BorderLayout.CENTER);
        filaKpis.add(crearChipKpi("💲", lblKpiBalance, new Color(209, 250, 229), new Color(6, 95, 70), new Color(167, 243, 208)), BorderLayout.EAST);

        p.add(filaSuperior, BorderLayout.NORTH);
        p.add(filaKpis, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearChipKpi(String icono, JLabel valor, Color fondo, Color colorTexto, Color colorBorde) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chip.setBackground(fondo);
        chip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorBorde, 1),
            BorderFactory.createEmptyBorder(9, 14, 9, 14)));

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        chip.add(lblIcono);

        valor.setFont(new Font("Segoe UI", Font.BOLD, 12));
        valor.setForeground(colorTexto);
        chip.add(valor);

        return chip;
    }

    private void exportarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Movimientos_Materias_Primas.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getAbsolutePath() + ".csv");
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            int cols = tableModel.getColumnCount();
            StringBuilder header = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                header.append(tableModel.getColumnName(c));
                if (c < cols - 1) header.append(";");
            }
            pw.println(header);

            for (int r = 0; r < tableModel.getRowCount(); r++) {
                StringBuilder linea = new StringBuilder();
                for (int c = 0; c < cols; c++) {
                    Object valor = tableModel.getValueAt(r, c);
                    linea.append(valor != null ? valor.toString() : "");
                    if (c < cols - 1) linea.append(";");
                }
                pw.println(linea);
            }

            DialogStyler.mostrarExito(this, "CSV exportado correctamente:<br><b>" + archivo.getAbsolutePath() + "</b>", "Exportado");
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al exportar CSV: " + ex.getMessage());
        }
    }

    public void cargarDatos() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        try {
            tableModel.setRowCount(0);
            codigoPorNombre.clear();
            
            // IMPORTANTE: antes esto se calculaba sumando el historial de
            // "movimientos_inventario" completo desde el inicio de los tiempos.
            // El problema es que ese historial NO se corrige cuando se edita o
            // elimina algo (por ejemplo, al eliminar una fórmula se devuelve el
            // stock, pero el movimiento de "uso" ya registrado se queda ahí),
            // así que con el tiempo los números se desincronizan de la realidad.
            //
            // Ahora se calcula directamente de las tablas que SÍ reflejan el
            // estado real y actual:
            //   - Stock actual  -> materias_primas (siempre correcto, es lo que se ve en Inventario)
            //   - Usado         -> SUM(formulacion_detalle.cantidad_usada) de fórmulas que
            //                      existen ahora mismo (si una fórmula se elimina, sus filas
            //                      de detalle se borran en cascada, así que esto ya no cuenta
            //                      usos que fueron revertidos)
            //   - Comprado      -> Usado + Stock actual (por definición: todo lo que entró
            //                      o se usó, o sigue en stock)
            //   - Dinero usado  -> SUM(formulacion_detalle.subtotal) de esas mismas fórmulas activas
            //   - Dinero invertido -> Dinero usado + (Stock actual x precio actual por unidad)
            
            Map<Integer, ResumenProducto> resumenMap = new HashMap<>();
            
            for (MateriaPrima mp : materiaDAO.listarTodos()) {
                ResumenProducto rp = new ResumenProducto(mp.getNombre(), mp.getUnidad());
                rp.stockActual = mp.getCantidadDisponible();
                rp.precioActual = mp.getPrecioPorUnidad();
                resumenMap.put(mp.getId(), rp);
                if (mp.getCodigo() != null) {
                    codigoPorNombre.put(mp.getNombre(), mp.getCodigo());
                }
            }
            
            for (DetalleUso d : obtenerUsoActivoPorProducto()) {
                ResumenProducto rp = resumenMap.get(d.materiaPrimaId);
                if (rp != null) {
                    // IMPORTANTE: para productos líquidos, formulacion_detalle.cantidad_usada
                    // guarda el equivalente en LIBRAS (así se calcula el costo/FIFO
                    // internamente), no en litros. Si no se reconvierte aquí, "Usado en
                    // fórmulas" mostraba el número en libras mezclado con una tabla que
                    // dice "litro" como unidad — un desajuste confuso e incorrecto.
                    double cantidadEnUnidadCorrecta = d.cantidadUsada;
                    if ("litro".equals(rp.unidad) || "litros".equals(rp.unidad)) {
                        cantidadEnUnidadCorrecta = DensidadManager.convertirLibrasALitros(d.cantidadUsada, rp.nombre);
                    }
                    rp.totalUsado += cantidadEnUnidadCorrecta;
                    rp.dineroUsado += d.subtotal;
                }
            }
            
            double granTotalInvertido = 0;
            double granTotalGastado = 0;
            
            for (ResumenProducto rp : resumenMap.values()) {
                double comprado = rp.totalUsado + rp.stockActual;
                double dineroInvertido = rp.dineroUsado + (rp.stockActual * rp.precioActual);
                
                tableModel.addRow(new Object[]{
                    rp.nombre,
                    rp.unidad,
                    String.format("%.2f", comprado),
                    String.format("%.2f", rp.totalUsado),
                    String.format("%.2f", rp.stockActual),
                    String.format("$%.2f", dineroInvertido),
                    String.format("$%.2f", rp.dineroUsado)
                });
                
                granTotalInvertido += dineroInvertido;
                granTotalGastado += rp.dineroUsado;
            }
            
            lblKpiProductos.setText(resumenMap.size() + " productos activos");
            lblKpiInvertido.setText(String.format("Total Invertido: $%.2f", granTotalInvertido));
            lblKpiGastado.setText(String.format("Total Gastado: $%.2f", granTotalGastado));
            lblKpiBalance.setText(String.format("Balance: $%.2f", granTotalInvertido - granTotalGastado));
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private List<DetalleUso> obtenerUsoActivoPorProducto() throws SQLException {
        List<DetalleUso> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(
                "SELECT materia_prima_id, SUM(cantidad_usada) as total_cantidad, SUM(subtotal) as total_subtotal " +
                "FROM formulacion_detalle GROUP BY materia_prima_id"
            );
            while (rs.next()) {
                DetalleUso d = new DetalleUso();
                d.materiaPrimaId = rs.getInt("materia_prima_id");
                d.cantidadUsada = rs.getDouble("total_cantidad");
                d.subtotal = rs.getDouble("total_subtotal");
                lista.add(d);
            }
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    private static class DetalleUso {
        int materiaPrimaId;
        double cantidadUsada;
        double subtotal;
    }

    private static class ResumenProducto {
        String nombre;
        String unidad;
        double stockActual = 0;
        double precioActual = 0;
        double totalUsado = 0;
        double dineroUsado = 0;
        
        ResumenProducto(String nombre, String unidad) {
            this.nombre = nombre;
            this.unidad = unidad;
        }
    }
}
