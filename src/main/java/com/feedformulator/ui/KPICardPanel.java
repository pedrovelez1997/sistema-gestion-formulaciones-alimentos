package com.feedformulator.ui;

import javax.swing.*;
import java.awt.*;

public class KPICardPanel extends JPanel {

    private String categoria;
    private String valorPrincipal;
    private String subtitulo;
    private String iconoEmoji;
    private Color colorIndicador;
    private JLabel lblValor;
    private JLabel lblSub;

    public KPICardPanel(String categoria, String valorPrincipal, String subtitulo, String iconoEmoji, Color colorIndicador) {
        this.categoria = categoria;
        this.valorPrincipal = valorPrincipal;
        this.subtitulo = subtitulo;
        this.iconoEmoji = iconoEmoji;
        this.colorIndicador = colorIndicador;

        setLayout(new BorderLayout());
        setOpaque(false);

        // Tarjeta con fondo blanco y esquinas redondeadas
        JPanel cardContent = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fondo Blanco
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                
                // Borde suave
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                // Barra de acento lateral izquierda (4px de grosor)
                g2.setColor(colorIndicador);
                g2.fillRoundRect(0, 0, 6, getHeight() - 1, 6, 6);

                // Línea de progreso al pie de la tarjeta
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(18, getHeight() - 12, getWidth() - 36, 6, 3, 3);
                
                g2.setColor(colorIndicador);
                g2.fillRoundRect(18, getHeight() - 12, (int)((getWidth() - 36) * 0.65), 6, 3, 3);

                g2.dispose();
            }
        };
        cardContent.setOpaque(false);
        cardContent.setBorder(BorderFactory.createEmptyBorder(16, 22, 22, 22));

        // Superior: Categoría (Mayúsculas) + Ícono en círculo
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblCat = new JLabel(categoria.toUpperCase());
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblCat.setForeground(new Color(100, 116, 139));

        // Burbuja de ícono derecha
        JLabel lblIcon = new JLabel(iconoEmoji);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setOpaque(true);
        lblIcon.setBackground(new Color(colorIndicador.getRed(), colorIndicador.getGreen(), colorIndicador.getBlue(), 25));
        lblIcon.setPreferredSize(new Dimension(36, 36));
        lblIcon.setBorder(BorderFactory.createLineBorder(new Color(colorIndicador.getRed(), colorIndicador.getGreen(), colorIndicador.getBlue(), 60), 1, true));

        topRow.add(lblCat, BorderLayout.WEST);
        topRow.add(lblIcon, BorderLayout.EAST);

        // Centro: Valor grande y Subtítulo
        JPanel centerGroup = new JPanel();
        centerGroup.setLayout(new BoxLayout(centerGroup, BoxLayout.Y_AXIS));
        centerGroup.setOpaque(false);
        centerGroup.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel lblValor = new JLabel(valorPrincipal);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValor.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));

        this.lblValor = lblValor;
        this.lblSub = lblSub;

        centerGroup.add(lblValor);
        centerGroup.add(Box.createVerticalStrut(4));
        centerGroup.add(lblSub);

        cardContent.add(topRow, BorderLayout.NORTH);
        cardContent.add(centerGroup, BorderLayout.CENTER);

        add(cardContent, BorderLayout.CENTER);
    }

    /**
     * Actualiza el valor y subtítulo mostrados sin reconstruir la tarjeta.
     * Necesario porque el Dashboard recalcula estos datos cada vez que se
     * refresca (al guardar/editar/eliminar algo en el sistema).
     */
    public void actualizar(String nuevoValor, String nuevoSubtitulo) {
        this.valorPrincipal = nuevoValor;
        this.subtitulo = nuevoSubtitulo;
        if (lblValor != null) lblValor.setText(nuevoValor);
        if (lblSub != null) lblSub.setText(nuevoSubtitulo);
    }

    /** Cambia el color de acento de la tarjeta (barra, ícono, barra de progreso) y repinta. */
    public void setColorIndicador(Color nuevoColor) {
        this.colorIndicador = nuevoColor;
        repaint();
    }
}