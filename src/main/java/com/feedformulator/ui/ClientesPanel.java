package com.feedformulator.ui;

import com.feedformulator.dao.ClienteDAO;
import com.feedformulator.dao.VentaCreditoDAO;
import com.feedformulator.model.Cliente;
import com.feedformulator.model.VentaCredito;
import com.feedformulator.model.Abono;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Panel de Gestión de Clientes y Cartera de Créditos.
 * Diseñado con estética idéntica a la vista web: pastillas de cliente,
 * segmentador de pestañas redondeado, botones píldora y tabla Slate.
 */
public class ClientesPanel extends JPanel {
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VentaCreditoDAO ventaCreditoDAO = new VentaCreditoDAO();
    
    private DefaultTableModel tableModelClientes;
    private DefaultTableModel tableModelCreditos;
    private DefaultTableModel tableModelAbonos;
    private JTable tablaClientes;
    private JTable tablaCreditos;
    private JTable tablaAbonos;
    
    private JLabel lblTotalClientes;
    private JLabel lblTotalPendiente;
    private JLabel lblTotalAbonado;
    
    // Elementos del encabezado inferior idénticos a la imagen
    private PildoraClienteBadge pildoraCliente;
    private CardLayout cardLayoutCreditos;
    private JPanel panelCardsCreditos;
    private BotonSegmentadoTab btnTabCreditos;
    private BotonSegmentadoTab btnTabAbonos;
    private int cantidadCreditosActual = 0;
    private int cantidadAbonosActual = 0;

    private JTextField txtBuscarCliente;
    private TableRowSorter<DefaultTableModel> sorterClientes;
    
    // Paleta de Colores idéntica a la interfaz moderna
    private static final Color VERDE_ACCION = new Color(5, 150, 105);      // #059669
    private static final Color AZUL_ACCION = new Color(37, 99, 235);       // #2563EB
    private static final Color ROSA_ROJO = new Color(244, 63, 94);         // #F43F5E
    private static final Color FONDO = new Color(248, 250, 252);           // #F8FAFC
    private static final Color SLATE_OSCURO = new Color(15, 23, 42);       // #0F172A
    private static final Color BORDE_SUAVE = new Color(226, 232, 240);     // #E2E8F0
    private static final Color TEXTO_MUTED = new Color(100, 116, 139);     // #64748B
    private static final Color TEAL_PILDORA_BG = new Color(240, 253, 250); // #F0FDFA
    private static final Color TEAL_PILDORA_BORDER = new Color(153, 246, 228); // #99F6E4
    private static final Color TEAL_PILDORA_TEXT = new Color(19, 78, 74);  // #134E4A

    public ClientesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        
        add(crearTitulo(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelResumen(), BorderLayout.SOUTH);
        
        cargarDatos();
    }
    
    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("👥", "Gestión de Clientes y Créditos", "Clientes registrados, cartera de créditos y abonos recibidos");
    }
    
    private JSplitPane crearPanelCentral() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.38);
        split.setBackground(FONDO);
        split.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        split.setDividerSize(6);
        
        // ============================================================
        // 1. PANEL SUPERIOR: CLIENTES REGISTRADOS
        // ============================================================
        JPanel panelClientes = new JPanel(new BorderLayout(0, 6));
        panelClientes.setBackground(Color.WHITE);
        panelClientes.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        
        JPanel headerClientes = new JPanel(new BorderLayout(8, 0));
        headerClientes.setOpaque(false);
        
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tituloPanel.setOpaque(false);
        JLabel lblClientes = new JLabel("Clientes Registrados");
        lblClientes.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblClientes.setForeground(SLATE_OSCURO);
        try {
            ImageIcon iconClipboard = new ImageIcon(getClass().getResource("/icons/clipboard-icon.png"));
            Image imgClipboard = iconClipboard.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            lblClientes.setIcon(new ImageIcon(imgClipboard));
        } catch (Exception e) {
            lblClientes.setText("📋 Clientes Registrados");
        }
        tituloPanel.add(lblClientes);
        headerClientes.add(tituloPanel, BorderLayout.WEST);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setOpaque(false);
        
        JLabel lblBuscar = new JLabel("🔍");
        try {
            ImageIcon iconSearch = new ImageIcon(getClass().getResource("/icons/magnifying-glass.png"));
            Image imgSearch = iconSearch.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            lblBuscar.setIcon(new ImageIcon(imgSearch));
            lblBuscar.setText("");
        } catch (Exception ex) {}
        btnPanel.add(lblBuscar);
        
        txtBuscarCliente = new JTextField(18);
        txtBuscarCliente.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscarCliente.setToolTipText("Buscar cliente por nombre, código o cédula");
        txtBuscarCliente.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrarClientes();
            }
        });
        btnPanel.add(txtBuscarCliente);
        
        JButton btnNuevo = crearBotonCuadrado("+ Nuevo", "/icons/add.png", "➕", VERDE_ACCION);
        JButton btnEditar = crearBotonCuadrado("Editar", "/icons/edit.png", "✏️", new Color(217, 119, 6));
        JButton btnEliminar = crearBotonCuadrado("Eliminar", "/icons/delete.png", "🗑️", new Color(220, 38, 38));
        JButton btnRefrescar = crearBotonCuadrado("Refrescar", "/icons/refresh.png", "🔄", new Color(100, 116, 139));
        
        btnNuevo.addActionListener(evt -> mostrarDialogoNuevoCliente());
        btnEditar.addActionListener(evt -> editarCliente());
        btnEliminar.addActionListener(evt -> eliminarCliente());
        btnRefrescar.addActionListener(evt -> cargarDatos());
        
        btnPanel.add(btnNuevo);
        btnPanel.add(btnEditar);
        btnPanel.add(btnEliminar);
        btnPanel.add(btnRefrescar);
        headerClientes.add(btnPanel, BorderLayout.EAST);
        panelClientes.add(headerClientes, BorderLayout.NORTH);
        
        String[] colsClientes = {"Código", "Nombre / Razón Social", "Teléfono", "Email", "RUC / Cédula", "Estado"};
        tableModelClientes = new DefaultTableModel(colsClientes, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaClientes = new JTable(tableModelClientes);
        sorterClientes = new TableRowSorter<>(tableModelClientes);
        tablaClientes.setRowSorter(sorterClientes);
        estilizarTabla(tablaClientes, SLATE_OSCURO);
        
        tablaClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaClientes.getSelectedRow() >= 0) {
                    editarCliente();
                }
            }
        });
        
        JScrollPane scrollClientes = new JScrollPane(tablaClientes);
        scrollClientes.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panelClientes.add(scrollClientes, BorderLayout.CENTER);
        
        // ============================================================
        // 2. PANEL INFERIOR: DISEÑO EXACTO A LA IMAGEN (CRÉDITOS Y ABONOS)
        // ============================================================
        JPanel panelCreditos = new JPanel(new BorderLayout(0, 10));
        panelCreditos.setBackground(Color.WHITE);
        panelCreditos.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE_SUAVE, 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        
        // Encabezado con 2 Filas
        JPanel pnlHeaderCreditos = new JPanel(new BorderLayout(10, 8));
        pnlHeaderCreditos.setOpaque(false);
        
        // Lado Izquierdo: Título con icono de tarjeta + Píldora del Cliente
        JPanel pnlIzq = new JPanel();
        pnlIzq.setLayout(new BoxLayout(pnlIzq, BoxLayout.Y_AXIS));
        pnlIzq.setOpaque(false);
        
        JPanel pnlTituloLado = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlTituloLado.setOpaque(false);
        
        JLabel lblIconoTarjeta = new JLabel("💳");
        lblIconoTarjeta.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        lblIconoTarjeta.setForeground(VERDE_ACCION);
        JLabel lblTituloCred = new JLabel("Créditos del Cliente");
        lblTituloCred.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTituloCred.setForeground(SLATE_OSCURO);
        
        pnlTituloLado.add(lblIconoTarjeta);
        pnlTituloLado.add(lblTituloCred);
        pnlIzq.add(pnlTituloLado);
        pnlIzq.add(Box.createVerticalStrut(6));
        
        // Píldora del cliente (idéntica a la imagen: "ADRIAN XPRES (ADR-001)")
        pildoraCliente = new PildoraClienteBadge("Seleccione un cliente");
        JPanel wrapPildora = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        wrapPildora.setOpaque(false);
        wrapPildora.add(pildoraCliente);
        pnlIzq.add(wrapPildora);
        
        pnlHeaderCreditos.add(pnlIzq, BorderLayout.WEST);
        
        // Lado Derecho: Segmentador tipo pastilla arriba + Botones Píldora abajo
        JPanel pnlDer = new JPanel();
        pnlDer.setLayout(new BoxLayout(pnlDer, BoxLayout.Y_AXIS));
        pnlDer.setOpaque(false);
        
        // Fila 1 Derecha: Segmentador de Pestañas [ Créditos (1) ] [ Abonos (1) ]
        JPanel wrapSegmentador = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrapSegmentador.setOpaque(false);
        
        JPanel pnlSegmentado = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249)); // Slate-100
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(BORDE_SUAVE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlSegmentado.setOpaque(false);
        pnlSegmentado.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        
        btnTabCreditos = new BotonSegmentadoTab("Créditos", 0, true);
        btnTabAbonos = new BotonSegmentadoTab("Abonos", 0, false);
        
        btnTabCreditos.addActionListener(e -> activarTabCreditos(true));
        btnTabAbonos.addActionListener(e -> activarTabCreditos(false));
        
        pnlSegmentado.add(btnTabCreditos);
        pnlSegmentado.add(btnTabAbonos);
        wrapSegmentador.add(pnlSegmentado);
        pnlDer.add(wrapSegmentador);
        pnlDer.add(Box.createVerticalStrut(6));
        
        // Fila 2 Derecha: 3 Botones Píldora idénticos a la captura
        JPanel pnlBotonesPildora = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlBotonesPildora.setOpaque(false);
        
        BotonPildoraRedondeada btnAbonar = new BotonPildoraRedondeada("$ Abonar", VERDE_ACCION);
        BotonPildoraRedondeada btnCancelar = new BotonPildoraRedondeada("✔ Cancelar Deuda", AZUL_ACCION);
        BotonPildoraRedondeada btnEliminarAbono = new BotonPildoraRedondeada("🗑 Eliminar Abono", ROSA_ROJO);
        
        btnAbonar.addActionListener(evt -> registrarAbono());
        btnCancelar.addActionListener(evt -> cancelarDeuda());
        btnEliminarAbono.addActionListener(evt -> eliminarAbono());
        
        pnlBotonesPildora.add(btnAbonar);
        pnlBotonesPildora.add(btnCancelar);
        pnlBotonesPildora.add(btnEliminarAbono);
        pnlDer.add(pnlBotonesPildora);
        
        pnlHeaderCreditos.add(pnlDer, BorderLayout.EAST);
        panelCreditos.add(pnlHeaderCreditos, BorderLayout.NORTH);
        
        // Contenedor CardLayout para las dos tablas (Créditos y Abonos)
        cardLayoutCreditos = new CardLayout();
        panelCardsCreditos = new JPanel(cardLayoutCreditos);
        panelCardsCreditos.setOpaque(false);
        
        // TABLA CRÉDITOS
        String[] colsCreditos = {"ID", "PRODUCTO", "LIBRAS", "TOTAL", "ABONADO", "PENDIENTE", "ESTADO", "FECHA"};
        tableModelCreditos = new DefaultTableModel(colsCreditos, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaCreditos = new JTable(tableModelCreditos);
        estilizarTabla(tablaCreditos, SLATE_OSCURO);
        
        tablaCreditos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaCreditos.getSelectedRow() >= 0) {
                    registrarAbono();
                }
            }
        });
        
        JScrollPane scrollCreditos = new JScrollPane(tablaCreditos);
        scrollCreditos.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panelCardsCreditos.add(scrollCreditos, "CREDITOS");
        
        // TABLA ABONOS
        String[] colsAbonos = {"ID ABONO", "MONTO ABONADO", "FORMA DE PAGO", "FECHA", "HORA"};
        tableModelAbonos = new DefaultTableModel(colsAbonos, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaAbonos = new JTable(tableModelAbonos);
        estilizarTabla(tablaAbonos, SLATE_OSCURO);
        JScrollPane scrollAbonos = new JScrollPane(tablaAbonos);
        scrollAbonos.setBorder(BorderFactory.createLineBorder(BORDE_SUAVE, 1));
        panelCardsCreditos.add(scrollAbonos, "ABONOS");
        
        panelCreditos.add(panelCardsCreditos, BorderLayout.CENTER);
        
        // Sincronización de selección de clientes
        tablaClientes.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                cargarCreditosCliente();
                tableModelAbonos.setRowCount(0);
                cantidadAbonosActual = 0;
                btnTabAbonos.actualizarConteo(0);
                tablaCreditos.clearSelection();
                activarTabCreditos(true);
            }
        });
        
        tablaCreditos.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                cargarAbonosCliente();
            }
        });
        
        split.setTopComponent(panelClientes);
        split.setBottomComponent(panelCreditos);
        
        return split;
    }
    
    private void activarTabCreditos(boolean verCreditos) {
        btnTabCreditos.setActivo(verCreditos);
        btnTabAbonos.setActivo(!verCreditos);
        cardLayoutCreditos.show(panelCardsCreditos, verCreditos ? "CREDITOS" : "ABONOS");
    }
    
    // ============================================================
    // COMPONENTES GRÁFICOS PERSONALIZADOS
    // ============================================================
    
    /** Píldora del Cliente con fondo menta y borde esmeralda */
    private static class PildoraClienteBadge extends JPanel {
        private final JLabel lblTexto;
        
        public PildoraClienteBadge(String textoInicial) {
            setLayout(new FlowLayout(FlowLayout.CENTER, 8, 3));
            setOpaque(false);
            
            lblTexto = new JLabel(textoInicial);
            lblTexto.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblTexto.setForeground(TEAL_PILDORA_TEXT);
            add(lblTexto);
        }
        
        public void setTextoCliente(String nombre, String codigo) {
            lblTexto.setText(nombre + "  (" + codigo + ")");
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEAL_PILDORA_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(TEAL_PILDORA_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    /** Botón de pestaña segmentada tipo pastilla [ Créditos 1 ] [ Abonos 1 ] */
    private static class BotonSegmentadoTab extends JButton {
        private boolean activo;
        private final String textoBase;
        private int conteo;
        
        public BotonSegmentadoTab(String texto, int conteo, boolean activo) {
            this.textoBase = texto;
            this.conteo = conteo;
            this.activo = activo;
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            actualizarLabel();
        }
        
        public void setActivo(boolean activo) {
            this.activo = activo;
            actualizarLabel();
            repaint();
        }
        
        public void actualizarConteo(int conteo) {
            this.conteo = conteo;
            actualizarLabel();
            repaint();
        }
        
        private void actualizarLabel() {
            String icono = "Créditos".equals(textoBase) ? "💳 " : "💵 ";
            setText(icono + textoBase + "  " + conteo);
            setForeground(activo ? SLATE_OSCURO : TEXTO_MUTED);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            if (activo) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
    
    /** Botón de acción redondeado tipo Píldora ($ Abonar, ✔ Cancelar, etc.) */
    private static class BotonPildoraRedondeada extends JButton {
        private final Color colorFondo;
        
        public BotonPildoraRedondeada(String texto, Color colorFondo) {
            super(texto);
            this.colorFondo = colorFondo;
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color colorFinal = getModel().isPressed() ? colorFondo.darker() : 
                              (getModel().isRollover() ? colorFondo.brighter() : colorFondo);
            
            g2.setColor(colorFinal);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    private JButton crearBotonCuadrado(String texto, String iconoPath, String emojiFallback, Color colorFondo) {
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
        btn.setBorder(BorderFactory.createEmptyBorder(6, 11, 6, 11));
        return btn;
    }
    
    // ============================================================
    // 3. BARRA INFERIOR DE RESUMEN / KPIS
    // ============================================================
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        lblTotalClientes = new JLabel("0", SwingConstants.LEFT);
        lblTotalPendiente = new JLabel("$0.00", SwingConstants.LEFT);
        lblTotalAbonado = new JLabel("$0.00", SwingConstants.LEFT);

        panel.add(crearChipResumen("👤", "CLIENTES ACTIVOS", lblTotalClientes, Color.WHITE, SLATE_OSCURO, BORDE_SUAVE));
        panel.add(crearChipResumen("⚠️", "CARTERA PENDIENTE (POR COBRAR)", lblTotalPendiente, new Color(254, 226, 226), new Color(185, 28, 28), new Color(252, 165, 165)));
        panel.add(crearChipResumen("💳", "TOTAL ABONADO / RECAUDADO", lblTotalAbonado, new Color(209, 250, 229), new Color(4, 120, 87), new Color(167, 243, 208)));

        return panel;
    }

    private JPanel crearChipResumen(String icono, String titulo, JLabel valor, Color fondo, Color colorTexto, Color colorBorde) {
        JPanel chip = new JPanel(new BorderLayout(10, 0));
        chip.setBackground(fondo);
        chip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorBorde, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        chip.add(lblIcono, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitulo.setForeground(colorTexto.equals(SLATE_OSCURO) ? TEXTO_MUTED : colorTexto);
        textos.add(lblTitulo);

        valor.setFont(new Font("Segoe UI", Font.BOLD, 17));
        valor.setForeground(colorTexto);
        textos.add(valor);

        chip.add(textos, BorderLayout.CENTER);
        return chip;
    }
    
    // ============================================================
    // ESTILIZADOR DE TABLAS CON BADGES Y ENCABEZADO OSCURO SLATE
    // ============================================================
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
                String nombreCol = table.getColumnName(column).toUpperCase();
                boolean esNumerica = nombreCol.contains("LIBRAS") || nombreCol.contains("TOTAL")
                    || nombreCol.contains("ABONADO") || nombreCol.contains("PENDIENTE")
                    || nombreCol.contains("MONTO") || nombreCol.equals("ID");
                boolean esEstado = nombreCol.contains("ESTADO");

                if (esEstado && value != null) {
                    String estado = value.toString().toUpperCase().trim();
                    JLabel pastilla = new JLabel(estado);
                    pastilla.setHorizontalAlignment(SwingConstants.CENTER);
                    pastilla.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    pastilla.setOpaque(true);
                    pastilla.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                    
                    if (estado.contains("PAGADO") || estado.contains("ACTIVO") || estado.contains("CANCELADO")) {
                        pastilla.setBackground(new Color(209, 250, 229)); // Verde menta
                        pastilla.setForeground(new Color(6, 95, 70));
                    } else if (estado.contains("PARCIAL") || estado.contains("ABONADO")) {
                        pastilla.setBackground(new Color(219, 234, 254)); // Azul
                        pastilla.setForeground(new Color(30, 64, 175));
                    } else { // PENDIENTE
                        pastilla.setBackground(new Color(254, 243, 199)); // Ámbar cálido como en la foto
                        pastilla.setForeground(new Color(146, 64, 14));
                    }
                    
                    JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
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
                    
                    if (nombreCol.contains("PENDIENTE") && value != null) {
                        try {
                            double v = Double.parseDouble(value.toString().replace("$", "").replace(",", "."));
                            if (v > 0) {
                                c.setForeground(ROSA_ROJO);
                                c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            }
                        } catch (Exception ignored) {}
                    }
                    if (nombreCol.contains("ABONADO")) {
                        c.setForeground(VERDE_ACCION);
                    }
                }
                return c;
            }
        });
    }
    
    private void filtrarClientes() {
        String texto = txtBuscarCliente.getText().trim().toLowerCase();
        if (texto.isEmpty()) {
            sorterClientes.setRowFilter(null);
        } else {
            sorterClientes.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 0, 1, 4));
        }
    }
    
    public void cargarDatos() {
        cargarClientes();
        actualizarResumen();
        txtBuscarCliente.setText("");
        filtrarClientes();
    }
    
    private void cargarClientes() {
        try {
            tableModelClientes.setRowCount(0);
            List<Cliente> clientes = clienteDAO.listarTodos();
            
            for (Cliente c : clientes) {
                tableModelClientes.addRow(new Object[]{
                    c.getCodigo(),
                    c.getNombre(),
                    c.getTelefono() != null ? c.getTelefono() : "-",
                    c.getEmail() != null ? c.getEmail() : "-",
                    c.getRucCedula() != null ? c.getRucCedula() : "-",
                    c.getEstado()
                });
            }
            
            lblTotalClientes.setText(String.valueOf(clientes.size()) + " Registrados");
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar clientes: " + e.getMessage());
        }
    }
    
    private void cargarCreditosCliente() {
        int row = tablaClientes.getSelectedRow();
        if (row < 0) {
            tableModelCreditos.setRowCount(0);
            pildoraCliente.setTextoCliente("Seleccione un cliente", "---");
            cantidadCreditosActual = 0;
            btnTabCreditos.actualizarConteo(0);
            return;
        }
        
        try {
            int modelRow = tablaClientes.convertRowIndexToModel(row);
            String codigo = (String) tableModelClientes.getValueAt(modelRow, 0);
            String nombre = (String) tableModelClientes.getValueAt(modelRow, 1);
            Cliente cliente = clienteDAO.buscarPorCodigo(codigo);
            
            if (cliente == null) {
                tableModelCreditos.setRowCount(0);
                return;
            }
            
            pildoraCliente.setTextoCliente(nombre, codigo);
            
            tableModelCreditos.setRowCount(0);
            List<VentaCredito> creditos = ventaCreditoDAO.listarPorCliente(cliente.getId());
            
            for (VentaCredito vc : creditos) {
                tableModelCreditos.addRow(new Object[]{
                    vc.getId(),
                    vc.getNombreProducto(),
                    String.format("%.2f lb", vc.getTotalLibras()),
                    String.format("$%.2f", vc.getPrecioTotal()),
                    String.format("$%.2f", vc.getAbonado()),
                    String.format("$%.2f", vc.getSaldoPendiente()),
                    vc.getEstado(),
                    vc.getFechaVenta()
                });
            }
            
            cantidadCreditosActual = creditos.size();
            btnTabCreditos.actualizarConteo(cantidadCreditosActual);
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar créditos: " + e.getMessage());
        }
    }
    
    private void cargarAbonosCliente() {
        int rowCredito = tablaCreditos.getSelectedRow();
        if (rowCredito < 0) {
            tableModelAbonos.setRowCount(0);
            cantidadAbonosActual = 0;
            btnTabAbonos.actualizarConteo(0);
            return;
        }
        
        try {
            int creditoId = (int) tableModelCreditos.getValueAt(rowCredito, 0);
            tableModelAbonos.setRowCount(0);
            List<Abono> abonos = ventaCreditoDAO.listarAbonosPorCredito(creditoId);
            
            if (abonos.isEmpty()) {
                tableModelAbonos.addRow(new Object[]{"-", "$0.00", "Sin abonos registrados", "-", "-"});
                cantidadAbonosActual = 0;
                btnTabAbonos.actualizarConteo(0);
                return;
            }
            
            for (Abono a : abonos) {
                tableModelAbonos.addRow(new Object[]{
                    "#" + a.getId(),
                    String.format("$%.2f", a.getMonto()),
                    a.getFormaPago(),
                    a.getFechaAbono(),
                    a.getHoraAbono()
                });
            }
            
            cantidadAbonosActual = abonos.size();
            btnTabAbonos.actualizarConteo(cantidadAbonosActual);
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar abonos: " + e.getMessage());
        }
    }
    
    private void actualizarResumen() {
        try {
            double totalPendiente = ventaCreditoDAO.getTotalPendiente();
            double totalAbonado = ventaCreditoDAO.getTotalAbonado();
            lblTotalPendiente.setText("$" + String.format("%.2f", totalPendiente));
            lblTotalAbonado.setText("$" + String.format("%.2f", totalAbonado));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ============================================================
    // DIÁLOGOS DE CLIENTES Y TRANSACCIONES
    // ============================================================
    private void mostrarDialogoNuevoCliente() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Registrar Nuevo Cliente", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(520, 560);
        dialog.setLocationRelativeTo(this);
        DialogStyler.aplicar(dialog, "Registrar Nuevo Cliente", "👤");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

        JTextField txtNombre = DialogStyler.crearCampo(20);
        JTextField txtTelefono = DialogStyler.crearCampoNumerico(20);
        JTextField txtEmail = DialogStyler.crearCampo(20);
        JTextField txtDireccion = DialogStyler.crearCampo(20);
        JTextField txtRuc = DialogStyler.crearCampo(20);

        int fila = 0;
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Nombre / Razón Social:*"), gbc);
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
        gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("RUC / Cédula:"), gbc);
        gbc.gridx = 1; form.add(txtRuc, gbc);

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Cliente");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "El nombre del cliente no puede estar vacío.");
                    txtNombre.requestFocus();
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

                dialog.dispose();
                cargarDatos();

                DialogStyler.mostrarExito(this,
                    "Cliente <b>" + nombre + "</b> guardado correctamente.<br>Código asignado: <b>" + cliente.getCodigo() + "</b>",
                    "Éxito"
                );

            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void editarCliente() {
        int row = tablaClientes.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un cliente de la tabla para editar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tablaClientes.convertRowIndexToModel(row);
            String codigo = (String) tableModelClientes.getValueAt(modelRow, 0);
            Cliente cliente = clienteDAO.buscarPorCodigo(codigo);
            
            if (cliente == null) {
                DialogStyler.mostrarError(this, "Cliente no encontrado.");
                return;
            }
            
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Editar Cliente - " + cliente.getCodigo(), Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(520, 600);
            dialog.setLocationRelativeTo(this);
            DialogStyler.aplicar(dialog, "Editar Cliente", "✏️");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            JTextField txtNombre = DialogStyler.crearCampo(20);
            txtNombre.setText(cliente.getNombre());
            JTextField txtTelefono = DialogStyler.crearCampoNumerico(20);
            txtTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "");
            JTextField txtEmail = DialogStyler.crearCampo(20);
            txtEmail.setText(cliente.getEmail() != null ? cliente.getEmail() : "");
            JTextField txtDireccion = DialogStyler.crearCampo(20);
            txtDireccion.setText(cliente.getDireccion() != null ? cliente.getDireccion() : "");
            JTextField txtRuc = DialogStyler.crearCampo(20);
            txtRuc.setText(cliente.getRucCedula() != null ? cliente.getRucCedula() : "");
            JComboBox<String> cmbEstado = DialogStyler.crearCombo(new String[]{"ACTIVO", "INACTIVO"});
            cmbEstado.setSelectedItem(cliente.getEstado());

            int fila = 0;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Código:"), gbc);
            gbc.gridx = 1;
            JLabel lblCodigo = new JLabel(cliente.getCodigo());
            lblCodigo.setFont(DialogStyler.FUENTE_CAMPO);
            lblCodigo.setForeground(SLATE_OSCURO);
            form.add(lblCodigo, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Nombre / Razón:*"), gbc);
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
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("RUC / Cédula:"), gbc);
            gbc.gridx = 1; form.add(txtRuc, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila; form.add(DialogStyler.crearLabel("Estado:"), gbc);
            gbc.gridx = 1; form.add(cmbEstado, gbc);

            JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Cambios");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(e -> {
                try {
                    String nombre = txtNombre.getText().trim();
                    if (nombre.isEmpty()) {
                        DialogStyler.mostrarError(dialog, "El nombre del cliente no puede estar vacío.");
                        return;
                    }

                    cliente.setNombre(nombre);
                    cliente.setTelefono(txtTelefono.getText().trim());
                    cliente.setEmail(txtEmail.getText().trim());
                    cliente.setDireccion(txtDireccion.getText().trim());
                    cliente.setRucCedula(txtRuc.getText().trim());
                    cliente.setEstado((String) cmbEstado.getSelectedItem());

                    clienteDAO.actualizar(cliente);

                    dialog.dispose();
                    cargarDatos();

                    DialogStyler.mostrarExito(this, "Cliente <b>" + nombre + "</b> actualizado correctamente.", "Éxito");

                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
        }
    }
    
    private void eliminarCliente() {
        int row = tablaClientes.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un cliente para eliminar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tablaClientes.convertRowIndexToModel(row);
            String codigo = (String) tableModelClientes.getValueAt(modelRow, 0);
            String nombre = (String) tableModelClientes.getValueAt(modelRow, 1);
            
            Cliente cliente = clienteDAO.buscarPorCodigo(codigo);
            if (cliente == null) return;
            
            if (clienteDAO.tieneCreditosPendientes(cliente.getId())) {
                DialogStyler.mostrarError(this,
                    "NO SE PUEDE ELIMINAR<br><br>" +
                    "El cliente <b>" + nombre + "</b> tiene créditos con saldo pendiente.<br>" +
                    "Primero debe liquidar o cancelar todas sus deudas."
                );
                return;
            }
            
            String mensaje = "⚠️ ¿Eliminar el cliente <b>" + nombre + "</b>?<br><br>" +
                "Esta acción eliminará todos los registros y su historial de abonos.<br>" +
                "¿Está completamente seguro?";
            
            if (!DialogStyler.confirmar(this, mensaje, "Eliminar cliente")) return;
            
            List<VentaCredito> creditos = ventaCreditoDAO.listarPorCliente(cliente.getId());
            for (VentaCredito vc : creditos) {
                ventaCreditoDAO.eliminar(vc.getId());
            }
            
            clienteDAO.eliminar(cliente.getId());
            cargarDatos();
            
            DialogStyler.mostrarExito(this, "Cliente <b>" + nombre + "</b> eliminado correctamente.", "Éxito");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar cliente: " + ex.getMessage());
        }
    }
    
    private void registrarAbono() {
        int rowCredito = tablaCreditos.getSelectedRow();
        if (rowCredito < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un crédito de la lista para registrar abono.", "Aviso");
            return;
        }
        
        try {
            int creditoId = (int) tableModelCreditos.getValueAt(rowCredito, 0);
            String producto = (String) tableModelCreditos.getValueAt(rowCredito, 1);
            double pendiente = Double.parseDouble(((String) tableModelCreditos.getValueAt(rowCredito, 5)).replace("$", "").replace(",", "."));
            
            if (pendiente <= 0) {
                DialogStyler.mostrarAdvertencia(this, "Este crédito ya se encuentra totalmente cancelado.", "Aviso");
                return;
            }
            
            String input = JOptionPane.showInputDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:13px;padding:8px;'>" +
                "<b>REGISTRAR ABONO A CRÉDITO #" + creditoId + "</b><br>" +
                "Producto: <i>" + producto + "</i><br><br>" +
                "Saldo pendiente actual: <b style='color:#DC2626'>$" + String.format("%.2f", pendiente) + "</b><br><br>" +
                "Ingrese el monto a abonar en dólares ($):" +
                "</body></html>",
                "Registrar Abono",
                JOptionPane.QUESTION_MESSAGE);
            
            if (input == null) return;
            
            String inputClean = input.trim().replace(",", ".");
            if (!inputClean.matches("[0-9.]*")) {
                DialogStyler.mostrarError(this, "Ingrese un valor numérico válido.");
                return;
            }
            
            double monto = Double.parseDouble(inputClean);
            if (monto <= 0) {
                DialogStyler.mostrarError(this, "El monto a abonar debe ser mayor a $0.00.");
                return;
            }
            
            if (monto > pendiente) {
                String mensaje = "⚠️ El monto ingresado (<b>$" + String.format("%.2f", monto) + "</b>) es mayor al saldo pendiente (<b>$" + 
                    String.format("%.2f", pendiente) + "</b>).<br><br>" +
                    "¿Desea liquidar y cancelar la deuda con este pago?";
                if (!DialogStyler.confirmar(this, mensaje, "Confirmar liquidación")) return;
                monto = pendiente;
            }
            
            ventaCreditoDAO.registrarAbono(creditoId, monto);
            
            DialogStyler.mostrarExito(this,
                "Abono registrado correctamente.<br><br>" +
                "Monto recibido: <b>$" + String.format("%.2f", monto) + "</b><br>" +
                "Saldo restante: <b>$" + String.format("%.2f", pendiente - monto) + "</b>",
                "Abono Registrado"
            );
            
            cargarCreditosCliente();
            cargarAbonosCliente();
            actualizarResumen();
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
                ((MainFrame) window).refrescarVentas();
            }
            
        } catch (NumberFormatException ex) {
            DialogStyler.mostrarError(this, "Ingrese un monto numérico válido.");
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al registrar abono: " + ex.getMessage());
        }
    }
    
    private void cancelarDeuda() {
        int rowCredito = tablaCreditos.getSelectedRow();
        if (rowCredito < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un crédito para cancelar.", "Aviso");
            return;
        }
        
        try {
            int creditoId = (int) tableModelCreditos.getValueAt(rowCredito, 0);
            String producto = (String) tableModelCreditos.getValueAt(rowCredito, 1);
            double pendiente = Double.parseDouble(((String) tableModelCreditos.getValueAt(rowCredito, 5)).replace("$", "").replace(",", "."));
            
            if (pendiente <= 0) {
                DialogStyler.mostrarAdvertencia(this, "Este crédito ya está totalmente cancelado.", "Aviso");
                return;
            }
            
            String mensaje = "✅ CANCELAR DEUDA COMPLETA<br><br>" +
                "Crédito: <b>#" + creditoId + "</b> (" + producto + ")<br>" +
                "Total saldo a saldar: <b style='color:#DC2626'>$" + String.format("%.2f", pendiente) + "</b><br><br>" +
                "¿Desea registrar el pago total y marcar el crédito como CANCELADO?";
            
            if (!DialogStyler.confirmar(this, mensaje, "Confirmar cancelación de deuda")) return;
            
            ventaCreditoDAO.registrarAbono(creditoId, pendiente);
            
            DialogStyler.mostrarExito(this,
                "Deuda saldada completamente.<br><br>" +
                "Monto total cancelado: <b>$" + String.format("%.2f", pendiente) + "</b>",
                "Deuda Cancelada"
            );
            
            cargarCreditosCliente();
            cargarAbonosCliente();
            actualizarResumen();
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
                ((MainFrame) window).refrescarVentas();
            }
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al cancelar deuda: " + ex.getMessage());
        }
    }
    
    private void eliminarAbono() {
        int rowAbono = tablaAbonos.getSelectedRow();
        if (rowAbono < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un abono de la lista para eliminar.", "Aviso");
            return;
        }
        
        Object idObj = tableModelAbonos.getValueAt(rowAbono, 0);
        if (idObj == null || "-".equals(idObj.toString())) {
            DialogStyler.mostrarAdvertencia(this, "No hay un abono válido seleccionado.", "Aviso");
            return;
        }
        
        try {
            int abonoId = Integer.parseInt(idObj.toString().replace("#", "").trim());
            double monto = Double.parseDouble(((String) tableModelAbonos.getValueAt(rowAbono, 1)).replace("$", "").replace(",", "."));

            int rowCreditoActual = tablaCreditos.getSelectedRow();
            Integer creditoIdActual = null;
            if (rowCreditoActual >= 0) {
                creditoIdActual = (int) tableModelCreditos.getValueAt(rowCreditoActual, 0);
            }

            String mensaje = "⚠️ ¿ELIMINAR ESTE ABONO?<br><br>" +
                "Monto a revertir: <b>$" + String.format("%.2f", monto) + "</b><br><br>" +
                "Al eliminar el abono, la deuda del crédito aumentará automáticamente.<br>" +
                "¿Está seguro de anular este pago?";

            if (!DialogStyler.confirmar(this, mensaje, "Anular abono")) return;

            ventaCreditoDAO.eliminarAbono(abonoId);

            DialogStyler.mostrarExito(this,
                "Abono anulado con éxito.<br>Se restablecieron <b>$" + String.format("%.2f", monto) + "</b> a la deuda del crédito.",
                "Abono Eliminado"
            );

            cargarCreditosCliente();

            if (creditoIdActual != null) {
                for (int i = 0; i < tableModelCreditos.getRowCount(); i++) {
                    if (creditoIdActual.equals(tableModelCreditos.getValueAt(i, 0))) {
                        tablaCreditos.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }

            cargarAbonosCliente();
            actualizarResumen();
            
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame) {
                ((MainFrame) window).refrescarDashboard();
                ((MainFrame) window).refrescarVentas();
            }
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar abono: " + ex.getMessage());
        }
    }
}