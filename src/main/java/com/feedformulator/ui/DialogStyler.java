package com.feedformulator.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * DialogStyler — Motor de estilos visuales modernos 2026 para Java Swing.
 * Optimizado para Feed Formulator Pro.
 */
public class DialogStyler {

    // ── CONSTANTES DE TAMAÑOS PARA DIÁLOGOS ──────────────────────────────
    public static final Dimension TAMANO_PEQUENO = new Dimension(420, 320);
    public static final Dimension TAMANO_MEDIANO = new Dimension(500, 410);
    public static final Dimension TAMANO_GRANDE  = new Dimension(580, 480);
    public static final Dimension TAMANO_XGRANDE = new Dimension(660, 560);
    
    // ── PALETA DE COLORES 2026 ─────────────────────────────────────────────
    public static final Color FONDO            = new Color(241, 245, 249); // Slate-100 suave
    public static final Color FONDO_FORM       = new Color(255, 255, 255);
    public static final Color GRADIENTE_START  = new Color(15, 23, 42);    // Slate-900 oscuro elegante
    public static final Color GRADIENTE_END    = new Color(16, 185, 129);  // Esmeralda vibrante
    public static final Color ACENTO           = new Color(16, 185, 129);  // Esmeralda 500
    public static final Color ACENTO_HOVER     = new Color(5, 150, 105);   // Esmeralda 600
    public static final Color SECUNDARIO       = new Color(100, 116, 139); // Slate-500
    public static final Color SECUNDARIO_HOV   = new Color(71, 85, 105);   // Slate-600
    public static final Color PELIGRO          = new Color(225, 29, 72);   // Rose-600
    public static final Color PELIGRO_HOV      = new Color(190, 18, 60);   // Rose-700
    public static final Color NARANJA          = new Color(217, 119, 6);   // Amber-600
    public static final Color BORDE            = new Color(226, 232, 240); // Slate-200
    public static final Color BORDE_FOCUS      = new Color(16, 185, 129);  // Esmeralda focus halo
    public static final Color TEXTO            = new Color(15, 23, 42);    // Slate-900
    public static final Color TEXTO_LABEL      = new Color(51, 65, 85);    // Slate-700
    public static final Color TEXTO_SECUNDARIO = new Color(100, 116, 139); // Slate-500
    
    // Banners y Fondos de Estado
    public static final Color INFO_BG          = new Color(239, 246, 255); // Blue-50
    public static final Color EXITO_BG         = new Color(236, 253, 245); // Emerald-50
    public static final Color ADVERTENCIA_BG   = new Color(255, 251, 235); // Amber-50
    public static final Color ERROR_BG         = new Color(254, 242, 242); // Rose-50

    // ── FUENTES DEL SISTEMA WINDOWS ───────────────────────────────────────
    public static final Font FUENTE_TITULO    = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FUENTE_SUBTIT    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FUENTE_LABEL     = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_CAMPO     = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FUENTE_BOTON     = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_INFO      = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FUENTE_BADGE     = new Font("Segoe UI", Font.BOLD, 10);

    // ────────────────────────────────────────────────────────────────────────
    //  MÉTODO PRINCIPAL
    // ────────────────────────────────────────────────────────────────────────

    public static void aplicar(JDialog dialog, String titulo, String icono) {
        dialog.setUndecorated(false);
        dialog.setBackground(FONDO);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", Boolean.TRUE);
        
        JPanel header = crearHeader(titulo, icono);
        dialog.add(header, BorderLayout.NORTH);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HEADER OSCURO DE MARCA CON DEGRADADO Y SOMBRA
    // ────────────────────────────────────────────────────────────────────────

    public static JPanel crearHeader(String titulo, String icono) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fondo con degradado Slate-900 a Esmeralda
                GradientPaint gp = new GradientPaint(0, 0, GRADIENTE_START, getWidth(), 0, GRADIENTE_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Línea inferior de brillo sutil
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        lblIcono.setForeground(Color.WHITE);
        lblIcono.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(FUENTE_TITULO);
        lblTitulo.setForeground(Color.WHITE);

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        izq.setOpaque(false);
        izq.add(lblIcono);
        izq.add(lblTitulo);
        header.add(izq, BorderLayout.WEST);

        return header;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  COMPONENTES DE FORMULARIO
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Panel de formulario "Scrollable": cuando se coloca dentro de un
     * JScrollPane, su ancho siempre se ajusta al del viewport (nunca crece
     * hacia los lados), evitando la barra de scroll horizontal.
     */
    private static class PanelFormularioScrollable extends JPanel implements Scrollable {
        PanelFormularioScrollable(LayoutManager lm) { super(lm); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static JPanel crearPanelFormulario() {
        JPanel panel = new PanelFormularioScrollable(new GridBagLayout());
        panel.setBackground(FONDO_FORM);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        return panel;
    }

    /**
     * Envuelve un formulario en un JScrollPane configurado para que NUNCA
     * aparezca la barra horizontal: solo permite scroll vertical cuando el
     * contenido no cabe en alto. Úsese junto con crearPanelFormulario().
     */
    public static JScrollPane crearScrollFormulario(JComponent form) {
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(FONDO_FORM);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static GridBagConstraints crearGBC() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    /**
     * Versión "blindada" de GBC: fija gridx/gridy y controla el peso
     * horizontal por columna (columna 0 = etiqueta, no se expande; columna
     * 1+ = campo, se expande de forma controlada) para que ninguna columna
     * intente estirarse hasta el borde de la ventana.
     */
    public static GridBagConstraints crearGBC(int gridx, int gridy) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = (gridx == 0) ? 0.0 : 1.0;
        return gbc;
    }

    public static JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(FUENTE_LABEL);
        lbl.setForeground(TEXTO_LABEL);
        return lbl;
    }

    public static JTextField crearCampo(int columnas) {
        JTextField campo = new JTextField(columnas) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = isFocusOwner() ? BORDE_FOCUS : BORDE;
                g2.setColor(color);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2.0f : 1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        campo.setFont(FUENTE_CAMPO);
        campo.setForeground(TEXTO);
        campo.setBackground(new Color(248, 250, 252)); // Slate-50
        campo.setOpaque(false);
        campo.setMargin(new Insets(6, 12, 6, 12));
        campo.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // Ancho proporcional a las columnas pedidas, con un piso mínimo
        // (60px) que GridBagLayout nunca puede aplastar por debajo de él,
        // así los campos numéricos angostos (ej. filas de Pedidos) siguen
        // siendo legibles aunque haya poco espacio horizontal disponible.
        int anchoPreferido = Math.max(campo.getPreferredSize().width, 60);
        campo.setPreferredSize(new Dimension(anchoPreferido, 34));
        campo.setMinimumSize(new Dimension(60, 34));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        campo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { 
                campo.setBackground(Color.WHITE); 
                campo.repaint(); 
            }
            public void focusLost(FocusEvent e) { 
                campo.setBackground(new Color(248, 250, 252)); 
                campo.repaint(); 
            }
        });
        return campo;
    }


    public static JTextField crearCampoNumerico(int columnas) {
        JTextField campo = crearCampo(columnas);
        campo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != '.' && c != KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
            }
        });
        return campo;
    }

    public static <T> JComboBox<T> crearCombo(T[] opciones) {
        JComboBox<T> combo = new JComboBox<>(opciones);
        combo.setFont(FUENTE_CAMPO);
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXTO);
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 36));
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDE, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        return combo;
    }

    public static JButton crearBotonPrimario(String texto) {
        JButton btn = crearBotonBase(texto);
        btn.setBackground(ACENTO);
        btn.setForeground(Color.WHITE);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACENTO_HOVER); btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.setBackground(ACENTO); btn.repaint(); }
        });
        return btn;
    }

    public static JButton crearBotonSecundario(String texto) {
        JButton btn = crearBotonBase(texto);
        btn.setBackground(SECUNDARIO);
        btn.setForeground(Color.WHITE);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(SECUNDARIO_HOV); btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.setBackground(SECUNDARIO); btn.repaint(); }
        });
        return btn;
    }

    public static JButton crearBotonPeligro(String texto) {
        JButton btn = crearBotonBase(texto);
        btn.setBackground(PELIGRO);
        btn.setForeground(Color.WHITE);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(PELIGRO_HOV); btn.repaint(); }
            public void mouseExited(MouseEvent e) { btn.setBackground(PELIGRO); btn.repaint(); }
        });
        return btn;
    }

    private static JButton crearBotonBase(String texto) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FUENTE_BOTON);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 38));
        return btn;
    }

    public static JPanel crearPanelBotones(JButton... botones) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDE));
        for (JButton b : botones) {
            panel.add(b);
        }
        return panel;
    }

    public static JPanel crearBannerInfo(String tipo, String html) {
        Color bg, bd;
        String icono;
        switch (tipo) {
            case "error":       bg = ERROR_BG;       bd = new Color(252, 165, 165); icono = "❌"; break;
            case "advertencia": bg = ADVERTENCIA_BG; bd = new Color(252, 211, 77);   icono = "⚠️"; break;
            case "exito":       bg = EXITO_BG;       bd = new Color(110, 231, 183);  icono = "✅"; break;
            default:            bg = INFO_BG;        bd = new Color(147, 197, 253);  icono = "ℹ️";
        }

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(bd);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel lbl = new JLabel("<html><body style='width:380px;font-size:11px;font-family:Segoe UI'>" + icono + "&nbsp;&nbsp;" + html + "</body></html>");
        lbl.setForeground(TEXTO);
        panel.add(lbl, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel crearSeparadorSeccion(String textoSeccion) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));

        JLabel lbl = new JLabel("▸  " + textoSeccion.toUpperCase());
        lbl.setFont(FUENTE_BADGE);
        lbl.setForeground(ACENTO);

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDE);

        panel.add(lbl, BorderLayout.WEST);
        panel.add(sep, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel crearTarjetaInfo(String titulo, String contenido, Color color) {
        JPanel panel = new JPanel(new BorderLayout(6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(FUENTE_LABEL);
        lblTitulo.setForeground(color);
        
        JLabel lblContenido = new JLabel("<html>" + contenido + "</html>");
        lblContenido.setFont(FUENTE_CAMPO);
        lblContenido.setForeground(TEXTO);
        
        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(lblContenido, BorderLayout.CENTER);
        return panel;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  MÉTODOS DE DIÁLOGOS DE NOTIFICACIÓN
    // ────────────────────────────────────────────────────────────────────────

    public static boolean confirmar(Component padre, String mensaje, String titulo) {
        int result = JOptionPane.showConfirmDialog(
            padre,
            "<html><body style='font-family:Segoe UI;font-size:12px;padding:8px;max-width:380px;'>" + mensaje + "</body></html>",
            titulo,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    public static void mostrarExito(Component padre, String mensaje, String titulo) {
        JOptionPane.showMessageDialog(
            padre,
            "<html><body style='font-family:Segoe UI;font-size:12px;padding:6px;max-width:320px;'>"
                + "✅&nbsp;&nbsp;" + mensaje + "</body></html>",
            titulo,
            JOptionPane.PLAIN_MESSAGE
        );
    }

    public static void mostrarError(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(
            padre,
            "<html><body style='font-family:Segoe UI;font-size:12px;color:#E11D48;padding:6px;max-width:320px;'>"
                + "❌&nbsp;&nbsp;" + mensaje + "</body></html>",
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void mostrarAdvertencia(Component padre, String mensaje, String titulo) {
        JOptionPane.showMessageDialog(
            padre,
            "<html><body style='font-family:Segoe UI;font-size:12px;color:#D97706;padding:6px;max-width:320px;'>"
                + "⚠️&nbsp;&nbsp;" + mensaje + "</body></html>",
            titulo,
            JOptionPane.WARNING_MESSAGE
        );
    }

    public static void mostrarInfo(Component padre, String mensaje, String titulo) {
        JOptionPane.showMessageDialog(
            padre,
            "<html><body style='font-family:Segoe UI;font-size:12px;padding:6px;max-width:320px;'>"
                + "ℹ️&nbsp;&nbsp;" + mensaje + "</body></html>",
            titulo,
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HEADER DE PÁGINA REUTILIZABLE
    // ────────────────────────────────────────────────────────────────────────

    public static JPanel crearHeaderPagina(String icono, String titulo, String subtitulo) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENTE_START, getWidth(), 0, GRADIENTE_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        izq.setOpaque(false);

        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
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
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);
        tituloCol.add(lblTitulo);

        if (subtitulo != null && !subtitulo.isEmpty()) {
            JLabel lblSubtitulo = new JLabel(subtitulo);
            lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblSubtitulo.setForeground(new Color(209, 250, 229)); // Mint light text
            tituloCol.add(lblSubtitulo);
        }

        izq.add(tituloCol);
        header.add(izq, BorderLayout.WEST);

        return header;
    }

    public static JPanel crearHeaderPagina(String icono, String titulo) {
        return crearHeaderPagina(icono, titulo, null);
    }
}